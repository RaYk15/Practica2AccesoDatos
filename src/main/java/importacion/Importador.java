package importacion;

import conexion.Conexiones;
import conexion.ErrorBaseDatos;
import dao.CampeonDao;
import dao.PartidaDao;
import dao.ResumenDao;
import lectura.ErrorLectura;
import lectura.FilaCsv;
import lectura.LectorCsv;
import lectura.LectorJson;
import modelo.Campeon;
import modelo.Partida;
import org.json.JSONObject;
import validacion.Motivo;
import validacion.Rechazo;
import validacion.ValidadorCampeon;
import validacion.ValidadorPartida;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Importa campeones.csv y partidas.json a la base de datos sin duplicar nada.
 *
 * Pasos:
 *   1. Leo los dos archivos. Si alguno falla, paro sin tocar la base.
 *   2. Guardo los campeones (se preparan antes, fuera de la transacción).
 *   3. Valido y clasifico las partidas, todavía sin guardar nada.
 *   4. Guardo las partidas nuevas y el resumen en UNA transacción: o todo o nada.
 *
 * Cómo clasifico cada registro (igual que en la P1, pero mirando en la base):
 *   - no existe esa clave              -> NUEVO, se guarda
 *   - existe y es igual                -> DUPLICADO, no se hace nada
 *   - existe pero con otros datos      -> CONFLICTO, me quedo con el que ya estaba
 *   - no cumple las reglas             -> RECHAZADO, no se guarda
 * Nunca sobrescribo un registro que ya existe.
 */
public class Importador {

    public static final String ARCHIVO_CAMPEONES = "campeones.csv";
    public static final String ARCHIVO_PARTIDAS = "partidas.json";

    /** Lo que se enseña al terminar: los dos contadores y la lista de incidencias. */
    public record Resultado(Contador campeones, Contador partidas, List<Incidencia> incidencias) {
    }

    /** Error falso que lanzo a propósito para probar el rollback. */
    public static class FalloDePrueba extends RuntimeException {
        private static final long serialVersionUID = 1L;

        FalloDePrueba(String mensaje) {
            super(mensaje);
        }
    }

    private final Conexiones conexiones;
    private final CampeonDao campeonDao;
    private final PartidaDao partidaDao;
    private final ResumenDao resumenDao;
    private final ValidadorCampeon validadorCampeon = new ValidadorCampeon();
    private final ValidadorPartida validadorPartida = new ValidadorPartida();

    public Importador(Conexiones conexiones) {
        this.conexiones = conexiones;
        this.campeonDao = new CampeonDao(conexiones);
        this.partidaDao = new PartidaDao(conexiones);
        this.resumenDao = new ResumenDao(conexiones);
    }

    /**
     * @param carpeta              carpeta donde están campeones.csv y partidas.json
     * @param fallarAntesDelCommit si es true, lanzo un error después de guardar la segunda
     *                             partida nueva y antes del commit (prueba de rollback)
     */
    public Resultado importar(Path carpeta, boolean fallarAntesDelCommit) throws ErrorLectura, ErrorBaseDatos {
        // 1. Leer los archivos antes de tocar la base
        List<FilaCsv> filas = new LectorCsv().leer(carpeta.resolve(ARCHIVO_CAMPEONES));
        List<Object> registros = new LectorJson().leer(carpeta.resolve(ARCHIVO_PARTIDAS));

        Contador contCampeones = new Contador();
        Contador contPartidas = new Contador();
        List<Incidencia> incidencias = new ArrayList<>();

        // 2. Campeones
        importarCampeones(filas, contCampeones, incidencias);

        // 3. Partidas: validar y clasificar (sin guardar)
        List<Partida> nuevas = clasificarPartidas(registros, contPartidas, incidencias);

        // 4. Transacción: partidas nuevas + resumen
        guardarLote(nuevas, contPartidas, fallarAntesDelCommit);

        return new Resultado(contCampeones, contPartidas, incidencias);
    }

    // ---------- Paso 2: campeones ----------

    private void importarCampeones(List<FilaCsv> filas, Contador cont, List<Incidencia> incidencias)
            throws ErrorBaseDatos {
        for (FilaCsv fila : filas) {
            Campeon c;
            try {
                c = validadorCampeon.validar(fila);
            } catch (Rechazo r) {
                cont.contar(Contador.Clasificacion.RECHAZADO);
                incidencias.add(new Incidencia(ARCHIVO_CAMPEONES, fila.numero(), claveCampeon(fila), r.motivo(), r.getMessage()));
                continue;
            }
            // miro si ya existe en la base y lo comparo
            Optional<Campeon> existente = campeonDao.buscarPorClave(c.id());
            if (existente.isEmpty()) {
                try {
                    campeonDao.crear(c);
                } catch (Rechazo r) {   // no puede pasar, ya está validado
                    throw new IllegalStateException(r);
                }
                cont.contar(Contador.Clasificacion.NUEVO);
            } else if (existente.get().equals(c)) {
                cont.contar(Contador.Clasificacion.DUPLICADO);
            } else {
                cont.contar(Contador.Clasificacion.CONFLICTO);
                incidencias.add(conflicto(ARCHIVO_CAMPEONES, fila.numero(), c.id()));
            }
        }
    }

    // ---------- Paso 3: partidas ----------

    /** Devuelve solo las partidas nuevas, que son las que hay que guardar. */
    private List<Partida> clasificarPartidas(List<Object> registros, Contador cont, List<Incidencia> incidencias)
            throws ErrorBaseDatos {
        // Los campeones que hay en la base (ya con los que acabo de importar),
        // para comprobar que cada partida usa campeones que existen
        Set<String> campeones = new HashSet<>();
        for (Campeon c : campeonDao.listar(null, CampeonDao.Orden.ID)) {
            campeones.add(c.id());
        }

        // Partidas nuevas de este mismo archivo, por si una se repite dentro del lote
        Map<String, Partida> delLote = new LinkedHashMap<>();

        try (Connection lectura = conexiones.abrir()) {
            for (int i = 0; i < registros.size(); i++) {
                int numero = i + 1;
                Partida p;
                try {
                    p = validadorPartida.validar(registros.get(i), campeones);
                } catch (Rechazo r) {
                    cont.contar(Contador.Clasificacion.RECHAZADO);
                    incidencias.add(new Incidencia(ARCHIVO_PARTIDAS, numero, clavePartida(registros.get(i)),
                            r.motivo(), r.getMessage()));
                    continue;
                }
                // primero miro en las de este lote y luego en la base
                Partida anterior = delLote.get(p.clave());
                if (anterior == null) {
                    anterior = partidaDao.buscarPorClave(lectura, p.servidor(), p.idPartida()).orElse(null);
                }
                if (anterior == null) {
                    delLote.put(p.clave(), p);
                    cont.contar(Contador.Clasificacion.NUEVO);
                } else if (anterior.equals(p)) {
                    cont.contar(Contador.Clasificacion.DUPLICADO);
                } else {
                    cont.contar(Contador.Clasificacion.CONFLICTO);
                    incidencias.add(conflicto(ARCHIVO_PARTIDAS, numero, p.clave()));
                }
            }
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "la clasificación de las partidas");
        }
        return new ArrayList<>(delLote.values());
    }

    // ---------- Paso 4: la transacción ----------

    /**
     * Guarda las partidas nuevas y el resumen en una sola transacción, con una sola conexión.
     * Si algo falla antes del commit hago rollback: no se queda ninguna partida del lote
     * ni el resumen. Lo que ya estaba guardado de antes no se toca.
     */
    private void guardarLote(List<Partida> nuevas, Contador cont, boolean fallarAntesDelCommit)
            throws ErrorBaseDatos {
        try (Connection con = conexiones.abrir()) {          // se cierra siempre al salir
            con.setAutoCommit(false);                        // empieza la transacción
            try {
                for (int i = 0; i < nuevas.size(); i++) {
                    partidaDao.insertar(con, nuevas.get(i));
                    if (fallarAntesDelCommit && i == 1) {
                        throw new FalloDePrueba("fallo provocado tras insertar la 2.ª partida nueva");
                    }
                }
                if (fallarAntesDelCommit) {                  // por si el lote tiene menos de 2 nuevas
                    throw new FalloDePrueba("fallo provocado antes de guardar el resumen");
                }
                resumenDao.insertar(con, cont);
                con.commit();                                // se confirma todo junto
            } catch (SQLException | RuntimeException e) {
                try {
                    con.rollback();                          // se deshace todo lo de esta transacción
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);          // así no pierdo el error original
                }
                throw new ErrorBaseDatos("No se ha guardado el lote: se ha hecho ROLLBACK, así que no se ha "
                        + "guardado ninguna partida nueva ni su resumen (causa: " + e.getMessage() + ")", e);
            }
        } catch (SQLException e) {                           // fallo al preparar o cerrar la conexión
            throw ErrorBaseDatos.desde(e, "el lote de partidas");
        }
    }

    // ---------- Métodos de ayuda ----------

    private static Incidencia conflicto(String archivo, int numero, String clave) {
        return new Incidencia(archivo, numero, clave, Motivo.CONFLICTO,
                "ya existe con otro contenido; se conserva el registro anterior");
    }

    /** Saco la clave de un campeón rechazado para el informe, si se puede. */
    private static String claveCampeon(FilaCsv fila) {
        String id = fila.valores().get("idCampeon");
        return id == null || id.isBlank() ? null : id.strip();
    }

    /** Lo mismo para una partida rechazada: "EUW/P001" o null. */
    private static String clavePartida(Object registro) {
        if (registro instanceof JSONObject o
                && o.opt("servidor") instanceof String servidor && !servidor.isBlank()
                && o.opt("idPartida") instanceof String id && !id.isBlank()) {
            return Partida.clave(servidor.strip(), id.strip());
        }
        return null;
    }
}

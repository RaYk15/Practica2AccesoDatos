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
 * Importa campeones.csv y partidas.json en la base de datos sin duplicar.
 *
 *   1. Leer las dos entradas. Si una falla, se termina SIN tocar la base.
 *   2. Campeones (A): se preparan antes, fuera de la transacción del lote.
 *   3. Partidas (B): se validan y se clasifican, todavía sin escribir nada.
 *   4. Transacción: partidas nuevas + resumen, en UNA conexión. Commit o rollback.
 *
 * Clasificación (igual que en la P1, pero comparando con lo que hay en la base):
 *   clave desconocida -> NUEVO · mismo contenido -> DUPLICADO · otro contenido -> CONFLICTO
 *   (se conserva el anterior) · inválido -> RECHAZADO (no reserva su clave).
 * No se usa un "upsert": nunca se sobrescribe un registro existente.
 */
public class Importador {

    public static final String ARCHIVO_CAMPEONES = "campeones.csv";
    public static final String ARCHIVO_PARTIDAS = "partidas.json";

    /** Lo que se muestra al terminar. */
    public record Resultado(Contador campeones, Contador partidas, List<Incidencia> incidencias) {
    }

    /** Excepción de la prueba de rollback: simula un fallo antes del commit. */
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
     * @param fallarAntesDelCommit prueba de rollback: lanza un error después de insertar
     *                             la segunda partida nueva y antes del commit
     */
    public Resultado importar(Path carpeta, boolean fallarAntesDelCommit) throws ErrorLectura, ErrorBaseDatos {
        // 1. Interpretar las entradas antes de tocar la base
        List<FilaCsv> filas = new LectorCsv().leer(carpeta.resolve(ARCHIVO_CAMPEONES));
        List<Object> registros = new LectorJson().leer(carpeta.resolve(ARCHIVO_PARTIDAS));

        Contador contCampeones = new Contador();
        Contador contPartidas = new Contador();
        List<Incidencia> incidencias = new ArrayList<>();

        // 2. Campeones (A)
        importarCampeones(filas, contCampeones, incidencias);

        // 3. Partidas (B): validar y clasificar sin escribir
        List<Partida> nuevas = clasificarPartidas(registros, contPartidas, incidencias);

        // 4. Transacción del lote
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
            Optional<Campeon> existente = campeonDao.buscarPorClave(c.id());
            if (existente.isEmpty()) {
                try {
                    campeonDao.crear(c);
                } catch (Rechazo r) {   // no ocurre: ya está validado
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

    private List<Partida> clasificarPartidas(List<Object> registros, Contador cont, List<Incidencia> incidencias)
            throws ErrorBaseDatos {
        // Referencias válidas: los campeones que hay en la base (incluidos los recién importados)
        Set<String> campeones = new HashSet<>();
        for (Campeon c : campeonDao.listar(null, CampeonDao.Orden.ID)) {
            campeones.add(c.id());
        }

        Map<String, Partida> delLote = new LinkedHashMap<>();   // nuevas aceptadas en ESTE lote
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
                // Se compara con lo aceptado en el lote y con lo guardado en la base
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
     * Partidas nuevas + resumen en una sola transacción y una sola conexión.
     * Si algo falla antes del commit, rollback: no queda ninguna partida del lote ni su resumen.
     * Lo confirmado en importaciones anteriores no se toca.
     */
    private void guardarLote(List<Partida> nuevas, Contador cont, boolean fallarAntesDelCommit)
            throws ErrorBaseDatos {
        try (Connection con = conexiones.abrir()) {          // se cierra SIEMPRE al salir
            con.setAutoCommit(false);                        // INICIAR transacción
            try {
                for (int i = 0; i < nuevas.size(); i++) {
                    partidaDao.insertar(con, nuevas.get(i));
                    if (fallarAntesDelCommit && i == 1) {
                        throw new FalloDePrueba("fallo provocado tras insertar la 2.ª partida nueva");
                    }
                }
                if (fallarAntesDelCommit) {                  // por si el lote trae menos de 2 nuevas
                    throw new FalloDePrueba("fallo provocado antes de guardar el resumen");
                }
                resumenDao.insertar(con, cont);
                con.commit();                                // CONFIRMAR
            } catch (SQLException | RuntimeException e) {
                try {
                    con.rollback();                          // REVERTIR
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);          // se conserva la causa original
                }
                throw new ErrorBaseDatos("No se ha guardado el lote: se ha hecho ROLLBACK, así que no se ha "
                        + "guardado ninguna partida nueva ni su resumen (causa: " + e.getMessage() + ")", e);
            }
        } catch (SQLException e) {                           // al preparar o cerrar la conexión
            throw ErrorBaseDatos.desde(e, "el lote de partidas");
        }
    }

    // ---------- Utilidades ----------

    private static Incidencia conflicto(String archivo, int numero, String clave) {
        return new Incidencia(archivo, numero, clave, Motivo.CONFLICTO,
                "ya existe con otro contenido; se conserva el registro anterior");
    }

    private static String claveCampeon(FilaCsv fila) {
        String id = fila.valores().get("idCampeon");
        return id == null || id.isBlank() ? null : id.strip();
    }

    private static String clavePartida(Object registro) {
        if (registro instanceof JSONObject o
                && o.opt("servidor") instanceof String servidor && !servidor.isBlank()
                && o.opt("idPartida") instanceof String id && !id.isBlank()) {
            return Partida.clave(servidor.strip(), id.strip());
        }
        return null;
    }
}

package dao;

import conexion.Conexiones;
import conexion.ErrorBaseDatos;
import modelo.Campeon;
import validacion.Rechazo;
import validacion.ValidadorCampeon;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Todo el SQL de la tabla campeon está aquí (DAO = Data Access Object).
 * El resto del programa trabaja con objetos Campeon y no sabe nada de SQL.
 *
 * - Los valores van siempre con ? en un PreparedStatement. Así "Kai'Sa" se guarda
 *   como un dato normal y nadie puede meter SQL por un campo de texto.
 * - Cada método abre su conexión y la cierra al acabar con try-with-resources,
 *   también si hay un error.
 */
public class CampeonDao {

    /**
     * Los órdenes que se pueden elegir al listar.
     * Un nombre de columna NO se puede pasar con ?, solo los valores. Por eso el usuario
     * elige una opción y yo la traduzco a un ORDER BY escrito aquí a mano.
     * Al final siempre ordeno por id para que el orden sea siempre el mismo.
     */
    public enum Orden {
        NOMBRE("nombre, id_campeon"),
        DIFICULTAD("dificultad, nombre, id_campeon"),
        ID("id_campeon");

        private final String sql;

        Orden(String sql) {
            this.sql = sql;
        }

        /** Pasa el texto a la opción ("nombre" -> NOMBRE). Si no es ninguna, lanza error. */
        public static Orden desde(String texto) {
            for (Orden o : values()) {
                if (o.name().equalsIgnoreCase(texto)) {
                    return o;
                }
            }
            throw new IllegalArgumentException("Orden '" + texto + "' no válido: usa nombre, dificultad o id");
        }
    }

    private static final String COLUMNAS = "id_campeon, nombre, rol, region, dificultad";

    private final Conexiones conexiones;
    private final ValidadorCampeon validador = new ValidadorCampeon();

    public CampeonDao(Conexiones conexiones) {
        this.conexiones = conexiones;
    }

    // ---------- Crear ----------

    /** Valida el campeón y lo guarda. Devuelve el campeón tal cual se ha guardado. */
    public Campeon crear(Campeon nuevo) throws Rechazo, ErrorBaseDatos {
        Campeon c = validador.validar(nuevo);
        String sql = "INSERT INTO campeon (" + COLUMNAS + ") VALUES (?, ?, ?, ?, ?)";
        try (Connection con = conexiones.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.id());
            ps.setString(2, c.nombre());
            ps.setString(3, c.rol());
            ps.setString(4, c.region());
            ps.setInt(5, c.dificultad());
            ps.executeUpdate();
            return c;
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "el campeón " + c.id());
        }
    }

    // ---------- Buscar por clave ----------

    /** Si no lo encuentra devuelve Optional vacío, no es un error. */
    public Optional<Campeon> buscarPorClave(String id) throws ErrorBaseDatos {
        String sql = "SELECT " + COLUMNAS + " FROM campeon WHERE id_campeon = ?";
        try (Connection con = conexiones.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(desdeFila(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "la búsqueda del campeón " + id);
        }
    }

    // ---------- Listar con filtro y orden ----------

    /**
     * @param rol   rol por el que filtrar, o null para sacarlos todos
     * @param orden cómo ordenar
     * @return la lista, vacía si no hay ninguno
     */
    public List<Campeon> listar(String rol, Orden orden) throws ErrorBaseDatos {
        String sql = "SELECT " + COLUMNAS + " FROM campeon"
                + (rol == null ? "" : " WHERE rol = ?")
                + " ORDER BY " + orden.sql;
        try (Connection con = conexiones.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (rol != null) {
                ps.setString(1, rol);
            }
            List<Campeon> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(desdeFila(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "el listado de campeones");
        }
    }

    // ---------- Modificar ----------

    /**
     * Valida los datos nuevos y los guarda. El id no se cambia nunca.
     * @return filas cambiadas: 1 si se ha modificado, 0 si no existe ese campeón
     */
    public int modificar(Campeon cambios) throws Rechazo, ErrorBaseDatos {
        Campeon c = validador.validar(cambios);
        String sql = "UPDATE campeon SET nombre = ?, rol = ?, region = ?, dificultad = ? WHERE id_campeon = ?";
        try (Connection con = conexiones.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.nombre());
            ps.setString(2, c.rol());
            ps.setString(3, c.region());
            ps.setInt(4, c.dificultad());
            ps.setString(5, c.id());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "el campeón " + c.id());
        }
    }

    // ---------- Borrar ----------

    /**
     * Un campeón que sale en alguna partida no se puede borrar, la clave foránea no deja.
     * Así no se pierde el historial. Para probar el borrado uso un campeón sin partidas.
     * @return filas borradas: 1 si se ha borrado, 0 si no existía
     */
    public int borrar(String id) throws ErrorBaseDatos {
        try (Connection con = conexiones.abrir();
             PreparedStatement ps = con.prepareStatement("DELETE FROM campeon WHERE id_campeon = ?")) {
            ps.setString(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "el campeón " + id + " (aparece en partidas)");
        }
    }

    // ---------- Procedimiento almacenado ----------

    /**
     * Llama al procedimiento renombrar_campeon (está en sql/procedimiento.sql).
     * Lo que comprueba (nombre no vacío y que el campeón exista) lo hace la base de datos,
     * no Java. Como la conexión está en autocommit, el cambio se guarda al acabar.
     */
    public void renombrar(String id, String nombreNuevo) throws ErrorBaseDatos {
        try (Connection con = conexiones.abrir();
             CallableStatement cs = con.prepareCall("CALL renombrar_campeon(?, ?)")) {
            cs.setString(1, id);
            cs.setString(2, nombreNuevo);
            cs.execute();
        } catch (SQLException e) {
            throw errorProcedimiento(e, id);
        }
    }

    /**
     * Prueba de que el procedimiento se puede deshacer: lo llamo dentro de una transacción,
     * leo el nombre en esa misma transacción (ya sale cambiado) y hago rollback.
     * Como el procedimiento no hace commit por su cuenta, el cambio desaparece.
     * @return el nombre que se veía dentro de la transacción, antes del rollback
     */
    public String renombrarYRevertir(String id, String nombreNuevo) throws ErrorBaseDatos {
        try (Connection con = conexiones.abrir()) {
            con.setAutoCommit(false);
            try {
                try (CallableStatement cs = con.prepareCall("CALL renombrar_campeon(?, ?)")) {
                    cs.setString(1, id);
                    cs.setString(2, nombreNuevo);
                    cs.execute();
                }
                String dentro;
                try (PreparedStatement ps = con.prepareStatement("SELECT nombre FROM campeon WHERE id_campeon = ?")) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        dentro = rs.getString(1);
                    }
                }
                return dentro;
            } finally {
                con.rollback();   // siempre, porque es una prueba
            }
        } catch (SQLException e) {
            throw errorProcedimiento(e, id);
        }
    }

    /** Los errores que lanza el procedimiento (RAISE EXCEPTION) llegan con su propio SQLState. */
    private static ErrorBaseDatos errorProcedimiento(SQLException e, String id) {
        return switch (e.getSQLState() == null ? "" : e.getSQLState()) {
            case "P0002" -> new ErrorBaseDatos("No existe el campeón " + id + ": el procedimiento no ha cambiado nada.", e);
            case "22023" -> new ErrorBaseDatos("El procedimiento rechaza el nombre: no puede estar vacío.", e);
            case "42883" -> new ErrorBaseDatos("El procedimiento renombrar_campeon no existe en la base: "
                    + "aplica sql/procedimiento.sql.", e);
            default -> ErrorBaseDatos.desde(e, "la llamada a renombrar_campeon");
        };
    }

    // ---------- Pasar una fila a objeto ----------

    /**
     * Convierte la fila actual del ResultSet en un Campeon.
     * Todas las columnas son NOT NULL en el esquema, así que nunca llega un null.
     * Ojo: si una columna de número pudiera ser NULL, getInt() devolvería 0 sin avisar,
     * y habría que usar rs.getObject("columna", Integer.class), que sí devuelve null.
     */
    static Campeon desdeFila(ResultSet rs) throws SQLException {
        return new Campeon(
                rs.getString("id_campeon"),
                rs.getString("nombre"),
                rs.getString("rol"),
                rs.getString("region"),
                rs.getInt("dificultad"));
    }
}

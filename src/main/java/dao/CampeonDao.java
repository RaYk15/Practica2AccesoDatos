package dao;

import conexion.Conexiones;
import conexion.ErrorBaseDatos;
import modelo.Campeon;
import validacion.Rechazo;
import validacion.ValidadorCampeon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla campeon. Es la única clase que contiene SQL de campeones.
 *
 * - Los VALORES siempre van como parámetros (?) de un PreparedStatement:
 *   "Kai'Sa" se guarda como dato y nunca puede romper la sentencia.
 * - Cada método abre su conexión y la cierra al terminar (try-with-resources),
 *   también si hay un error.
 */
public class CampeonDao {

    /**
     * Órdenes permitidos para el listado. El usuario elige una opción y se traduce a
     * columnas fijas escritas aquí: un nombre de columna NO puede ir como parámetro (?).
     * El último criterio es siempre la clave, para que el orden sea estable.
     */
    public enum Orden {
        NOMBRE("nombre, id_campeon"),
        DIFICULTAD("dificultad, nombre, id_campeon"),
        ID("id_campeon");

        private final String sql;

        Orden(String sql) {
            this.sql = sql;
        }

        /** "nombre" -> NOMBRE. Cualquier otra cosa se rechaza. */
        public static Orden desde(String texto) {
            for (Orden o : values()) {
                if (o.name().equalsIgnoreCase(texto)) {
                    return o;
                }
            }
            throw new IllegalArgumentException("Orden «" + texto + "» no válido: usa nombre, dificultad o id");
        }
    }

    private static final String COLUMNAS = "id_campeon, nombre, rol, region, dificultad";

    private final Conexiones conexiones;
    private final ValidadorCampeon validador = new ValidadorCampeon();

    public CampeonDao(Conexiones conexiones) {
        this.conexiones = conexiones;
    }

    // ---------- Crear ----------

    /** Valida y guarda un campeón nuevo. Devuelve el campeón normalizado que se ha guardado. */
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

    /** Sin resultados devuelve Optional.empty(): no es un error. */
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
     * @param rol   filtro por rol, o null para todos
     * @param orden criterio de orden (lista cerrada)
     * @return lista vacía si no hay coincidencias
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
     * Valida el nuevo contenido y lo guarda. La identidad (id_campeon) no cambia nunca.
     * @return filas afectadas: 1 si se ha modificado, 0 si no existe ese campeón
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
     * Un campeón que aparece en partidas NO se puede borrar (la clave foránea lo impide):
     * así no se pierde el historial. Para la prueba de borrado se usa un campeón sin partidas.
     * @return filas afectadas: 1 si se ha borrado, 0 si no existía
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

    // ---------- Conversión fila -> modelo ----------

    /**
     * Convierte la fila actual del ResultSet en un Campeon.
     * Todas las columnas son NOT NULL en el esquema, así que nunca llega un null.
     * Ojo: si una columna numérica admitiera NULL, getInt() devolvería 0 sin avisar;
     * habría que usar rs.getObject("columna", Integer.class), que sí devuelve null.
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

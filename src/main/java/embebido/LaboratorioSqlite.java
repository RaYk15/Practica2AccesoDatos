package embebido;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Laboratorio con SQLite, una base de datos embebida: no hay servidor ni Docker,
 * la base es el archivo lab/lol.db y el motor va dentro del propio driver JDBC.
 * Tampoco hay usuario ni contraseña, la URL es solo la ruta del archivo.
 *
 *   laboratorio crear                                              crea el archivo y la tabla
 *   laboratorio insertar <id> <nombre> <rol> <region> <dificultad>
 *   laboratorio listar
 *   laboratorio modificar <id> <nombre> <rol> <region> <dificultad>
 *   laboratorio borrar <id>
 *
 * Cada comando abre el archivo, hace su operación y lo cierra. Para comprobar que
 * los datos se guardan basta con insertar y luego hacer "listar" en otra ejecución.
 */
public class LaboratorioSqlite {

    private static final Path ARCHIVO = Path.of("lab", "lol.db");
    private static final Path ESQUEMA = Path.of("sql", "esquema-sqlite.sql");
    private static final String URL = "jdbc:sqlite:" + ARCHIVO;

    /** Devuelve 0 si todo va bien y 1 si hay algún error. */
    public int ejecutar(List<String> args) {
        String accion = args.isEmpty() ? "" : args.get(0);
        // Si el archivo no existe, SQLite lo crearía vacío sin avisar y luego fallaría con
        // "no such table", así que antes de cualquier otra cosa hay que hacer "laboratorio crear"
        if (List.of("insertar", "listar", "modificar", "borrar").contains(accion) && !Files.exists(ARCHIVO)) {
            System.out.println("ERROR: no existe " + ARCHIVO + ". Ejecuta antes 'laboratorio crear'.");
            return 1;
        }
        try {
            switch (accion) {
                case "crear" -> { if (args.size() != 1) return ayuda(); crear(); }
                case "insertar" -> { if (args.size() != 6) return ayuda(); insertar(args); }
                case "listar" -> { if (args.size() != 1) return ayuda(); listar(); }
                case "modificar" -> { if (args.size() != 6) return ayuda(); modificar(args); }
                case "borrar" -> { if (args.size() != 2) return ayuda(); borrar(args.get(1)); }
                default -> { return ayuda(); }
            }
            return 0;
        } catch (SQLException e) {
            // SQLite no rellena el SQLState como PostgreSQL, así que enseño su mensaje tal cual
            System.out.println("ERROR SQLite: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR: no se puede leer o crear " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("ERROR: la dificultad '" + args.get(5) + "' no es un número entero");
        }
        return 1;
    }

    /** Crea la carpeta lab/ y el archivo si no existen, y aplica el esquema. */
    private void crear() throws IOException, SQLException {
        boolean existia = Files.exists(ARCHIVO);
        Files.createDirectories(ARCHIVO.getParent());

        // El script puede tener varias sentencias, así que lo parto por ";" y las lanzo una a una
        String script = quitarComentarios(Files.readString(ESQUEMA));
        try (Connection c = abrir(); Statement st = c.createStatement()) {
            for (String sentencia : script.split(";")) {
                if (!sentencia.isBlank()) {
                    st.executeUpdate(sentencia);
                }
            }
        }
        System.out.println(existia
                ? "El archivo " + ARCHIVO + " ya existía: la tabla se queda como estaba."
                : "Creado el archivo " + ARCHIVO + " con la tabla campeon.");
        System.out.println("Campeones en la tabla: " + contar());
    }

    private void insertar(List<String> a) throws SQLException {
        String sql = "INSERT INTO campeon (id_campeon, nombre, rol, region, dificultad) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.get(1));
            ps.setString(2, a.get(2));
            ps.setString(3, a.get(3));
            ps.setString(4, a.get(4));
            ps.setInt(5, Integer.parseInt(a.get(5).strip()));
            ps.executeUpdate();
        }
        System.out.println("Insertado: " + a.get(1));
    }

    private void listar() throws SQLException {
        String sql = "SELECT id_campeon, nombre, rol, region, dificultad FROM campeon ORDER BY nombre, id_campeon";
        try (Connection c = abrir();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("Campeones en " + ARCHIVO + ":");
            int n = 0;
            while (rs.next()) {
                System.out.println("  " + rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3)
                        + " | " + rs.getString(4) + " | dificultad " + rs.getInt(5));
                n++;
            }
            if (n == 0) {
                System.out.println("  (vacía)");
            }
        }
    }

    private void modificar(List<String> a) throws SQLException {
        String sql = "UPDATE campeon SET nombre = ?, rol = ?, region = ?, dificultad = ? WHERE id_campeon = ?";
        int filas;
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.get(2));
            ps.setString(2, a.get(3));
            ps.setString(3, a.get(4));
            ps.setInt(4, Integer.parseInt(a.get(5).strip()));
            ps.setString(5, a.get(1));
            filas = ps.executeUpdate();
        }
        // igual que en PostgreSQL: 0 filas afectadas quiere decir que no existe
        System.out.println(filas == 0
                ? "No existe el campeón " + a.get(1) + ": no se ha modificado nada (0 filas afectadas)"
                : "Modificado (" + filas + " fila): " + a.get(1));
    }

    private void borrar(String id) throws SQLException {
        int filas;
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement("DELETE FROM campeon WHERE id_campeon = ?")) {
            ps.setString(1, id);
            filas = ps.executeUpdate();
        }
        System.out.println(filas == 0
                ? "No existe el campeón " + id + ": no se ha borrado nada (0 filas afectadas)"
                : "Borrado (" + filas + " fila): " + id);
    }

    private int contar() throws SQLException {
        try (Connection c = abrir();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM campeon")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Abre el archivo. Con SQLite abrir una conexión es abrir el archivo, no hay red. */
    private Connection abrir() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Quita las líneas que empiezan por "--" para que no molesten al partir por ";". */
    private String quitarComentarios(String script) {
        StringBuilder sb = new StringBuilder();
        for (String linea : script.split("\n")) {
            if (!linea.strip().startsWith("--")) {
                sb.append(linea).append('\n');
            }
        }
        return sb.toString();
    }

    private int ayuda() {
        System.out.println("""
                Uso:
                  laboratorio crear
                  laboratorio insertar <id> <nombre> <rol> <region> <dificultad>
                  laboratorio listar
                  laboratorio modificar <id> <nombre> <rol> <region> <dificultad>
                  laboratorio borrar <id>""");
        return 1;
    }
}

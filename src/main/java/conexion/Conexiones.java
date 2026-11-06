package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Abre conexiones con PostgreSQL mediante JDBC.
 *
 * Cada operación pide una conexión nueva con abrir() y QUIEN LA PIDE
 * LA CIERRA, con try-with-resources. Así la conexión se libera también si hay un error.
 */
public final class Conexiones {

    private final ConfiguracionBd config;

    public Conexiones(ConfiguracionBd config) {
        this.config = config;
    }

    public Connection abrir() throws ErrorConexion {
        try {
            return DriverManager.getConnection(config.url(), config.usuario(), config.contrasena());
        } catch (SQLException e) {
            throw new ErrorConexion(explicar(e), e);
        }
    }

    /** Consulta mínima: abre una conexión, pregunta la versión del servidor y la cierra. */
    public String comprobar() throws ErrorConexion {
        try (Connection c = abrir();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT version()")) {
            rs.next();
            return rs.getString(1);
        } catch (SQLException e) {
            throw new ErrorConexion(explicar(e), e);
        }
    }

    /**
     * Traduce el error de JDBC a un mensaje útil. Se decide por el SQLState
     * (código estándar del error) y nunca se muestra la contraseña.
     */
    private String explicar(SQLException e) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String mensaje = e.getMessage() == null ? "" : e.getMessage();

        if (mensaje.startsWith("No suitable driver")) {
            return "La URL de config/bd.properties no es válida (debe empezar por jdbc:postgresql://).";
        }
        return switch (estado) {
            case "28P01", "28000" -> "Usuario o contraseña incorrectos para el usuario «" + config.usuario()
                    + "». Revisa config/bd.properties y el .env de Docker.";
            case "3D000" -> "La base de datos de la URL no existe en el servidor (" + servidor() + ").";
            case "08001", "08004", "08006" -> "No se puede conectar con el servidor " + servidor()
                    + ". ¿Está arrancado el contenedor? (docker compose up -d)";
            default -> "Error de base de datos (SQLState " + estado + "): " + mensaje;
        };
    }

    /** "localhost:5433" a partir de la URL, para los mensajes de error. */
    private String servidor() {
        String url = config.url();
        int inicio = url.indexOf("//");
        if (inicio < 0) {
            return url;
        }
        int fin = url.indexOf('/', inicio + 2);
        return fin < 0 ? url.substring(inicio + 2) : url.substring(inicio + 2, fin);
    }
}

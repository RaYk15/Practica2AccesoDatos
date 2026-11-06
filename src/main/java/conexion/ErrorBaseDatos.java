package conexion;

import java.sql.SQLException;

/**
 * Error al trabajar con la base de datos, con un mensaje que se pueda enseñar al usuario.
 * Traduce los errores de las restricciones del esquema (UNIQUE, FOREIGN KEY, CHECK...)
 * a una explicación normal.
 */
public class ErrorBaseDatos extends Exception {

    private static final long serialVersionUID = 1L;

    public ErrorBaseDatos(String mensaje) {
        super(mensaje);
    }

    public ErrorBaseDatos(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /**
     * Crea el error a partir de la SQLException mirando su SQLState.
     * @param que lo que se intentaba hacer, por ejemplo "el campeón AHR"
     */
    public static ErrorBaseDatos desde(SQLException e, String que) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String mensaje = switch (estado) {
            case "23505" -> "Ya existe " + que + " (clave repetida, la base lo rechaza).";
            // 23503 es clave foránea; 23001 es lo mismo cuando la clave foránea es ON DELETE RESTRICT
            case "23503", "23001" -> "No se puede guardar o borrar " + que
                    + ": rompería una referencia entre tablas (clave foránea).";
            case "23514" -> "La base rechaza " + que + ": incumple una restricción CHECK del esquema.";
            case "23502" -> "La base rechaza " + que + ": falta un campo obligatorio (NOT NULL).";
            default -> "Error de base de datos con " + que + " (SQLState " + estado + "): " + e.getMessage();
        };
        return new ErrorBaseDatos(mensaje, e);
    }
}

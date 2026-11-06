package conexion;

/**
 * No se ha podido conectar con la base de datos, o falta la configuración.
 * Hereda de ErrorBaseDatos, así que con un solo catch (ErrorBaseDatos e) se cogen los dos.
 * El mensaje nunca lleva la contraseña.
 */
public class ErrorConexion extends ErrorBaseDatos {

    private static final long serialVersionUID = 1L;

    public ErrorConexion(String mensaje) {
        super(mensaje);
    }

    public ErrorConexion(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

package conexion;

/**
 * No se ha podido conectar con la base de datos (o falta la configuración).
 * El mensaje está pensado para mostrarse al usuario: nunca incluye la contraseña.
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

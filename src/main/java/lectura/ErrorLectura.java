package lectura;

/**
 * Error cuando no se puede leer un archivo de entrada entero: no existe, está mal escrito
 * o la cabecera del CSV no es la correcta. Si pasa esto, la importación no toca la base.
 * (Un registro suelto incorrecto NO es este error: ese se rechaza en la validación.)
 */
public class ErrorLectura extends Exception {

    private static final long serialVersionUID = 1L;

    public ErrorLectura(String mensaje) {
        super(mensaje);
    }

    public ErrorLectura(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

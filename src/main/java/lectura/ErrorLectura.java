package lectura;

/**
 * Error que impide interpretar un archivo de entrada completo.
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

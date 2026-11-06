package validacion;

/**
 * Se lanza en cuanto un registro no cumple una regla.
 * Como es una excepción, el validador se para en el primer fallo que encuentra.
 */
public class Rechazo extends Exception {

    private static final long serialVersionUID = 1L;

    private final Motivo motivo;

    public Rechazo(Motivo motivo, String detalle) {
        super(detalle);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}

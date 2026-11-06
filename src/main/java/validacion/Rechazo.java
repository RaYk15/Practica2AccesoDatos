package validacion;

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

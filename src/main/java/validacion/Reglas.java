package validacion;

public class Reglas {
    static void rango(String campo, long valor, long minimo, long maximo) throws Rechazo {
        if (valor < minimo || valor > maximo) {
            throw new Rechazo(Motivo.FUERA_DE_RANGO,
                    campo + " = " + valor + " (debe estar entre " + minimo + " y " + maximo + ")");
        }
    }
}

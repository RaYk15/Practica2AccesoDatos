package validacion;

/** Comprobación de rangos que usan los dos validadores. */
public class Reglas {

    /** Rechaza el valor si no está entre minimo y maximo (ambos incluidos). */
    static void rango(String campo, long valor, long minimo, long maximo) throws Rechazo {
        if (valor < minimo || valor > maximo) {
            throw new Rechazo(Motivo.FUERA_DE_RANGO,
                    campo + " = " + valor + " (debe estar entre " + minimo + " y " + maximo + ")");
        }
    }
}

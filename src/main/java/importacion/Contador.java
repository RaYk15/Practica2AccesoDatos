package importacion;

/**
 * Cuenta cómo acaba cada registro en una importación (uno para campeones y otro para partidas).
 * Cada registro suma en una sola caja, así que siempre se cumple:
 * procesados = nuevos + duplicados + conflictos + rechazados.
 */
public class Contador {

    /** Las cuatro cajas en las que puede acabar un registro. */
    public enum Clasificacion {
        NUEVO, DUPLICADO, CONFLICTO, RECHAZADO
    }

    private int procesados;
    private int nuevos;
    private int duplicados;
    private int conflictos;
    private int rechazados;

    /** Suma uno a procesados y uno a la caja que toque. */
    public void contar(Clasificacion clasificacion) {
        procesados++;
        switch (clasificacion) {
            case NUEVO -> nuevos++;
            case DUPLICADO -> duplicados++;
            case CONFLICTO -> conflictos++;
            case RECHAZADO -> rechazados++;
        }
    }

    // Solo getters: los números solo cambian con contar(), así siempre cuadran

    public int getProcesados() {
        return procesados;
    }

    public int getNuevos() {
        return nuevos;
    }

    public int getDuplicados() {
        return duplicados;
    }

    public int getConflictos() {
        return conflictos;
    }

    public int getRechazados() {
        return rechazados;
    }

    @Override
    public String toString() {
        return procesados + " procesados -> " + nuevos + " nuevos, " + duplicados + " duplicados, "
                + conflictos + " conflictos, " + rechazados + " rechazados";
    }
}

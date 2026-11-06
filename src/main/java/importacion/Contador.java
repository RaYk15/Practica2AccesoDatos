package importacion;

import org.json.JSONObject;

/**
 * Contadores de un conjunto (campeones o partidas) en una importación.
 * Cada registro procesado suma en exactamente una caja, así que siempre
 * procesados = nuevos + duplicados + conflictos + rechazados.
 */
public class Contador {

    private int procesados;
    private int nuevos;
    private int duplicados;
    private int conflictos;
    private int rechazados;

    public enum Clasificacion {
        NUEVO, DUPLICADO, CONFLICTO, RECHAZADO
    }

    public void contar(Clasificacion clasificacion) {
        procesados++;
        switch (clasificacion) {
            case NUEVO -> nuevos++;
            case DUPLICADO -> duplicados++;
            case CONFLICTO -> conflictos++;
            case RECHAZADO -> rechazados++;
        }
    }

    @Override
    public String toString() {
        return procesados + " procesados -> " + nuevos + " nuevos, " + duplicados + " duplicados, "
                + conflictos + " conflictos, " + rechazados + " rechazados";
    }

    // Getters y setters
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

    public void setProcesados(int procesados) {
        this.procesados = procesados;
    }

    public void setNuevos(int nuevos) {
        this.nuevos = nuevos;
    }

    public void setDuplicados(int duplicados) {
        this.duplicados = duplicados;
    }

    public void setConflictos(int conflictos) {
        this.conflictos = conflictos;
    }

    public void setRechazados(int rechazados) {
        this.rechazados = rechazados;
    }
}
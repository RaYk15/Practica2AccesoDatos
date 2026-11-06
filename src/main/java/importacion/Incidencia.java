package importacion;

import validacion.Motivo;

/**
 * Un registro que se ha rechazado o que ha dado conflicto, para enseñarlo al terminar.
 *
 * @param archivo  campeones.csv o partidas.json
 * @param registro número de registro (en el CSV sin contar la cabecera)
 * @param clave    identidad del registro, o null si no se ha podido sacar
 * @param motivo   por qué se ha rechazado
 * @param detalle  explicación del problema
 */
public record Incidencia(String archivo, int registro, String clave, Motivo motivo, String detalle) {

    /** Por ejemplo: partidas.json, registro 9, EUW/P009, REFERENCIA_INEXISTENTE: ZED */
    @Override
    public String toString() {
        return archivo + ", registro " + registro + ", " + (clave == null ? "sin clave" : clave)
                + ", " + motivo + ": " + detalle;
    }
}

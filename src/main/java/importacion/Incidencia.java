package importacion;

import org.json.JSONObject;
import validacion.Motivo;

/**
 * Un registro rechazado o en conflicto.
 *
 * @param registro número de registro lógico (en el CSV no cuenta la cabecera)
 * @param clave    identidad del registro, o null si no se pudo obtener
 */
public record Incidencia(String archivo, int registro, String clave, Motivo motivo, String detalle) {

    @Override
    public String toString() {
        return archivo + ", registro " + registro + ", " + (clave == null ? "sin clave" : clave)
                + ", " + motivo + ": " + detalle;
    }

    public JSONObject aJson() {
        return new JSONObject()
                .put("archivo", archivo)
                .put("registro", registro)
                .put("clave", clave == null ? JSONObject.NULL : clave)
                .put("motivo", motivo.name())
                .put("detalle", detalle);
    }
}
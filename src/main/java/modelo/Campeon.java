package modelo;

import org.json.JSONObject;

/**
 * Clase correspondiente al campeón ya validado.
 * @param id
 * @param nombre
 * @param rol
 * @param region
 * @param dificultad
 */
public record Campeon(String id, String nombre, String rol, String region, int dificultad) {

    public JSONObject aJson() {
        return new JSONObject()
                .put("idCampeon", id)
                .put("nombre", nombre)
                .put("rol", rol)
                .put("region", region)
                .put("dificultad", dificultad);
    }

    /** Reconstruye el campeón desde json */
    public static Campeon desdeJson(JSONObject o) {
        return new Campeon(o.getString("idCampeon"), o.getString("nombre"), o.getString("rol"),
                o.getString("region"), o.getInt("dificultad"));
    }
}
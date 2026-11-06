package modelo;

import org.json.JSONObject;

/**
 * Esta clase representa un jugador dentro de una partida.
 * @param jugador
 * @param campeonId
 * @param kills
 * @param deaths
 * @param assists
 */
public record Participante(String jugador, String campeonId, int kills, int deaths, int assists) {

    /** Fórmula pa sacar el KDA (kills + asistencias / muertes) */
    public double kda() {
        return (kills + assists) / (double) Math.max(1, deaths);
    }

    public JSONObject aJson() {
        return new JSONObject()
                .put("jugador", jugador)
                .put("campeonId", campeonId)
                .put("kills", kills)
                .put("deaths", deaths)
                .put("assists", assists);
    }

    public static Participante desdeJson(JSONObject o) {
        return new Participante(o.getString("jugador"), o.getString("campeonId"),
                o.getInt("kills"), o.getInt("deaths"), o.getInt("assists"));
    }
}

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
}

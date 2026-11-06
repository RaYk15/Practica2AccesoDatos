package modelo;

/**
 * Un jugador dentro de una partida. No es una entidad propia: forma parte de la partida.
 * Aquí está la referencia al campeón (campeonId).
 *
 * @param jugador   nombre del jugador
 * @param campeonId campeón que ha usado (tiene que existir en la tabla campeon)
 * @param kills     asesinatos
 * @param deaths    muertes
 * @param assists   asistencias
 */
public record Participante(String jugador, String campeonId, int kills, int deaths, int assists) {

    /** KDA = (kills + assists) / deaths. Si no ha muerto divido entre 1 para no dividir entre 0. */
    public double kda() {
        return (kills + assists) / (double) Math.max(1, deaths);
    }
}

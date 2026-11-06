package modelo;

import java.util.List;
import java.util.stream.Stream;

/**
 * Una partida ya validada (es la entidad B de la práctica).
 * Se identifica por servidor + idPartida, por ejemplo "EUW/P001".
 *
 * @param servidor    EUW, EUNE, NA o KR
 * @param idPartida   identificador dentro del servidor
 * @param fecha       en formato AAAA-MM-DD
 * @param duracionMin duración en minutos
 * @param ganador     "azul" o "rojo"
 * @param azul        los 5 jugadores del equipo azul, en orden
 * @param rojo        los 5 jugadores del equipo rojo, en orden
 */
public record Partida(String servidor, String idPartida, String fecha, int duracionMin,
                      String ganador, List<Participante> azul, List<Participante> rojo) {

    /**
     * Copio las listas para que no se puedan modificar desde fuera una vez creada la partida.
     * Si no, alguien podría añadir un jugador a la lista original y la partida cambiaría.
     */
    public Partida {
        azul = List.copyOf(azul);
        rojo = List.copyOf(rojo);
    }

    /** Devuelve la clave de la partida, por ejemplo "EUW/P001". */
    public String clave() {
        return clave(servidor, idPartida);
    }

    /** Lo mismo pero sin tener la partida, solo con los datos sueltos (lo uso al buscar). */
    public static String clave(String servidor, String idPartida) {
        return servidor + "/" + idPartida;
    }

    /** Devuelve los jugadores de un equipo: "azul" o "rojo". */
    public List<Participante> equipo(String color) {
        return color.equals("azul") ? azul : rojo;
    }

    /** Junta los 10 jugadores de los dos equipos, para cuando no importa en qué equipo estaban. */
    public Stream<Participante> participantes() {
        return Stream.concat(azul.stream(), rojo.stream());
    }
}

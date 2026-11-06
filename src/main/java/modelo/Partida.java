package modelo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Clase correspondiente a la partida ya validada.
 * @param servidor
 * @param idPartida
 * @param fecha
 * @param duracionMin
 * @param ganador
 * @param azul
 * @param rojo
 */
public record Partida(String servidor, String idPartida, String fecha, int duracionMin,
                      String ganador, List<Participante> azul, List<Participante> rojo) {

    public Partida {
        azul = List.copyOf(azul);   // listas inmutables
        rojo = List.copyOf(rojo);
    }

    /** Clave de identidad, por ejemplo "EUW/P001". */
    public String clave() {
        return clave(servidor, idPartida);
    }

    public static String clave(String servidor, String idPartida) {
        return servidor + "/" + idPartida;
    }

    public List<Participante> equipo(String color) {
        return color.equals("azul") ? azul : rojo;
    }

    public Stream<Participante> participantes() {
        return Stream.concat(azul.stream(), rojo.stream());
    }
}

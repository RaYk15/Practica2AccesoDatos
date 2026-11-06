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

    public JSONObject aJson() {
        return new JSONObject()
                .put("servidor", servidor)
                .put("idPartida", idPartida)
                .put("fecha", fecha)
                .put("duracionMin", duracionMin)
                .put("ganador", ganador)
                .put("equipos", new JSONObject()
                        .put("azul", aJson(azul))
                        .put("rojo", aJson(rojo)));
    }

    private static JSONArray aJson(List<Participante> equipo) {
        JSONArray array = new JSONArray();
        for (Participante p : equipo) {
            array.put(p.aJson());
        }
        return array;
    }

    /** Reconstruye la partida desde json */
    public static Partida desdeJson(JSONObject o) {
        JSONObject equipos = o.getJSONObject("equipos");
        return new Partida(o.getString("servidor"), o.getString("idPartida"), o.getString("fecha"),
                o.getInt("duracionMin"), o.getString("ganador"),
                desdeJson(equipos.getJSONArray("azul")), desdeJson(equipos.getJSONArray("rojo")));
    }

    private static List<Participante> desdeJson(JSONArray array) {
        List<Participante> equipo = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            equipo.add(Participante.desdeJson(array.getJSONObject(i)));
        }
        return equipo;
    }
}

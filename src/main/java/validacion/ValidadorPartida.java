package validacion;

import modelo.Participante;
import modelo.Partida;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ValidadorPartida {

    public static final List<String> CAMPOS =
            List.of("servidor", "idPartida", "fecha", "duracionMin", "ganador", "equipos");
    public static final List<String> CAMPOS_PARTICIPANTE =
            List.of("jugador", "campeonId", "kills", "deaths", "assists");
    public static final List<String> COLORES = List.of("azul", "rojo");
    public static final Set<String> SERVIDORES = Set.of("EUW", "EUNE", "NA", "KR");
    public static final int JUGADORES_POR_EQUIPO = 5;

    /**
     * @param campeonesValidos ids de los campeones del estado anterior y de los aceptados en este lote
     */
    public Partida validar(Object elemento, Set<String> campeonesValidos) throws Rechazo {

        // Obligatorios y tipos
        JSONObject o = objeto(elemento, "el registro");
        for (String campo : CAMPOS) {
            obligatorio(o, campo, campo);
        }
        String servidor = texto(o, "servidor", "servidor");
        String idPartida = texto(o, "idPartida", "idPartida");
        String fecha = texto(o, "fecha", "fecha");
        int duracionMin = entero(o, "duracionMin", "duracionMin");
        String ganador = texto(o, "ganador", "ganador");

        JSONObject equipos = objeto(o.get("equipos"), "equipos");
        List<Participante> azul = equipo(equipos, "azul");
        List<Participante> rojo = equipo(equipos, "rojo");

        // Valores admitidos y rangos
        if (!SERVIDORES.contains(servidor)) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "servidor «" + servidor + "» no es " + SERVIDORES);
        }
        comprobarFecha(fecha);
        Reglas.rango("duracionMin", duracionMin, 1, 120);
        if (!COLORES.contains(ganador)) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "ganador «" + ganador + "» no es azul ni rojo");
        }

        Set<String> vistos = new HashSet<>();
        for (String color : COLORES) {
            List<Participante> equipo = color.equals("azul") ? azul : rojo;
            if (equipo.size() != JUGADORES_POR_EQUIPO) {
                throw new Rechazo(Motivo.EQUIPO_INCOMPLETO, "el equipo " + color + " tiene "
                        + equipo.size() + " jugadores (deben ser " + JUGADORES_POR_EQUIPO + ")");
            }
            for (int i = 0; i < equipo.size(); i++) {
                Participante p = equipo.get(i);
                String donde = "equipos." + color + "[" + i + "].";
                Reglas.rango(donde + "kills", p.kills(), 0, 99);
                Reglas.rango(donde + "deaths", p.deaths(), 0, 99);
                Reglas.rango(donde + "assists", p.assists(), 0, 99);
                if (!vistos.add(p.campeonId())) {
                    throw new Rechazo(Motivo.CAMPEON_REPETIDO,
                            "el campeón " + p.campeonId() + " aparece más de una vez en la partida");
                }
            }
        }

        // Referencias
        for (String color : COLORES) {
            for (Participante p : color.equals("azul") ? azul : rojo) {
                if (!campeonesValidos.contains(p.campeonId())) {
                    throw new Rechazo(Motivo.REFERENCIA_INEXISTENTE, p.campeonId());
                }
            }
        }

        return new Partida(servidor, idPartida, fecha, duracionMin, ganador, azul, rojo);
    }

    /** Valida un equipo: debe ser un array de participantes */
    private List<Participante> equipo(JSONObject equipos, String color) throws Rechazo {
        String nombre = "equipos." + color;
        obligatorio(equipos, color, nombre);
        if (!(equipos.get(color) instanceof JSONArray array)) {
            throw new Rechazo(Motivo.TIPO_INCORRECTO, nombre + " debe ser un array");
        }
        List<Participante> equipo = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String donde = nombre + "[" + i + "]";
            JSONObject p = objeto(array.get(i), donde);
            for (String campo : CAMPOS_PARTICIPANTE) {
                obligatorio(p, campo, donde + "." + campo);
            }
            equipo.add(new Participante(
                    texto(p, "jugador", donde + ".jugador"),
                    texto(p, "campeonId", donde + ".campeonId"),
                    entero(p, "kills", donde + ".kills"),
                    entero(p, "deaths", donde + ".deaths"),
                    entero(p, "assists", donde + ".assists")));
        }
        return equipo;
    }

    // Utilidades

    private JSONObject objeto(Object valor, String nombre) throws Rechazo {
        if (valor instanceof JSONObject o) {
            return o;
        }
        throw new Rechazo(Motivo.TIPO_INCORRECTO, nombre + " debe ser un objeto JSON");
    }

    /** Es null o es un texto vacío */
    private void obligatorio(JSONObject o, String clave, String nombre) throws Rechazo {
        Object valor = o.opt(clave);   // opt no convierte nada: devuelve el valor tal cual o null
        boolean vacio = valor == null || valor == JSONObject.NULL
                || (valor instanceof String s && s.isBlank());
        if (vacio) {
            throw new Rechazo(Motivo.CAMPO_OBLIGATORIO, nombre + " falta o está vacío");
        }
    }

    /** Comprueba que es un string de verdad */
    private String texto(JSONObject o, String clave, String nombre) throws Rechazo {
        if (o.opt(clave) instanceof String s) {
            return s.strip();
        }
        throw new Rechazo(Motivo.TIPO_INCORRECTO, nombre + " debe ser texto y es " + describir(o.opt(clave)));
    }

    /**
     * Entero: tiene que ser un número entero JSON */
    private int entero(JSONObject o, String clave, String nombre) throws Rechazo {
        Object valor = o.opt(clave);
        if (valor instanceof Integer i) {
            return i;
        }
        if (valor instanceof Long || valor instanceof BigInteger) {
            throw new Rechazo(Motivo.FUERA_DE_RANGO, nombre + " = " + valor + " es demasiado grande");
        }
        throw new Rechazo(Motivo.TIPO_INCORRECTO, nombre + " debe ser un entero y es " + describir(valor));
    }

    private void comprobarFecha(String fecha) throws Rechazo {
        try {
            if (!fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new DateTimeParseException("formato", fecha, 0);
            }
            LocalDate.parse(fecha);   // estricto: 2026-02-30 no existe y falla
        } catch (DateTimeParseException e) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "fecha «" + fecha + "» no es una fecha válida AAAA-MM-DD");
        }
    }

    private String describir(Object valor) {
        if (valor instanceof String s) {
            return "el texto \"" + s + "\"";
        }
        if (valor instanceof Number) {
            return "el número " + valor;
        }
        return valor == null ? "nada" : valor.getClass().getSimpleName() + " " + valor;
    }
}

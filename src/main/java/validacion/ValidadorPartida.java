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

/**
 * Comprueba que una partida del JSON cumple las reglas del contrato y la convierte en Partida.
 * Si algo falla lanza Rechazo con el motivo.
 *
 * Orden de comprobación:
 *   1. obligatorios y tipos (primero la partida, luego los equipos y luego cada jugador)
 *   2. valores permitidos y rangos
 *   3. que los campeones existan
 * Si es nueva, duplicada o conflicto lo decide después el Importador.
 *
 * Importante: en JSON los tipos ya vienen dados y NO convierto nada.
 * "8" entre comillas es texto, no el número 8, así que se rechaza.
 * Las propiedades que no están en el contrato se ignoran (no se leen).
 */
public class ValidadorPartida {

    public static final List<String> CAMPOS =
            List.of("servidor", "idPartida", "fecha", "duracionMin", "ganador", "equipos");
    public static final List<String> CAMPOS_PARTICIPANTE =
            List.of("jugador", "campeonId", "kills", "deaths", "assists");
    public static final List<String> COLORES = List.of("azul", "rojo");
    public static final Set<String> SERVIDORES = Set.of("EUW", "EUNE", "NA", "KR");
    public static final int JUGADORES_POR_EQUIPO = 5;

    /**
     * @param campeonesValidos ids de los campeones que hay en la base, para comprobar las referencias
     */
    public Partida validar(Object elemento, Set<String> campeonesValidos) throws Rechazo {

        // 1. Obligatorios y tipos
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

        // 2. Valores permitidos y rangos
        if (!SERVIDORES.contains(servidor)) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "servidor '" + servidor + "' no es " + SERVIDORES);
        }
        comprobarFecha(fecha);
        Reglas.rango("duracionMin", duracionMin, 1, 120);
        if (!COLORES.contains(ganador)) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "ganador '" + ganador + "' no es azul ni rojo");
        }

        // en 'vistos' voy guardando los campeones para detectar si alguno se repite
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
                if (!vistos.add(p.campeonId())) {   // add devuelve false si ya estaba
                    throw new Rechazo(Motivo.CAMPEON_REPETIDO,
                            "el campeón " + p.campeonId() + " aparece más de una vez en la partida");
                }
            }
        }

        // 3. Referencias: cada campeón tiene que existir. Nunca me invento un campeón.
        for (String color : COLORES) {
            for (Participante p : color.equals("azul") ? azul : rojo) {
                if (!campeonesValidos.contains(p.campeonId())) {
                    throw new Rechazo(Motivo.REFERENCIA_INEXISTENTE, p.campeonId());
                }
            }
        }

        return new Partida(servidor, idPartida, fecha, duracionMin, ganador, azul, rojo);
    }

    /** Lee un equipo: tiene que ser un array de jugadores (cuántos hay se mira después). */
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

    // ---------- Métodos de ayuda ----------

    /** Comprueba que el valor es un objeto JSON { ... }. */
    private JSONObject objeto(Object valor, String nombre) throws Rechazo {
        if (valor instanceof JSONObject o) {
            return o;
        }
        throw new Rechazo(Motivo.TIPO_INCORRECTO, nombre + " debe ser un objeto JSON");
    }

    /** Rechaza el campo si falta, es null o es un texto vacío. */
    private void obligatorio(JSONObject o, String clave, String nombre) throws Rechazo {
        Object valor = o.opt(clave);   // opt devuelve el valor tal cual (o null), sin convertirlo
        boolean vacio = valor == null || valor == JSONObject.NULL
                || (valor instanceof String s && s.isBlank());
        if (vacio) {
            throw new Rechazo(Motivo.CAMPO_OBLIGATORIO, nombre + " falta o está vacío");
        }
    }

    /** Comprueba que el valor es un String de verdad y le quita los espacios de los lados. */
    private String texto(JSONObject o, String clave, String nombre) throws Rechazo {
        if (o.opt(clave) instanceof String s) {
            return s.strip();
        }
        throw new Rechazo(Motivo.TIPO_INCORRECTO, nombre + " debe ser texto y es " + describir(o.opt(clave)));
    }

    /**
     * Comprueba que el valor es un número entero de JSON. Ni "8" (texto) ni 8.0 (decimal) valen.
     * No uso o.getInt() porque getInt("8") convertiría el texto sin avisar.
     */
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

    /** La fecha tiene que tener formato AAAA-MM-DD y existir (el 2026-02-30 no existe). */
    private void comprobarFecha(String fecha) throws Rechazo {
        try {
            if (!fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new DateTimeParseException("formato", fecha, 0);
            }
            LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "fecha '" + fecha + "' no es una fecha válida AAAA-MM-DD");
        }
    }

    /** Describe un valor para los mensajes de error, por ejemplo: el texto "8". */
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

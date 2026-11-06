package validacion;

import lectura.FilaCsv;
import lectura.LectorCsv;
import modelo.Campeon;

import java.util.Set;

/**
 * Convierte una fila del CSV (todo texto) en un Campeon, o la rechaza.
 * Orden: estructura -> obligatorios -> tipos -> valores/rangos.
 */
public class ValidadorCampeon {

    public static final Set<String> ROLES = Set.of("top", "jungla", "mid", "adc", "support");

    public Campeon validar(FilaCsv fila) throws Rechazo {
        // 0. Estructura: la fila debe tener las mismas columnas que la cabecera
        if (!fila.completa()) {
            throw new Rechazo(Motivo.ESTRUCTURA_INCORRECTA,
                    "la fila no tiene " + LectorCsv.CABECERA.size() + " columnas");
        }

        // 1. Obligatorios (y normalización: quitar espacios al principio y al final)
        String id = obligatorio(fila, "idCampeon");
        String nombre = obligatorio(fila, "nombre");
        String rol = obligatorio(fila, "rol");
        String region = obligatorio(fila, "region");
        String textoDificultad = obligatorio(fila, "dificultad");

        // 2. Tipos: en CSV todo es texto, así que aquí se CONVIERTE según el diccionario
        if (!textoDificultad.matches("-?\\d{1,9}")) {
            throw new Rechazo(Motivo.TIPO_INCORRECTO,
                    "dificultad «" + textoDificultad + "» no es un número entero");
        }
        int dificultad = Integer.parseInt(textoDificultad);

        // 3. Valores admitidos y rangos
        if (!ROLES.contains(rol)) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "rol «" + rol + "» no es top, jungla, mid, adc ni support");
        }
        Reglas.rango("dificultad", dificultad, 1, 3);

        return new Campeon(id, nombre, rol, region, dificultad);
    }

    /**
     * Valida y normaliza un campeón que no viene del CSV (por ejemplo, una alta o una
     * modificación desde el programa). Mismas reglas que validar(FilaCsv).
     */
    public Campeon validar(Campeon c) throws Rechazo {
        String id = obligatorio(c.id(), "idCampeon");
        String nombre = obligatorio(c.nombre(), "nombre");
        String rol = obligatorio(c.rol(), "rol");
        String region = obligatorio(c.region(), "region");
        if (!ROLES.contains(rol)) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "rol «" + rol + "» no es top, jungla, mid, adc ni support");
        }
        Reglas.rango("dificultad", c.dificultad(), 1, 3);
        return new Campeon(id, nombre, rol, region, c.dificultad());
    }

    private String obligatorio(String valor, String campo) throws Rechazo {
        if (valor == null || valor.isBlank()) {
            throw new Rechazo(Motivo.CAMPO_OBLIGATORIO, campo + " está vacío");
        }
        return valor.strip();
    }

    private String obligatorio(FilaCsv fila, String columna) throws Rechazo {
        String valor = fila.valores().get(columna);
        if (valor == null || valor.isBlank()) {
            throw new Rechazo(Motivo.CAMPO_OBLIGATORIO, columna + " está vacío");
        }
        return valor.strip();
    }
}

package validacion;

import lectura.FilaCsv;
import lectura.LectorCsv;
import modelo.Campeon;

import java.util.Set;

/**
 * Comprueba que un campeón cumple las reglas del contrato y lo convierte en un Campeon.
 * Si algo falla lanza Rechazo con el motivo.
 * Orden de comprobación: columnas, obligatorios, tipos y por último valores y rangos.
 */
public class ValidadorCampeon {

    public static final Set<String> ROLES = Set.of("top", "jungla", "mid", "adc", "support");

    /** Valida una fila del CSV, donde todo llega como texto. */
    public Campeon validar(FilaCsv fila) throws Rechazo {
        // 0. La fila tiene que tener las mismas columnas que la cabecera
        if (!fila.completa()) {
            throw new Rechazo(Motivo.ESTRUCTURA_INCORRECTA,
                    "la fila no tiene " + LectorCsv.CABECERA.size() + " columnas");
        }

        // 1. Obligatorios (de paso quito los espacios del principio y del final)
        String id = obligatorio(fila, "idCampeon");
        String nombre = obligatorio(fila, "nombre");
        String rol = obligatorio(fila, "rol");
        String region = obligatorio(fila, "region");
        String textoDificultad = obligatorio(fila, "dificultad");

        // 2. Tipos: en el CSV todo es texto, así que aquí paso la dificultad a número
        if (!textoDificultad.matches("-?\\d{1,9}")) {
            throw new Rechazo(Motivo.TIPO_INCORRECTO,
                    "dificultad '" + textoDificultad + "' no es un número entero");
        }
        int dificultad = Integer.parseInt(textoDificultad);

        // 3. Valores permitidos y rangos
        if (!ROLES.contains(rol)) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "rol '" + rol + "' no es top, jungla, mid, adc ni support");
        }
        Reglas.rango("dificultad", dificultad, 1, 3);

        return new Campeon(id, nombre, rol, region, dificultad);
    }

    /**
     * Valida un campeón que no viene del CSV, por ejemplo cuando lo creo o lo modifico
     * desde el menú. Las reglas son las mismas que en validar(FilaCsv).
     */
    public Campeon validar(Campeon c) throws Rechazo {
        String id = obligatorio(c.id(), "idCampeon");
        String nombre = obligatorio(c.nombre(), "nombre");
        String rol = obligatorio(c.rol(), "rol");
        String region = obligatorio(c.region(), "region");
        if (!ROLES.contains(rol)) {
            throw new Rechazo(Motivo.VALOR_NO_ADMITIDO, "rol '" + rol + "' no es top, jungla, mid, adc ni support");
        }
        Reglas.rango("dificultad", c.dificultad(), 1, 3);
        return new Campeon(id, nombre, rol, region, c.dificultad());
    }

    /** Si el texto es null o está vacío lo rechazo; si no, lo devuelvo sin espacios a los lados. */
    private String obligatorio(String valor, String campo) throws Rechazo {
        if (valor == null || valor.isBlank()) {
            throw new Rechazo(Motivo.CAMPO_OBLIGATORIO, campo + " está vacío");
        }
        return valor.strip();
    }

    private String obligatorio(FilaCsv fila, String columna) throws Rechazo {
        return obligatorio(fila.valores().get(columna), columna);
    }
}

package validacion;

/** Los motivos por los que se rechaza un registro (o se marca como conflicto). */
public enum Motivo {
    ESTRUCTURA_INCORRECTA,   // la fila del CSV no tiene las columnas de la cabecera
    CAMPO_OBLIGATORIO,       // falta un campo o está vacío
    TIPO_INCORRECTO,         // por ejemplo "8" (texto) donde va un número
    VALOR_NO_ADMITIDO,       // un rol, servidor o ganador que no existe, o una fecha imposible
    FUERA_DE_RANGO,          // un número fuera de sus límites
    EQUIPO_INCOMPLETO,       // un equipo sin exactamente 5 jugadores
    CAMPEON_REPETIDO,        // el mismo campeón dos veces en una partida
    REFERENCIA_INEXISTENTE,  // un campeonId que no está en la tabla campeon
    CONFLICTO                // misma clave que uno ya guardado pero con otros datos
}

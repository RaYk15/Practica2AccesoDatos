package lectura;

import java.util.Map;

/**
 * Una fila del CSV tal cual llega: todo es texto y todavía no está validado.
 *
 * @param numero   número de registro (1 = primera fila de datos, sin contar la cabecera)
 * @param valores  columna -> texto (null si a la fila le falta esa columna)
 * @param completa false si la fila no tiene las mismas columnas que la cabecera
 */
public record FilaCsv(int numero, Map<String, String> valores, boolean completa) {
}

package lectura;

import java.util.Map;

/**
 * Una fila de datos del CSV tal como llega.
 *
 * @param numero   número de registro lógico (1 = primera fila de datos, sin contar la cabecera)
 * @param valores  columna -> texto; vale null si a la fila le falta esa columna
 * @param completa false si la fila no tiene el mismo número de columnas que la cabecera
 */
public record FilaCsv(int numero, Map<String, String> valores, boolean completa) {
}

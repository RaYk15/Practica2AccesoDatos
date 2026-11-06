package lectura;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee campeones.csv línea a línea con Commons CSV (lectura secuencial).
 * Solo lee el texto: no convierte ni valida nada, eso lo hace ValidadorCampeon.
 */
public class LectorCsv {

    /** La cabecera tiene que ser exactamente esta. */
    public static final List<String> CABECERA =
            List.of("idCampeon", "nombre", "rol", "region", "dificultad");

    /**
     * Formato CSV normal: separado por comas y con comillas dobles.
     * Así "Vacío, Runaterra" entre comillas cuenta como un solo campo.
     */
    private static final CSVFormat FORMATO = CSVFormat.DEFAULT.builder()
            .setHeader()                // la primera fila es la cabecera
            .setSkipHeaderRecord(true)  // y no la devuelvo como si fuera un dato
            .setIgnoreEmptyLines(true)  // las líneas vacías se saltan
            .get();

    public List<FilaCsv> leer(Path ruta) throws ErrorLectura {
        if (!Files.isRegularFile(ruta)) {
            throw new ErrorLectura("No existe el archivo " + ruta);
        }

        List<FilaCsv> filas = new ArrayList<>();

        // try-with-resources: el lector y el parser se cierran solos aunque haya un error
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8);
             CSVParser parser = FORMATO.parse(lector)) {

            comprobarCabecera(ruta, parser.getHeaderNames());

            int numero = 0;
            for (CSVRecord registro : parser) {
                numero++;
                filas.add(new FilaCsv(numero, aMapa(registro), registro.isConsistent()));
            }

        } catch (IOException | UncheckedIOException e) {
            // el archivo no se puede leer: comillas sin cerrar, codificación mala, etc.
            throw new ErrorLectura("No se puede interpretar " + ruta + ": " + e.getMessage(), e);
        }

        return filas;
    }

    private void comprobarCabecera(Path ruta, List<String> cabecera) throws ErrorLectura {
        if (!cabecera.equals(CABECERA)) {
            throw new ErrorLectura("Cabecera incorrecta en " + ruta
                    + ". Esperada " + CABECERA + " y encontrada " + cabecera
                    + " (si el archivo viene de Excel, guárdalo como UTF-8 sin BOM)");
        }
    }

    /** Pasa la fila a un mapa columna -> texto, en el orden de la cabecera. */
    private Map<String, String> aMapa(CSVRecord registro) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (String columna : CABECERA) {
            // si a la fila le falta esa columna, guardo null
            valores.put(columna, registro.isSet(columna) ? registro.get(columna) : null);
        }
        return valores;
    }
}

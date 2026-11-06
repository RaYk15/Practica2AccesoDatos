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

public class LectorCsv {
    public static final List<String> CABECERA =
            List.of("idCampeon", "nombre", "rol", "region", "dificultad");

    /**
     * Formato CSV estándar.
     */
    private static final CSVFormat FORMATO = CSVFormat.DEFAULT.builder()
            .setHeader()                // la primera fila es la cabecera...
            .setSkipHeaderRecord(true)  // ...y no se devuelve como un registro de datos
            .setIgnoreEmptyLines(true)  // las líneas vacías se saltan, no terminan la lectura
            .get();

    public List<FilaCsv> leer(Path ruta) throws ErrorLectura {
        if (!Files.isRegularFile(ruta)) {
            throw new ErrorLectura("No existe el archivo " + ruta);
        }

        List<FilaCsv> filas = new ArrayList<>();

        // try-with-resources pal parser y el lector
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8);
             CSVParser parser = FORMATO.parse(lector)) {

            comprobarCabecera(ruta, parser.getHeaderNames());

            int numero = 0;
            for (CSVRecord registro : parser) {
                numero++;
                filas.add(new FilaCsv(numero, aMapa(registro), registro.isConsistent()));
            }

        } catch (IOException | UncheckedIOException e) {
            // tira error de lectura si da algún fallo
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

    /** Pasa el registro a un mapa columna -> texto */
    private Map<String, String> aMapa(CSVRecord registro) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (String columna : CABECERA) {
            valores.put(columna, registro.isSet(columna) ? registro.get(columna) : null);
        }
        return valores;
    }
}

package lectura;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONParserConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee partidas.json entero de golpe y lo convierte con org.json.
 * Solo lee: la validación de cada partida la hace ValidadorPartida.
 */
public class LectorJson {

    /** Como el JSON se carga entero en memoria, pongo un tamaño máximo (5 MB). */
    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    /**
     * Modo estricto: sin esto org.json acepta cosas que no son JSON de verdad,
     * como comillas simples o una coma al final.
     */
    private static final JSONParserConfiguration ESTRICTO =
            new JSONParserConfiguration().withStrictMode();

    /**
     * Devuelve los elementos del array del archivo, en orden.
     * Los devuelvo como Object y no como JSONObject para que el validador
     * pueda rechazar los que no sean objetos.
     */
    public List<Object> leer(Path ruta) throws ErrorLectura {
        if (!Files.isRegularFile(ruta)) {
            throw new ErrorLectura("No existe el archivo " + ruta);
        }

        try {
            long tamano = Files.size(ruta);
            if (tamano > TAMANO_MAXIMO) {
                throw new ErrorLectura(ruta + " ocupa " + tamano
                        + " bytes y el máximo es " + TAMANO_MAXIMO);
            }

            // readString abre el archivo, lo lee entero y lo cierra
            String texto = Files.readString(ruta, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(texto, ESTRICTO);

            List<Object> elementos = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                elementos.add(array.get(i));
            }
            return elementos;

        } catch (IOException e) {
            throw new ErrorLectura("No se puede leer " + ruta + ": " + e.getMessage(), e);
        } catch (JSONException e) {
            throw new ErrorLectura("JSON mal formado en " + ruta + ": " + e.getMessage(), e);
        }
    }
}

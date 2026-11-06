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

public class LectorJson {
    /** Tamaño máximo admitido */
    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    /**
     * Modo estricto: sin él, org.json acepta cosas que no son JSON válido.
     */
    private static final JSONParserConfiguration ESTRICTO =
            new JSONParserConfiguration().withStrictMode();

    /**
     * @return los elementos del array raíz en orden
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

            // readString abre y cierra el archivo por sí mismo
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

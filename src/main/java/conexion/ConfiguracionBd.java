package conexion;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Datos de conexión leídos de config/bd.properties.
 * La contraseña no está en el código: está en ese archivo, que no se entrega.
 */
public record ConfiguracionBd(String url, String usuario, String contrasena) {

    public static final Path ARCHIVO = Path.of("config", "bd.properties");

    public static ConfiguracionBd cargar() throws ErrorConexion {
        return cargar(ARCHIVO);
    }

    public static ConfiguracionBd cargar(Path archivo) throws ErrorConexion {
        if (!Files.isRegularFile(archivo)) {
            throw new ErrorConexion("No existe " + archivo
                    + ". Copia config/bd.example.properties a config/bd.properties y pon tu contraseña.");
        }
        Properties p = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            p.load(lector);
        } catch (IOException e) {
            throw new ErrorConexion("No se puede leer " + archivo + ": " + e.getMessage(), e);
        }
        return new ConfiguracionBd(
                obligatoria(p, "url", archivo),
                obligatoria(p, "usuario", archivo),
                obligatoria(p, "contrasena", archivo));
    }

    private static String obligatoria(Properties p, String clave, Path archivo) throws ErrorConexion {
        String valor = p.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new ErrorConexion("Falta «" + clave + "» en " + archivo);
        }
        return valor.strip();
    }

    /**
     * Un record imprime TODOS sus campos en toString(). Se redefine para que
     * la contraseña no aparezca nunca si alguien imprime la configuración.
     */
    @Override
    public String toString() {
        return "ConfiguracionBd[url=" + url + ", usuario=" + usuario + ", contrasena=****]";
    }
}

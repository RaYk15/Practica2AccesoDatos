import conexion.Conexiones;
import conexion.ConfiguracionBd;
import conexion.ErrorBaseDatos;
import consola.ComandosCampeon;
import consola.ComandosImportacion;
import consola.ComandosPartida;
import consola.Menu;
import dao.CampeonDao;
import dao.PartidaDao;

import java.util.List;

/**
 * Punto de entrada de la P2.
 *   java -jar target/p2-lol.jar                         abre el menú interactivo
 *   java -jar target/p2-lol.jar <comando> [argumentos]  ejecuta un comando y termina
 *                                                       (así se repiten y documentan las pruebas)
 * Se ejecuta desde la carpeta del proyecto (lee config/bd.properties).
 */
public class App {

    public static void main(String[] args) {
        List<String> resto = List.of(args);
        String comando = resto.isEmpty() ? "" : resto.get(0);

        int codigo;
        try {
            codigo = switch (comando) {
                // Sin argumentos: menú interactivo
                case "" -> {
                    new Menu(conexiones()).iniciar();
                    yield 0;
                }
                case "conexion" -> comprobarConexion(conexiones());
                case "campeon" -> new ComandosCampeon(new CampeonDao(conexiones()))
                        .ejecutar(resto.subList(1, resto.size()));
                case "partida" -> new ComandosPartida(new PartidaDao(conexiones()))
                        .ejecutar(resto.subList(1, resto.size()));
                // Lista entera: la clase necesita saber si es "importar" o "resumenes"
                case "importar", "resumenes" -> new ComandosImportacion(conexiones()).ejecutar(resto);
                default -> ayuda();
            };
        } catch (ErrorBaseDatos e) {
            // Aquí solo llegan los errores de configuración (falta config/bd.properties o un dato)
            System.out.println("ERROR: " + e.getMessage());
            codigo = 1;
        }

        if (codigo != 0) {
            System.exit(codigo);
        }
    }

    /** Lee config/bd.properties. Cada comando abre y cierra sus propias conexiones. */
    private static Conexiones conexiones() throws ErrorBaseDatos {
        return new Conexiones(ConfiguracionBd.cargar());
    }

    private static int comprobarConexion(Conexiones conexiones) throws ErrorBaseDatos {
        System.out.println("Conectado: " + conexiones.comprobar());
        return 0;
    }

    private static int ayuda() {
        System.out.println("""
                Uso: java -jar target/p2-lol.jar             (sin argumentos: menú)
                     java -jar target/p2-lol.jar <comando>

                  conexion                                      prueba la conexión (SELECT version())

                  campeon ver <id>                              búsqueda por identidad
                  campeon listar [rol|todos] [nombre|dificultad|id]
                  campeon crear <id> <nombre> <rol> <region> <dificultad>
                  campeon modificar <id> <nombre> <rol> <region> <dificultad>
                  campeon borrar <id>
                  campeon renombrar <id> <nombre>               procedimiento almacenado
                  campeon renombrar-revertir <id> <nombre>      procedimiento + rollback

                  partida ver <servidor> <idPartida>            búsqueda por identidad
                  partida con-campeon <idCampeon>               partidas de un campeón (JOIN)
                  partida recuento                              partidas por campeón

                  importar [--entrada=CARPETA] [--fallo]        --fallo: prueba de rollback
                  resumenes                                     resúmenes de importación

                Textos con espacios o apóstrofo entre comillas dobles: "Kai'Sa".""");
        return 1;
    }
}

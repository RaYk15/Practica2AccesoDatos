import conexion.Conexiones;
import conexion.ConfiguracionBd;
import conexion.ErrorBaseDatos;
import consola.ComandosCampeon;
import consola.ComandosImportacion;
import consola.ComandosPartida;
import consola.Menu;
import dao.CampeonDao;
import dao.PartidaDao;
import embebido.LaboratorioSqlite;

import java.util.List;

/**
 * Clase principal de la P2. Se puede usar de dos formas:
 *   java -jar target/p2-lol.jar                         abre el menú
 *   java -jar target/p2-lol.jar <comando> [argumentos]  ejecuta un comando y termina
 * Los comandos los uso para las pruebas, porque cada prueba es una línea que se puede repetir.
 * Hay que ejecutarlo desde la carpeta del proyecto, porque lee config/bd.properties.
 */
public class App {

    public static void main(String[] args) {
        List<String> resto = List.of(args);
        String comando = resto.isEmpty() ? "" : resto.get(0);

        int codigo;
        try {
            codigo = switch (comando) {
                // sin argumentos: menú
                case "" -> {
                    new Menu(conexiones()).iniciar();
                    yield 0;
                }
                case "conexion" -> comprobarConexion(conexiones());
                case "campeon" -> new ComandosCampeon(new CampeonDao(conexiones()))
                        .ejecutar(resto.subList(1, resto.size()));
                case "partida" -> new ComandosPartida(new PartidaDao(conexiones()))
                        .ejecutar(resto.subList(1, resto.size()));
                // aquí paso la lista entera porque la clase tiene que saber si es "importar" o "resumenes"
                case "importar", "resumenes" -> new ComandosImportacion(conexiones()).ejecutar(resto);
                // el laboratorio no usa config/bd.properties: SQLite no tiene servidor ni contraseña
                case "laboratorio" -> new LaboratorioSqlite().ejecutar(resto.subList(1, resto.size()));
                default -> ayuda();
            };
        } catch (ErrorBaseDatos e) {
            // aquí solo llegan los errores de configuración (falta config/bd.properties o algún dato)
            System.out.println("ERROR: " + e.getMessage());
            codigo = 1;
        }

        if (codigo != 0) {
            System.exit(codigo);
        }
    }

    /** Lee config/bd.properties. Las conexiones las abre y cierra cada operación por su cuenta. */
    private static Conexiones conexiones() throws ErrorBaseDatos {
        return new Conexiones(ConfiguracionBd.cargar());
    }

    /** Prueba que se puede conectar y enseña la versión de PostgreSQL. */
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

                  laboratorio crear                             SQLite: crea lab/lol.db y la tabla
                  laboratorio insertar <id> <nombre> <rol> <region> <dificultad>
                  laboratorio listar
                  laboratorio modificar <id> <nombre> <rol> <region> <dificultad>
                  laboratorio borrar <id>

                Textos con espacios o apóstrofo entre comillas dobles: "Kai'Sa".""");
        return 1;
    }
}

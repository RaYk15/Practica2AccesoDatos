import conexion.Conexiones;
import conexion.ConfiguracionBd;
import conexion.ErrorConexion;
import consola.ComandosCampeon;
import consola.ComandosPartida;
import dao.CampeonDao;
import dao.PartidaDao;
import lectura.ErrorLectura;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        Path entrada = Path.of("entrada");
        Path salida = Path.of("salida");
        List<String> resto = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--entrada=")) {
                entrada = Path.of(arg.substring("--entrada=".length()));
            } else if (arg.startsWith("--salida=")) {
                salida = Path.of(arg.substring("--salida=".length()));
            } else {
                resto.add(arg);
            }
        }

        String comando = resto.isEmpty() ? "" : resto.get(0);
        int codigo = switch (comando) {
            case "laboratorio" -> new Laboratorio(Path.of("."), System.out).ejecutar() ? 0 : 1;
            case "importar" -> importar(entrada, salida);
            case "consultar" -> consultar(salida, resto.subList(1, resto.size()));
            case "conexion" -> comprobarConexion();
            case "campeon" -> new ComandosCampeon(new CampeonDao(conexiones)).ejecutar(resto.subList(1, resto.size()));
            case "partida" -> new ComandosPartida(new PartidaDao(conexiones)).ejecutar(resto.subList(1, resto.size()));

            default -> ayuda();
        };
        if (codigo != 0) {
            System.exit(codigo);
        }
    }

    private static int importar(Path entrada, Path salida) {
        System.out.println("Importando desde " + entrada + "/ hacia " + salida + "/");
        try {
            ResultadoImportacion r = new Importador().importar(entrada, salida);
            System.out.println("  Campeones: " + r.campeones());
            System.out.println("  Partidas:  " + r.partidas());
            if (!r.incidencias().isEmpty()) {
                System.out.println("Incidencias:");
                for (Incidencia i : r.incidencias()) {
                    System.out.println("  " + i);
                }
            }
            System.out.println("Guardado correctamente en " + salida.resolve(AlmacenEstado.ESTADO)
                    + " y " + salida.resolve(AlmacenEstado.INFORME));
            return 0;
        } catch (ErrorEstado e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (ErrorLectura e) {
            System.out.println("ERROR: " + e.getMessage());
            System.out.println("No se ha guardado nada: el estado anterior sigue igual.");
        } catch (ErrorEscritura e) {
            System.out.println("ERROR: " + e.getMessage());
            System.out.println("La importación NO se ha guardado correctamente.");
        }
        return 1;
    }

    private static int consultar(Path salida, List<String> args) {
        Path archivo = salida.resolve(AlmacenEstado.ESTADO);
        if (!Files.exists(archivo)) {
            System.out.println("No hay estado guardado todavía: se parte de un estado vacío.");
        }
        try {
            Estado estado = AlmacenEstado.cargar(archivo);
            return new Consultas(estado).ejecutar(args, System.out);
        } catch (ErrorEstado e) {
            System.out.println("ERROR: " + e.getMessage());
            return 1;
        }
    }

    private static int ayuda() {
        System.out.println("""
                Uso: java -jar target/p1-lol.jar <comando> [--entrada=CARPETA] [--salida=CARPETA]

                Comandos:
                  laboratorio   crea carpetas, lista archivos y prueba copiar/mover/borrar en trabajo/
                  importar      lee entrada/campeones.csv y entrada/partidas.json y guarda salida/
                  consultar     busca y filtra en salida/estado.json (sin importar):
                                  consultar campeon AHR
                                  consultar partida EUW P001
                                  consultar con-campeon AHR""");
        return 1;
    }

    private static int comprobarConexion() {
        try {
            Conexiones conexiones = new Conexiones(ConfiguracionBd.cargar());
            System.out.println("Conectado: " + conexiones.comprobar());
            return 0;
        } catch (ErrorConexion e) {
            System.out.println("ERROR: " + e.getMessage());
            return 1;
        }
    }
}

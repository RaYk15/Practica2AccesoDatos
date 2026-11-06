package consola;

import conexion.Conexiones;
import conexion.ErrorBaseDatos;
import dao.ResumenDao;
import importacion.Importador;
import importacion.Incidencia;
import lectura.ErrorLectura;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Comandos:
 *   importar [--entrada=CARPETA] [--fallo]   importa campeones.csv y partidas.json
 *   resumenes                                 lista los resúmenes guardados (lectura nueva)
 * --fallo es la prueba de rollback: provoca un error antes del commit.
 */
public class ComandosImportacion {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Conexiones conexiones;

    public ComandosImportacion(Conexiones conexiones) {
        this.conexiones = conexiones;
    }

    /** @param args el comando completo, empezando por "importar" o "resumenes" */
    public int ejecutar(List<String> args) {
        try {
            return switch (args.get(0)) {
                case "importar" -> importar(args.subList(1, args.size()));
                case "resumenes" -> resumenes();
                default -> ayuda();
            };
        } catch (ErrorBaseDatos e) {
            System.out.println("ERROR: " + e.getMessage());
            return 1;
        }
    }

    private int importar(List<String> opciones) throws ErrorBaseDatos {
        Path entrada = Path.of("entrada");
        boolean fallo = false;
        for (String o : opciones) {
            if (o.startsWith("--entrada=")) {
                entrada = Path.of(o.substring("--entrada=".length()));
            } else if (o.equals("--fallo")) {
                fallo = true;
            } else {
                return ayuda();
            }
        }
        System.out.println("Importando desde " + entrada + (fallo ? " (con fallo provocado antes del commit)" : ""));
        try {
            Importador.Resultado r = new Importador(conexiones).importar(entrada, fallo);
            System.out.println("  Campeones: " + r.campeones());
            System.out.println("  Partidas:  " + r.partidas());
            if (!r.incidencias().isEmpty()) {
                System.out.println("Incidencias:");
                for (Incidencia i : r.incidencias()) {
                    System.out.println("  " + i);
                }
            }
            System.out.println("Lote confirmado (commit): partidas nuevas y resumen guardados.");
            return 0;
        } catch (ErrorLectura e) {
            System.out.println("ERROR: " + e.getMessage());
            System.out.println("No se ha tocado la base de datos.");
            return 1;
        }
    }

    private int resumenes() throws ErrorBaseDatos {
        List<ResumenDao.Resumen> lista = new ResumenDao(conexiones).listar();
        if (lista.isEmpty()) {
            System.out.println("No hay resúmenes de importación");
            return 0;
        }
        System.out.println("Resúmenes de importación de partidas (procesadas: nuevas/duplicadas/conflictos/rechazadas):");
        for (ResumenDao.Resumen r : lista) {
            System.out.println("  R" + r.id() + " · " + r.momento().format(HORA) + " · " + r.procesadas() + ": "
                    + r.nuevas() + "/" + r.duplicadas() + "/" + r.conflictos() + "/" + r.rechazadas());
        }
        return 0;
    }

    private int ayuda() {
        System.out.println("""
                Uso:
                  importar [--entrada=CARPETA] [--fallo]
                  resumenes""");
        return 1;
    }
}

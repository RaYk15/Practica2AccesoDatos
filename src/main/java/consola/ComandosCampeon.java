package consola;

import conexion.ErrorBaseDatos;
import dao.CampeonDao;
import modelo.Campeon;
import validacion.Rechazo;

import java.util.List;
import java.util.Optional;

/**
 * Comando "campeon" de la consola:
 *   campeon ver <id>
 *   campeon listar [rol|todos] [nombre|dificultad|id]
 *   campeon crear <id> <nombre> <rol> <region> <dificultad>
 *   campeon modificar <id> <nombre> <rol> <region> <dificultad>
 *   campeon borrar <id>
 * Los textos con espacios o apóstrofo van entre comillas dobles: "Kai'Sa".
 */
public class ComandosCampeon {

    private final CampeonDao dao;

    public ComandosCampeon(CampeonDao dao) {
        this.dao = dao;
    }

    /** @return código de salida: 0 si va bien */
    public int ejecutar(List<String> args) {
        String accion = args.isEmpty() ? "" : args.get(0);
        try {
            switch (accion) {
                case "ver" -> { if (args.size() != 2) return ayuda(); ver(args.get(1)); }
                case "listar" -> { if (args.size() > 3) return ayuda(); listar(args); }
                case "crear" -> { if (args.size() != 6) return ayuda(); crear(leer(args)); }
                case "modificar" -> { if (args.size() != 6) return ayuda(); modificar(leer(args)); }
                case "borrar" -> { if (args.size() != 2) return ayuda(); borrar(args.get(1)); }
                default -> { return ayuda(); }
            }
            return 0;
        } catch (Rechazo r) {
            System.out.println("RECHAZADO (" + r.motivo() + "): " + r.getMessage());
        } catch (ErrorBaseDatos e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        return 1;
    }

    private void ver(String id) throws ErrorBaseDatos {
        Optional<Campeon> c = dao.buscarPorClave(id);
        if (c.isEmpty()) {
            System.out.println("No existe el campeón " + id);
        } else {
            System.out.println(formato(c.get()));
        }
    }

    private void listar(List<String> args) throws ErrorBaseDatos {
        String rol = args.size() >= 2 && !args.get(1).equalsIgnoreCase("todos") ? args.get(1) : null;
        CampeonDao.Orden orden = args.size() == 3 ? CampeonDao.Orden.desde(args.get(2)) : CampeonDao.Orden.NOMBRE;
        List<Campeon> lista = dao.listar(rol, orden);
        if (lista.isEmpty()) {
            System.out.println(rol == null ? "No hay campeones" : "Ningún campeón con rol " + rol);
            return;
        }
        System.out.println(lista.size() + " campeones" + (rol == null ? "" : " con rol " + rol)
                + ", ordenados por " + orden.name().toLowerCase() + ":");
        lista.forEach(c -> System.out.println("  " + formato(c)));
    }

    private void crear(Campeon c) throws Rechazo, ErrorBaseDatos {
        Campeon guardado = dao.crear(c);
        System.out.println("Creado: " + formato(guardado));
    }

    private void modificar(Campeon c) throws Rechazo, ErrorBaseDatos {
        int filas = dao.modificar(c);
        if (filas == 0) {
            System.out.println("No existe el campeón " + c.id() + ": no se ha modificado nada (0 filas afectadas)");
        } else {
            System.out.println("Modificado (" + filas + " fila): " + c.id());
        }
    }

    private void borrar(String id) throws ErrorBaseDatos {
        int filas = dao.borrar(id);
        System.out.println(filas == 0
                ? "No existe el campeón " + id + ": no se ha borrado nada (0 filas afectadas)"
                : "Borrado (" + filas + " fila): " + id);
    }

    /** crear/modificar <id> <nombre> <rol> <region> <dificultad> */
    private Campeon leer(List<String> args) {
        int dificultad;
        try {
            dificultad = Integer.parseInt(args.get(5).strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("la dificultad «" + args.get(5) + "» no es un número entero");
        }
        return new Campeon(args.get(1), args.get(2), args.get(3), args.get(4), dificultad);
    }

    private static String formato(Campeon c) {
        return c.id() + " · " + c.nombre() + " · " + c.rol() + " · " + c.region() + " · dificultad " + c.dificultad();
    }

    private int ayuda() {
        System.out.println("""
                Uso:
                  campeon ver <id>
                  campeon listar [rol|todos] [nombre|dificultad|id]
                  campeon crear <id> <nombre> <rol> <region> <dificultad>
                  campeon modificar <id> <nombre> <rol> <region> <dificultad>
                  campeon borrar <id>
                Textos con espacios o apóstrofo entre comillas dobles, por ejemplo "Kai'Sa".""");
        return 1;
    }
}

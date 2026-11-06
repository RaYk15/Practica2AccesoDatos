package consola;

import conexion.Conexiones;
import conexion.ErrorBaseDatos;
import dao.CampeonDao;
import dao.PartidaDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Menú interactivo. No contiene lógica propia: pide los datos por teclado
 * y llama a las mismas clases de consola que los comandos.
 * Cada dato se lee en su propia línea, así que no hacen falta comillas.
 */
public class Menu {

    /** Se lanza si se cierra la entrada estándar (Ctrl+Z / Ctrl+D): termina el programa. */
    private static class FinDeEntrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final Scanner teclado = new Scanner(System.in);
    private final Conexiones conexiones;
    private final ComandosCampeon campeones;
    private final ComandosPartida partidas;
    private final ComandosImportacion importacion;

    public Menu(Conexiones conexiones) {
        this.conexiones = conexiones;
        this.campeones = new ComandosCampeon(new CampeonDao(conexiones));
        this.partidas = new ComandosPartida(new PartidaDao(conexiones));
        this.importacion = new ComandosImportacion(conexiones);
    }

    public void iniciar() {
        try {
            while (true) {
                int opcion = elegir("ALMACÉN DE PARTIDAS DE LOL", "Salir",
                        "Comprobar conexión",
                        "Campeones",
                        "Partidas",
                        "Importación",
                        "Procedimiento almacenado");
                switch (opcion) {
                    case 0 -> {
                        System.out.println("¡Hasta luego!");
                        return;
                    }
                    case 1 -> comprobarConexion();
                    case 2 -> menuCampeones();
                    case 3 -> menuPartidas();
                    case 4 -> menuImportacion();
                    case 5 -> menuProcedimiento();
                    default -> opcionNoValida();
                }
            }
        } catch (FinDeEntrada e) {
            System.out.println();
        }
    }

    // submenús

    private void menuCampeones() {
        while (true) {
            int opcion = elegir("CAMPEONES", "Volver",
                    "Ver un campeón",
                    "Listar campeones",
                    "Crear campeón",
                    "Modificar campeón",
                    "Borrar campeón");
            switch (opcion) {
                case 0 -> { return; }
                case 1 -> campeones.ejecutar(List.of("ver", pedir("Id del campeón")));
                case 2 -> {
                    String rol = pedir("Rol (top, jungla, mid, adc, support; Intro = todos)");
                    String orden = pedir("Ordenar por (nombre, dificultad, id; Intro = nombre)");
                    campeones.ejecutar(List.of("listar", rol.isBlank() ? "todos" : rol,
                            orden.isBlank() ? "nombre" : orden));
                }
                case 3 -> campeones.ejecutar(datosCampeon("crear"));
                case 4 -> campeones.ejecutar(datosCampeon("modificar"));
                case 5 -> campeones.ejecutar(List.of("borrar", pedir("Id del campeón a borrar")));
                default -> { opcionNoValida(); continue; }
            }
            pausa();
        }
    }

    private void menuPartidas() {
        while (true) {
            int opcion = elegir("PARTIDAS", "Volver",
                    "Ver una partida",
                    "Partidas de un campeón",
                    "Recuento de partidas por campeón");
            switch (opcion) {
                case 0 -> { return; }
                case 1 -> partidas.ejecutar(List.of("ver",
                        pedir("Servidor (EUW, EUNE, NA, KR)"), pedir("Id de la partida")));
                case 2 -> partidas.ejecutar(List.of("con-campeon", pedir("Id del campeón")));
                case 3 -> partidas.ejecutar(List.of("recuento"));
                default -> { opcionNoValida(); continue; }
            }
            pausa();
        }
    }

    private void menuImportacion() {
        while (true) {
            int opcion = elegir("IMPORTACIÓN", "Volver",
                    "Importar",
                    "Importar con fallo antes del commit (prueba de rollback)",
                    "Ver resúmenes de importación");
            switch (opcion) {
                case 0 -> { return; }
                case 1, 2 -> {
                    String carpeta = pedir("Carpeta de entrada (Intro = entrada)");
                    List<String> args = new ArrayList<>(List.of("importar",
                            "--entrada=" + (carpeta.isBlank() ? "entrada" : carpeta)));
                    if (opcion == 2) {
                        args.add("--fallo");
                    }
                    importacion.ejecutar(args);
                }
                case 3 -> importacion.ejecutar(List.of("resumenes"));
                default -> { opcionNoValida(); continue; }
            }
            pausa();
        }
    }

    private void menuProcedimiento() {
        while (true) {
            int opcion = elegir("PROCEDIMIENTO ALMACENADO renombrar_campeon", "Volver",
                    "Renombrar campeón (se confirma)",
                    "Renombrar dentro de una transacción y revertir");
            switch (opcion) {
                case 0 -> { return; }
                case 1 -> campeones.ejecutar(List.of("renombrar",
                        pedir("Id del campeón"), pedir("Nombre nuevo")));
                case 2 -> campeones.ejecutar(List.of("renombrar-revertir",
                        pedir("Id del campeón"), pedir("Nombre nuevo")));
                default -> { opcionNoValida(); continue; }
            }
            pausa();
        }
    }

    // acciones sueltas

    private void comprobarConexion() {
        try {
            System.out.println("Conectado: " + conexiones.comprobar());
        } catch (ErrorBaseDatos e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        pausa();
    }

    private List<String> datosCampeon(String accion) {
        return List.of(accion,
                pedir("Id"),
                pedir("Nombre"),
                pedir("Rol (top, jungla, mid, adc, support)"),
                pedir("Región"),
                pedir("Dificultad (1-3)"));
    }

    // entrada por teclado

    /** Muestra un menú numerado (0 = salir/volver) y devuelve la opción, o -1 si no es un número. */
    private int elegir(String titulo, String opcionCero, String... opciones) {
        System.out.println();
        System.out.println("=== " + titulo + " ===");
        for (int i = 0; i < opciones.length; i++) {
            System.out.println("  " + (i + 1) + ". " + opciones[i]);
        }
        System.out.println("  0. " + opcionCero);
        String texto = pedir("Opción");
        try {
            int n = Integer.parseInt(texto.strip());
            return n >= 0 && n <= opciones.length ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String pedir(String etiqueta) {
        System.out.print(etiqueta + ": ");
        if (!teclado.hasNextLine()) {
            throw new FinDeEntrada();
        }
        return teclado.nextLine();
    }

    private void pausa() {
        System.out.print("(Intro para continuar)");
        if (!teclado.hasNextLine()) {
            throw new FinDeEntrada();
        }
        teclado.nextLine();
    }

    private void opcionNoValida() {
        System.out.println("Opción no válida");
    }
}

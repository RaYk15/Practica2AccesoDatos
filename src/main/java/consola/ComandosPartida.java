package consola;

import conexion.ErrorBaseDatos;
import dao.PartidaDao;
import modelo.Participante;
import modelo.Partida;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Comando "partida" de la consola:
 *   partida ver <servidor> <idPartida>     búsqueda por identidad
 *   partida con-campeon <idCampeon>        partidas en las que se jugó un campeón (JOIN)
 *   partida recuento                       nº de partidas de cada campeón
 */
public class ComandosPartida {

    private final PartidaDao dao;

    public ComandosPartida(PartidaDao dao) {
        this.dao = dao;
    }

    public int ejecutar(List<String> args) {
        String accion = args.isEmpty() ? "" : args.get(0);
        try {
            switch (accion) {
                case "ver" -> { if (args.size() != 3) return ayuda(); ver(args.get(1), args.get(2)); }
                case "con-campeon" -> { if (args.size() != 2) return ayuda(); conCampeon(args.get(1)); }
                case "recuento" -> { if (args.size() != 1) return ayuda(); recuento(); }
                default -> { return ayuda(); }
            }
            return 0;
        } catch (ErrorBaseDatos e) {
            System.out.println("ERROR: " + e.getMessage());
            return 1;
        }
    }

    private void ver(String servidor, String id) throws ErrorBaseDatos {
        Optional<Partida> encontrada = dao.buscarPorClave(servidor, id);
        if (encontrada.isEmpty()) {
            System.out.println("No existe la partida " + Partida.clave(servidor, id));
            return;
        }
        Partida p = encontrada.get();
        System.out.println(p.clave() + " · " + p.fecha() + " · " + p.duracionMin() + " min · gana " + p.ganador());
        for (String color : List.of("azul", "rojo")) {
            System.out.println("  Equipo " + color + (p.ganador().equals(color) ? " (ganador)" : ""));
            for (Participante j : p.equipo(color)) {
                System.out.printf(Locale.ROOT, "    %-10s %-4s %2d/%2d/%2d   KDA %.2f%n", j.jugador(), j.campeonId(),
                        j.kills(), j.deaths(), j.assists(), j.kda());
            }
        }
    }

    private void conCampeon(String idCampeon) throws ErrorBaseDatos {
        List<PartidaDao.PartidaDeCampeon> lista = dao.partidasDeCampeon(idCampeon);
        if (lista.isEmpty()) {
            System.out.println("Ninguna partida con " + idCampeon);
            return;
        }
        System.out.println(lista.size() + " partidas con " + idCampeon + ":");
        for (PartidaDao.PartidaDeCampeon f : lista) {
            System.out.println("  " + f.clave() + " · " + f.fecha() + " · " + f.jugador() + " (" + f.equipo() + ") · "
                    + f.kills() + "/" + f.deaths() + "/" + f.assists() + " · " + (f.victoria() ? "victoria" : "derrota"));
        }
    }

    private void recuento() throws ErrorBaseDatos {
        List<PartidaDao.Recuento> lista = dao.recuentoPorCampeon();
        if (lista.isEmpty()) {
            System.out.println("No hay campeones");
            return;
        }
        System.out.println("Partidas por campeón:");
        lista.forEach(r -> System.out.printf("  %-4s %-18s %d%n", r.idCampeon(), r.nombre(), r.partidas()));
    }

    private int ayuda() {
        System.out.println("""
                Uso:
                  partida ver <servidor> <idPartida>   p. ej. partida ver EUW P001
                  partida con-campeon <idCampeon>      p. ej. partida con-campeon LUL
                  partida recuento""");
        return 1;
    }
}

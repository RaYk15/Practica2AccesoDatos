package dao;

import conexion.Conexiones;
import conexion.ErrorBaseDatos;
import modelo.Participante;
import modelo.Partida;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a las tablas partida y participante.
 *
 * Una partida se guarda en DOS tablas: una fila en partida y diez en participante.
 * Por eso insertar() recibe la conexión de fuera: quien la llama (la importación)
 * controla la transacción y decide si se confirma o se revierte todo junto.
 */
public class PartidaDao {

    /** Una fila de la consulta "partidas en las que se jugó un campeón". */
    public record PartidaDeCampeon(String clave, String fecha, String equipo, String jugador,
                                   int kills, int deaths, int assists, boolean victoria) {
    }

    /** Una fila del recuento de partidas por campeón. */
    public record Recuento(String idCampeon, String nombre, long partidas) {
    }

    private final Conexiones conexiones;

    public PartidaDao(Conexiones conexiones) {
        this.conexiones = conexiones;
    }

    // ---------- Insertar (dentro de una transacción ajena) ----------

    /**
     * Inserta la partida y sus diez participantes usando la conexión recibida.
     * No hace commit ni cierra la conexión: eso es responsabilidad de quien la llama.
     * Si algo falla, lanza SQLException para que el llamante haga rollback.
     */
    public void insertar(Connection con, Partida p) throws SQLException {
        String sqlPartida = "INSERT INTO partida (servidor, id_partida, fecha, duracion_min, ganador) "
                + "VALUES (?, ?, ?, ?, ?)";
        int idInterno;
        // new String[]{"id"}: pide a la base que devuelva el id generado
        try (PreparedStatement ps = con.prepareStatement(sqlPartida, new String[]{"id"})) {
            ps.setString(1, p.servidor());
            ps.setString(2, p.idPartida());
            ps.setObject(3, LocalDate.parse(p.fecha()));   // texto "2026-10-01" -> DATE
            ps.setInt(4, p.duracionMin());
            ps.setString(5, p.ganador());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                idInterno = claves.getInt(1);
            }
        }

        String sqlParticipante = "INSERT INTO participante "
                + "(partida_id, equipo, posicion, jugador, campeon_id, kills, deaths, assists) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlParticipante)) {
            for (String color : List.of("azul", "rojo")) {
                List<Participante> equipo = p.equipo(color);
                for (int i = 0; i < equipo.size(); i++) {
                    Participante j = equipo.get(i);
                    ps.setInt(1, idInterno);
                    ps.setString(2, color);
                    ps.setInt(3, i + 1);                    // posición 1..5: conserva el orden
                    ps.setString(4, j.jugador());
                    ps.setString(5, j.campeonId());
                    ps.setInt(6, j.kills());
                    ps.setInt(7, j.deaths());
                    ps.setInt(8, j.assists());
                    ps.addBatch();                          // se envían las 10 de una vez
                }
            }
            ps.executeBatch();
        }
    }

    // ---------- Buscar por identidad ----------

    /** Abre su propia conexión. Sin resultados devuelve Optional.empty(). */
    public Optional<Partida> buscarPorClave(String servidor, String idPartida) throws ErrorBaseDatos {
        try (Connection con = conexiones.abrir()) {
            return buscarPorClave(con, servidor, idPartida);
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "la búsqueda de la partida " + Partida.clave(servidor, idPartida));
        }
    }

    /** Versión con conexión ajena, para usarla dentro de una transacción (importación). */
    public Optional<Partida> buscarPorClave(Connection con, String servidor, String idPartida) throws SQLException {
        String sqlPartida = "SELECT id, fecha, duracion_min, ganador FROM partida "
                + "WHERE servidor = ? AND id_partida = ?";
        int idInterno;
        String fecha;
        int duracion;
        String ganador;
        try (PreparedStatement ps = con.prepareStatement(sqlPartida)) {
            ps.setString(1, servidor);
            ps.setString(2, idPartida);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                idInterno = rs.getInt("id");
                fecha = rs.getObject("fecha", LocalDate.class).toString();   // DATE -> "2026-10-01"
                duracion = rs.getInt("duracion_min");
                ganador = rs.getString("ganador");
            }
        }

        List<Participante> azul = new ArrayList<>();
        List<Participante> rojo = new ArrayList<>();
        String sqlParticipantes = "SELECT equipo, jugador, campeon_id, kills, deaths, assists FROM participante "
                + "WHERE partida_id = ? ORDER BY equipo, posicion";
        try (PreparedStatement ps = con.prepareStatement(sqlParticipantes)) {
            ps.setInt(1, idInterno);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Participante j = new Participante(rs.getString("jugador"), rs.getString("campeon_id"),
                            rs.getInt("kills"), rs.getInt("deaths"), rs.getInt("assists"));
                    (rs.getString("equipo").equals("azul") ? azul : rojo).add(j);
                }
            }
        }
        return Optional.of(new Partida(servidor, idPartida, fecha, duracion, ganador, azul, rojo));
    }

    // ---------- Consulta que relaciona A con B ----------

    /** Partidas en las que se jugó un campeón, por fecha. Lista vacía si ninguna. */
    public List<PartidaDeCampeon> partidasDeCampeon(String idCampeon) throws ErrorBaseDatos {
        String sql = "SELECT p.servidor, p.id_partida, p.fecha, pa.equipo, pa.jugador, "
                + "       pa.kills, pa.deaths, pa.assists, (pa.equipo = p.ganador) AS victoria "
                + "FROM participante pa "
                + "JOIN partida p ON p.id = pa.partida_id "
                + "WHERE pa.campeon_id = ? "
                + "ORDER BY p.fecha, p.servidor, p.id_partida";
        try (Connection con = conexiones.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idCampeon);
            List<PartidaDeCampeon> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PartidaDeCampeon(
                            Partida.clave(rs.getString("servidor"), rs.getString("id_partida")),
                            rs.getObject("fecha", LocalDate.class).toString(),
                            rs.getString("equipo"), rs.getString("jugador"),
                            rs.getInt("kills"), rs.getInt("deaths"), rs.getInt("assists"),
                            rs.getBoolean("victoria")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "las partidas del campeón " + idCampeon);
        }
    }

    /**
     * Recuento de partidas por campeón. LEFT JOIN para que también salgan
     * los campeones con 0 partidas (con JOIN normal desaparecerían).
     */
    public List<Recuento> recuentoPorCampeon() throws ErrorBaseDatos {
        String sql = "SELECT c.id_campeon, c.nombre, COUNT(pa.partida_id) AS partidas "
                + "FROM campeon c "
                + "LEFT JOIN participante pa ON pa.campeon_id = c.id_campeon "
                + "GROUP BY c.id_campeon, c.nombre "
                + "ORDER BY partidas DESC, c.id_campeon";
        try (Connection con = conexiones.abrir();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Recuento> lista = new ArrayList<>();
            while (rs.next()) {
                // COUNT devuelve BIGINT en PostgreSQL: se lee como long
                lista.add(new Recuento(rs.getString("id_campeon"), rs.getString("nombre"), rs.getLong("partidas")));
            }
            return lista;
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "el recuento de partidas");
        }
    }
}

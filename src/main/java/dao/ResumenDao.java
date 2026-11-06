package dao;

import conexion.Conexiones;
import conexion.ErrorBaseDatos;
import importacion.Contador;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla resumen_importacion. */
public class ResumenDao {

    /** Una fila de resumen_importacion. */
    public record Resumen(int id, OffsetDateTime momento, int procesadas, int nuevas,
                          int duplicadas, int conflictos, int rechazadas) {
    }

    private final Conexiones conexiones;

    public ResumenDao(Conexiones conexiones) {
        this.conexiones = conexiones;
    }

    /** Usa la conexión de la transacción del lote: no hace commit. */
    public void insertar(Connection con, Contador c) throws SQLException {
        String sql = "INSERT INTO resumen_importacion (procesadas, nuevas, duplicadas, conflictos, rechazadas) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, c.getProcesados());
            ps.setInt(2, c.getNuevos());
            ps.setInt(3, c.getDuplicados());
            ps.setInt(4, c.getConflictos());
            ps.setInt(5, c.getRechazados());
            ps.executeUpdate();
        }
    }

    /** Todos los resúmenes, del más antiguo al más reciente. Abre su propia conexión. */
    public List<Resumen> listar() throws ErrorBaseDatos {
        String sql = "SELECT id, momento, procesadas, nuevas, duplicadas, conflictos, rechazadas "
                + "FROM resumen_importacion ORDER BY id";
        try (Connection con = conexiones.abrir();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Resumen> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new Resumen(rs.getInt("id"), rs.getObject("momento", OffsetDateTime.class),
                        rs.getInt("procesadas"), rs.getInt("nuevas"), rs.getInt("duplicadas"),
                        rs.getInt("conflictos"), rs.getInt("rechazadas")));
            }
            return lista;
        } catch (SQLException e) {
            throw ErrorBaseDatos.desde(e, "el listado de resúmenes");
        }
    }
}

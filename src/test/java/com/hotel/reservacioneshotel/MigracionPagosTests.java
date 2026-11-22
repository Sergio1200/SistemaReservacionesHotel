package com.hotel.reservacioneshotel;

import java.sql.SQLException;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigracionPagosTests {

    @Test
    void v3ConservaPagosExistentesYEvitaReutilizarUnaClaveEnLaMismaReserva() throws SQLException {
        var datos = new DriverManagerDataSource(
                "jdbc:h2:mem:pagos_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        try (var conexion = datos.getConnection()) {
            JdbcTemplate sql = new JdbcTemplate(datos);
            Flyway.configure().dataSource(datos).target(MigrationVersion.fromVersion("2")).load().migrate();
            sql.update("INSERT INTO huespedes (id_huesped, nombre, correo) VALUES (1, 'Legado QA', 'legado@example.test')");
            sql.update("INSERT INTO tipos_habitacion (id_tipo, nombre, capacidad) VALUES (1, 'Doble QA', 2)");
            sql.update("""
                    INSERT INTO habitaciones (id_habitacion, numero, piso, estado_operativo, precio_por_noche, id_tipo)
                    VALUES (1, 'QA-101', 1, 'DISPONIBLE', 100, 1)
                    """);
            sql.update("""
                    INSERT INTO reservaciones (id_reservacion, id_huesped, id_habitacion, fecha_entrada, fecha_salida,
                    numero_huespedes, precio_noche_aplicado, costo_total, estado)
                    VALUES (1, 1, 1, '2027-05-01', '2027-05-02', 1, 100, 100, 'PENDIENTE')
                    """);
            sql.update("""
                    INSERT INTO pagos (id_reservacion, fecha, monto, metodo_pago, estado) VALUES
                    (1, CURRENT_TIMESTAMP, 30.25, 'SIMULADO', 'PAGADO'),
                    (1, CURRENT_TIMESTAMP, 20.75, 'SIMULADO', 'RECHAZADO')
                    """);
            var historial = sql.queryForList("SELECT id_pago, monto, estado, metodo_pago FROM pagos ORDER BY id_pago");

            Flyway.configure().dataSource(datos).load().migrate();
            assertThat(sql.queryForList("SELECT id_pago, monto, estado, metodo_pago FROM pagos ORDER BY id_pago")).isEqualTo(historial);
            assertThat(sql.queryForObject("SELECT COUNT(*) FROM pagos WHERE clave_intento IS NULL", Integer.class)).isEqualTo(2);

            String insertar = """
                    INSERT INTO pagos (id_reservacion, fecha, monto, metodo_pago, estado, clave_intento)
                    VALUES (1, CURRENT_TIMESTAMP, 10, 'EFECTIVO', 'PENDIENTE', ?)
                    """;
            String clave = UUID.randomUUID().toString();
            sql.update(insertar, clave);
            assertThatThrownBy(() -> sql.update(insertar, clave)).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(sql.queryForObject("SELECT COUNT(*) FROM pagos", Integer.class)).isEqualTo(3);
        }
    }
}

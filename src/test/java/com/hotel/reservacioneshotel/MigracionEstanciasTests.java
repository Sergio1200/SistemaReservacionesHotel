package com.hotel.reservacioneshotel;

import java.sql.SQLException;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MigracionEstanciasTests {

    @Test
    void v4DejaNulasLasHorasHistoricasYConservaEstanciasYPagos() throws SQLException {
        var datos = new DriverManagerDataSource(
                "jdbc:h2:mem:estancias_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        try (var conexion = datos.getConnection()) {
            JdbcTemplate sql = new JdbcTemplate(datos);
            Flyway.configure().dataSource(datos).target(MigrationVersion.fromVersion("3")).load().migrate();
            sql.update("INSERT INTO huespedes (id_huesped, nombre, correo) VALUES (1, 'Legado QA', 'legado@example.test')");
            sql.update("INSERT INTO tipos_habitacion (id_tipo, nombre, capacidad) VALUES (1, 'Sencilla QA', 1)");
            sql.update("""
                    INSERT INTO habitaciones (id_habitacion, numero, piso, estado_operativo, precio_por_noche, id_tipo)
                    VALUES (1, 'QA-101', 1, 'OCUPADA', 100, 1)
                    """);
            sql.update("""
                    INSERT INTO reservaciones (id_reservacion, id_huesped, id_habitacion, fecha_entrada, fecha_salida,
                        numero_huespedes, precio_noche_aplicado, costo_total, estado)
                    VALUES (1, 1, 1, '2027-02-01', '2027-02-02', 1, 90, 90, 'FINALIZADA'),
                           (2, 1, 1, '2027-02-02', '2027-02-03', 1, 100, 100, 'EN_CURSO')
                    """);
            sql.update("""
                    INSERT INTO pagos (id_reservacion, fecha, monto, metodo_pago, estado, clave_intento)
                    VALUES (1, '2027-01-15 14:00:00', 90, 'EFECTIVO', 'PAGADO', ?)
                    """, UUID.randomUUID().toString());
            String reservas = """
                    SELECT id_reservacion, id_huesped, id_habitacion, fecha_entrada, fecha_salida, numero_huespedes,
                        precio_noche_aplicado, costo_total, estado FROM reservaciones ORDER BY id_reservacion
                    """;
            var historial = sql.queryForList(reservas);
            var pagos = sql.queryForList("SELECT * FROM pagos");
            var habitaciones = sql.queryForList("SELECT * FROM habitaciones");

            var migracion = Flyway.configure().dataSource(datos).load().migrate();
            assertThat(migracion.migrationsExecuted).isEqualTo(1);
            assertThat(migracion.targetSchemaVersion).isEqualTo("4");
            assertThat(sql.queryForList(reservas)).isEqualTo(historial);
            assertThat(sql.queryForList("SELECT * FROM pagos")).isEqualTo(pagos);
            assertThat(sql.queryForList("SELECT * FROM habitaciones")).isEqualTo(habitaciones);
            assertThat(sql.queryForObject("""
                    SELECT COUNT(*) FROM reservaciones WHERE fecha_hora_check_in IS NULL AND fecha_hora_check_out IS NULL
                    """, Integer.class)).isEqualTo(2);
        }
    }
}

package com.hotel.reservacioneshotel;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MigracionCuentasTests {

    @Test
    void v2ConservaHuespedesYEmpleadosDeV1YReservaCorreosSinDarAcceso() throws Exception {
        var datos = new DriverManagerDataSource(
                "jdbc:h2:mem:migracion_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        try (var conexion = datos.getConnection()) {
            JdbcTemplate sql = new JdbcTemplate(datos);
            Flyway.configure().dataSource(datos).target(MigrationVersion.fromVersion("1")).load().migrate();
            sql.update("INSERT INTO huespedes (nombre, correo) VALUES (?, ?), (?, ?), (?, ?)",
                    "Huésped existente", "Anterior@example.test",
                    "Registro A", "Duplicado@example.test", "Registro B", "duplicado@example.test");
            sql.update("INSERT INTO empleados (nombre, usuario, contrasena_hash, activo, rol) VALUES (?, ?, ?, ?, ?)",
                    "Empleado existente", "empleado_previo", "$2b$12$" + ".".repeat(53), true, "RECEPCIONISTA");

            Flyway.configure().dataSource(datos).load().migrate();

            assertThat(sql.queryForObject("SELECT COUNT(*) FROM huespedes", Integer.class)).isEqualTo(3);
            assertThat(sql.queryForObject("SELECT correo FROM huespedes WHERE nombre = ?", String.class, "Huésped existente"))
                    .isEqualTo("Anterior@example.test");
            assertThat(sql.queryForObject("SELECT COUNT(*) FROM empleados WHERE usuario = ?", Integer.class, "empleado_previo"))
                    .isEqualTo(1);
            assertThat(sql.queryForObject("SELECT correo FROM cuentas", String.class)).isEqualTo("anterior@example.test");
            assertThat(sql.queryForObject("SELECT contrasena_hash FROM cuentas", String.class)).isNull();
        }
    }
}

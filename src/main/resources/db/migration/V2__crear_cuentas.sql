CREATE TABLE cuentas (
    id_cuenta BIGINT NOT NULL AUTO_INCREMENT,
    correo VARCHAR(254) NOT NULL,
    contrasena_hash VARCHAR(255),
    id_huesped BIGINT,
    id_empleado BIGINT,
    PRIMARY KEY (id_cuenta),
    CONSTRAINT uk_cuentas_correo UNIQUE (correo),
    CONSTRAINT uk_cuentas_huesped UNIQUE (id_huesped),
    CONSTRAINT uk_cuentas_empleado UNIQUE (id_empleado),
    CONSTRAINT fk_cuentas_huesped FOREIGN KEY (id_huesped) REFERENCES huespedes (id_huesped),
    CONSTRAINT fk_cuentas_empleado FOREIGN KEY (id_empleado) REFERENCES empleados (id_empleado),
    CONSTRAINT ck_cuentas_titular CHECK (
        (id_huesped IS NOT NULL AND id_empleado IS NULL)
        OR (id_huesped IS NULL AND id_empleado IS NOT NULL AND contrasena_hash IS NULL)
    )
) ENGINE=InnoDB;

-- Reserva correos históricos sin conceder acceso hasta asignar una contraseña verificada.
INSERT INTO cuentas (correo, id_huesped)
SELECT LOWER(TRIM(correo)), MIN(id_huesped)
FROM huespedes
WHERE TRIM(correo) <> ''
GROUP BY LOWER(TRIM(correo))
HAVING COUNT(*) = 1;

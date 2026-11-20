CREATE TABLE huespedes (
    id_huesped BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    telefono VARCHAR(32),
    correo VARCHAR(254) NOT NULL,
    PRIMARY KEY (id_huesped),
    CONSTRAINT uk_huespedes_correo UNIQUE (correo)
) ENGINE=InnoDB;

CREATE TABLE empleados (
    id_empleado BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    usuario VARCHAR(80) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL,
    rol VARCHAR(16) NOT NULL,
    PRIMARY KEY (id_empleado),
    CONSTRAINT uk_empleados_usuario UNIQUE (usuario),
    CONSTRAINT ck_empleados_rol CHECK (rol IN ('RECEPCIONISTA', 'ADMINISTRADOR'))
) ENGINE=InnoDB;

CREATE TABLE tipos_habitacion (
    id_tipo BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    capacidad INT NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (id_tipo),
    CONSTRAINT uk_tipos_habitacion_nombre UNIQUE (nombre),
    CONSTRAINT ck_tipos_habitacion_capacidad CHECK (capacidad > 0)
) ENGINE=InnoDB;

CREATE TABLE habitaciones (
    id_habitacion BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(20) NOT NULL,
    piso INT NOT NULL,
    estado_operativo VARCHAR(16) NOT NULL,
    precio_por_noche DECIMAL(12, 2) NOT NULL,
    id_tipo BIGINT NOT NULL,
    PRIMARY KEY (id_habitacion),
    CONSTRAINT uk_habitaciones_numero UNIQUE (numero),
    CONSTRAINT ck_habitaciones_estado CHECK (estado_operativo IN ('DISPONIBLE', 'OCUPADA')),
    CONSTRAINT ck_habitaciones_precio CHECK (precio_por_noche >= 0),
    CONSTRAINT fk_habitaciones_tipo FOREIGN KEY (id_tipo) REFERENCES tipos_habitacion (id_tipo)
) ENGINE=InnoDB;

CREATE TABLE reservaciones (
    id_reservacion BIGINT NOT NULL AUTO_INCREMENT,
    id_huesped BIGINT NOT NULL,
    id_habitacion BIGINT NOT NULL,
    fecha_entrada DATE NOT NULL,
    fecha_salida DATE NOT NULL,
    numero_huespedes INT NOT NULL,
    precio_noche_aplicado DECIMAL(12, 2) NOT NULL,
    costo_total DECIMAL(12, 2) NOT NULL,
    estado VARCHAR(16) NOT NULL,
    PRIMARY KEY (id_reservacion),
    CONSTRAINT ck_reservaciones_fechas CHECK (fecha_salida > fecha_entrada),
    CONSTRAINT ck_reservaciones_numero_huespedes CHECK (numero_huespedes > 0),
    CONSTRAINT ck_reservaciones_importes CHECK (precio_noche_aplicado >= 0 AND costo_total >= 0),
    CONSTRAINT ck_reservaciones_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'EN_CURSO', 'FINALIZADA')),
    CONSTRAINT fk_reservaciones_huesped FOREIGN KEY (id_huesped) REFERENCES huespedes (id_huesped),
    CONSTRAINT fk_reservaciones_habitacion FOREIGN KEY (id_habitacion) REFERENCES habitaciones (id_habitacion)
) ENGINE=InnoDB;

CREATE TABLE pagos (
    id_pago BIGINT NOT NULL AUTO_INCREMENT,
    id_reservacion BIGINT NOT NULL,
    fecha DATETIME(6) NOT NULL,
    monto DECIMAL(12, 2) NOT NULL,
    metodo_pago VARCHAR(40) NOT NULL,
    estado VARCHAR(16) NOT NULL,
    PRIMARY KEY (id_pago),
    CONSTRAINT ck_pagos_monto CHECK (monto > 0),
    CONSTRAINT ck_pagos_estado CHECK (estado IN ('PENDIENTE', 'PROCESANDO', 'PAGADO', 'RECHAZADO')),
    CONSTRAINT fk_pagos_reservacion FOREIGN KEY (id_reservacion) REFERENCES reservaciones (id_reservacion)
) ENGINE=InnoDB;

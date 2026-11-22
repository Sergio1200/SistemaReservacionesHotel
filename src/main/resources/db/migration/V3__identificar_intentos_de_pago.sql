ALTER TABLE pagos ADD COLUMN clave_intento VARCHAR(36);

ALTER TABLE pagos ADD CONSTRAINT uk_pagos_reservacion_intento UNIQUE (id_reservacion, clave_intento);

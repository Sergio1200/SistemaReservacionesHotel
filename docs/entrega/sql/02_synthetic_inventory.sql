-- Opt in only when this same connection points to an isolated demonstration instance.
-- Run: SET @hotel_demo_scope = 'ISOLATED_CLASSROOM';
-- Execute only after Flyway V1 through V4 have been applied.

START TRANSACTION;

INSERT INTO tipos_habitacion (nombre, capacidad, descripcion)
SELECT 'ENTREGA Demo Double', 2, 'Synthetic inventory for the academic delivery'
WHERE DATABASE() = 'hotel_reservaciones'
  AND @hotel_demo_scope = 'ISOLATED_CLASSROOM'
  AND (SELECT COUNT(DISTINCT version) FROM flyway_schema_history
       WHERE success = 1 AND version IN ('1', '2', '3', '4')) = 4
  AND NOT EXISTS (SELECT 1 FROM tipos_habitacion WHERE nombre = 'ENTREGA Demo Double');

INSERT INTO habitaciones (numero, piso, estado_operativo, precio_por_noche, id_tipo)
SELECT 'ENTREGA-201', 2, 'DISPONIBLE', 850.75, t.id_tipo
FROM tipos_habitacion t
WHERE DATABASE() = 'hotel_reservaciones'
  AND @hotel_demo_scope = 'ISOLATED_CLASSROOM'
  AND (SELECT COUNT(DISTINCT version) FROM flyway_schema_history
       WHERE success = 1 AND version IN ('1', '2', '3', '4')) = 4
  AND t.nombre = 'ENTREGA Demo Double'
  AND t.capacidad = 2
  AND t.descripcion = 'Synthetic inventory for the academic delivery'
  AND NOT EXISTS (SELECT 1 FROM habitaciones WHERE numero = 'ENTREGA-201');

COMMIT;

SET @hotel_demo_scope = NULL;

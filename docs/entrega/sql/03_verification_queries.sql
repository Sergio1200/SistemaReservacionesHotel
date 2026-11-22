-- Run on the isolated hotel_reservaciones schema after application startup.
-- These queries list no contact information or credential fields.

SELECT version, description, success
FROM flyway_schema_history
WHERE version IS NOT NULL
ORDER BY installed_rank;

SELECT table_name, column_name, referenced_table_name, referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL
ORDER BY table_name, column_name;

SELECT h.numero, t.nombre AS room_type, t.capacidad, h.estado_operativo, h.precio_por_noche
FROM habitaciones h
JOIN tipos_habitacion t ON h.id_tipo = t.id_tipo
WHERE h.numero = 'ENTREGA-201';

SELECT r.id_reservacion, h.numero, r.estado, r.fecha_entrada, r.fecha_salida,
       r.fecha_hora_check_in, r.fecha_hora_check_out, r.precio_noche_aplicado, r.costo_total
FROM reservaciones r
JOIN habitaciones h ON r.id_habitacion = h.id_habitacion
WHERE h.numero IN ('ENTREGA-201', 'LIVE-201')
ORDER BY r.id_reservacion;

SELECT p.id_pago, p.id_reservacion, p.fecha, p.monto, p.metodo_pago, p.estado
FROM pagos p
JOIN reservaciones r ON p.id_reservacion = r.id_reservacion
JOIN habitaciones h ON r.id_habitacion = h.id_habitacion
WHERE h.numero IN ('ENTREGA-201', 'LIVE-201')
ORDER BY p.id_reservacion, p.id_pago;

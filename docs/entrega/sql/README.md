# SQL delivery resources

Use a separate local MySQL instance for the demonstration and tests. The connection must point to that instance before opening any script. The application expects the schema `hotel_reservaciones`.

1. Run the repository's existing [schema creation script](../../../database/crear_base_datos.sql). It creates an empty schema. Use an administrative client for this initial step.
2. Assign a separate application account on that instance through MySQL Workbench or the MySQL client. For an already created local account, the following are the grants used for normal app data operations and these migrations. Replace the example account and match its actual local host.

```sql
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON hotel_reservaciones.* TO 'hotel_app'@'127.0.0.1';
```

3. Configure the application's connection and start it once. Flyway applies the existing migrations under [src/main/resources/db/migration](../../../src/main/resources/db/migration) and records their order in `flyway_schema_history`.

| Version | File | Purpose |
| --- | --- | --- |
| V1 | `V1__crear_tablas_del_hotel.sql` | Establishes guests, employees, room types, rooms, reservations and payments. |
| V2 | `V2__crear_cuentas.sql` | Adds the account table, owner constraints and reserved legacy guest addresses. |
| V3 | `V3__identificar_intentos_de_pago.sql` | Adds the optional attempt key and unique pair per reservation. |
| V4 | `V4__registrar_entrada_y_salida.sql` | Adds nullable columns for actual check-in and check-out. |

4. Run [02_synthetic_inventory.sql](02_synthetic_inventory.sql) only on the isolated demo instance and after V1–V4. It inserts an optional room type and available room with an `ENTREGA-` identifier. It requires an explicit session opt-in, preserves existing rows, and leaves matching identifiers alone.
5. Create employees via the existing `demo` profile and register a fictitious guest via `/registro`. See the [installation manual](../manual_instalacion.md). The SQL data script contains no passwords, hashes or guest records.
6. Execute [03_verification_queries.sql](03_verification_queries.sql) on the same isolated instance. The queries use **SELECT** only and do not print credentials, hashes, names, email addresses or phone numbers.

## Relationships

`habitaciones.id_tipo` references `tipos_habitacion.id_tipo`. Each reservation links a guest and a room using `reservaciones.id_huesped` and `reservaciones.id_habitacion`. Each payment links a reservation using `pagos.id_reservacion`. The account table has two individually unique, optional owner keys, `id_huesped` and `id_empleado`, with a constraint requiring exactly one owner. These are the six foreign keys examined in MySQL.

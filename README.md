# ReservacionesHotel

Sistema de Gestión de Reservaciones de Hotel para la materia Paradigmas de Programación I. La aplicación ofrece registro de huéspedes, autenticación, gestión de huéspedes e inventario, consulta pública de disponibilidad y reservaciones para huéspedes y recepción.

## Autor y contexto académico

- **Desarrollador:** Sergio Garduño Arriaga.
- **Grupo:** ICO 30.
- **Unidad de Aprendizaje:** Paradigmas de Programación I.
- **Institución:** Centro Universitario UAEMéx Atlacomulco.
- **Periodo escolar:** 2026B.

La página de inicio y la sección `/acerca-de` muestran los logotipos institucionales. El pie de página compartido identifica la autoría en todas las vistas.

## Tecnologías

- Java 21 y Spring Boot 4.2.0 estable.
- Spring Web MVC con Tomcat embebido, Thymeleaf para las vistas y Jakarta Validation para validación.
- Bootstrap 5.3.9 desde jsDelivr y estilos propios. El navegador necesita acceso al CDN para cargar Bootstrap.
- Apache Maven 3.9.16 mediante Maven Wrapper 3.3.4, con empaquetado como JAR ejecutable.
- Spring Data JPA con Hibernate, MySQL Connector/J y Flyway para aplicar migraciones SQL versionadas.
- Spring Security para el acceso por roles, la protección CSRF y la autenticación con BCrypt y sesiones de servidor.
- JUnit Jupiter y MockMvc para las pruebas.
- H2 en memoria, disponible solo en las pruebas.

Las coordenadas Maven son `com.hotel:reservacioneshotel:0.1.0` y el paquete base es `com.hotel.reservacioneshotel`.

## Requisitos

- JDK 21. Configura `JAVA_HOME` con su ruta y agrega el directorio `bin` al `PATH`.
- Conexión a Maven Central para la primera descarga de Maven y sus dependencias.
- MySQL Server 8.x (8.0.16 o posterior) para ejecutar la aplicación. Las pruebas habituales funcionan con H2 en memoria.
- Apache NetBeans con soporte para Java 21 y proyectos Maven, si se trabaja desde el IDE.

## Preparar MySQL

1. Conéctate a MySQL con una cuenta con permiso para crear bases de datos. Ejecuta [database/crear_base_datos.sql](database/crear_base_datos.sql) en MySQL Workbench o en el cliente de MySQL. El script crea `hotel_reservaciones` si aún no existe.
2. Usa una cuenta de desarrollo con permisos de lectura, escritura y creación de tablas en ese esquema. Consulta [`.env.example`](.env.example) para ver las variables de entorno. `HOTEL_DB_HOST` y `HOTEL_DB_PORT` tienen como valores predeterminados `127.0.0.1` y `3306`. Proporciona `HOTEL_DB_USER` y `HOTEL_DB_PASSWORD` en el entorno del proceso. Spring Boot no carga `.env` automáticamente.
3. En PowerShell, puedes capturar ambos valores sin escribir la contraseña en el historial de comandos.

   ```powershell
   $credencial = Get-Credential -Message 'Cuenta MySQL de desarrollo'
   $env:HOTEL_DB_USER = $credencial.UserName
   $env:HOTEL_DB_PASSWORD = $credencial.GetNetworkCredential().Password
   ```

Al arrancar, Flyway aplica las migraciones de `src/main/resources/db/migration/` y registra las versiones en `flyway_schema_history`. Hibernate usa `ddl-auto=validate` para comprobar el mapeo. Si el esquema ya contiene tablas de otro origen y carece de historial de Flyway, revisa esas tablas antes de migrar. Conserva las migraciones ya aplicadas, agrega una nueva versión para cada cambio y respalda los datos antes de migrar una base que quieras conservar.

## Ejecutar con Maven

Desde la carpeta que contiene `pom.xml`, en PowerShell. Configura las variables de MySQL en esa misma terminal antes de ejecutar `spring-boot:run`.

```powershell
.\mvnw.cmd --version
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

En Linux o macOS, exporta las variables de MySQL en la terminal desde la que ejecutarás Maven.

```sh
sh ./mvnw --version
sh ./mvnw clean verify
sh ./mvnw spring-boot:run
```

Abre [la página de inicio](http://localhost:8080/) cuando el servidor termine de arrancar. Desde ahí puedes navegar al panel y a los módulos. `Ctrl+C` detiene el servidor de la terminal. Para ejecutar el JAR generado por `clean verify`, usa `java -jar target/reservacioneshotel-0.1.0.jar`.

## Abrir y ejecutar en Apache NetBeans

Si definiste las variables en PowerShell, cierra NetBeans y ábrelo desde esa misma terminal con `& 'C:\ruta\a\netbeans\bin\netbeans64.exe'`, ajustando la ruta a tu instalación. Así Maven recibe las variables de esa sesión.

1. Usa **File > Open Project** (`Ctrl+Shift+O`) y selecciona la carpeta que contiene `pom.xml`.
2. Registra el JDK 21 en **Tools > Java Platforms** y selecciónalo como plataforma Java en las propiedades del proyecto, en **Build > Compile**.
3. En **Projects**, haz clic derecho sobre `ReservacionesHotel` y elige **Run Maven > Other Goals…**. Escribe `clean verify` en el campo de objetivos para compilar, ejecutar las pruebas y empaquetar el JAR.
4. Desde el mismo menú, ejecuta el objetivo `spring-boot:run` y abre [la página de inicio](http://localhost:8080/).

## Estructura

| Ruta | Contenido |
| --- | --- |
| `pom.xml`, `.mvn/wrapper/`, `mvnw`, `mvnw.cmd` | Configuración del proyecto y de Maven Wrapper. |
| `src/main/java/com/hotel/reservacioneshotel/` | Clase principal, `controller` y paquetes técnicos `config`, `service` y `repository`. |
| `src/main/java/com/hotel/reservacioneshotel/domain/` | Paquetes de dominio `usuarios`, `reservaciones`, `habitaciones` y `pagos`. |
| `src/main/resources/` | Conexión MySQL, migraciones, plantillas y fragmentos Thymeleaf, CSS, JavaScript del menú y fotografía del portal. |
| `database/` | Script de creación del esquema MySQL. |
| `src/test/` | Pruebas web, de navegación y de persistencia, y configuración de H2. |
| `docs/interfaz/` | Descripción del diseño y capturas de las vistas. |

## Modelo de persistencia

El modelo sigue los cuatro paquetes del ZIP `Diagramas_Hotel_PlantUML_Corregidos.zip`.

| Paquete | Entidades y relaciones |
| --- | --- |
| `usuarios` | `Huesped` puede tener muchas reservaciones. `Recepcionista` y `Administrador` heredan de `Empleado`. |
| `habitaciones` | `TipoHabitacion` clasifica muchas habitaciones. `Habitacion` puede aparecer en varias reservaciones de fechas diferentes. |
| `reservaciones` | Cada `Reservacion` pertenece a un huésped y a una habitación, y puede recibir varios pagos. |
| `pagos` | Cada `Pago` pertenece a una reservación. |

Las asociaciones se mapean desde el lado que tiene la clave foránea. La herencia `SINGLE_TABLE` guarda ambos tipos de empleado en `empleados` y usa la columna `rol` para distinguirlos. Comparte los campos comunes y permite consultar ambos tipos con el repositorio de empleados sin uniones adicionales.

[La migración V2](src/main/resources/db/migration/V2__crear_cuentas.sql) agrega `cuentas` con un correo normalizado único para los tres roles y un vínculo a un solo titular, `Huesped` o `Empleado`. El rol de los huéspedes se fija al registrarlos y el de los empleados se deriva de su subtipo. El hash BCrypt de un huésped vive en `cuentas`. Los empleados usan el hash de `empleados` y su indicador `activo`.

V2 reserva los correos históricos de huéspedes que sean únicos al normalizarlos y conserva sus fichas. Esas cuentas no tienen contraseña hasta verificar su titularidad. Los empleados existentes conservan su usuario y hash. Un empleado sin cuenta vinculada necesita que la administración le asigne un correo verificado antes de iniciar sesión.

### Reglas de datos

- La fecha de salida debe ser posterior a la entrada. El intervalo de ocupación es `[entrada, salida)`, así que una salida y otra entrada pueden coincidir el mismo día.
- La consulta pública comprueba las reservaciones activas del intervalo. El alta y la edición vuelven a comprobar el solapamiento dentro de la transacción después de bloquear las habitaciones.
- La reservación conserva el precio por noche aplicado y el costo total. Los importes se representan con `BigDecimal` y se almacenan como `DECIMAL(12, 2)`. El esquema exige importes de reservación y habitación no negativos, pagos positivos y capacidad y número de huéspedes mayores que cero.
- Los pagos serán simulados. Cada reintento conservará el registro anterior y solo los pagos con estado `PAGADO` reducirán el saldo cuando se implemente el cálculo.

## Verificar las tablas en MySQL Workbench

1. Arranca la aplicación una vez con el esquema creado y las variables de entorno configuradas.
2. Abre la conexión a la misma instancia en Workbench. En **Schemas**, elige **Refresh All** y expande **hotel_reservaciones > Tables**.
3. Comprueba que aparecen `huespedes`, `empleados`, `cuentas`, `tipos_habitacion`, `habitaciones`, `reservaciones`, `pagos` y `flyway_schema_history`.
4. Puedes comprobar las migraciones y las claves foráneas desde una pestaña SQL.

   ```sql
   SELECT version, description, success
   FROM hotel_reservaciones.flyway_schema_history
   ORDER BY installed_rank;

   SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
   FROM information_schema.KEY_COLUMN_USAGE
   WHERE TABLE_SCHEMA = 'hotel_reservaciones'
     AND REFERENCED_TABLE_NAME IS NOT NULL
   ORDER BY TABLE_NAME, COLUMN_NAME;
   ```

## Pruebas

`clean verify` ejecuta las pruebas web y de persistencia con H2 en memoria. Incluye las pruebas anteriores y `ReservacionMvcTests`, que cubre altas, propiedad, permisos, validación, cruces, tarifas, edición, cancelación, persistencia y CSRF. `ReservacionConcurrenciaMySqlTests` se habilita con el perfil MySQL.

Para ejecutar la integración contra MySQL, configura las variables de conexión de una **instancia aislada de pruebas** con el esquema `hotel_reservaciones` creado. Flyway aplica las migraciones. Las pruebas de concurrencia confirman transacciones independientes y retiran sus registros de prueba al finalizar.

```powershell
.\mvnw.cmd "-Dtest=ReservacionMvcTests,ReservacionConcurrenciaMySqlTests,DisponibilidadMvcTests,InventarioCrudMvcTests,HuespedCrudMvcTests,PersistenciaJpaTests,SeguridadMvcTests" "-Dhotel.test.profile=mysql" test
```

## Registro y sesión

1. Abre `/registro`. Introduce nombre completo, correo, teléfono y una contraseña de al menos ocho caracteres y como máximo 72 bytes en UTF-8. Confirma la contraseña y pulsa **Crear cuenta**.
2. En `/login`, inicia sesión con el correo y la contraseña. La cuenta pública recibe únicamente el rol de huésped.
3. Para salir, pulsa **Cerrar sesión** en el encabezado. Spring Security recibe un POST e invalida la sesión.

La validación se realiza en el servidor. El registro conserva un hash BCrypt y rechaza correos duplicados entre huéspedes y empleados. Spring Security comprueba el rol en cada solicitud, protege los formularios auténticos con CSRF y devuelve HTTP 403 con una página explicativa cuando el acceso está denegado. La aplicación utiliza cookies de sesión HttpOnly con SameSite=Lax.

### Cuentas de demostración

Usa el perfil `demo` con un esquema de desarrollo separado. El perfil crea las cuentas de empleado que falten. Los siguientes correos son de ejemplo y las contraseñas se eligen localmente.

| Rol | Correo |
| --- | --- |
| Administrador | `administrador@example.test` |
| Recepcionista | `recepcionista@example.test` |
| Huésped | El que registres mediante `/registro`. |

En PowerShell 7, con las variables de MySQL ya configuradas para la base de desarrollo:

```powershell
$env:HOTEL_DEMO_ADMIN_PASSWORD = Read-Host 'Contraseña para administrador de demostración' -MaskInput
$env:HOTEL_DEMO_RECEPCION_PASSWORD = Read-Host 'Contraseña para recepcionista de demostración' -MaskInput
.\mvnw.cmd "-Dspring-boot.run.profiles=demo" spring-boot:run
```

En NetBeans puedes ejecutar el mismo objetivo Maven con `-Dspring-boot.run.profiles=demo` desde una sesión que herede esas variables. Las contraseñas se solicitan para crear las cuentas. Los siguientes arranques conservan las cuentas y sus hashes. El arranque habitual sin el perfil `demo` deja desactivada la creación de cuentas de demostración.

### Rutas por rol

| Acceso | Rutas |
| --- | --- |
| Público | `/`, `/login`, `/registro`, `/acerca-de`, `/disponibilidad` y recursos bajo `/css/`, `/js/` e `/img/`. |
| Cualquier rol autenticado | `/panel` y `POST /logout`. |
| Huésped | `/reservaciones` y sus subrutas, con acceso a sus propias reservaciones. |
| Recepcionista | `/huespedes` y `/reservaciones` con sus subrutas, `/pagos` y `/check-in-check-out`. |
| Administrador | `/habitaciones`, `/tipos-habitacion` y sus subrutas. |

## Gestión de huéspedes

Inicia la aplicación con una base MySQL de desarrollo y una cuenta de recepción. Puedes usar el perfil `demo` descrito arriba. En el menú principal, abre **Huéspedes**, registra una ficha de prueba, recarga la página para comprobar que sigue disponible y búscala por nombre, correo o ID. Luego usa **Editar** para modificarla o **Eliminar** para revisar si procede su eliminación.

| Método y ruta | Operación |
| --- | --- |
| `GET /huespedes` | Muestra las fichas con ID, nombre, teléfono y correo. El parámetro opcional `q` busca coincidencias parciales en nombre o correo y coincidencias exactas de ID. |
| `POST /huespedes` | Registra una ficha desde recepción. |
| `GET /huespedes/{id}/editar` | Muestra el formulario con los datos de la ficha. |
| `POST /huespedes/{id}/editar` | Guarda los cambios permitidos. |
| `GET /huespedes/{id}/eliminar` | Muestra la confirmación o el motivo por el que debe conservarse la ficha. |
| `POST /huespedes/{id}/eliminar` | Elimina la ficha cuando se cumplen las condiciones. |

`HuespedController` recibe las solicitudes MVC, `HuespedService` aplica las reglas en transacciones y `HuespedRepository` consulta las fichas con JPA. `CuentaRepository` y `ReservacionRepository` comprueban las relaciones. Los formularios reutilizan `RegistroHuespedForm`, que exige nombre, correo y teléfono, valida el formato del correo y limita las longitudes a 120, 254 y 32 caracteres, respectivamente. Las operaciones POST requieren un token CSRF. Una ficha inexistente devuelve HTTP 404.

El alta de recepción guarda un `Huesped` y reserva su correo normalizado en una `Cuenta` sin contraseña, con el inicio de sesión deshabilitado. Al editar, siempre se pueden actualizar el nombre y el teléfono. Si hay una cuenta activa, su correo queda protegido hasta que exista un flujo de verificación de titularidad. Para una ficha sin cuenta activa, el correo nuevo debe estar libre entre huéspedes y cuentas de los tres roles, y se guarda en ambas tablas en la misma transacción.

Se exige confirmación para eliminar. Se conserva la ficha si existe cualquier reservación asociada, incluso cancelada, o si su cuenta está activa. En una baja permitida, se elimina explícitamente la reserva de correo inactiva y la ficha. Las claves foráneas protegen las reservaciones y el historial de pagos. Esta etapa usa las migraciones V1 y V2 existentes.

## Gestión de habitaciones y tipos

Solo el rol `ADMINISTRADOR` tiene acceso a ambos módulos. Las operaciones de escritura usan POST y requieren CSRF.

| Método y ruta | Operación |
| --- | --- |
| `GET /habitaciones` | Muestra número, piso, tipo, capacidad, tarifa por noche y estado. Admite `q` para buscar por número y `tipo` para filtrar por ID de tipo. |
| `POST /habitaciones` | Registra una habitación y su estado operativo inicial. |
| `GET /tipos-habitacion` y `POST /tipos-habitacion` | Muestra el catálogo y registra un tipo, respectivamente. |
| `GET` y `POST` en `/habitaciones/{id}/editar` o `/tipos-habitacion/{id}/editar` | Muestran el formulario y guardan los cambios, respectivamente. |
| `GET` y `POST` en `/habitaciones/{id}/eliminar` o `/tipos-habitacion/{id}/eliminar` | Muestran la confirmación o el motivo del bloqueo y ejecutan una eliminación permitida, respectivamente. |

`HabitacionController` y `TipoHabitacionController` enlazan los formularios validados con `HabitacionService` y `TipoHabitacionService`. Los servicios aplican las reglas en transacciones y reutilizan las entidades `Habitacion` y `TipoHabitacion` y sus repositorios. `ReservacionRepository` comprueba el cupo y las relaciones con reservaciones. Una habitación o un tipo inexistente devuelve HTTP 404.

- Habitación. Número único de hasta 20 caracteres, piso entero a partir de cero, tipo existente y tarifa en MXN mayor que cero, de hasta diez cifras enteras y dos decimales. Cero representa la planta baja. El inventario puede cambiar la tarifa durante una estancia en curso. En ese caso bloquea los cambios de número, piso y tipo.
- Tipo. Nombre único de hasta 80 caracteres, capacidad entera mayor que cero y descripción opcional de hasta 255 caracteres. Se admiten Individual, Doble, Suite u otros nombres. El catálogo muestra las filas que ya existen en MySQL. Si está vacío, el administrador puede crear los tipos desde la interfaz. El arranque no inserta categorías de forma automática.
- Cupo. Cambiar el tipo de una habitación o reducir la capacidad de un tipo debe conservar el cupo de toda reserva pendiente, confirmada o en curso. Cada reservación conserva su tarifa por noche y su costo total al editar la tarifa del inventario.
- Eliminación. Se bloquea la baja de cualquier habitación con reservaciones, incluso canceladas, y de cualquier tipo asignado a habitaciones. Las claves foráneas protegen las relaciones y el historial. Se utilizan las migraciones V1 y V2.

### Estado operativo y disponibilidad

El estado persistido es `DISPONIBLE` u `OCUPADA`, conforme al UML corregido. En el inventario se elige el estado inicial al registrar la habitación. Durante la edición se muestra el estado actual para consulta. Los flujos futuros de check-in y check-out gestionarán las transiciones. La condición «Reservada» se deduce para el intervalo consultado.

### Probar en Apache NetBeans

Con Java 21 y las variables de una base MySQL de desarrollo configuradas, ejecuta `clean verify` desde **Run Maven > Other Goals…**. Para probar en el navegador, ejecuta `spring-boot:run -Dspring-boot.run.profiles=demo` con las variables del perfil de demostración. Inicia sesión como administrador, abre **Tipos de habitación** y registra un tipo que aún falte. En **Habitaciones**, crea un registro de prueba, recarga, filtra por su número o tipo, edítalo y confirma su eliminación. Puedes probar la protección de un tipo intentando eliminarlo mientras conserva una habitación asociada.

## Consulta pública de disponibilidad

Abre **Disponibilidad**, elige las fechas de entrada y salida, indica cuántos huéspedes se alojarán y pulsa **Buscar habitaciones**. `GET /disponibilidad` muestra el formulario y `GET /disponibilidad?entrada=AAAA-MM-DD&salida=AAAA-MM-DD&huespedes=2` muestra los resultados. La entrada debe ser hoy o posterior según el servidor. La salida debe ser posterior a la entrada y la estancia puede durar de 1 a 365 noches. El número de huéspedes debe ser un entero positivo.

La consulta JPA de `HabitacionRepository` carga cada habitación con su tipo, comprueba que su capacidad alcance y descarta habitaciones con reservas `PENDIENTE`, `CONFIRMADA` o `EN_CURSO` que se crucen con las fechas pedidas. `CANCELADA` y `FINALIZADA` conservan su historial y dejan libre el intervalo para la consulta.

Una estancia incluye sus noches desde la entrada y excluye el día de salida. Hay conflicto cuando `entradaExistente < salidaSolicitada` y `salidaExistente > entradaSolicitada`. Por ejemplo, una estancia del 10 al 15 de diciembre y otra del 15 al 18 pueden usar la misma habitación. Si la segunda empieza el 14, ambas requieren la habitación esa noche.

Si hay exactamente una estancia `EN_CURSO` que empezó a más tardar hoy y cuya salida es hoy o posterior, la habitación puede aparecer a partir de esa salida, incluso cuando su estado operativo es `OCUPADA`. Los solapamientos con otras reservas activas siguen bloqueándola. Una habitación ocupada sin estancia en curso queda fuera de la oferta. Una estancia vencida, futura o duplicada marcada como `EN_CURSO` también deja la habitación fuera hasta aclarar su estado.

`DisponibilidadService` calcula las noches como diferencia entre las dos fechas y multiplica la tarifa vigente por ese número con `BigDecimal`. Tres noches a 850.75 MXN dan un estimado de 2,552.25 MXN. La consulta es de solo lectura. Se puede iniciar una reservación desde un resultado y el sistema vuelve a verificar la habitación y la tarifa al guardar.

## Gestión de reservaciones

| Método y ruta | Operación |
| --- | --- |
| `GET /reservaciones` | Lista las reservaciones propias del huésped. Recepción puede verlas todas y buscar con `q` por número, nombre o correo del huésped. |
| `GET /reservaciones/nueva` | Abre el formulario. Puede recibir habitación, fechas y número de personas desde Disponibilidad. |
| `POST /reservaciones/nueva` | Registra la reservación como `PENDIENTE`. Recepción selecciona una ficha existente y el huésped autenticado reserva para sí mismo. |
| `GET /reservaciones/{id}` | Muestra habitación, tipo, fechas, noches, personas, tarifa, total y estado. |
| `GET` y `POST /reservaciones/{id}/editar` | Abren el formulario y guardan los cambios de una reservación pendiente o confirmada. |
| `GET` y `POST /reservaciones/{id}/cancelar` | Muestran la confirmación y cambian a `CANCELADA` una reservación pendiente o confirmada. |

Las operaciones POST requieren CSRF. El servidor comprueba la titularidad en cada operación y responde HTTP 403 ante un acceso ajeno y HTTP 404 ante un identificador inexistente. El usuario sin sesión que abre el formulario se dirige a Iniciar sesión. Los administradores conservan el inventario y los tipos de habitación.

El alta y la edición validan de 1 a 365 noches, la fecha de entrada a partir de hoy, el orden de las fechas, el número de personas, la capacidad y los conflictos. La edición excluye la propia reservación del solapamiento y conserva al titular. Si cambian las fechas o la habitación, se acuerda una nueva tarifa con el precio vigente y se calcula el total con `BigDecimal`. Cambiar únicamente el número de personas conserva la tarifa y el total acordados. Los cambios en el inventario por sí solos conservan los importes ya guardados de las reservaciones. La cancelación conserva el registro, las relaciones y los pagos existentes y libera el intervalo para nuevas búsquedas.

Al recalcular, el total debe cubrir la suma de los pagos `PAGADO`. Un pago `PENDIENTE` o `PROCESANDO` impide cambiar el total. Los pagos `RECHAZADO` quedan en el historial y no cuentan como importe cobrado. La edición y la cancelación conservan los importes y estados de cada pago. La etapa 9 queda a cargo de los saldos, la conciliación y los posibles reembolsos de reservaciones canceladas. Las futuras operaciones de pago deberán adquirir primero el mismo bloqueo de la reservación que usa la edición.

### Concurrencia

`ReservacionService` ejecuta las escrituras con `READ_COMMITTED`. Una edición o cancelación bloquea primero su fila de reservación. El alta bloquea la habitación con `PESSIMISTIC_WRITE`. Una edición que cambia de habitación bloquea ambas filas por ID ascendente. Con el bloqueo adquirido, la consulta JPA reutiliza las reglas de disponibilidad en una lectura que ve las transacciones ya confirmadas. La capacidad del tipo se protege con un bloqueo compartido. El bloqueo de habitación se conserva hasta que termine la transacción, por lo que el segundo intento superpuesto vuelve a consultar tras el primero y se rechaza si este guardó la reservación. [Aislamiento de transacciones de InnoDB](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html)

Las pruebas MySQL envían solicitudes desde hilos y transacciones independientes. Cubren dos altas superpuestas, dos altas consecutivas en la misma habitación, dos altas en habitaciones distintas del mismo tipo y dos ediciones que intercambian habitaciones. Estas operaciones usan el esquema de V1 y V2.

## Vistas del portal

Las tablas marcadas «Registros de ejemplo» muestran datos ilustrativos. Los formularios de los prototipos mantienen deshabilitado el envío.

| Ruta | Vista |
| --- | --- |
| `/login` | Inicio de sesión con correo electrónico y contraseña. |
| `/registro` | Registro funcional de una cuenta de huésped. |
| `/panel` | Panel principal con accesos a las vistas de operación y gestión. |
| `/huespedes` | Directorio de MySQL con búsqueda, alta, edición y eliminación controlada. |
| `/habitaciones` | Inventario de MySQL con búsqueda, filtro, alta, edición y eliminación controlada. |
| `/tipos-habitacion` | Catálogo de MySQL con alta, edición y eliminación controlada. |
| `/disponibilidad` | Consulta pública por fechas y capacidad, con costo estimado en MXN. |
| `/reservaciones/nueva` | Alta funcional para huéspedes y recepción. |
| `/reservaciones` | Listado con consulta de detalle, edición y cancelación. |
| `/pagos` | Historial de ejemplo y formulario para un futuro pago simulado. |
| `/check-in-check-out` | Listado de estancias con acciones de entrada y salida deshabilitadas. |
| `/acerca-de` | Objetivo del sistema, autoría, contexto académico, tecnologías y logotipos institucionales. |

Quedan pendientes la confirmación y los pagos, el check-in y check-out, y la verificación de titularidad para activar cuentas registradas por recepción o cambiar el correo de cuentas activas.

Consulta las [notas de interfaz y capturas](docs/interfaz/README.md) para la revisión visual.

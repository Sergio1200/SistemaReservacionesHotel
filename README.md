# ReservacionesHotel

Sistema de Gestión de Reservaciones de Hotel para la materia Paradigmas de Programación I. La aplicación ofrece registro de huéspedes, autenticación por correo y un portal de operación con acceso por roles.

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
- La lógica futura de disponibilidad consultará las reservaciones activas del intervalo. Se comprobarán solapamientos al crear o cambiar una reservación y se protegerá esa comprobación frente a solicitudes concurrentes. El estado operativo representa si la habitación está disponible u ocupada en ese momento.
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

`clean verify` ejecuta las pruebas web y de persistencia con H2 en memoria. La suite comprueba el registro público y BCrypt, la sesión, la autorización y CSRF, las rutas del portal y la migración desde V1. `HuespedCrudMvcTests` cubre el directorio, la búsqueda, el alta, la edición, la eliminación y sus restricciones.

Para repetir las pruebas de huéspedes, seguridad y persistencia contra MySQL, configura las variables de conexión de una **instancia de pruebas** con el esquema `hotel_reservaciones` creado. Flyway aplica las migraciones y las pruebas revierten sus operaciones de datos al terminar.

```powershell
.\mvnw.cmd "-Dtest=HuespedCrudMvcTests,PersistenciaJpaTests,SeguridadMvcTests" "-Dhotel.test.profile=mysql" test
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
| Huésped | `/reservaciones` y `/reservaciones/nueva`. |
| Recepcionista | `/huespedes` y sus subrutas, `/reservaciones`, `/reservaciones/nueva`, `/pagos` y `/check-in-check-out`. |
| Administrador | `/habitaciones` y `/tipos-habitacion`. |

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

## Vistas del portal

Las tablas marcadas «Registros de ejemplo» muestran datos ilustrativos. Los formularios de los prototipos mantienen deshabilitado el envío.

| Ruta | Vista |
| --- | --- |
| `/login` | Inicio de sesión con correo electrónico y contraseña. |
| `/registro` | Registro funcional de una cuenta de huésped. |
| `/panel` | Panel principal con accesos a las vistas de operación y gestión. |
| `/huespedes` | Directorio de MySQL con búsqueda, alta, edición y eliminación controlada. |
| `/habitaciones` | Inventario y formulario con número, piso, tipo, precio y estado. |
| `/tipos-habitacion` | Catálogo de las categorías Individual, Doble y Suite. |
| `/disponibilidad` | Formulario de fechas y cantidad de huéspedes. |
| `/reservaciones/nueva` | Selección de huésped, habitación y fechas de estancia. |
| `/reservaciones` | Tabla de reservaciones con huésped, fechas, habitación y estado. |
| `/pagos` | Historial de ejemplo y formulario para un futuro pago simulado. |
| `/check-in-check-out` | Listado de estancias con acciones de entrada y salida deshabilitadas. |
| `/acerca-de` | Objetivo del sistema, autoría, contexto académico, tecnologías y logotipos institucionales. |

Quedan pendientes el CRUD de habitaciones, la disponibilidad en tiempo real, los flujos de reservaciones, pagos, check-in y check-out, y la verificación de titularidad para activar cuentas registradas por recepción o cambiar el correo de cuentas activas.

Consulta las [notas de interfaz y capturas](docs/interfaz/README.md) para la revisión visual.

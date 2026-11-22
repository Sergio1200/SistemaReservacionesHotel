# Evaluación de despliegue gratuito en alwaysdata

Verificaciones del 22 de noviembre de 2026 sobre `main` en el commit
`80447bc3e96d7cd65f2170e4a18a0086ebe331d2`. El árbol de trabajo estaba limpio al
inicio y GitHub devolvió el mismo hash para `refs/heads/main`.

**Decisión.** Se descartó el despliegue permanente de la versión evaluada en
alwaysdata Free porque las mediciones no demostraron margen suficiente para
su cuota de memoria. La evaluación terminó antes del alta. No se creó un
sitio, no hay una URL pública permanente y el gasto fue de **$0 MXN**.

## Plan Free

| Aspecto | Condiciones publicadas |
| --- | --- |
| Precio | Free figura como gratuito tanto en la tabla mensual como en la anual. Se ofrece para uso no lucrativo y la tabla no señala una fecha de vencimiento. La política vigente no garantiza que las condiciones futuras sean idénticas. |
| Recursos | 256 MB de RAM, ¼ de CPU y 1 GB de espacio total, compartido entre archivos, correo y bases de datos. Las copias de seguridad tienen una retención de tres días. |
| Java | Java 21 está disponible. Para el JAR de Spring Boot con Tomcat embebido, el tipo de sitio documentado es **User Program**. La JVM se ejecutaría en primer plano y escucharía en el puerto `$PORT` y en `::` o en la IP asignada. |
| MariaDB | Las ramas anunciadas son 10.11, 11.4 y 11.8. El servidor SQL usa `mysql-<cuenta>.alwaysdata.net` y normalmente el puerto 3306. El nombre de la base lleva el prefijo de la cuenta. Public Cloud documenta un máximo de 40 conexiones simultáneas por usuario. |
| Dirección y HTTPS | Free admite la dirección asignada bajo `alwaysdata.net`. El proveedor la cubre con su certificado comodín y permite redirigir HTTP a HTTPS desde el sitio. |
| Inicio e inactividad del sitio | El primer acceso inicia el proceso. Por defecto, tras 30 minutos sin solicitudes, el proveedor lo para y el siguiente acceso lo inicia de nuevo. La interfaz permite configurar este plazo. Hay una única instancia por sitio, incluso durante el reinicio. La ejecución continua no está garantizada. La función independiente **Services** es de pago. |
| Límite de ejecución | Las cuentas se aíslan mediante cgroups en Linux. La tabla técnica indica que no hay un límite general de duración para las solicitudes HTTP y que transcurren 30 segundos entre `SIGTERM` y `SIGKILL`. No se indica un máximo total de procesos por cuenta Free ni un plazo específico para que User Program quede listo al arrancar. |
| Inactividad de la cuenta | Se requiere entrar al panel cada 120 días durante el primer año, cada seis meses entre uno y cuatro años y una vez al año a partir de los cuatro años. Entrar al panel reactiva un perfil suspendido por inactividad. |
| Tarjeta | La documentación contempla cuentas sin tarjeta ni pagos en modo restringido, con filtrado HTTP adicional. El formulario de registro también contempla exigir validación bancaria como medida antifraude. La exigencia efectiva para un alta concreta no se comprobó. Si aparece esa exigencia, se abandona el proceso. |

Fuentes oficiales de alwaysdata

[Precios de Public Cloud](https://help.alwaysdata.com/en/docs/admin-billing/billing/public-cloud-prices/) ·
[Configuración de Java](https://help.alwaysdata.com/en/docs/web-hosting/languages/java/configuration/) ·
[User Program](https://help.alwaysdata.com/en/docs/web-hosting/sites/http-servers/user-program/) ·
[MariaDB](https://help.alwaysdata.com/en/docs/web-hosting/databases/mariadb/) ·
[Certificados HTTPS](https://help.alwaysdata.com/en/docs/web-hosting/sites/ssl-tls/lets-encrypt/) ·
[Opciones del sitio](https://help.alwaysdata.com/en/docs/web-hosting/sites/add-a-site/) ·
[Inactividad del sitio](https://help.alwaysdata.com/en/docs/web-hosting/sites/misc/) ·
[Services](https://help.alwaysdata.com/en/docs/web-hosting/services/) ·
[Arquitectura](https://help.alwaysdata.com/en/docs/technical-specifications/architecture/) ·
[Límites](https://help.alwaysdata.com/en/docs/technical-specifications/limits/) ·
[Suspensión del perfil](https://help.alwaysdata.com/en/docs/admin-billing/profile/suspension/) ·
[Modo restringido](https://help.alwaysdata.com/en/docs/admin-billing/profile/restricted-mode/) ·
[Formulario de alta](https://www.alwaysdata.com/en/register/) ·
[Creación de cuenta](https://help.alwaysdata.com/en/docs/admin-billing/accounts/create-account/)

## Construcción, RAM, CPU y almacenamiento

Se usaron Java 21.0.13.1, Maven Wrapper con Maven 3.9.16 y Spring Boot 4.2.0. El
objetivo `clean verify` del Maven Wrapper terminó con `BUILD SUCCESS`. Pasaron 181 de 197
pruebas. Las 16 pruebas de concurrencia se omiten en la ejecución predeterminada
y se ejecutaron posteriormente contra MariaDB aislada.

En el cierre se volvió a arrancar el JAR con Java 21.0.13.1 y una instancia
aislada de MySQL 8.0.46. Flyway aplicó V1–V4 y `GET /`, `GET /disponibilidad`
y `GET /login` devolvieron HTTP 200. Se conservaron los resultados de la suite
completa porque el cambio del cierre se limitó a esta guía.

El JAR ejecutable `target/reservacioneshotel-0.1.0.jar` mide **61 891 808 bytes**
(59,03 MiB). El esquema ficticio de MySQL 8.0.46 ocupó 327 680 bytes en datos e
índices estimados por `information_schema` y 1 130 496 bytes en sus archivos
locales. El esquema recién migrado en MariaDB también informó 327 680 bytes de
datos e índices. El controlador externo ocupa 767 410 bytes. Las estimaciones
SQL cubren las tablas e índices de la aplicación, incluida la tabla de Flyway.
El espacio real de la cuenta también dependería de los archivos, los datos que
se acumulen, el correo y los registros que el proveedor contabilice.

### Medición de la aplicación

La medición se hizo en Windows 11 con Java 21 y MySQL 8.0.46 en loopback, con
directorios de datos y puertos exclusivos de la prueba. Se muestreó
aproximadamente cada 120 ms el conjunto de trabajo (`WorkingSet64`, memoria
residente), el pico registrado por Windows (`PeakWorkingSet64`), la memoria
privada comprometida (`PrivateMemorySize64`) y el tiempo de CPU del proceso
Java. Los valores de RAM excluyen el navegador, PowerShell y el motor SQL local.

El flujo abrió la portada, registró e inició sesión como huésped, comprobó
denegaciones HTTP 403 por rol, creó un tipo y una habitación como administrador,
consultó disponibilidad y reservó como huésped. Recepción procesó y aprobó
250,75 MXN simulados y registró la entrada y salida. SQL confirmó el pago, el
costo histórico, la reservación finalizada, ambas marcas de tiempo y la
habitación disponible.

| Fase | JVM por defecto | Serial GC y heap de 80 MiB | Serial GC y heap de 64 MiB | Serial GC y heap de 56 MiB |
| --- | ---: | ---: | ---: | ---: |
| Pico residente en el arranque | 507,7 MiB | 274,2 MiB | 258,0 MiB | 243,4 MiB |
| Tras iniciar sesión | 501,2 MiB | 284,7 MiB | 270,3 MiB | 255,5 MiB |
| Tras gestionar inventario | 509,0 MiB | 286,6 MiB | 271,6 MiB | 256,9 MiB |
| Tras consultar disponibilidad | 454,2 MiB | 287,0 MiB | 271,6 MiB | 257,1 MiB |
| Tras crear la reservación | 458,2 MiB | 287,6 MiB | 272,0 MiB | 257,6 MiB |
| Tras aprobar el pago | 481,9 MiB | 289,3 MiB | 273,7 MiB | 256,3 MiB |
| Pico residente de toda la ejecución | 516,7 MiB | 290,2 MiB | 275,1 MiB | ≈258 MiB |
| Pico privado comprometido muestreado durante el flujo | 562,0 MiB | 276,0 MiB | 261,0 MiB | ≈244 MiB |
| Arranque hasta servir la portada, reloj / CPU | 7,07 s / 23,20 s | 7,28 s / 8,14 s | 7,79 s / 8,97 s | 10,80 s / 11,73 s |
| Flujo del navegador, reloj / CPU Java | 4,86 s / 5,42 s | 4,99 s / 3,44 s | 5,57 s / 3,81 s | 5,99 s / 4,86 s |

Los ensayos de 80 y 64 MiB usaron `-Xms32m`, `-Xmx80m` o `-Xmx64m`,
`-XX:+UseSerialGC`, `-XX:ActiveProcessorCount=1`,
`-XX:MaxMetaspaceSize=144m`, `-XX:TieredStopAtLevel=1`,
`-XX:ReservedCodeCacheSize=28m`, `-Xss512k` y
`-XX:MaxDirectMemorySize=12m`. Tomcat quedó en 2–12 hilos y HikariCP en 1–4
conexiones. Security, JPA, Flyway y validaciones siguieron activos. Un intento
con tope de metaspace de 96 MiB provocó `OutOfMemoryError: Metaspace` al servir
HTTP. El intento con heap máximo de 48 MiB agotó el heap durante el arranque.

Para 56 MiB se conservaron Serial GC, un procesador activo, el tope de
metaspace de 144 MiB y la compilación JIT de nivel 1. Se usaron
`-Xms24m -Xmx56m -XX:ReservedCodeCacheSize=20m -Xss384k -XX:MaxDirectMemorySize=8m`,
Tomcat con 2–8 hilos y HikariCP con 1–3 conexiones. Esta medición se hizo
sin NMT y completó el flujo, con un pico residente de unos 258 MiB en el
proceso Java por sí solo.

La medición de memoria nativa de la JVM, tomada con `jcmd VM.native_memory` tras
el flujo de 64 MiB, informó 240 MiB comprometidos en total, incluidos 64 de
heap, 81 de metaspace, 17 para clases, 13 de código generado y 2 de hilos.
Los 434 MiB reservados son espacio virtual. Las cifras de NMT y el conjunto
residente de Windows son mediciones distintas. Con un cuarto de CPU dedicado,
el trabajo de CPU de los arranques con 64 y 56 MiB equivaldría a unos 36 y
47 segundos respectivamente. La duración real también depende de la contención,
el disco, la base remota y los límites de arranque del proveedor.

No se midió el cgroup de una cuenta real en Linux. Las pruebas de Windows
muestran que la configuración más pequeña que completó el flujo no deja margen
suficiente para dar por demostrada la viabilidad bajo la cuota total de 256 MB.

## Compatibilidad con MariaDB

Se ejecutó MariaDB **11.4.8** de forma portátil, en un puerto de loopback y con
dos esquemas aislados. Con la URL `jdbc:mysql:` y MySQL Connector/J del JAR,
Flyway aplicó V1–V4, pero Hibernate falló al consultar metadatos con
`Unknown column 'RESERVED' in 'WHERE'`. Con **MariaDB Connector/J 3.5.8**
cargado externamente mediante `PropertiesLauncher`, la URL `jdbc:mariadb:`,
y el driver `org.mariadb.jdbc.Driver`, el mismo JAR arrancó, Hibernate validó el
esquema y `GET /disponibilidad` devolvió HTTP 200.

En MariaDB se verificó lo siguiente.

- V1–V4 aplicadas con éxito, ocho tablas InnoDB contando el historial de Flyway,
  once restricciones `CHECK`, seis claves foráneas y ocho restricciones
  `UNIQUE`.
- Columnas monetarias `DECIMAL(12,2)`, fechas reales de la estancia
  `DATETIME(6)` anulables, `clave_intento VARCHAR(36)` anulable y el índice
  único compuesto `uk_pagos_reservacion_intento`.
- Cuatro pruebas de JPA y dieciséis de concurrencia aprobaron sin fallos ni
  omisiones. Cubrieron reservaciones (5), pagos (6) y estancias (5), incluidos
  `PESSIMISTIC_WRITE`, las operaciones simultáneas y la recuperación tras agotar
  una espera de bloqueo. En una copia temporal se cambió la aserción del
  motor esperado en esas tres clases de concurrencia y se añadió el controlador
  al classpath. El código, los escenarios de prueba y las migraciones del
  repositorio conservaron su contenido.
- La instancia probada reconoció `utf8mb4_0900_ai_ci`. Para un esquema nuevo en
  el panel se puede elegir `utf8mb4_unicode_ci`, que fue la colación usada en
  esta prueba. El panel asigna el nombre prefijado, así que se configura por
  URL JDBC completa.

La conclusión de compatibilidad corresponde a MariaDB 11.4.8 con su conector
nativo. Las otras ramas y la infraestructura del proveedor requieren
validación propia. La base MySQL habitual quedó fuera de las pruebas.

## Procedimiento de instalación condicionado a una evaluación satisfactoria

Estos pasos describen cómo se podría hacer una prueba en alwaysdata una vez
demostrada la capacidad dentro de 256 MB y confirmada la posibilidad de alta
gratuita para la cuenta.

1. El titular completa la autenticación y revisa las condiciones. En
   **Subscriptions > Open a new account**, elige **Public Cloud > Free**. Si el panel exige tarjeta,
   facturación, un trial con cargo posterior o cualquier pago, el alta se
   detiene. La validación bancaria queda fuera incluso si anuncia cero cargos.
2. En el panel **Databases**, crear un esquema nuevo sin datos reales y un
   usuario limitado a ese esquema, con permisos de migración y de operación.
   Usar MariaDB 11.4 de estar disponible. Dejar a Flyway crear las tablas y
   verificar su historial y el arranque de Hibernate con `ddl-auto=validate`.
3. Compilar localmente con Java 21 y `./mvnw clean verify` (Linux) o
   `mvnw.cmd clean verify` (Windows). Subir por SFTP el JAR y el conector
   MariaDB por separado a un directorio de trabajo privado, con el JAR del
   conector en `drivers/`. Los ajustes se aplican en el entorno del sitio,
   separados de la configuración habitual de MySQL.
4. Seleccionar **Web > Sites > Add a site > User Program** y la dirección
   asignada a la cuenta. Seleccionar el directorio que contiene el JAR y usar
   la consola del proveedor para revisar los registros del sitio. El comando
   siguiente configura Java 21 y el transporte al proxy. Las opciones de
   memoria se fijarían según mediciones positivas en el entorno de destino.

   ```sh
   JAVA_VERSION=21 java -Dloader.path=drivers/ \
     -cp reservacioneshotel-0.1.0.jar \
     org.springframework.boot.loader.launch.PropertiesLauncher \
     --server.address=:: --server.port="$PORT"
   ```

5. Activar la redirección HTTP a HTTPS desde **Web > Sites** y usar cookies de
   sesión seguras. Verificar que la primera petición en frío responda, que el
   certificado corresponda al nombre asignado y que `JSESSIONID` tenga
   `Secure`, `HttpOnly` y `SameSite=Lax`.
6. Verificar en el sitio real el registro, los permisos de los tres roles, el
   inventario, la disponibilidad, las reservaciones, los pagos simulados y las
   estancias. Reiniciar el sitio y volver a verificar una reservación de
   prueba, su pago y sus fechas reales. Comprobar los flujos desde escritorio y
   celular con datos ficticios.

### Variables del sitio

| Variable | Valor esperado |
| --- | --- |
| `PORT` | Puerto interno asignado por alwaysdata. |
| `SPRING_DATASOURCE_URL` | `jdbc:mariadb://mysql-<cuenta>.alwaysdata.net:3306/<cuenta>_reservaciones`, usando el host y el esquema que muestre el panel. |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.mariadb.jdbc.Driver`. |
| `HOTEL_DB_USER`, `HOTEL_DB_PASSWORD` | Usuario y secreto exclusivos del esquema ficticio en la configuración privada del sitio. |
| `HOTEL_ZONA_HORARIA` | `America/Mexico_City`, valor predeterminado de la aplicación. |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` en el sitio HTTPS. |
| `SPRING_PROFILES_ACTIVE`; `HOTEL_DEMO_ADMIN_PASSWORD`, `HOTEL_DEMO_RECEPCION_PASSWORD` | Solo para el aprovisionamiento inicial en el esquema ficticio. Usar el perfil `demo` y secretos únicos durante la creación de las dos cuentas. Después retirar el perfil y ambas variables y reiniciar. |

### Desactivar un sitio

Guardar los parámetros del sitio sin copiar credenciales y retirar su
entrada en **Web > Sites**, como indica la guía oficial. Comprobar desde otro
navegador que la dirección dejó de servir la aplicación. La base se administra
por separado en **Databases** y se conserva para su revisión. Para reactivar
el sitio se vuelve a declarar su entrada con esos parámetros. Detener solo
el proceso permite que la siguiente solicitud lo vuelva a iniciar.
[Desactivar un sitio](https://help.alwaysdata.com/en/docs/web-hosting/sites/deactivate-a-site/) ·
[Inactividad del sitio](https://help.alwaysdata.com/en/docs/web-hosting/sites/misc/)

## Alternativas de $0 MXN

### Demostración temporal con Cloudflare Quick Tunnel

Cloudflare Quick Tunnel permite compartir una aplicación local por HTTPS sin
cuenta ni dominio propio. La opción `--allowed-mail` limita el acceso a una
dirección autorizada mediante un PIN de un solo uso que Cloudflare envía por
correo. El visitante todavía debe iniciar sesión en el hotel para utilizar
funciones protegidas por roles. Esta etapa dejó el túnel sin iniciar.
[Guía oficial de Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/) ·
[Autenticación por correo](https://developers.cloudflare.com/changelog/post/2026-10-02-protected-quick-tunnels/)

1. Preparar una instancia MySQL aislada, con un directorio de datos propio,
   un usuario exclusivo del esquema `hotel_demo` y escucha local en
   `127.0.0.1:3307`. Usar únicamente huéspedes, habitaciones, reservaciones
   y pagos ficticios. Mantener MySQL fuera del túnel y sin redirección de
   puertos hacia Internet.
2. Desde la raíz del proyecto, iniciar el JAR con Java 21 en una terminal
   de PowerShell 7. Usar los valores de la instancia aislada y mantener los
   secretos en la sesión. Si la demostración requiere cuentas de personal,
   seguir las instrucciones de [cuentas de demostración](../../README.md#cuentas-de-demostración)
   sobre ese mismo esquema y mantener las credenciales fuera de grabaciones y
   archivos compartidos. Los ajustes del ejemplo se aplican al proceso local.

   ```powershell
   $env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3307/hotel_demo'
   $env:HOTEL_DB_USER = 'hotel_demo'
   $env:HOTEL_DB_PASSWORD = Read-Host 'Contraseña del usuario SQL de demostración' -MaskInput
   java -jar target/reservacioneshotel-0.1.0.jar --server.address=127.0.0.1 --server.port=18080 --server.servlet.session.cookie.secure=true
   ```

3. En otra terminal, verificar primero que la portada responda en loopback
   con `Invoke-WebRequest http://127.0.0.1:18080/`. Para compartir la demo,
   instalar una versión reciente de `cloudflared` y comprobar que
   `cloudflared tunnel --help` muestre `--allowed-mail`. Si la opción sigue
   ausente tras actualizar, usar la alternativa en red local.
   [Instalación de cloudflared](https://developers.cloudflare.com/tunnel/downloads/)
4. Cuando llegue el momento de la demostración, capturar la dirección del
   visitante en la terminal y arrancar el túnel hacia el puerto HTTP del JAR.
   Compartir la URL que imprima el proceso únicamente con la persona autorizada.

   ```powershell
   $correoAutorizado = Read-Host 'Correo de la persona autorizada'
   if (-not [string]::IsNullOrWhiteSpace($correoAutorizado)) {
       cloudflared tunnel --url http://127.0.0.1:18080 --allowed-mail $correoAutorizado
   }
   ```

5. Abrir la URL temporal en una ventana privada y comprobar que solicite el
   PIN antes de mostrar la aplicación. Al terminar, pulsar `Ctrl+C` en la
   terminal de `cloudflared`, comprobar que el acceso terminó y cerrar la
   aplicación y la instancia de demostración. El hostname cambia al iniciar
   otro túnel. El servicio carece de garantía de disponibilidad.

### Demostración en red local

La aplicación puede ejecutarse con un esquema ficticio y una dirección de
escucha accesible desde la misma red privada. El celular y el equipo deben
estar en esa red. MySQL permanece enlazado a loopback y la demostración no
requiere cuenta externa.

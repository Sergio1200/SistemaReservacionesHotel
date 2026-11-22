# Installation and operation manual

This procedure runs ReservacionesHotel on a local workstation. Use a separate MySQL instance for demonstrations and integration tests. The application uses the schema name `hotel_reservaciones` by default, including on the separate instance.

## 1. Workstation and software

Use a computer that can run a JDK, MySQL and a browser together. As a practical classroom baseline, allow about 8 GB of RAM and a few GB of free space for the Maven cache, IDE and database. This is a setup recommendation. The project has no measured hardware minimum.

- Install a **Java 21 JDK**. Select the installer for your operating system and architecture. Confirm `java -version` and `javac -version` after setting `JAVA_HOME` to the JDK folder and adding its `bin` directory to the terminal path. [Oracle Java downloads](https://www.oracle.com/java/technologies/downloads/#java21)
- Install **MySQL Server 8.x**, including the command-line client, or use an existing local installation prepared for classroom use. Integration tests used **8.0.46**. Start the dedicated instance and record its local host and port. MySQL Workbench is optional. [MySQL installation manual](https://dev.mysql.com/doc/refman/8.0/en/installing.html)
- Install **Git** and use a current browser. The first Maven Wrapper run needs access to Maven Central. Bootstrap is loaded from jsDelivr, so the browser needs access to that CDN for the full styling. [Maven Wrapper](https://maven.apache.org/tools/wrapper/)
- Install **Apache NetBeans** with Java and Maven support only if you plan to use an IDE. [Apache NetBeans](https://netbeans.apache.org/)

On Windows, after substituting the actual JDK folder:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

On macOS or Linux, set `JAVA_HOME` to your installed JDK 21 directory and run `export PATH="$JAVA_HOME/bin:$PATH"`.

## 2. Clone and confirm the build tools

```sh
git clone https://github.com/Sergio1200/SistemaReservacionesHotel.git
cd SistemaReservacionesHotel
```

From the folder that contains `pom.xml`, use `.\mvnw.cmd --version` in PowerShell or `sh ./mvnw --version` on macOS/Linux. Check that Maven reports Java 21. The wrapper pins Maven 3.9.16. An installed Maven can also run `mvn clean verify` if it uses Java 21.

## 3. Create and configure the database

1. Connect a MySQL administrative client to the intended **local development or isolated test instance**. In Workbench, open and execute [crear_base_datos.sql](../../database/crear_base_datos.sql). In the MySQL client, run `SOURCE database/crear_base_datos.sql;` from a client launched in the repository root.
2. Use an account dedicated to this schema. Its permissions must cover reading and writing the application tables and the DDL required by Flyway, including creating and altering tables and constraints. See the [SQL guide](sql/README.md) for a grant example. Enter the password interactively.
3. Set the environment variables in the terminal that will launch the application. The same environment must reach NetBeans if it runs Maven for you.

| Variable | Value or purpose |
| --- | --- |
| `HOTEL_DB_HOST` | Defaults to `127.0.0.1`. Use the chosen instance. |
| `HOTEL_DB_PORT` | Defaults to `3306`. Set the dedicated port when using an isolated instance. |
| `HOTEL_DB_USER` and `HOTEL_DB_PASSWORD` | MySQL credentials supplied to the process. |
| `HOTEL_ZONA_HORARIA` | Defaults to `America/Mexico_City`. The hotel clock controls arrival dates and actual stay timestamps. |
| `HOTEL_DEMO_ADMIN_PASSWORD` and `HOTEL_DEMO_RECEPCION_PASSWORD` | Used to create the two employee accounts on the first startup with the `demo` profile. |

PowerShell example. Replace the demonstration host or port only as needed.

```powershell
$env:HOTEL_DB_HOST = '127.0.0.1'
$env:HOTEL_DB_PORT = '3306'
$cred = Get-Credential -Message 'MySQL account for ReservacionesHotel'
$env:HOTEL_DB_USER = $cred.UserName
$env:HOTEL_DB_PASSWORD = $cred.GetNetworkCredential().Password
Remove-Variable cred
```

Bash example for macOS/Linux.

```bash
export HOTEL_DB_HOST=127.0.0.1
export HOTEL_DB_PORT=3306
read -r -p 'MySQL user: ' HOTEL_DB_USER
read -r -s -p 'MySQL password: ' HOTEL_DB_PASSWORD
printf '\n'
export HOTEL_DB_USER HOTEL_DB_PASSWORD
```

The repository's `.env.example` is a reference. Spring Boot does not automatically load it. Keep secrets out of shell history, screenshots and Git.

## 4. Start the application and run Flyway

Use `.\mvnw.cmd spring-boot:run` in PowerShell or `sh ./mvnw spring-boot:run` on macOS/Linux. For a packaged build, run `.\mvnw.cmd clean verify` and then `java -jar target/reservacioneshotel-0.1.0.jar` from the same configured shell.

At startup, Flyway applies the existing scripts V1 through V4 to `hotel_reservaciones` and records each version in `flyway_schema_history`. Hibernate checks the resulting mapping with `spring.jpa.hibernate.ddl-auto=validate`. Wait for the server to finish starting, then open [localhost on port 8080](http://localhost:8080/). Stop the process with **Ctrl+C**.

## 5. Run the project from NetBeans

1. Close any running IDE instance. After setting the environment, launch NetBeans from that terminal so the Maven process inherits it. On Windows, invoke your actual `netbeans64.exe` path using PowerShell's `&` operator.
2. Choose **File → Open Project** and select the folder containing `pom.xml`.
3. In **Tools → Java Platforms**, register the Java 21 JDK. Confirm the project's platform and Maven execution use Java 21.
4. In **Projects**, right-click `ReservacionesHotel` and select **Run Maven → Other Goals**. Run `clean verify` to build and test, or `spring-boot:run` to run it. Pass `-Dspring-boot.run.profiles=demo` with the run goal only for isolated demonstrations.
5. Open [localhost on port 8080](http://localhost:8080/) after Tomcat starts.

## 6. Prepare demonstration accounts

Use a separate database instance and enable `demo` for the demonstration. The existing `CuentasDemo` component creates `administrador@example.test` and `recepcionista@example.test` if they are missing. Choose local passwords of at least eight characters and at most 72 bytes in UTF-8. Existing demo accounts retain their passwords.

```powershell
# PowerShell 7. The regular MySQL variables must already be set.
$env:HOTEL_DEMO_ADMIN_PASSWORD = Read-Host 'Local administrator demo password' -MaskInput
$env:HOTEL_DEMO_RECEPCION_PASSWORD = Read-Host 'Local receptionist demo password' -MaskInput
.\mvnw.cmd "-Dspring-boot.run.profiles=demo" spring-boot:run
```

For a prebuilt JAR, use `java -jar target/reservacioneshotel-0.1.0.jar --spring.profiles.active=demo` with the same variables set. Register the fictitious guest using `/registro`. An isolated instance may optionally load [synthetic inventory](sql/02_synthetic_inventory.sql) after migrations. The [live presentation script](guion_demostracion.md) instead creates inventory through the application.

## 7. Run and interpret the tests

The regular suite runs on H2 and needs no local MySQL password. Use Java 21.

```powershell
.\mvnw.cmd clean verify
```

For the MySQL suite, point the same environment variables to a separate MySQL 8.0.46 instance that has an empty `hotel_reservaciones` schema. Stop the demo application first to keep the test setup predictable. These tests perform database writes and exercise lock timeouts.

```powershell
$tests = 'EstanciaMvcTests,EstanciaConcurrenciaMySqlTests,PagoMvcTests,PagoConcurrenciaMySqlTests,ReservacionMvcTests,ReservacionConcurrenciaMySqlTests,DisponibilidadMvcTests,InventarioCrudMvcTests,HuespedCrudMvcTests,PersistenciaJpaTests,SeguridadMvcTests'
.\mvnw.cmd "-Dtest=$tests" "-Dhotel.test.profile=mysql" test
```

On macOS/Linux, use `sh ./mvnw` with the same Maven arguments. Maven's final summary reports failures, errors and skips. See the [validation evidence](documentacion_tecnica.md#10-validation-and-results) for the documented totals.

## 8. Troubleshooting

| Symptom | Check |
| --- | --- |
| Maven uses a different Java version | Check `.\mvnw.cmd --version`, `JAVA_HOME` and the IDE's Maven runtime. |
| Connection refused | Confirm MySQL is running on the configured host and port. A separate instance usually uses a different port. |
| Access denied at startup | Retype the MySQL credentials and check the account's schema grants and host binding. |
| Unknown database | Execute the schema creation script on that instance before starting the application. |
| Flyway or schema validation fails | Print the Flyway history, confirm V1–V4 are intact and verify the configured schema. Never clean a database with data you intend to keep. |
| Port 8080 already in use | Stop the other local process or run the packaged application with `--server.port=8081` and use that port in the browser. |
| Missing demo login | Use the `demo` profile and supply the local passwords on its first run. Existing records retain their hashes. |
| Check-in button unavailable | Confirm the reservation is paid and confirmed, the room can be occupied and the hotel-local date falls within the planned stay. |
| Styles appear incomplete | Verify that the browser can reach jsDelivr for Bootstrap and that local assets load. |

The final verification used the Maven Wrapper with Java 21 and started the executable JAR against a fresh, isolated MySQL 8.0.46 instance. Flyway applied V1 through V4, Hibernate validated the schema, and the availability page returned HTTP 200. The optional inventory script was also run twice and kept a single fictional room. The NetBeans steps follow the existing project setup instructions and were reviewed against the repository configuration.

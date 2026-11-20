# ReservacionesHotel

Sistema de Gestión de Reservaciones de Hotel para la materia Paradigmas de Programación I. La etapa 1 inicializa una aplicación web que sirve la página de bienvenida en español. El proyecto contempla atención a huéspedes, operación de recepción y administración del hotel.

## Tecnologías

- Java 21 y Spring Boot 4.2.0 estable.
- Spring Web MVC con Tomcat embebido, Thymeleaf para las vistas y Jakarta Validation para validación.
- Bootstrap 5.3.9 desde jsDelivr y estilos propios. El navegador necesita acceso al CDN para cargar Bootstrap.
- Apache Maven 3.9.16 mediante Maven Wrapper 3.3.4, con empaquetado como JAR ejecutable.
- JUnit Jupiter y MockMvc para las pruebas.

Las coordenadas Maven son `com.hotel:reservacioneshotel:0.1.0` y el paquete base es `com.hotel.reservacioneshotel`.

## Requisitos

- JDK 21. Configura `JAVA_HOME` con su ruta y agrega el directorio `bin` al `PATH`.
- Conexión a Maven Central para la primera descarga de Maven y sus dependencias.
- Apache NetBeans con soporte para Java 21 y proyectos Maven, si se trabaja desde el IDE.

## Ejecutar con Maven

Desde la carpeta que contiene `pom.xml`, en PowerShell:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```sh
sh ./mvnw --version
sh ./mvnw clean verify
sh ./mvnw spring-boot:run
```

Abre [la página de inicio](http://localhost:8080/) cuando el servidor termine de arrancar. `Ctrl+C` detiene el servidor de la terminal. Para ejecutar el JAR generado por `clean verify`, usa `java -jar target/reservacioneshotel-0.1.0.jar`.

## Abrir y ejecutar en Apache NetBeans

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
| `src/main/resources/` | Configuración de la aplicación, plantilla `templates/index.html` y estilos `static/css/estilos.css`. |
| `src/test/java/` | Pruebas de arranque del contexto y respuesta de `GET /`. |

## Estado de la etapa 1

La aplicación sirve `GET /` en el puerto `8080`. El menú identifica con «Próximamente» los módulos de usuarios, reservaciones, habitaciones y pagos. El arranque solo requiere la configuración web incluida en el proyecto.

Quedan para etapas posteriores el registro y la autenticación, las entidades y las operaciones de gestión, la persistencia con JPA y MySQL y los pagos simulados. Los roles previstos son `Huesped`, `Recepcionista` y `Administrador`. Los huéspedes podrán registrarse, autenticarse y gestionar sus propias reservaciones.

### Referencia UML para etapas posteriores

El ZIP `Diagramas_Hotel_PlantUML_Corregidos.zip` sirve de referencia para los cuatro paquetes del dominio y sus entidades previstas.

| Paquete | Entidades previstas |
| --- | --- |
| `usuarios` | `Huesped`, `Empleado`, `Recepcionista`, `Administrador` |
| `reservaciones` | `Reservacion` |
| `habitaciones` | `Habitacion`, `TipoHabitacion` |
| `pagos` | `Pago` |

- El intervalo de ocupación incluye la entrada y excluye la salida. La disponibilidad se calcula por fechas y reservaciones activas. La aplicación deberá verificar solapamientos al crear o modificar una reservación.
- `Reservacion` conserva el precio por noche aplicado. Los importes se representan con `BigDecimal`.
- Solo un pago con estado `PAGADO` reduce el saldo pendiente. Cada reintento genera un registro y conserva el historial de pagos.

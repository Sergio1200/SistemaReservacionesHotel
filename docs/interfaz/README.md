# Interfaz de Reservaciones Hotel

El portal usa un menú lateral en computadora y un menú plegable en celular y tableta. Las vistas comparten la estructura mediante fragmentos de Thymeleaf. Las tablas anchas se desplazan dentro de su propia región y los botones de operación indican que estarán disponibles en una próxima etapa.

El inicio y «Acerca del proyecto» muestran los logotipos de la UAEMéx y del Centro Universitario Atlacomulco. El pie compartido incorpora la autoría y el contexto académico.

La fotografía del vestíbulo se sirve como recurso local. [Fotografía de Neon Wang en Unsplash](https://unsplash.com/photos/modern-hotel-lobby-with-leather-chairs-and-reception-desk-Is2xP3QGb74)

## Capturas de la aplicación

| Vista | Ruta | Escritorio |
| --- | --- | --- |
| Inicio | `/` | [Inicio](capturas/inicio.png) |
| Inicio de sesión | `/login` | [Inicio de sesión](capturas/inicio-sesion.png) |
| Panel principal | `/panel` | [Panel y pie de página con autoría](capturas/panel.png) |
| Acerca del proyecto | `/acerca-de` | [Acerca del proyecto](capturas/acerca-de.png) |
| Gestión de huéspedes | `/huespedes` | [Huéspedes](capturas/huespedes.png) |
| Gestión de habitaciones | `/habitaciones` | [Habitaciones](capturas/habitaciones.png) |
| Tipos de habitación | `/tipos-habitacion` | [Tipos de habitación](capturas/tipos-habitacion.png) |
| Consultar disponibilidad | `/disponibilidad` | [Disponibilidad](capturas/disponibilidad.png) |
| Nueva reservación | `/reservaciones/nueva` | [Nueva reservación](capturas/nueva-reservacion.png) |
| Consultar reservaciones | `/reservaciones` | [Reservaciones](capturas/reservaciones.png) |
| Gestión de pagos | `/pagos` | [Pagos](capturas/pagos.png) |
| Check-in y check-out | `/check-in-check-out` | [Check-in y check-out](capturas/check-in-check-out.png) |

- **Celular, 390 × 844**. [Inicio](capturas/inicio-390.png), [Acerca del proyecto](capturas/acerca-de-390.png) y [tabla de reservaciones](capturas/reservaciones-390.png).
- **Tableta, 768 × 1024**. [Panel principal](capturas/panel-768.png).

## Verificación

Las capturas proceden de la aplicación Spring Boot en ejecución con el perfil de pruebas y H2 en memoria. Las capturas de escritorio corresponden a una ventana de 1440 × 1000.

- **Automatizada.** Se recorrieron las doce rutas en Edge a 1440, 768, 390 y 320 píxeles de ancho. Se comprobó el pie compartido, la navegación a «Acerca del proyecto», la carga de ambos logotipos y la conservación de sus proporciones.
- **Visual.** La revisión se centró en el inicio y «Acerca del proyecto» en escritorio y celular, y en la autoría del pie de página del panel. Los tamaños de tableta y celular se reprodujeron en el navegador.

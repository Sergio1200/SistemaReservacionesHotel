# Interfaz de Reservaciones Hotel

El portal usa un menú lateral en computadora y un menú plegable en celular y tableta. Las vistas comparten la estructura mediante fragmentos de Thymeleaf. Las tablas anchas se desplazan dentro de su propia región. Los controles de las vistas de prototipo indican las operaciones previstas para próximas etapas.

El inicio y «Acerca del proyecto» muestran los logotipos de la UAEMéx y del Centro Universitario Atlacomulco. El pie compartido incorpora la autoría y el contexto académico.

La fotografía del vestíbulo se sirve como recurso local. [Fotografía de Neon Wang en Unsplash](https://unsplash.com/photos/modern-hotel-lobby-with-leather-chairs-and-reception-desk-Is2xP3QGb74)

## Capturas de la etapa 9

El flujo se verificó en Edge a 1440 y 390 píxeles con una instancia aislada de MySQL 8.0.46. Una reservación ficticia de 850.75 MXN recibió dos pagos aprobados, de 300.00 y 550.75 MXN. Un intento rechazado por 200.00 MXN quedó en el historial y no redujo el saldo. Al cancelar la reservación, se comprobó en MySQL que los tres pagos conservaron su importe, fecha y estado. Las capturas terminan antes del pie académico.

| Flujo | Captura |
| --- | --- |
| Registro con la cuenta del huésped | [Intento parcial](capturas/etapa-9/intento-parcial.png) |
| Búsqueda por identificador | [Recepción](capturas/etapa-9/busqueda-recepcion.png) |
| Procesamiento y decisión manual | [Validación en recepción](capturas/etapa-9/validacion-recepcion.png) |
| Aprobación de un pago parcial | [Saldo restante](capturas/etapa-9/saldo-parcial.png) |
| Importe superior al saldo | [Validación en el formulario](capturas/etapa-9/importe-invalido.png) |
| Rechazo de un intento | [Historial y saldo](capturas/etapa-9/rechazado.png) |
| Aprobación del último importe | [Reservación confirmada](capturas/etapa-9/pago-completo.png) |
| Acceso a pagos de otra cuenta | [HTTP 403](capturas/etapa-9/acceso-denegado.png) |
| Consulta del huésped a 390 píxeles | [Historial en celular](capturas/etapa-9/historial-390.png) |
| Cancelación con pagos previos | [Aviso de conciliación](capturas/etapa-9/cancelada.png) |

## Capturas de la etapa 8

Se probó en Edge a 1440 y 390 píxeles con MySQL 8.0.46 aislado. La búsqueda pública conservó la selección después de iniciar sesión. Se crearon reservaciones como huésped y como recepcionista, se consultó y filtró el listado, y se cambiaron fechas y habitación. Al cancelar, la habitación volvió a aparecer en Disponibilidad. Los datos son ficticios y las capturas terminan antes del pie académico.

| Flujo | Captura |
| --- | --- |
| Alta para la propia cuenta | [Huésped](capturas/etapa-8/alta-huesped.png) |
| Alta para una ficha existente | [Recepción](capturas/etapa-8/alta-recepcion.png) |
| Consulta de reservaciones | [Listado de recepción](capturas/etapa-8/listado.png) |
| Cambio de fechas y habitación | [Detalle actualizado](capturas/etapa-8/edicion.png) |
| Cancelación | [Resultado en escritorio](capturas/etapa-8/cancelacion.png) · [Detalle en celular](capturas/etapa-8/detalle-390.png) |
| Solicitud para fechas superpuestas | [Conflicto de disponibilidad](capturas/etapa-8/conflicto.png) |
| Acceso directo a una reservación ajena | [HTTP 403](capturas/etapa-8/acceso-denegado.png) |

Las pruebas de concurrencia se ejecutaron en paralelo contra MySQL. De dos altas superpuestas se guardó una. Los dos intervalos consecutivos en una misma habitación se guardaron. Una solicitud para otra habitación del mismo tipo terminó mientras la primera estaba bloqueada. Las dos ediciones que intercambiaron habitaciones con fechas consecutivas también se guardaron.

## Capturas de la etapa 7

La búsqueda se probó sin iniciar sesión, en Edge a 1440 y 390 píxeles y con una instancia aislada de MySQL 8.0.46. Las habitaciones y las reservaciones eran ficticias. Las capturas terminan antes del pie académico.

| Consulta | Captura |
| --- | --- |
| Tres noches para dos huéspedes, con tarifas y costos estimados | [Resultados en escritorio](capturas/etapa-7/resultados.png) |
| Tres noches para cuatro huéspedes | [Resultados en celular](capturas/etapa-7/resultados-390.png) |
| La única habitación con el cupo solicitado tiene una reserva superpuesta | [Sin resultados](capturas/etapa-7/sin-resultados.png) |
| El número de huéspedes supera la capacidad de todas las habitaciones | [Capacidad insuficiente](capturas/etapa-7/capacidad-insuficiente.png) |
| La salida es anterior a la entrada | [Error de fechas](capturas/etapa-7/fechas-invalidas.png) |

También se comprobó que una habitación con salida prevista aparece al buscar desde ese día y que una habitación ocupada sin salida fiable queda fuera de los resultados. Las consultas conservaron las reservaciones y sus importes.

## Capturas de la etapa 6

Las capturas se tomaron en Edge con una sesión de administración, en una instancia aislada de MySQL 8.0.46. El catálogo empezó vacío. Se registraron tipos y habitaciones ficticias desde el portal y se comprobó que los cambios seguían visibles al recargar. Para verificar el bloqueo se agregó una reservación cancelada de prueba en esa base temporal. El encuadre termina antes del pie académico.

| Flujo | Captura |
| --- | --- |
| Inventario | [Escritorio, ancho de 1440 px](capturas/etapa-6/listado.png) · [Celular, ancho de 390 px](capturas/etapa-6/listado-390.png) |
| Alta | [Registro exitoso](capturas/etapa-6/registro-exitoso.png) |
| Consulta | [Búsqueda por número y filtro por tipo](capturas/etapa-6/busqueda.png) |
| Edición | [Formulario de habitación y estado operativo](capturas/etapa-6/editar.png) |
| Baja | [Confirmación](capturas/etapa-6/confirmar-eliminacion.png) · [Bloqueo por reservación histórica](capturas/etapa-6/eliminacion-bloqueada.png) |
| Tipos | [Catálogo y formulario](capturas/etapa-6/tipos.png) |

En el celular, el formulario queda debajo del listado y la tabla se desplaza dentro de su región. También se verificaron el menú plegable, el pie académico y la carga de los logotipos.

## Capturas de la etapa 5

Las capturas corresponden a la aplicación ejecutada en Edge con la sesión de recepción y una instancia aislada de MySQL 8.0.46. Se crearon fichas de prueba desde el formulario con direcciones `example.test`. El encuadre termina antes del pie académico. Se comprobó la persistencia tras recargar, la búsqueda, la edición, el rechazo de correos duplicados y la eliminación confirmada.

| Flujo | Captura |
| --- | --- |
| Directorio de huéspedes | [Escritorio, ancho de 1440 px](capturas/etapa-5/listado.png) · [Celular, ancho de 390 px](capturas/etapa-5/listado-390.png) |
| Alta | [Registro exitoso](capturas/etapa-5/registro-exitoso.png) |
| Edición | [Formulario de modificación](capturas/etapa-5/editar.png) |
| Validación | [Correo duplicado](capturas/etapa-5/validacion.png) |

## Capturas de la etapa 4

Las capturas se tomaron en Edge con la aplicación conectada a una instancia MySQL 8.0.46 aislada. La prueba creó un huésped desde el formulario, inició y cerró sesión con los tres roles y comprobó el mensaje 403 al solicitar directamente una ruta reservada a otro rol.

| Flujo | Captura |
| --- | --- |
| Registro de huéspedes | [Escritorio](capturas/etapa-4/registro.png) · [Celular, 390 × 844](capturas/etapa-4/registro-390.png) |
| Inicio de sesión | [Login](capturas/etapa-4/login.png) |
| Huésped | [Panel de huésped](capturas/etapa-4/panel-huesped.png) |
| Recepcionista | [Panel de recepción](capturas/etapa-4/panel-recepcion.png) |
| Administrador | [Panel de administración](capturas/etapa-4/panel-administrador.png) |
| Acceso denegado | [Respuesta 403](capturas/etapa-4/acceso-denegado.png) |

## Capturas de la etapa 3

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

### Verificación de la etapa 3

Las capturas proceden de la aplicación Spring Boot en ejecución con el perfil de pruebas y H2 en memoria. Las capturas de escritorio corresponden a una ventana de 1440 × 1000.

- **Automatizada.** Se recorrieron las doce rutas en Edge a 1440, 768, 390 y 320 píxeles de ancho. Se comprobó el pie compartido, la navegación a «Acerca del proyecto», la carga de ambos logotipos y la conservación de sus proporciones.
- **Visual.** La revisión se centró en el inicio y «Acerca del proyecto» en escritorio y celular, y en la autoría del pie de página del panel. Los tamaños de tableta y celular se reprodujeron en el navegador.

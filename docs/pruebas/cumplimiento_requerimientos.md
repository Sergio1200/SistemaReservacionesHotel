# Cumplimiento de requerimientos

La numeración y el alcance de RF01 a RF14 se cotejaron con la presentación original «A6 Exposición Proyecto (equipos) (Primeros diagramas UML)», diapositivas 3 a 5. Los requisitos no funcionales proceden de la diapositiva 6. Los identificadores de prueba corresponden a la [matriz de la etapa 11](matriz_pruebas.md).

## Requerimientos funcionales

| ID | Requisito original | Funcionalidad implementada | Rutas o clases | Prueba | Cumplimiento |
| --- | --- | --- | --- | --- | --- |
| RF01 | Consultar disponibilidad con entrada, salida y número de huéspedes. | Consulta pública por intervalo y capacidad. Las reservaciones vigentes bloquean fechas superpuestas. | `GET /disponibilidad`, `DisponibilidadService` | E04, A07, A08 | Cumple. |
| RF02 | Registrar una reservación para un huésped y verificar que la habitación esté disponible. | El huésped reserva para su cuenta y recepción selecciona una ficha existente. Se verifica disponibilidad antes de persistir. | `GET/POST /reservaciones/nueva`, `ReservacionService` | E05, A09, A10 | Cumple. |
| RF03 | Calcular el costo en función de las noches y la tarifa por noche. | Usa `BigDecimal`, guarda la tarifa aplicada y conserva el total ante cambios en el inventario. | `ReservacionService`, `Reservacion` | E05, A08, A11 | Cumple. |
| RF04 | Modificar fechas, habitación o número de huéspedes y volver a verificar disponibilidad. | Permite editar reservaciones pendientes o confirmadas. Verifica disponibilidad, cupo y pagos. Revisa la confirmación cuando cambia el total. | `GET/POST /reservaciones/{id}/editar` | E10, A12, A17 | Cumple. |
| RF05 | Cancelar si el estado de la reservación lo permite. | Permite cancelar en `PENDIENTE` o `CONFIRMADA`, libera el periodo y conserva la reservación y sus pagos. | `GET/POST /reservaciones/{id}/cancelar` | A13 | Cumple. |
| RF06 | Buscar reservaciones por número o datos del huésped. | Recepción busca por ID, nombre o correo. El huésped consulta su propio listado y detalle. | `GET /reservaciones`, `GET /reservaciones/{id}` | A06, A14 | Cumple con acceso según el rol. |
| RF07 | Registrar uno o varios pagos con identificador, fecha, monto, método y estado. | Registra intentos simulados vinculados a la reservación y conserva el historial. Un reenvío repetido queda asociado al intento original. | `GET/POST /reservaciones/{id}/pagos`, `PagoService` | E06, A15, A16 | Cumple en el alcance simulado. |
| RF08 | Verificar la validez de un pago antes de confirmarlo. | Recepción procesa y decide. El sistema controla el saldo y los estados. El total aprobado confirma la reservación. | `POST .../pagos/{pagoId}/procesar` y `/validar`, `PagoService` | E07, A15, A17 | Cumple con validación manual simulada. |
| RF09 | Registrar la llegada de un huésped con reservación válida y confirmada. | Registra la hora real, cambia a `EN_CURSO` y marca la habitación `OCUPADA` después de verificar fecha, cupo y ocupación. | `POST /check-in-check-out/{id}/check-in`, `EstanciaService` | E08, A18, A21 | Cumple. |
| RF10 | Registrar la salida y finalizar la reservación; dejar disponible la habitación. | Registra la hora real y finaliza la estancia. Libera la habitación si no queda otra estancia activa. | `POST /check-in-check-out/{id}/check-out`, `EstanciaService` | E08, A19, A21 | Cumple con liberación condicionada a la ocupación. |
| RF11 | Registrar ID, nombre, teléfono y correo del huésped. | El alta pública crea una ficha y una cuenta. Recepción puede crear y administrar fichas. | `GET/POST /registro`, `GET/POST /huespedes` | E03, A02, A24 | Cumple. |
| RF12 | Permitir a administración registrar y modificar habitaciones con número, piso, tipo, precio y estado. | El inventario guarda esos datos y admite alta y modificación. El estado operativo se elige al dar de alta. Recepción lo cambia al registrar movimientos de estancia. | `/habitaciones`, `HabitacionService` | E02, A04, A11, A25 | Cumple con estado operativo gestionado por estancias. |
| RF13 | Administrar tipos de habitación con capacidad y descripción. | Catálogo con altas, ediciones y eliminaciones sujetas a integridad. | `/tipos-habitacion`, `TipoHabitacionService` | E02, A25 | Cumple. |
| RF14 | Permitir acceso a empleados autorizados y distinguir recepción de administración. | Inicio por correo, contraseñas BCrypt, sesión y permisos separados según el rol. | `/login`, `POST /logout`, `SeguridadConfig` | E01, E09, A03, A04, A05 | Cumple. |

## Requerimientos no funcionales

| ID | Requisito | Evidencia | Estado |
| --- | --- | --- | --- |
| RNF01 | Facilidad de uso | Navegación, formularios, validaciones, tablas y botones revisados en Edge a 1440, 768, 390 y 320 px. | Verificado en las vistas probadas. |
| RNF02 | Seguridad | Roles, acceso directo y titularidad, BCrypt, CSRF, cierre de sesión y respuestas de error depuradas. | Verificado para los perfiles y rutas probados. |
| RNF03 | Persistencia | MySQL 8.0.46, validación JPA, migraciones V1–V4 en orden y consulta de relaciones, datos históricos y fechas reales. | Verificado. |
| RNF04 | Validación de datos | Se probaron formularios vacíos, correos duplicados, fechas fuera de rango, capacidad, montos y estados. | Verificado para los casos de la matriz. |
| RNF05 | Integridad de reservaciones | Fechas superpuestas, escrituras simultáneas y recuperación al agotar un bloqueo en MySQL. | Verificado. |
| RNF06 | Rendimiento | Las búsquedas completaron el flujo funcional con datos de prueba. La presentación carece de un umbral cuantitativo y esta etapa carece de una prueba de carga. | Parcial. |

## Diferencias con el UML original

- **Diapositivas 19–21, clases.** Los importes usan `BigDecimal`, las fechas programadas usan `LocalDate`, y pagos y horas reales usan `LocalDateTime`. La reservación guarda la tarifa histórica y dos horas reales anulables. Los identificadores técnicos son `Long`, el número de habitación admite letras y los tipos pueden existir antes de que se les asigne una habitación. Los servicios hacen las validaciones y los repositorios JPA persisten los datos.
- **Acceso.** RF14 describe la entrada de empleados. La implementación conserva los dos roles de empleado y agrega la cuenta pública de huésped con acceso limitado a sus propias reservaciones y pagos.
- **Diapositiva 30, estados de reservación.** La cobertura del total aprobado confirma la reservación. Una reprogramación que aumenta el costo por encima del total pagado regresa de `CONFIRMADA` a `PENDIENTE`. El historial de pagos sigue vinculado. Si un total reducido queda exactamente cubierto por pagos ya aprobados, la reservación queda confirmada.
- **Diapositiva 31, estados de habitación.** La condición «Reservada» se calcula por intervalo, tal como indica el ZIP PlantUML corregido. La habitación guarda `DISPONIBLE` u `OCUPADA`. En la salida se libera únicamente si no queda otra estancia activa.
- **Diapositiva 32, estados de pago.** La aprobación y el rechazo son simulados. Los rechazos se conservan. Reintentar crea un pago nuevo en `PENDIENTE`, mientras que reenviar el mismo formulario recupera el intento original.
- **Secuencias de entrada y salida.** El registro reside en `EstanciaService`. La transacción bloquea reservación y habitación antes de actualizar estado y hora. La salida tardía se registra sin cargos adicionales. Una estancia histórica que carece de hora real de entrada requiere revisión.

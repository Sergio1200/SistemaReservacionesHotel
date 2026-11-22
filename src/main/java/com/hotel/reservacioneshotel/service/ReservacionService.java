package com.hotel.reservacioneshotel.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.CuentaRepository;
import com.hotel.reservacioneshotel.repository.HabitacionRepository;
import com.hotel.reservacioneshotel.repository.HuespedRepository;
import com.hotel.reservacioneshotel.repository.PagoRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import com.hotel.reservacioneshotel.repository.TipoHabitacionRepository;
import com.hotel.reservacioneshotel.service.DisponibilidadService.Periodo;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservacionService {

    private static final BigDecimal MAXIMO_TOTAL = new BigDecimal("9999999999.99");

    private final ReservacionRepository reservaciones;
    private final HabitacionRepository habitaciones;
    private final TipoHabitacionRepository tipos;
    private final HuespedRepository huespedes;
    private final CuentaRepository cuentas;
    private final PagoRepository pagos;
    private final DisponibilidadService disponibilidad;

    public ReservacionService(ReservacionRepository reservaciones, HabitacionRepository habitaciones,
                              TipoHabitacionRepository tipos, HuespedRepository huespedes,
                              CuentaRepository cuentas, PagoRepository pagos, DisponibilidadService disponibilidad) {
        this.reservaciones = reservaciones;
        this.habitaciones = habitaciones;
        this.tipos = tipos;
        this.huespedes = huespedes;
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.disponibilidad = disponibilidad;
    }

    @Transactional(readOnly = true)
    public List<Detalle> listar(Operador operador, String criterio) {
        if (operador.recepcion()) {
            String q = criterio.strip();
            return reservaciones.buscarParaRecepcion(q, buscarId(q)).stream().map(this::detalle).toList();
        }
        return idDelHuesped(operador)
                .map(reservaciones::findByHuesped_IdHuespedOrderByFechaEntradaDescIdReservacionDesc)
                .orElseGet(List::of).stream().map(this::detalle).toList();
    }

    @Transactional(readOnly = true)
    public Detalle consultar(Operador operador, Long id) {
        return detalle(autorizar(operador, reservaciones.findOneByIdReservacion(id).orElseThrow(NoEncontrada::new)));
    }

    @Transactional(readOnly = true)
    public List<Seleccion> opcionesHabitaciones() {
        return habitaciones.buscar("", null).stream()
                .map(h -> new Seleccion(h.getIdHabitacion(), h.getNumero() + " · " + h.getTipoHabitacion().getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Seleccion> opcionesHuespedes(Operador operador) {
        if (!operador.recepcion()) {
            return List.of();
        }
        return huespedes.findAll(Sort.by("nombre", "idHuesped")).stream()
                .map(h -> new Seleccion(h.getIdHuesped(), h.getNombre() + " · #" + h.getIdHuesped())).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long crear(Operador operador, Long huespedId, Long habitacionId, LocalDate entrada, LocalDate salida, int personas) {
        Huesped huesped = elegirHuesped(operador, huespedId);
        Habitacion habitacion = bloquearHabitacion(habitacionId);
        Periodo periodo = comprobarDisponibilidad(habitacion, entrada, salida, personas, null);
        BigDecimal tarifa = habitacion.getPrecioPorNoche();
        Reservacion nueva = new Reservacion(huesped, habitacion, entrada, salida, personas,
                tarifa, calcularTotal(tarifa, periodo.noches()), EstadoReservacion.PENDIENTE);
        return reservaciones.save(nueva).getIdReservacion();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void actualizar(Operador operador, Long id, Long huespedId, Long habitacionId,
                           LocalDate entrada, LocalDate salida, int personas) {
        Reservacion reservacion = bloquearReservacion(operador, id);
        exigirEstadoEditable(reservacion);
        if (huespedId != null && !huespedId.equals(reservacion.getHuesped().getIdHuesped())) {
            throw new AccessDeniedException("No está permitido cambiar el titular.");
        }
        Long anterior = reservacion.getHabitacion().getIdHabitacion();
        Habitacion habitacion = bloquearHabitaciones(anterior, habitacionId);
        Periodo periodo = comprobarDisponibilidad(habitacion, entrada, salida, personas, id);
        boolean cambiaEstancia = !anterior.equals(habitacionId) || !entrada.equals(reservacion.getFechaEntrada())
                || !salida.equals(reservacion.getFechaSalida());
        BigDecimal tarifa = cambiaEstancia ? habitacion.getPrecioPorNoche() : reservacion.getPrecioNocheAplicado();
        BigDecimal total = cambiaEstancia ? calcularTotal(tarifa, periodo.noches()) : reservacion.getCostoTotal();
        if (cambiaEstancia) {
            validarPagosAlEditar(reservacion, total);
        }
        reservacion.reprogramar(habitacion, entrada, salida, personas, tarifa, total);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelar(Operador operador, Long id) {
        Reservacion reservacion = bloquearReservacion(operador, id);
        exigirEstadoEditable(reservacion);
        bloquearHabitacion(reservacion.getHabitacion().getIdHabitacion());
        reservacion.cancelar();
    }

    private Huesped elegirHuesped(Operador operador, Long seleccionado) {
        if (operador.recepcion()) {
            if (seleccionado == null) {
                throw new OperacionNoPermitida("huespedId", "Selecciona un huésped.");
            }
            return huespedes.buscarParaReservar(seleccionado)
                    .orElseThrow(() -> new OperacionNoPermitida("huespedId", "El huésped seleccionado ya no existe."));
        }
        Long propio = idDelHuesped(operador).orElseThrow(() -> new AccessDeniedException("Cuenta sin ficha de huésped."));
        if (seleccionado != null && !propio.equals(seleccionado)) {
            throw new AccessDeniedException("Solo puedes reservar para tu propia cuenta.");
        }
        return huespedes.buscarParaReservar(propio)
                .orElseThrow(() -> new AccessDeniedException("Cuenta sin ficha de huésped."));
    }

    private Optional<Long> idDelHuesped(Operador operador) {
        return cuentas.findByCorreo(Cuenta.normalizarCorreo(operador.correo()))
                .filter(Cuenta::isActiva).map(Cuenta::getHuesped).map(Huesped::getIdHuesped);
    }

    private Reservacion autorizar(Operador operador, Reservacion reservacion) {
        if (!operador.recepcion() && idDelHuesped(operador).filter(reservacion.getHuesped().getIdHuesped()::equals).isEmpty()) {
            throw new AccessDeniedException("La reservación pertenece a otra cuenta.");
        }
        return reservacion;
    }

    private Reservacion bloquearReservacion(Operador operador, Long id) {
        return autorizar(operador, reservaciones.buscarParaActualizar(id).orElseThrow(NoEncontrada::new));
    }

    private Habitacion bloquearHabitacion(Long id) {
        return habitaciones.buscarParaActualizar(id)
                .orElseThrow(() -> new OperacionNoPermitida("habitacionId", "La habitación seleccionada ya no existe."));
    }

    private Habitacion bloquearHabitaciones(Long anterior, Long nueva) {
        if (anterior.equals(nueva)) {
            return bloquearHabitacion(nueva);
        }
        // Las ediciones con dos habitaciones toman sus filas en orden ascendente.
        if (anterior < nueva) {
            bloquearHabitacion(anterior);
            return bloquearHabitacion(nueva);
        }
        Habitacion destino = bloquearHabitacion(nueva);
        bloquearHabitacion(anterior);
        return destino;
    }

    private Periodo comprobarDisponibilidad(Habitacion habitacion, LocalDate entrada, LocalDate salida,
                                            int personas, Long excluirId) {
        Periodo periodo = disponibilidad.validarPeriodo(entrada, salida);
        if (personas <= 0) {
            throw new OperacionNoPermitida("personas", "El número de huéspedes debe ser mayor que cero.");
        }
        // El bloqueo compartido evita que se reduzca la capacidad mientras se guarda la reservación.
        TipoHabitacion tipo = tipos.buscarParaReservar(habitacion.getTipoHabitacion().getIdTipo()).orElseThrow();
        if (personas > tipo.getCapacidad()) {
            throw new OperacionNoPermitida("personas", "La capacidad de la habitación es insuficiente.");
        }
        if (!disponibilidad.estaDisponible(habitacion.getIdHabitacion(), periodo, personas, excluirId)) {
            throw new OperacionNoPermitida("habitacionId", "La habitación ya no está disponible para esas fechas.");
        }
        return periodo;
    }

    private BigDecimal calcularTotal(BigDecimal tarifa, long noches) {
        BigDecimal total = tarifa.multiply(BigDecimal.valueOf(noches));
        if (total.compareTo(MAXIMO_TOTAL) > 0) {
            throw new OperacionNoPermitida("salida", "El importe supera el máximo permitido para una reservación.");
        }
        return total;
    }

    private void validarPagosAlEditar(Reservacion reservacion, BigDecimal total) {
        Long id = reservacion.getIdReservacion();
        if (total.compareTo(pagos.sumarMontos(id, EstadoPago.PAGADO)) < 0) {
            throw new OperacionNoPermitida(null, "El nuevo total no puede ser menor que el importe ya pagado.");
        }
        if (total.compareTo(reservacion.getCostoTotal()) != 0
                && pagos.existsByReservacion_IdReservacionAndEstadoIn(id, List.of(EstadoPago.PENDIENTE, EstadoPago.PROCESANDO))) {
            throw new OperacionNoPermitida(null, "El total no puede cambiar mientras haya un pago pendiente o en proceso.");
        }
    }

    private void exigirEstadoEditable(Reservacion reservacion) {
        if (!reservacion.getEstado().permiteCambios()) {
            throw new OperacionNoPermitida(null, "Solo se pueden cambiar o cancelar reservaciones pendientes o confirmadas.");
        }
    }

    private Long buscarId(String q) {
        try {
            return Long.valueOf(q.startsWith("#") ? q.substring(1) : q);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Detalle detalle(Reservacion reservacion) {
        Habitacion habitacion = reservacion.getHabitacion();
        return new Detalle(reservacion.getIdReservacion(), reservacion.getHuesped().getNombre(),
                habitacion.getIdHabitacion(), habitacion.getNumero(), habitacion.getTipoHabitacion().getNombre(),
                reservacion.getFechaEntrada(), reservacion.getFechaSalida(), reservacion.getNumeroHuespedes(),
                ChronoUnit.DAYS.between(reservacion.getFechaEntrada(), reservacion.getFechaSalida()),
                reservacion.getPrecioNocheAplicado(), reservacion.getCostoTotal(), reservacion.getEstado());
    }

    public record Operador(String correo, boolean recepcion) {
        public Operador {
            Objects.requireNonNull(correo);
        }
    }

    public record Seleccion(Long id, String etiqueta) {
    }

    public record Detalle(Long id, String titular, Long habitacionId, String habitacion, String tipo,
                         LocalDate entrada, LocalDate salida, int personas, long noches, BigDecimal tarifa,
                         BigDecimal total, EstadoReservacion estado) {
    }

    public static class NoEncontrada extends RuntimeException {
    }

    public static class OperacionNoPermitida extends RuntimeException {
        private final String campo;

        public OperacionNoPermitida(String campo, String mensaje) {
            super(mensaje);
            this.campo = campo;
        }

        public String getCampo() {
            return campo;
        }
    }
}

package com.hotel.reservacioneshotel.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.repository.HabitacionRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import com.hotel.reservacioneshotel.repository.TipoHabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstanciaService {

    private static final List<EstadoReservacion> ESTADOS_VISIBLES = List.of(
            EstadoReservacion.CONFIRMADA, EstadoReservacion.EN_CURSO, EstadoReservacion.FINALIZADA);

    private final ReservacionService reservas;
    private final ReservacionRepository reservaciones;
    private final HabitacionRepository habitaciones;
    private final TipoHabitacionRepository tipos;
    private final Clock relojHotel;

    public EstanciaService(ReservacionService reservas, ReservacionRepository reservaciones,
                           HabitacionRepository habitaciones, TipoHabitacionRepository tipos, Clock relojHotel) {
        this.reservas = reservas;
        this.reservaciones = reservaciones;
        this.habitaciones = habitaciones;
        this.tipos = tipos;
        this.relojHotel = relojHotel;
    }

    @Transactional(readOnly = true)
    public List<Movimiento> listar(String criterio, LocalDate hoy) {
        return reservas.buscarParaRecepcion(criterio, ESTADOS_VISIBLES).stream()
                .map(reserva -> movimiento(reserva, hoy))
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void registrarEntrada(Long id) {
        Reservacion reserva = reservas.bloquearReservacion(id);
        if (reserva.getFechaHoraCheckIn() != null || reserva.getEstado() == EstadoReservacion.EN_CURSO) {
            throw new Conflicto("La entrada de esta reservación ya se registró o la estancia está en curso.");
        }
        if (reserva.getEstado() != EstadoReservacion.CONFIRMADA) {
            throw new Conflicto("Solo se puede registrar la entrada de una reservación confirmada.");
        }

        // Igual que al editar una reservación: primero su fila, después la habitación y el tipo.
        Habitacion habitacion = bloquearHabitacion(reserva);
        TipoHabitacion tipo = tipos.buscarParaReservar(habitacion.getTipoHabitacion().getIdTipo())
                .orElseThrow(() -> new Conflicto("El tipo de habitación ya no está disponible."));
        LocalDateTime momento = LocalDateTime.now(relojHotel).truncatedTo(ChronoUnit.MICROS);
        if (!admiteEntradaEn(reserva, momento.toLocalDate())) {
            throw new Conflicto("La entrada se registra desde el día programado de llegada y antes del día de salida.");
        }
        if (reserva.getNumeroHuespedes() > tipo.getCapacidad()) {
            throw new Conflicto("El número de huéspedes supera la capacidad de la habitación.");
        }
        if (habitacion.getEstadoOperativo() == EstadoHabitacion.OCUPADA || hayOtraEstancia(reserva)) {
            throw new Conflicto("La habitación está ocupada. Revisa la estancia activa antes de registrar la entrada.");
        }

        reserva.registrarEntrada(momento);
        habitacion.ocupar();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void registrarSalida(Long id) {
        Reservacion reserva = reservas.bloquearReservacion(id);
        if (reserva.getFechaHoraCheckOut() != null || reserva.getEstado() == EstadoReservacion.FINALIZADA) {
            throw new Conflicto("Esta estancia ya está finalizada.");
        }
        if (reserva.getEstado() != EstadoReservacion.EN_CURSO || reserva.getFechaHoraCheckIn() == null) {
            throw new Conflicto("Para registrar la salida, la estancia debe estar en curso y tener una entrada registrada.");
        }

        Habitacion habitacion = bloquearHabitacion(reserva);
        LocalDateTime momento = LocalDateTime.now(relojHotel).truncatedTo(ChronoUnit.MICROS);
        if (momento.isBefore(reserva.getFechaHoraCheckIn())) {
            throw new Conflicto("La salida no puede ser anterior al momento de entrada. Revisa el horario del hotel.");
        }
        boolean otraEstancia = hayOtraEstancia(reserva);
        reserva.registrarSalida(momento);
        if (otraEstancia) {
            habitacion.ocupar();
        } else {
            habitacion.liberar();
        }
    }

    private Habitacion bloquearHabitacion(Reservacion reserva) {
        return habitaciones.buscarParaActualizar(reserva.getHabitacion().getIdHabitacion())
                .orElseThrow(() -> new Conflicto("La habitación asignada ya no está disponible."));
    }

    private boolean hayOtraEstancia(Reservacion reserva) {
        return reservaciones.existsByHabitacion_IdHabitacionAndEstadoAndIdReservacionNot(
                reserva.getHabitacion().getIdHabitacion(), EstadoReservacion.EN_CURSO, reserva.getIdReservacion());
    }

    private boolean admiteEntradaEn(Reservacion reserva, LocalDate dia) {
        return !dia.isBefore(reserva.getFechaEntrada()) && dia.isBefore(reserva.getFechaSalida());
    }

    private Movimiento movimiento(Reservacion reserva, LocalDate hoy) {
        Habitacion habitacion = reserva.getHabitacion();
        String motivo = switch (reserva.getEstado()) {
            case CONFIRMADA -> !admiteEntradaEn(reserva, hoy) ? "Fuera del periodo de entrada."
                    : habitacion.getEstadoOperativo() == EstadoHabitacion.OCUPADA ? "Habitación ocupada." : null;
            case EN_CURSO -> reserva.getFechaHoraCheckIn() == null ? "Entrada histórica sin hora registrada. Requiere revisión."
                    : null;
            default -> null;
        };
        return new Movimiento(reserva.getIdReservacion(), reserva.getHuesped().getNombre(), habitacion.getNumero(),
                reserva.getFechaEntrada(), reserva.getFechaSalida(), reserva.getEstado(), habitacion.getEstadoOperativo(),
                reserva.getFechaHoraCheckIn(), reserva.getFechaHoraCheckOut(), motivo);
    }

    public record Movimiento(Long id, String huesped, String habitacion, LocalDate entrada, LocalDate salida,
                             EstadoReservacion estado, EstadoHabitacion estadoHabitacion, LocalDateTime checkIn,
                             LocalDateTime checkOut, String motivo) {
    }

    public static class Conflicto extends RuntimeException {
        public Conflicto(String mensaje) {
            super(mensaje);
        }
    }
}

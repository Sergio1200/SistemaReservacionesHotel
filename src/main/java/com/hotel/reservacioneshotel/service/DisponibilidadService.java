package com.hotel.reservacioneshotel.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.repository.HabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisponibilidadService {

    private static final int MAXIMO_NOCHES = 365;
    private static final LocalDate ULTIMA_FECHA_MYSQL = LocalDate.of(9999, 12, 31);

    private final HabitacionRepository habitaciones;
    private final Clock relojHotel;

    public DisponibilidadService(HabitacionRepository habitaciones, Clock relojHotel) {
        this.habitaciones = habitaciones;
        this.relojHotel = relojHotel;
    }

    public LocalDate hoy() {
        return LocalDate.now(relojHotel);
    }

    @Transactional(readOnly = true)
    public List<HabitacionDisponible> consultar(LocalDate entrada, LocalDate salida, int huespedes) {
        Periodo periodo = validarPeriodo(entrada, salida);
        return buscar(periodo, huespedes, null, null)
                .stream()
                .map(habitacion -> cotizar(habitacion, periodo.noches()))
                .toList();
    }

    boolean estaDisponible(Long idHabitacion, Periodo periodo, int huespedes, Long excluirId) {
        return !buscar(periodo, huespedes, idHabitacion, excluirId).isEmpty();
    }

    private List<Habitacion> buscar(Periodo periodo, int huespedes, Long idHabitacion, Long excluirId) {
        return habitaciones.buscarDisponibles(periodo.entrada(), periodo.salida(), huespedes, periodo.hoy(),
                EstadoHabitacion.DISPONIBLE, EstadoReservacion.EN_CURSO, EstadoReservacion.QUE_REQUIEREN_CUPO,
                idHabitacion, excluirId);
    }

    Periodo validarPeriodo(LocalDate entrada, LocalDate salida) {
        LocalDate hoy = hoy();
        if (entrada.isBefore(hoy)) {
            throw new PeriodoInvalido("entrada", "La fecha de entrada debe ser hoy o posterior.");
        }
        if (!salida.isAfter(entrada)) {
            throw new PeriodoInvalido("salida", "La fecha de salida debe ser posterior a la fecha de entrada.");
        }
        if (salida.isAfter(ULTIMA_FECHA_MYSQL)) {
            throw new PeriodoInvalido("salida", "La fecha de salida debe corresponder a un año de hasta cuatro cifras.");
        }
        long noches = ChronoUnit.DAYS.between(entrada, salida);
        if (noches > MAXIMO_NOCHES) {
            throw new PeriodoInvalido("salida", "La consulta admite estancias de hasta " + MAXIMO_NOCHES + " noches.");
        }
        return new Periodo(entrada, salida, hoy, noches);
    }

    private HabitacionDisponible cotizar(Habitacion habitacion, long noches) {
        BigDecimal precio = habitacion.getPrecioPorNoche();
        BigDecimal total = precio.multiply(BigDecimal.valueOf(noches));
        return new HabitacionDisponible(habitacion.getIdHabitacion(), habitacion.getNumero(), habitacion.getTipoHabitacion().getNombre(),
                habitacion.getTipoHabitacion().getCapacidad(), precio, noches, total);
    }

    public record HabitacionDisponible(Long id, String numero, String tipo, int capacidad, BigDecimal precioPorNoche,
                                       long noches, BigDecimal costoEstimado) {
    }

    record Periodo(LocalDate entrada, LocalDate salida, LocalDate hoy, long noches) {
    }

    public static class PeriodoInvalido extends RuntimeException {

        private final String campo;

        public PeriodoInvalido(String campo, String mensaje) {
            super(mensaje);
            this.campo = campo;
        }

        public String getCampo() {
            return campo;
        }
    }
}

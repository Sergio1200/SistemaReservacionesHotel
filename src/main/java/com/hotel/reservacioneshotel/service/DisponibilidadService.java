package com.hotel.reservacioneshotel.service;

import java.math.BigDecimal;
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

    public DisponibilidadService(HabitacionRepository habitaciones) {
        this.habitaciones = habitaciones;
    }

    public LocalDate hoy() {
        return LocalDate.now();
    }

    @Transactional(readOnly = true)
    public List<HabitacionDisponible> consultar(LocalDate entrada, LocalDate salida, int huespedes) {
        LocalDate hoy = hoy();
        long noches = validarPeriodo(entrada, salida, hoy);
        return habitaciones.buscarDisponibles(entrada, salida, huespedes, hoy, EstadoHabitacion.DISPONIBLE,
                        EstadoReservacion.EN_CURSO, EstadoReservacion.QUE_REQUIEREN_CUPO)
                .stream()
                .map(habitacion -> cotizar(habitacion, noches))
                .toList();
    }

    private long validarPeriodo(LocalDate entrada, LocalDate salida, LocalDate hoy) {
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
        return noches;
    }

    private HabitacionDisponible cotizar(Habitacion habitacion, long noches) {
        BigDecimal precio = habitacion.getPrecioPorNoche();
        BigDecimal total = precio.multiply(BigDecimal.valueOf(noches));
        return new HabitacionDisponible(habitacion.getNumero(), habitacion.getTipoHabitacion().getNombre(),
                habitacion.getTipoHabitacion().getCapacidad(), precio, noches, total);
    }

    public record HabitacionDisponible(String numero, String tipo, int capacidad, BigDecimal precioPorNoche,
                                       long noches, BigDecimal costoEstimado) {
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

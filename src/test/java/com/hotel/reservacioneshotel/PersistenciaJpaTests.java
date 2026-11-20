package com.hotel.reservacioneshotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.pagos.Pago;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.domain.usuarios.Administrador;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.domain.usuarios.Recepcionista;
import com.hotel.reservacioneshotel.repository.EmpleadoRepository;
import com.hotel.reservacioneshotel.repository.PagoRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.profiles.active=${hotel.test.profile:test}")
@Transactional
class PersistenciaJpaTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ReservacionRepository reservaciones;

    @Autowired
    private PagoRepository pagos;

    @Autowired
    private EmpleadoRepository empleados;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void guardaYRecuperaUnaReservacionConDosPagos() {
        Reservacion reservacion = guardarReservacion();
        LocalDateTime fechaPago = LocalDateTime.of(2027, 1, 10, 14, 30);
        Pago primerIntento = pagos.save(new Pago(
                reservacion, fechaPago, new BigDecimal("400.00"), "SIMULADO", EstadoPago.RECHAZADO));
        Pago segundoIntento = pagos.saveAndFlush(new Pago(
                reservacion, fechaPago.plusMinutes(5), new BigDecimal("400.00"), "SIMULADO", EstadoPago.PAGADO));
        entityManager.clear();

        Reservacion guardada = reservaciones.findById(reservacion.getIdReservacion()).orElseThrow();
        assertThat(guardada.getHuesped().getCorreo()).isEqualTo("huesped@example.test");
        assertThat(guardada.getHabitacion().getTipoHabitacion().getCapacidad()).isEqualTo(2);
        assertThat(guardada.getFechaSalida()).isEqualTo(LocalDate.of(2027, 1, 12));
        assertThat(guardada.getPrecioNocheAplicado()).isEqualByComparingTo("200.00");
        assertThat(guardada.getCostoTotal()).isEqualByComparingTo("400.00");
        assertThat(guardada.getEstado()).isEqualTo(EstadoReservacion.PENDIENTE);

        List<Pago> pagosGuardados = pagos.findAllById(List.of(primerIntento.getIdPago(), segundoIntento.getIdPago()));
        assertThat(pagosGuardados).extracting(Pago::getEstado)
                .containsExactlyInAnyOrder(EstadoPago.RECHAZADO, EstadoPago.PAGADO);
        assertThat(pagosGuardados).allSatisfy(pago -> {
            assertThat(pago.getReservacion().getIdReservacion()).isEqualTo(guardada.getIdReservacion());
            assertThat(pago.getMonto()).isEqualByComparingTo("400.00");
        });
    }

    @Test
    void recuperaAmbosSubtiposMedianteElRepositorioDeEmpleados() {
        String hashDePrueba = "$2b$12$" + ".".repeat(53);
        Recepcionista recepcionista = empleados.save(new Recepcionista(
                "Recepcionista de prueba", "recepcion_prueba", hashDePrueba, true));
        Administrador administrador = empleados.saveAndFlush(new Administrador(
                "Administrador de prueba", "admin_prueba", hashDePrueba, true));
        entityManager.clear();

        assertThat(empleados.findById(recepcionista.getIdEmpleado()).orElseThrow())
                .isInstanceOf(Recepcionista.class);
        assertThat(empleados.findById(administrador.getIdEmpleado()).orElseThrow())
                .isInstanceOf(Administrador.class);
    }

    @Test
    void laBaseDeDatosRechazaUnaSalidaIgualALaEntrada() {
        Reservacion reservacion = guardarReservacion();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE reservaciones SET fecha_salida = fecha_entrada WHERE id_reservacion = ?",
                reservacion.getIdReservacion()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_reservaciones_fechas");
    }

    @Test
    void laBaseDeDatosRechazaUnPagoSinReservacion() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO pagos (id_reservacion, fecha, monto, metodo_pago, estado)
                VALUES (?, ?, ?, ?, ?)
                """, Long.MAX_VALUE, LocalDateTime.of(2027, 1, 10, 14, 30),
                new BigDecimal("100.00"), "SIMULADO", EstadoPago.PENDIENTE.name()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Reservacion guardarReservacion() {
        Huesped huesped = new Huesped("Huésped de prueba", null, "huesped@example.test");
        TipoHabitacion tipo = new TipoHabitacion("Doble de prueba", 2, null);
        entityManager.persist(huesped);
        entityManager.persist(tipo);

        BigDecimal precioNoche = new BigDecimal("200.00");
        Habitacion habitacion = new Habitacion("P-101", 1, EstadoHabitacion.DISPONIBLE, precioNoche, tipo);
        entityManager.persist(habitacion);

        LocalDate entrada = LocalDate.of(2027, 1, 10);
        return reservaciones.saveAndFlush(new Reservacion(huesped, habitacion, entrada, entrada.plusDays(2),
                2, precioNoche, new BigDecimal("400.00"), EstadoReservacion.PENDIENTE));
    }
}

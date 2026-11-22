package com.hotel.reservacioneshotel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.pagos.Pago;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.service.DisponibilidadService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=${hotel.test.profile:test}")
@AutoConfigureMockMvc
@Transactional
class EstanciaMvcTests {

    private static final String RUTA = "/check-in-check-out";
    private static final BigDecimal TARIFA = new BigDecimal("380.75");
    private static final BigDecimal TOTAL = new BigDecimal("761.50");
    private static final LocalDateTime MOMENTO = LocalDateTime.of(2027, 2, 11, 23, 50);
    private static final LocalDate HOY = MOMENTO.toLocalDate();

    private Habitacion habitacion;
    private Huesped titular;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;
    @Autowired
    private DisponibilidadService disponibilidad;

    @TestConfiguration
    static class HorarioDePrueba {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(Instant.parse("2027-02-12T05:50:00Z"), ZoneId.of("America/Mexico_City"));
        }
    }

    @BeforeEach
    void prepararHabitacion() {
        titular = new Huesped("Titular Estancia QA", null, "estancia-titular@example.test");
        entidades.persist(titular);
        TipoHabitacion tipo = new TipoHabitacion("Tipo Estancia QA", 2, null);
        entidades.persist(tipo);
        habitacion = new Habitacion("ESTANCIA-QA", 1, EstadoHabitacion.DISPONIBLE, TARIFA, tipo);
        entidades.persist(habitacion);
    }

    @Test
    void registraEntradaYSalidaEnHorarioDelHotelYConservaPagosTarifaYFechasProgramadas() throws Exception {
        Reservacion reserva = reservacion(EstadoReservacion.CONFIRMADA);
        Long id = reserva.getIdReservacion();
        LocalDateTime fechaPago = MOMENTO.minusDays(1);
        Pago pago = new Pago(reserva, fechaPago, TOTAL, "EFECTIVO", EstadoPago.PAGADO);
        entidades.persist(pago);
        Long pagoId = pago.getIdPago();

        mvc.perform(operacion(id, "check-in"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(RUTA + "?q=" + id))
                .andExpect(flash().attribute("mensajeExito", containsString("Check-in registrado")));
        Reservacion entrada = releer(id);
        assertThat(entrada.getEstado()).isEqualTo(EstadoReservacion.EN_CURSO);
        assertThat(entrada.getHabitacion().getEstadoOperativo()).isEqualTo(EstadoHabitacion.OCUPADA);
        assertThat(entrada.getFechaHoraCheckIn()).isEqualTo(MOMENTO);
        assertThat(entrada.getFechaHoraCheckOut()).isNull();

        mvc.perform(operacion(id, "check-out"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensajeExito", containsString("Check-out registrado")));
        Reservacion salida = releer(id);
        assertThat(salida.getEstado()).isEqualTo(EstadoReservacion.FINALIZADA);
        assertThat(salida.getHabitacion().getEstadoOperativo()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        assertThat(salida.getFechaHoraCheckIn()).isEqualTo(MOMENTO);
        assertThat(salida.getFechaHoraCheckOut()).isEqualTo(MOMENTO);
        assertThat(salida.getFechaEntrada()).isEqualTo(HOY);
        assertThat(salida.getFechaSalida()).isEqualTo(HOY.plusDays(2));
        assertThat(salida.getPrecioNocheAplicado()).isEqualByComparingTo(TARIFA);
        assertThat(salida.getCostoTotal()).isEqualByComparingTo(TOTAL);
        assertThat(entidades.createQuery("from Pago p where p.reservacion.idReservacion = :id", Pago.class)
                .setParameter("id", id).getResultList()).singleElement().satisfies(guardado -> {
                    assertThat(guardado.getIdPago()).isEqualTo(pagoId);
                    assertThat(guardado.getEstado()).isEqualTo(EstadoPago.PAGADO);
                    assertThat(guardado.getMonto()).isEqualByComparingTo(TOTAL);
                    assertThat(guardado.getFecha()).isEqualTo(fechaPago);
                });
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReservacion.class, names = {"PENDIENTE", "CANCELADA", "FINALIZADA"})
    void soloPermiteEntradaDeConfirmadas(EstadoReservacion estado) throws Exception {
        Long id = reservacion(estado).getIdReservacion();
        mvc.perform(operacion(id, "check-in")).andExpect(status().isConflict())
                .andExpect(content().string(containsString("reservación confirmada")));
        Reservacion guardada = releer(id);
        assertThat(guardada.getEstado()).isEqualTo(estado);
        assertThat(guardada.getFechaHoraCheckIn()).isNull();
        assertThat(guardada.getHabitacion().getEstadoOperativo()).isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    void rechazaEntrarAntesDeLaLlegadaOElDiaDeSalida() throws Exception {
        Long futura = reservacion(EstadoReservacion.CONFIRMADA, HOY.plusDays(1), HOY.plusDays(3), 1).getIdReservacion();
        Long venceHoy = reservacion(EstadoReservacion.CONFIRMADA, HOY.minusDays(2), HOY, 1).getIdReservacion();
        for (Long id : List.of(futura, venceHoy)) {
            mvc.perform(operacion(id, "check-in")).andExpect(status().isConflict())
                    .andExpect(content().string(containsString("antes del día de salida")));
        }
    }

    @Test
    void permiteEntrarUnDiaDespuesDeLaLlegadaSiTodaviaNoEsLaSalida() throws Exception {
        Long id = reservacion(EstadoReservacion.CONFIRMADA, HOY.minusDays(1), HOY.plusDays(1), 1).getIdReservacion();
        mvc.perform(operacion(id, "check-in")).andExpect(status().is3xxRedirection());
        assertThat(releer(id).getEstado()).isEqualTo(EstadoReservacion.EN_CURSO);
    }

    @Test
    void rechazaCapacidadInsuficienteYOtraEstanciaAunqueElEstadoOperativoSeaInconsistente() throws Exception {
        Long exceso = reservacion(EstadoReservacion.CONFIRMADA, HOY, HOY.plusDays(2), 3).getIdReservacion();
        mvc.perform(operacion(exceso, "check-in")).andExpect(status().isConflict())
                .andExpect(content().string(containsString("supera la capacidad")));
        reservacion(EstadoReservacion.EN_CURSO, HOY.minusDays(3), HOY.minusDays(1), 1);
        Long id = reservacion(EstadoReservacion.CONFIRMADA).getIdReservacion();
        mvc.perform(operacion(id, "check-in")).andExpect(status().isConflict())
                .andExpect(content().string(containsString("habitación está ocupada")));
        assertThat(releer(id).getEstado()).isEqualTo(EstadoReservacion.CONFIRMADA);
    }

    @Test
    void rechazaEntrarEnUnaHabitacionMarcadaOcupadaSinEstanciaRegistrada() throws Exception {
        habitacion.ocupar();
        mvc.perform(operacion(reservacion(EstadoReservacion.CONFIRMADA).getIdReservacion(), "check-in"))
                .andExpect(status().isConflict()).andExpect(content().string(containsString("habitación está ocupada")));
    }

    @Test
    void rechazaEntradasYSalidasDuplicadas() throws Exception {
        Long id = reservacion(EstadoReservacion.CONFIRMADA).getIdReservacion();
        mvc.perform(operacion(id, "check-in")).andExpect(status().is3xxRedirection());
        mvc.perform(operacion(id, "check-in")).andExpect(status().isConflict())
                .andExpect(content().string(containsString("entrada de esta reservación ya se registró")));
        mvc.perform(operacion(id, "check-out")).andExpect(status().is3xxRedirection());
        mvc.perform(operacion(id, "check-out")).andExpect(status().isConflict())
                .andExpect(content().string(containsString("ya está finalizada")));
        assertThat(releer(id).getFechaHoraCheckIn()).isEqualTo(MOMENTO);
        assertThat(releer(id).getFechaHoraCheckOut()).isEqualTo(MOMENTO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReservacion.class, names = {"PENDIENTE", "CONFIRMADA", "CANCELADA", "EN_CURSO"})
    void requiereEntradaRegistradaParaSalir(EstadoReservacion estado) throws Exception {
        Long id = reservacion(estado).getIdReservacion();
        mvc.perform(operacion(id, "check-out")).andExpect(status().isConflict())
                .andExpect(content().string(containsString("tener una entrada registrada")));
        assertThat(releer(id).getEstado()).isEqualTo(estado);
        assertThat(releer(id).getFechaHoraCheckOut()).isNull();
    }

    @Test
    void permiteSalidaTardiaPeroMantieneLaOcupacionSiHayOtraEstanciaActiva() throws Exception {
        Reservacion tardia = reservacion(EstadoReservacion.CONFIRMADA, HOY.minusDays(4), HOY.minusDays(1), 1);
        tardia.registrarEntrada(MOMENTO.minusDays(4));
        habitacion.ocupar();
        reservacion(EstadoReservacion.EN_CURSO);
        mvc.perform(operacion(tardia.getIdReservacion(), "check-out")).andExpect(status().is3xxRedirection());
        Reservacion salida = releer(tardia.getIdReservacion());
        assertThat(salida.getEstado()).isEqualTo(EstadoReservacion.FINALIZADA);
        assertThat(salida.getHabitacion().getEstadoOperativo()).isEqualTo(EstadoHabitacion.OCUPADA);
    }

    @Test
    void rechazaSalidaAnteriorALaHoraDeEntrada() throws Exception {
        Reservacion reserva = reservacion(EstadoReservacion.CONFIRMADA);
        reserva.registrarEntrada(MOMENTO.plusMinutes(1));
        mvc.perform(operacion(reserva.getIdReservacion(), "check-out")).andExpect(status().isConflict())
                .andExpect(content().string(containsString("salida no puede ser anterior")));
        assertThat(releer(reserva.getIdReservacion()).getFechaHoraCheckOut()).isNull();
    }

    @Test
    void conservaDisponibilidadPorFechasYActivaLaLiberacionAnticipada() throws Exception {
        Long idHabitacion = habitacion.getIdHabitacion();
        Long id = reservacion(EstadoReservacion.CONFIRMADA).getIdReservacion();
        reservacion(EstadoReservacion.CONFIRMADA, HOY.plusDays(3), HOY.plusDays(4), 1);
        mvc.perform(operacion(id, "check-in")).andExpect(status().is3xxRedirection());
        assertThat(ofrece(idHabitacion, HOY, HOY.plusDays(1))).isFalse();
        assertThat(ofrece(idHabitacion, HOY.plusDays(2), HOY.plusDays(3))).isTrue();
        mvc.perform(operacion(id, "check-out")).andExpect(status().is3xxRedirection());
        assertThat(ofrece(idHabitacion, HOY, HOY.plusDays(1))).isTrue();
        assertThat(ofrece(idHabitacion, HOY.plusDays(3), HOY.plusDays(4))).isFalse();
    }

    @Test
    void muestraLosGruposYFiltraPorNumeroONombre() throws Exception {
        Long llegada = reservacion(EstadoReservacion.CONFIRMADA).getIdReservacion();
        reservacion(EstadoReservacion.EN_CURSO);
        reservacion(EstadoReservacion.FINALIZADA);
        Long pendiente = reservacion(EstadoReservacion.PENDIENTE).getIdReservacion();
        mvc.perform(conRecepcion(get(RUTA).param("q", "titular estancia")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Llegadas pendientes")))
                .andExpect(content().string(containsString("Estancias en curso")))
                .andExpect(content().string(containsString("Reservaciones finalizadas")))
                .andExpect(content().string(containsString("Entrada histórica sin hora registrada")))
                .andExpect(content().string(not(containsString("estancia-" + pendiente + "\""))));
        mvc.perform(conRecepcion(get(RUTA).param("q", "#" + llegada)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Reservación #" + llegada)))
                .andExpect(content().string(containsString("Registrar check-in")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("Registrar check-out"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HUESPED", "ADMINISTRADOR"})
    void soloRecepcionPuedeConsultarYRegistrarMovimientos(String rol) throws Exception {
        Long id = reservacion(EstadoReservacion.CONFIRMADA).getIdReservacion();
        mvc.perform(get(RUTA).with(user("otro@example.test").roles(rol))).andExpect(status().isForbidden());
        for (String accion : List.of("check-in", "check-out")) {
            mvc.perform(post(ruta(id, accion)).with(user("otro@example.test").roles(rol)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void requiereCsrfYPostYValidaIdentificadores() throws Exception {
        Long id = reservacion(EstadoReservacion.CONFIRMADA).getIdReservacion();
        for (String accion : List.of("check-in", "check-out")) {
            mvc.perform(conRecepcion(post(ruta(id, accion)))).andExpect(status().isForbidden());
            mvc.perform(conRecepcion(get(ruta(id, accion)))).andExpect(status().isMethodNotAllowed());
        }
        mvc.perform(operacion(0L, "check-in")).andExpect(status().isBadRequest());
        mvc.perform(conRecepcion(post(RUTA + "/texto/check-out")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(operacion(Long.MAX_VALUE, "check-in")).andExpect(status().isNotFound());
        assertThat(releer(id).getEstado()).isEqualTo(EstadoReservacion.CONFIRMADA);
    }

    private Reservacion reservacion(EstadoReservacion estado) {
        return reservacion(estado, HOY, HOY.plusDays(2), 1);
    }

    private Reservacion reservacion(EstadoReservacion estado, LocalDate entrada, LocalDate salida, int personas) {
        Reservacion reserva = new Reservacion(titular, habitacion, entrada, salida, personas, TARIFA, TOTAL, estado);
        entidades.persist(reserva);
        return reserva;
    }

    private Reservacion releer(Long id) {
        entidades.flush();
        entidades.clear();
        return entidades.find(Reservacion.class, id);
    }

    private boolean ofrece(Long idHabitacion, LocalDate entrada, LocalDate salida) {
        return disponibilidad.consultar(entrada, salida, 1).stream().anyMatch(h -> h.id().equals(idHabitacion));
    }

    private MockHttpServletRequestBuilder operacion(Long id, String accion) {
        return conRecepcion(post(ruta(id, accion))).with(csrf());
    }

    private MockHttpServletRequestBuilder conRecepcion(MockHttpServletRequestBuilder peticion) {
        return peticion.with(user("recepcion-estancia@example.test").roles("RECEPCIONISTA"));
    }

    private String ruta(Long id, String accion) {
        return RUTA + "/" + id + "/" + accion;
    }
}

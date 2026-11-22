package com.hotel.reservacioneshotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.pagos.Pago;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.PagoRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import com.hotel.reservacioneshotel.service.ReservacionService;
import com.hotel.reservacioneshotel.service.ReservacionService.Operador;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@EnabledIfSystemProperty(named = "hotel.test.profile", matches = "mysql")
@SpringBootTest(properties = "spring.profiles.active=${hotel.test.profile:test}")
@AutoConfigureMockMvc
class PagoConcurrenciaMySqlTests {

    private static final BigDecimal TOTAL = new BigDecimal("100.00");
    private static final String RECEPCION = "recepcion@example.test";
    private final String clave = UUID.randomUUID().toString().substring(0, 10);
    private TransactionTemplate transaccion;
    private TipoHabitacion tipo;
    private Habitacion habitacion;
    private Huesped huesped;
    private Reservacion reserva;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;
    @Autowired
    private ReservacionRepository reservaciones;
    @Autowired
    private ReservacionService servicioReservaciones;
    @Autowired
    private PagoRepository pagos;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager gestor;

    @BeforeEach
    void prepararDatosConfirmados() {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8.");
        transaccion = new TransactionTemplate(gestor);
        transaccion.executeWithoutResult(estado -> {
            huesped = new Huesped("Titular QA", null, clave + "@example.test");
            entidades.persist(huesped);
            tipo = new TipoHabitacion("Tipo pago " + clave, 2, null);
            entidades.persist(tipo);
            habitacion = new Habitacion("P-" + clave, 1, EstadoHabitacion.DISPONIBLE, TOTAL, tipo);
            entidades.persist(habitacion);
            LocalDate entrada = LocalDate.now().plusDays(10);
            reserva = new Reservacion(huesped, habitacion, entrada, entrada.plusDays(1), 1, TOTAL, TOTAL,
                    EstadoReservacion.PENDIENTE);
            entidades.persist(reserva);
        });
    }

    @AfterEach
    void limpiarDatosFicticios() {
        if (reserva == null || reserva.getIdReservacion() == null) {
            return;
        }
        transaccion.executeWithoutResult(estado -> {
            jdbc.update("DELETE FROM pagos WHERE id_reservacion = ?", reserva.getIdReservacion());
            jdbc.update("DELETE FROM reservaciones WHERE id_reservacion = ?", reserva.getIdReservacion());
            jdbc.update("DELETE FROM habitaciones WHERE id_habitacion = ?", habitacion.getIdHabitacion());
            jdbc.update("DELETE FROM tipos_habitacion WHERE id_tipo = ?", tipo.getIdTipo());
            jdbc.update("DELETE FROM huespedes WHERE id_huesped = ?", huesped.getIdHuesped());
        });
    }

    @Test
    void dosRegistrosQueExcederianElTotalSoloCreanUnPago() {
        List<MvcResult> resultados = ejecutarEnParalelo(alta("80", UUID.randomUUID()), alta("80", UUID.randomUUID()));
        assertThat(resultados).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(200, 302);
        assertThat(registros()).isEqualTo(1);
        assertThat(pagos.sumarMontos(reserva.getIdReservacion(), List.of(EstadoPago.PENDIENTE))).isEqualByComparingTo("80.00");
    }

    @Test
    void reenviosSimultaneosDelMismoIntentoConservanUnSoloRegistro() {
        UUID intento = UUID.randomUUID();
        List<MvcResult> resultados = ejecutarEnParalelo(alta("30", intento), alta("30", intento));
        assertThat(resultados).allMatch(r -> r.getResponse().getStatus() == 302);
        assertThat(resultados.stream().map(r -> r.getFlashMap().get("mensajeExito").toString()))
                .anyMatch(mensaje -> mensaje.contains("ya estaba registrado"));
        assertThat(registros()).isEqualTo(1);
    }

    @Test
    void dosAprobacionesSimultaneasDelMismoPagoSoloAplicanUnaVez() {
        Pago unico = pagoEnProceso("100");
        List<MvcResult> resultados = ejecutarEnParalelo(aprobar(unico), aprobar(unico));
        assertThat(resultados).filteredOn(r -> r.getFlashMap().containsKey("mensajeExito")).hasSize(1);
        assertThat(resultados).filteredOn(r -> r.getFlashMap().containsKey("mensajeError")).hasSize(1);
        assertThat(registros()).isEqualTo(1);
        assertThat(totalPagado()).isEqualByComparingTo(TOTAL);
        comprobarEstado(EstadoReservacion.CONFIRMADA);
    }

    @Test
    void dosPagosValidosSimultaneosCompletanElTotalSinExcederlo() {
        Pago parcial = pagoEnProceso("40");
        Pago resto = pagoEnProceso("60");
        List<MvcResult> resultados = ejecutarEnParalelo(aprobar(parcial), aprobar(resto));
        assertThat(resultados).allMatch(r -> r.getFlashMap().containsKey("mensajeExito"));
        assertThat(totalPagado()).isEqualByComparingTo(TOTAL);
        comprobarEstado(EstadoReservacion.CONFIRMADA);
    }

    @Test
    void validacionQueEsperaUnaCancelacionNoAlteraElPagoNiLaReserva() {
        Pago enProceso = pagoEnProceso("100");
        try (ExecutorService tareas = Executors.newSingleThreadExecutor()) {
            Future<MvcResult> futuro = conReservaBloqueada(() -> {
                var solicitud = tareas.submit(() -> mvc.perform(aprobar(enProceso)).andReturn());
                exigeEspera(solicitud);
                servicioReservaciones.cancelar(new Operador(RECEPCION, true), reserva.getIdReservacion());
                return solicitud;
            });
            assertThat(terminar(futuro).getFlashMap().containsKey("mensajeError")).isTrue();
            comprobarEstado(EstadoReservacion.CANCELADA);
            assertThat(pagos.findById(enProceso.getIdPago()).orElseThrow().getEstado()).isEqualTo(EstadoPago.PROCESANDO);
            assertThat(totalPagado()).isEqualByComparingTo("0.00");
        }
    }

    @Test
    void agotamientoDeEsperaDejaElPagoEnProcesoYPermiteReintentar() throws Exception {
        Pago enProceso = pagoEnProceso("100");
        try (ExecutorService tareas = Executors.newSingleThreadExecutor()) {
            conReservaBloqueada(() -> {
                Future<MvcResult> fallo = tareas.submit(() -> aprobarConEsperaCorta(enProceso));
                assertThat(terminar(fallo).getResponse().getStatus()).isEqualTo(409);
                assertThat(totalPagado()).isEqualByComparingTo("0.00");
                return null;
            });
            comprobarEstado(EstadoReservacion.PENDIENTE);
            assertThat(pagos.findById(enProceso.getIdPago()).orElseThrow().getEstado()).isEqualTo(EstadoPago.PROCESANDO);
            assertThat(mvc.perform(aprobar(enProceso)).andReturn().getFlashMap().containsKey("mensajeExito")).isTrue();
            comprobarEstado(EstadoReservacion.CONFIRMADA);
            assertThat(totalPagado()).isEqualByComparingTo(TOTAL);
        }
    }

    private MvcResult aprobarConEsperaCorta(Pago pago) {
        return transaccion.execute(estado -> {
            Integer esperaOriginal = jdbc.queryForObject("SELECT @@session.innodb_lock_wait_timeout", Integer.class);
            jdbc.execute("SET SESSION innodb_lock_wait_timeout = 1");
            try {
                return mvc.perform(aprobar(pago)).andReturn();
            } catch (Exception exception) {
                throw new AssertionError("Falló la solicitud bajo bloqueo", exception);
            } finally {
                estado.setRollbackOnly();
                jdbc.execute("SET SESSION innodb_lock_wait_timeout = " + esperaOriginal);
            }
        });
    }

    private List<MvcResult> ejecutarEnParalelo(MockHttpServletRequestBuilder a, MockHttpServletRequestBuilder b) {
        try (ExecutorService tareas = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> futuras = conReservaBloqueada(() -> {
                var primera = tareas.submit(() -> mvc.perform(a).andReturn());
                var segunda = tareas.submit(() -> mvc.perform(b).andReturn());
                exigeEspera(primera);
                exigeEspera(segunda);
                return List.of(primera, segunda);
            });
            return List.of(terminar(futuras.getFirst()), terminar(futuras.getLast()));
        }
    }

    private <T> T conReservaBloqueada(Supplier<T> trabajo) {
        return transaccion.execute(estado -> {
            reservaciones.buscarParaActualizar(reserva.getIdReservacion()).orElseThrow();
            return trabajo.get();
        });
    }

    private void exigeEspera(Future<MvcResult> resultado) {
        assertThrows(TimeoutException.class, () -> resultado.get(350, TimeUnit.MILLISECONDS));
    }

    private MvcResult terminar(Future<MvcResult> resultado) {
        try {
            return resultado.get(12, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Verificación interrumpida", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new AssertionError("La solicitud no terminó dentro del límite", exception);
        }
    }

    private MockHttpServletRequestBuilder alta(String monto, UUID intento) {
        return recepcion(post(ruta()).param("monto", monto).param("metodo", "EFECTIVO").param("intento", intento.toString()));
    }

    private MockHttpServletRequestBuilder aprobar(Pago pago) {
        return recepcion(post(ruta() + "/" + pago.getIdPago() + "/validar").param("decision", "APROBAR"));
    }

    private MockHttpServletRequestBuilder recepcion(MockHttpServletRequestBuilder peticion) {
        return peticion.with(user(RECEPCION).roles("RECEPCIONISTA")).with(csrf());
    }

    private String ruta() {
        return "/reservaciones/" + reserva.getIdReservacion() + "/pagos";
    }

    private Pago pagoEnProceso(String monto) {
        return pagos.saveAndFlush(new Pago(reserva, LocalDateTime.now(), new BigDecimal(monto), "EFECTIVO", EstadoPago.PROCESANDO));
    }

    private int registros() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE id_reservacion = ?", Integer.class, reserva.getIdReservacion());
    }

    private BigDecimal totalPagado() {
        return pagos.sumarMontos(reserva.getIdReservacion(), List.of(EstadoPago.PAGADO));
    }

    private void comprobarEstado(EstadoReservacion estado) {
        assertThat(reservaciones.findById(reserva.getIdReservacion()).orElseThrow().getEstado()).isEqualTo(estado);
    }
}

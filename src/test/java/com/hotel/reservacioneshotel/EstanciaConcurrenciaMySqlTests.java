package com.hotel.reservacioneshotel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.HabitacionRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import com.hotel.reservacioneshotel.service.EstanciaService;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfSystemProperty(named = "hotel.test.profile", matches = "mysql")
@SpringBootTest(properties = "spring.profiles.active=${hotel.test.profile:test}")
@AutoConfigureMockMvc
class EstanciaConcurrenciaMySqlTests {

    private static final BigDecimal TARIFA = new BigDecimal("100.00");
    private TransactionTemplate transaccion;
    private Long idHuesped;
    private Long idTipo;
    private Long idHabitacion;
    private Long idReserva;
    private Long segundaReserva;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;
    @Autowired
    private ReservacionRepository reservaciones;
    @Autowired
    private HabitacionRepository habitaciones;
    @Autowired
    private EstanciaService estancias;
    @Autowired
    private Clock relojHotel;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager gestor;

    @BeforeEach
    void preparar() {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8.");
        transaccion = new TransactionTemplate(gestor);
        transaccion.executeWithoutResult(estado -> {
            String clave = UUID.randomUUID().toString().substring(0, 10);
            Huesped huesped = new Huesped("Estancia concurrente QA", null, clave + "@example.test");
            TipoHabitacion tipo = new TipoHabitacion("Tipo estancia " + clave, 2, null);
            entidades.persist(huesped);
            entidades.persist(tipo);
            Habitacion habitacion = new Habitacion("E-" + clave, 1, EstadoHabitacion.DISPONIBLE, TARIFA, tipo);
            entidades.persist(habitacion);
            idHuesped = huesped.getIdHuesped();
            idTipo = tipo.getIdTipo();
            idHabitacion = habitacion.getIdHabitacion();
            idReserva = crearReservacion();
        });
    }

    @AfterEach
    void limpiar() {
        if (idReserva != null) {
            transaccion.executeWithoutResult(estado -> {
                if (segundaReserva != null) {
                    jdbc.update("DELETE FROM reservaciones WHERE id_reservacion = ?", segundaReserva);
                }
                jdbc.update("DELETE FROM reservaciones WHERE id_reservacion = ?", idReserva);
                jdbc.update("DELETE FROM habitaciones WHERE id_habitacion = ?", idHabitacion);
                jdbc.update("DELETE FROM tipos_habitacion WHERE id_tipo = ?", idTipo);
                jdbc.update("DELETE FROM huespedes WHERE id_huesped = ?", idHuesped);
            });
        }
    }

    @Test
    void dosEntradasDeLaMismaReservaSoloRegistranUna() throws Exception {
        verificarUnaOperacion(idReserva, idReserva, "check-in", false);
        assertThat(estadoGuardado(idReserva)).isEqualTo(EstadoReservacion.EN_CURSO);
        assertThat(horasRegistradas("fecha_hora_check_in")).isEqualTo(1);
        assertThat(horasRegistradas("fecha_hora_check_out")).isZero();
        assertThat(estadoHabitacion()).isEqualTo(EstadoHabitacion.OCUPADA);
    }

    @Test
    void dosSalidasDeLaMismaEstanciaSoloLaFinalizanUnaVez() throws Exception {
        estancias.registrarEntrada(idReserva);
        LocalDateTime entrada = reservaciones.findById(idReserva).orElseThrow().getFechaHoraCheckIn();
        verificarUnaOperacion(idReserva, idReserva, "check-out", false);
        Reservacion finalizada = reservaciones.findById(idReserva).orElseThrow();
        assertThat(finalizada.getEstado()).isEqualTo(EstadoReservacion.FINALIZADA);
        assertThat(finalizada.getFechaHoraCheckIn()).isEqualTo(entrada);
        assertThat(finalizada.getFechaHoraCheckOut()).isAfterOrEqualTo(entrada);
        assertThat(horasRegistradas("fecha_hora_check_out")).isEqualTo(1);
        assertThat(estadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    void dosReservasCompitenPorLaMismaHabitacionYSoloUnaPuedeOcuparla() throws Exception {
        segundaReserva = transaccion.execute(estado -> crearReservacion());
        verificarUnaOperacion(idReserva, segundaReserva, "check-in", true);
        assertThat(List.of(estadoGuardado(idReserva), estadoGuardado(segundaReserva)))
                .containsExactlyInAnyOrder(EstadoReservacion.CONFIRMADA, EstadoReservacion.EN_CURSO);
        assertThat(horasRegistradas("fecha_hora_check_in")).isEqualTo(1);
        assertThat(estadoHabitacion()).isEqualTo(EstadoHabitacion.OCUPADA);
    }

    @Test
    void salidaYEntradaSimultaneasCambianLaOcupacionSinLiberarLaNuevaEstancia() throws Exception {
        segundaReserva = transaccion.execute(estado -> crearReservacion());
        estancias.registrarEntrada(idReserva);
        try (ExecutorService tareas = Executors.newSingleThreadExecutor()) {
            Future<MvcResult> entrada = bajoBloqueo(true, () -> {
                var solicitud = tareas.submit(() -> operar(segundaReserva, "check-in"));
                exigeEspera(solicitud);
                estancias.registrarSalida(idReserva);
                return solicitud;
            });
            assertThat(entrada.get(12, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(302);
        }
        assertThat(estadoGuardado(idReserva)).isEqualTo(EstadoReservacion.FINALIZADA);
        assertThat(estadoGuardado(segundaReserva)).isEqualTo(EstadoReservacion.EN_CURSO);
        assertThat(horasRegistradas("fecha_hora_check_out")).isEqualTo(1);
        assertThat(estadoHabitacion()).isEqualTo(EstadoHabitacion.OCUPADA);
    }

    @Test
    void esperaAgotadaNoDejaUnaEntradaParcialYElReintentoCompletaLaOperacion() throws Exception {
        try (ExecutorService tareas = Executors.newSingleThreadExecutor()) {
            transaccion.executeWithoutResult(estado -> {
                habitaciones.buscarParaActualizar(idHabitacion).orElseThrow();
                var intento = tareas.submit(() -> {
                    conEsperaCorta();
                    return null;
                });
                assertDoesNotThrow(() -> intento.get(12, TimeUnit.SECONDS));
                assertThat(estadoGuardado(idReserva)).isEqualTo(EstadoReservacion.CONFIRMADA);
                assertThat(horasRegistradas("fecha_hora_check_in")).isZero();
            });
        }
        assertThat(estadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        assertThat(operar(idReserva, "check-in").getResponse().getStatus()).isEqualTo(302);
        assertThat(estadoGuardado(idReserva)).isEqualTo(EstadoReservacion.EN_CURSO);
        assertThat(estadoHabitacion()).isEqualTo(EstadoHabitacion.OCUPADA);
    }

    private void conEsperaCorta() {
        transaccion.executeWithoutResult(estado -> {
            Integer espera = jdbc.queryForObject("SELECT @@session.innodb_lock_wait_timeout", Integer.class);
            jdbc.execute("SET SESSION innodb_lock_wait_timeout = 1");
            try {
                mvc.perform(post("/check-in-check-out/{id}/check-in", idReserva)
                                .with(user("recepcion-qa@example.test").roles("RECEPCIONISTA")).with(csrf()))
                        .andExpect(status().isConflict()).andExpect(content().string(containsString("vuelve a intentarlo")));
            } catch (Exception exception) {
                throw new AssertionError("Falló la solicitud bajo bloqueo", exception);
            } finally {
                estado.setRollbackOnly();
                jdbc.execute("SET SESSION innodb_lock_wait_timeout = " + espera);
            }
        });
    }

    private void verificarUnaOperacion(Long idA, Long idB, String accion, boolean bloquearHabitacion) throws Exception {
        try (ExecutorService tareas = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> solicitudes = bajoBloqueo(bloquearHabitacion, () -> {
                var primera = tareas.submit(() -> operar(idA, accion));
                var segunda = tareas.submit(() -> operar(idB, accion));
                exigeEspera(primera);
                exigeEspera(segunda);
                return List.of(primera, segunda);
            });
            assertThat(List.of(solicitudes.getFirst().get(12, TimeUnit.SECONDS).getResponse().getStatus(),
                    solicitudes.getLast().get(12, TimeUnit.SECONDS).getResponse().getStatus()))
                    .containsExactlyInAnyOrder(302, 409);
        }
    }

    private <T> T bajoBloqueo(boolean bloquearHabitacion, Supplier<T> operacion) {
        return transaccion.execute(estado -> {
            reservaciones.buscarParaActualizar(idReserva).orElseThrow();
            if (bloquearHabitacion) {
                habitaciones.buscarParaActualizar(idHabitacion).orElseThrow();
            }
            return operacion.get();
        });
    }

    private void exigeEspera(Future<MvcResult> resultado) {
        assertThrows(TimeoutException.class, () -> resultado.get(300, TimeUnit.MILLISECONDS));
    }

    private Long crearReservacion() {
        LocalDate hoy = LocalDate.now(relojHotel);
        Reservacion reserva = new Reservacion(entidades.getReference(Huesped.class, idHuesped),
                entidades.getReference(Habitacion.class, idHabitacion), hoy, hoy.plusDays(1), 1,
                TARIFA, TARIFA, EstadoReservacion.CONFIRMADA);
        entidades.persist(reserva);
        return reserva.getIdReservacion();
    }

    private MvcResult operar(Long id, String accion) throws Exception {
        return mvc.perform(post("/check-in-check-out/{id}/{accion}", id, accion)
                .with(user("recepcion-qa@example.test").roles("RECEPCIONISTA")).with(csrf())).andReturn();
    }

    private EstadoReservacion estadoGuardado(Long id) {
        return reservaciones.findById(id).orElseThrow().getEstado();
    }

    private EstadoHabitacion estadoHabitacion() {
        return habitaciones.findById(idHabitacion).orElseThrow().getEstadoOperativo();
    }

    private int horasRegistradas(String columna) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reservaciones WHERE id_habitacion = ? AND " + columna + " IS NOT NULL",
                Integer.class, idHabitacion);
    }
}

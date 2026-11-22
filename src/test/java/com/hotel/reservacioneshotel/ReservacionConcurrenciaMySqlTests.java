package com.hotel.reservacioneshotel;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.HabitacionRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIfSystemProperty(named = "hotel.test.profile", matches = "mysql")
@SpringBootTest(properties = "spring.profiles.active=${hotel.test.profile:test}")
@AutoConfigureMockMvc
class ReservacionConcurrenciaMySqlTests {

    private static final String NUEVA = "/reservaciones/nueva";
    private static final BigDecimal TARIFA = new BigDecimal("900.00");
    private final LocalDate entrada = LocalDate.now().plusDays(20);
    private final String clave = UUID.randomUUID().toString().substring(0, 10);
    private TransactionTemplate transaccion;
    private TipoHabitacion tipo;
    private Habitacion primera;
    private Habitacion segunda;
    private Huesped titular;
    private Huesped otro;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;
    @Autowired
    private HabitacionRepository habitaciones;
    @Autowired
    private ReservacionRepository reservaciones;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager gestor;

    @BeforeEach
    void prepararDatosConfirmados() {
        assertThat(jdbc.queryForObject("SELECT VERSION()", String.class)).startsWith("8.");
        transaccion = new TransactionTemplate(gestor);
        transaccion.executeWithoutResult(estado -> {
            tipo = new TipoHabitacion("Tipo de concurrencia " + clave, 2, null);
            entidades.persist(tipo);
            primera = crearHabitacion("A");
            segunda = crearHabitacion("B");
            titular = crearHuesped("a");
            otro = crearHuesped("b");
        });
    }

    @AfterEach
    void limpiarSoloLosDatosDeLaPrueba() {
        if (transaccion == null || primera == null || segunda == null || titular == null || otro == null) {
            return;
        }
        transaccion.executeWithoutResult(estado -> {
            jdbc.update("DELETE FROM reservaciones WHERE id_habitacion IN (?, ?)", primera.getIdHabitacion(), segunda.getIdHabitacion());
            jdbc.update("DELETE FROM habitaciones WHERE id_habitacion IN (?, ?)", primera.getIdHabitacion(), segunda.getIdHabitacion());
            jdbc.update("DELETE FROM cuentas WHERE id_huesped IN (?, ?)", titular.getIdHuesped(), otro.getIdHuesped());
            jdbc.update("DELETE FROM huespedes WHERE id_huesped IN (?, ?)", titular.getIdHuesped(), otro.getIdHuesped());
            jdbc.update("DELETE FROM tipos_habitacion WHERE id_tipo = ?", tipo.getIdTipo());
        });
    }

    @Test
    void soloUnAltaSuperpuestaSeGuardaTrasLiberarElBloqueoReal() throws Exception {
        try (ExecutorService tareas = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> futuras = conHabitacionBloqueada(() -> {
                var a = tareas.submit(() -> mvc.perform(peticion(NUEVA, titular, primera, 0, 3)).andReturn());
                var b = tareas.submit(() -> mvc.perform(peticion(NUEVA, otro, primera, 0, 3)).andReturn());
                exigeEspera(a);
                exigeEspera(b);
                return List.of(a, b);
            });
            var respuestas = List.of(terminar(futuras.getFirst()), terminar(futuras.getLast()));
            assertThat(respuestas).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(302, 200);
            for (MvcResult respuesta : respuestas) {
                if (respuesta.getResponse().getStatus() == 200) {
                    assertThat(respuesta.getResponse().getContentAsString()).contains("ya no está disponible");
                }
            }
            assertThat(cantidadPorHabitacion(primera)).isEqualTo(1);
        }
    }

    @Test
    void altasSimultaneasConsecutivasPuedenGuardarseEnLaMismaHabitacion() throws Exception {
        try (ExecutorService tareas = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> futuras = conHabitacionBloqueada(() -> {
                var a = tareas.submit(() -> mvc.perform(peticion(NUEVA, titular, primera, 0, 2)).andReturn());
                var b = tareas.submit(() -> mvc.perform(peticion(NUEVA, otro, primera, 2, 4)).andReturn());
                exigeEspera(a);
                exigeEspera(b);
                return List.of(a, b);
            });
            assertThat(terminar(futuras.getFirst()).getResponse().getStatus()).isEqualTo(302);
            assertThat(terminar(futuras.getLast()).getResponse().getStatus()).isEqualTo(302);
            assertThat(cantidadPorHabitacion(primera)).isEqualTo(2);
        }
    }

    @Test
    void otraHabitacionDelMismoTipoSeGuardaMientrasLaPrimeraEspera() throws Exception {
        try (ExecutorService tareas = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> primerAlta = conHabitacionBloqueada(() -> {
                var a = tareas.submit(() -> mvc.perform(peticion(NUEVA, titular, primera, 0, 3)).andReturn());
                exigeEspera(a);
                var b = tareas.submit(() -> mvc.perform(peticion(NUEVA, otro, segunda, 0, 3)).andReturn());
                assertThat(terminar(b).getResponse().getStatus()).isEqualTo(302);
                assertThat(cantidadPorHabitacion(segunda)).isEqualTo(1);
                assertThat(a.isDone()).isFalse();
                return a;
            });
            assertThat(terminar(primerAlta).getResponse().getStatus()).isEqualTo(302);
            assertThat(cantidadPorHabitacion(primera)).isEqualTo(1);
        }
    }

    @Test
    void dosEdicionesPuedenIntercambiarHabitacionesConFechasDistintas() throws Exception {
        Long reservaA = guardar(titular, primera, 0, 2);
        Long reservaB = guardar(otro, segunda, 2, 4);
        try (ExecutorService tareas = Executors.newFixedThreadPool(2)) {
            List<Future<MvcResult>> futuras = conHabitacionBloqueada(() -> {
                var a = tareas.submit(() -> mvc.perform(peticion(editar(reservaA), titular, segunda, 0, 2)).andReturn());
                var b = tareas.submit(() -> mvc.perform(peticion(editar(reservaB), otro, primera, 2, 4)).andReturn());
                exigeEspera(a);
                exigeEspera(b);
                return List.of(a, b);
            });
            assertThat(terminar(futuras.getFirst()).getResponse().getStatus()).isEqualTo(302);
            assertThat(terminar(futuras.getLast()).getResponse().getStatus()).isEqualTo(302);
            assertThat(habitacionDe(reservaA)).isEqualTo(segunda.getIdHabitacion());
            assertThat(habitacionDe(reservaB)).isEqualTo(primera.getIdHabitacion());
        }
    }

    @Test
    void esperaAgotadaNoPersisteDatosYSePuedeReintentarTrasLiberarElBloqueo() throws Exception {
        try (ExecutorService tareas = Executors.newSingleThreadExecutor()) {
            conHabitacionBloqueada(() -> {
                Future<MvcResult> fallo = tareas.submit(this::intentarAltaConEsperaCorta);
                MvcResult respuesta = terminar(fallo);
                assertThat(respuesta.getResponse().getStatus()).isEqualTo(409);
                assertThat(new String(respuesta.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
                        .contains("Vuelve a consultar");
                assertThat(cantidadPorHabitacion(primera)).isZero();
                return null;
            });
            mvc.perform(peticion(NUEVA, titular, primera, 0, 3))
                    .andExpect(status().isFound());
            assertThat(cantidadPorHabitacion(primera)).isEqualTo(1);
        }
    }

    private MvcResult intentarAltaConEsperaCorta() {
        return transaccion.execute(estado -> {
            Integer esperaOriginal = jdbc.queryForObject("SELECT @@session.innodb_lock_wait_timeout", Integer.class);
            jdbc.execute("SET SESSION innodb_lock_wait_timeout = 1");
            try {
                return mvc.perform(peticion(NUEVA, titular, primera, 0, 3)).andReturn();
            } catch (Exception exception) {
                throw new AssertionError("Falló la solicitud bajo bloqueo", exception);
            } finally {
                estado.setRollbackOnly();
                jdbc.execute("SET SESSION innodb_lock_wait_timeout = " + esperaOriginal);
            }
        });
    }

    private <T> T conHabitacionBloqueada(Supplier<T> trabajo) {
        return transaccion.execute(estado -> {
            habitaciones.buscarParaActualizar(primera.getIdHabitacion()).orElseThrow();
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
        } catch (java.util.concurrent.ExecutionException | TimeoutException exception) {
            throw new AssertionError("La solicitud no terminó dentro del límite", exception);
        }
    }

    private MockHttpServletRequestBuilder peticion(String ruta, Huesped huesped, Habitacion habitacion, int desde, int hasta) {
        return post(ruta).with(user(huesped.getCorreo()).roles("HUESPED")).with(csrf())
                .param("habitacionId", habitacion.getIdHabitacion().toString())
                .param("entrada", entrada.plusDays(desde).toString()).param("salida", entrada.plusDays(hasta).toString())
                .param("personas", "2");
    }

    private Long guardar(Huesped huesped, Habitacion habitacion, int desde, int hasta) {
        return reservaciones.saveAndFlush(new Reservacion(huesped, habitacion, entrada.plusDays(desde), entrada.plusDays(hasta),
                2, TARIFA, TARIFA.multiply(BigDecimal.valueOf(hasta - desde)), EstadoReservacion.PENDIENTE)).getIdReservacion();
    }

    private int cantidadPorHabitacion(Habitacion habitacion) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reservaciones WHERE id_habitacion = ?", Integer.class,
                habitacion.getIdHabitacion());
    }

    private Long habitacionDe(Long id) {
        return jdbc.queryForObject("SELECT id_habitacion FROM reservaciones WHERE id_reservacion = ?", Long.class, id);
    }

    private String editar(Long id) {
        return "/reservaciones/" + id + "/editar";
    }

    private Habitacion crearHabitacion(String sufijo) {
        Habitacion habitacion = new Habitacion("QC-" + clave + "-" + sufijo, 1, EstadoHabitacion.DISPONIBLE, TARIFA, tipo);
        entidades.persist(habitacion);
        return habitacion;
    }

    private Huesped crearHuesped(String sufijo) {
        Huesped huesped = new Huesped("Invitado de prueba", null, clave + sufijo + "@example.test");
        entidades.persist(huesped);
        entidades.persist(Cuenta.paraHuesped(huesped, "hash de prueba para autenticación simulada"));
        return huesped;
    }
}

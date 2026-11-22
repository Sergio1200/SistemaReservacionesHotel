package com.hotel.reservacioneshotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.pagos.Pago;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.PagoRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import com.hotel.reservacioneshotel.service.PagoService.Historial;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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
class PagoMvcTests {

    private static final BigDecimal TOTAL = new BigDecimal("100.00");
    private Huesped titular;
    private Huesped otro;
    private Habitacion habitacion;
    private Reservacion reserva;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;
    @Autowired
    private PagoRepository pagos;
    @Autowired
    private ReservacionRepository reservaciones;

    @BeforeEach
    void preparar() {
        titular = huesped("Titular de prueba", "pago-titular@example.test");
        otro = huesped("Otra ficha de prueba", "pago-ajeno@example.test");
        TipoHabitacion tipo = new TipoHabitacion("Tipo de pago QA", 2, null);
        entidades.persist(tipo);
        habitacion = new Habitacion("QA-PAGO", 1, EstadoHabitacion.DISPONIBLE, TOTAL, tipo);
        entidades.persist(habitacion);
        reserva = reservacion(titular, EstadoReservacion.PENDIENTE);
    }

    @Test
    void dosPagosSimuladosCompletanElSaldoYConfirmanLaReservacion() throws Exception {
        Pago parcial = registrar(reserva, "40.10", propio());
        assertThat(parcial.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(parcial.getFecha()).isNotNull();
        assertThat(parcial.getMetodoPago()).isEqualTo("EFECTIVO");
        comprobarSaldo("0.00", "100.00", "40.10", "59.90");

        procesar(reserva, parcial);
        assertThat(pagos.findById(parcial.getIdPago()).orElseThrow().getEstado()).isEqualTo(EstadoPago.PROCESANDO);
        comprobarSaldo("0.00", "100.00", "40.10", "59.90");
        validar(reserva, parcial, "APROBAR");
        comprobarSaldo("40.10", "59.90", "0.00", "59.90");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.PENDIENTE);

        Pago completo = registrar(reserva, "59.90", recepcion());
        procesar(reserva, completo);
        validar(reserva, completo, "APROBAR");
        entidades.flush();
        entidades.clear();
        comprobarSaldo("100.00", "0.00", "0.00", "0.00");
        Reservacion guardada = reservaciones.findById(reserva.getIdReservacion()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoReservacion.CONFIRMADA);
        assertThat(guardada.getPrecioNocheAplicado()).isEqualByComparingTo(TOTAL);
        assertThat(guardada.getCostoTotal()).isEqualByComparingTo(TOTAL);
        assertThat(pagos.findByReservacion_IdReservacionOrderByFechaDescIdPagoDesc(reserva.getIdReservacion()))
                .extracting(Pago::getEstado).containsExactly(EstadoPago.PAGADO, EstadoPago.PAGADO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10"})
    void rechazaCeroYNegativos(String monto) throws Exception {
        mvc.perform(formulario(reserva, monto, UUID.randomUUID()).with(propio()).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("El monto debe ser mayor que cero.")));
        assertThat(pagos.count()).isZero();
    }

    @Test
    void limitaDecimalesValidaMetodoYCompruebaQueExistaLaReservacion() throws Exception {
        mvc.perform(formulario(reserva, "1.001", UUID.randomUUID()).with(propio()).with(csrf()))
                .andExpect(content().string(containsString("hasta diez cifras enteras y dos decimales")));
        mvc.perform(post(ruta(reserva)).param("monto", "17.00").param("metodo", "METODO_REAL")
                        .param("intento", UUID.randomUUID().toString()).with(propio()).with(csrf()))
                .andExpect(content().string(containsString("Selecciona un método de pago simulado válido.")));
        mvc.perform(get("/reservaciones/" + Long.MAX_VALUE + "/pagos").with(recepcion()))
                .andExpect(status().isNotFound());
        assertThat(pagos.count()).isZero();
    }

    @Test
    void evitaSuperarSaldoConIntentosPendientesOEnProceso() throws Exception {
        Pago primero = registrar(reserva, "65.75", propio());
        mvc.perform(formulario(reserva, "34.26", UUID.randomUUID()).with(propio()).with(csrf()))
                .andExpect(content().string(containsString("El monto supera el saldo disponible")));
        procesar(reserva, primero);
        mvc.perform(formulario(reserva, "34.26", UUID.randomUUID()).with(recepcion()).with(csrf()))
                .andExpect(content().string(containsString("El monto supera el saldo disponible")));
        registrar(reserva, "34.25", propio());
        comprobarSaldo("0.00", "100.00", "100.00", "0.00");
        assertThat(pagos.count()).isEqualTo(2);
    }

    @Test
    void rechazoLiberaElImporteYUnIntentoNuevoConservaElHistorial() throws Exception {
        Pago rechazado = registrar(reserva, "100.00", propio());
        procesar(reserva, rechazado);
        validar(reserva, rechazado, "RECHAZAR");
        comprobarSaldo("0.00", "100.00", "0.00", "100.00");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.PENDIENTE);

        Pago nuevo = registrar(reserva, "100.00", propio());
        procesar(reserva, nuevo);
        validar(reserva, nuevo, "APROBAR");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.CONFIRMADA);
        assertThat(pagos.findById(rechazado.getIdPago()).orElseThrow().getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(nuevo.getIdPago()).isNotEqualTo(rechazado.getIdPago());
    }

    @Test
    void unReenvioIdempotenteNoCreaOtroPagoYNoAdmiteUnMontoDistinto() throws Exception {
        UUID intento = UUID.randomUUID();
        mvc.perform(formulario(reserva, "15", intento).param("estado", "PAGADO").with(propio()).with(csrf()))
                .andExpect(redirectedUrl(ruta(reserva)));
        Pago pago = pagos.findByReservacion_IdReservacionAndClaveIntento(reserva.getIdReservacion(), intento.toString()).orElseThrow();
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        procesar(reserva, pago);
        validar(reserva, pago, "APROBAR");
        mvc.perform(formulario(reserva, "15.00", intento).with(propio()).with(csrf()))
                .andExpect(flash().attribute("mensajeExito", containsString("ya estaba registrado")));
        mvc.perform(formulario(reserva, "14", intento).with(propio()).with(csrf()))
                .andExpect(content().string(containsString("ya se envió con otros datos")));
        assertThat(pagos.count()).isEqualTo(1);
    }

    @Test
    void noApruebaDirectamenteNiDosVecesYConservaElImporte() throws Exception {
        Pago pago = registrar(reserva, "100.00", propio());
        mvc.perform(accion(reserva, pago, "validar").param("decision", "APROBAR").with(recepcion()).with(csrf()))
                .andExpect(flash().attributeExists("mensajeError"));
        procesar(reserva, pago);
        validar(reserva, pago, "APROBAR");
        mvc.perform(accion(reserva, pago, "validar").param("decision", "APROBAR").with(recepcion()).with(csrf()))
                .andExpect(flash().attributeExists("mensajeError"));
        comprobarSaldo("100.00", "0.00", "0.00", "0.00");
        assertThat(pagos.count()).isEqualTo(1);
    }

    @Test
    void aprobacionRevisaTambienOtrosPagosYAnteConflictoNoCambiaEstados() throws Exception {
        Pago candidato = pagos.save(new Pago(reserva, LocalDateTime.now(), new BigDecimal("80.00"), "SIMULADO", EstadoPago.PROCESANDO));
        pagos.save(new Pago(reserva, LocalDateTime.now(), new BigDecimal("30.00"), "SIMULADO", EstadoPago.PENDIENTE));
        mvc.perform(accion(reserva, candidato, "validar").param("decision", "APROBAR").with(recepcion()).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("supera el saldo")));
        entidades.clear();
        assertThat(pagos.findById(candidato.getIdPago()).orElseThrow().getEstado()).isEqualTo(EstadoPago.PROCESANDO);
        assertThat(reservaciones.findById(reserva.getIdReservacion()).orElseThrow().getEstado()).isEqualTo(EstadoReservacion.PENDIENTE);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReservacion.class, names = {"CANCELADA", "FINALIZADA"})
    void bloqueaNuevosPagosYValidacionEnReservacionesCerradas(EstadoReservacion estado) throws Exception {
        Reservacion cerrada = reservacion(titular, estado);
        Pago existente = pagos.save(new Pago(cerrada, LocalDateTime.now(), new BigDecimal("13.00"), "SIMULADO", EstadoPago.PROCESANDO));
        mvc.perform(formulario(cerrada, "15", UUID.randomUUID()).with(propio()).with(csrf()))
                .andExpect(content().string(containsString("La reservación ya no admite pagos.")));
        mvc.perform(accion(cerrada, existente, "validar").param("decision", "APROBAR").with(recepcion()).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("ya no admite pagos")));
        assertThat(pagos.count()).isEqualTo(1);
        assertThat(pagos.findById(existente.getIdPago()).orElseThrow().getEstado()).isEqualTo(EstadoPago.PROCESANDO);
    }

    @Test
    void cancelacionConservaHistorialYAdviertePosibleConciliacion() throws Exception {
        Pago pagado = registrar(reserva, "40.00", propio());
        procesar(reserva, pagado);
        validar(reserva, pagado, "APROBAR");
        mvc.perform(post("/reservaciones/" + reserva.getIdReservacion() + "/cancelar").with(propio()).with(csrf()))
                .andExpect(status().isFound());
        mvc.perform(get(ruta(reserva)).with(propio()))
                .andExpect(content().string(containsString("pueden requerir conciliación o reembolso")))
                .andExpect(content().string(containsString("Saldo histórico")))
                .andExpect(content().string(not(containsString("Registrar intento"))));
        assertThat(pagos.findById(pagado.getIdPago()).orElseThrow().getMonto()).isEqualByComparingTo("40.00");
    }

    @Test
    void admitePagoEnCursoSinCambiarElEstadoDeLaEstancia() throws Exception {
        Reservacion enCurso = reservacion(titular, EstadoReservacion.EN_CURSO);
        Pago pago = registrar(enCurso, "100", recepcion());
        procesar(enCurso, pago);
        validar(enCurso, pago, "APROBAR");
        assertThat(enCurso.getEstado()).isEqualTo(EstadoReservacion.EN_CURSO);
    }

    @Test
    void restringeHistorialRegistroYValidacionPorRolYPropiedad() throws Exception {
        Reservacion ajena = reservacion(otro, EstadoReservacion.PENDIENTE);
        Pago pago = registrar(ajena, "10", recepcion());
        mvc.perform(get(ruta(ajena)).with(propio())).andExpect(status().isForbidden());
        mvc.perform(formulario(ajena, "5", UUID.randomUUID()).with(propio()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get(ruta(reserva))).andExpect(redirectedUrl("/login"));
        mvc.perform(formulario(reserva, "5", UUID.randomUUID()).with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(get(ruta(reserva)).with(user("admin@example.test").roles("ADMINISTRADOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/pagos").with(propio())).andExpect(status().isForbidden());
        mvc.perform(get("/pagos").param("q", "pago-ajeno@").with(recepcion()))
                .andExpect(content().string(containsString("Otra ficha de prueba")))
                .andExpect(content().string(not(containsString("Titular de prueba"))));
        mvc.perform(accion(reserva, pago, "procesar").with(recepcion()).with(csrf())).andExpect(status().isNotFound());
        Pago propio = registrar(reserva, "10", propio());
        mvc.perform(accion(reserva, propio, "procesar").with(propio()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(accion(reserva, propio, "validar").param("decision", "APROBAR").with(propio()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void pagoMantieneProteccionCsrfYElFormularioNoPideInformacionBancaria() throws Exception {
        mvc.perform(formulario(reserva, "10", UUID.randomUUID()).with(propio())).andExpect(status().isForbidden());
        Pago pago = registrar(reserva, "10", propio());
        for (String operacion : List.of("procesar", "validar")) {
            mvc.perform(accion(reserva, pago, operacion).with(recepcion())).andExpect(status().isForbidden());
        }
        mvc.perform(get(ruta(reserva)).with(propio()))
                .andExpect(content().string(containsString("Simulación académica")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("CVV"))));
    }

    @Test
    void ampliarUnaReservaPagadaExigeCubrirLaDiferenciaAntesDelCheckIn() throws Exception {
        Pago primero = registrar(reserva, "100", propio());
        procesar(reserva, primero);
        validar(reserva, primero, "APROBAR");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.CONFIRMADA);

        LocalDate nuevaEntrada = reserva.getFechaEntrada().plusDays(1);
        cambiarPeriodo(nuevaEntrada, 1);
        assertThat(reserva.getFechaEntrada()).isEqualTo(nuevaEntrada);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.CONFIRMADA);
        comprobarSaldo("100", "0", "0", "0");

        cambiarNoches(2);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.PENDIENTE);
        comprobarSaldo("100", "100", "0", "100");
        mvc.perform(post("/check-in-check-out/{id}/check-in", reserva.getIdReservacion()).with(recepcion()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("reservación confirmada")));

        Pago diferencia = registrar(reserva, "100", propio());
        procesar(reserva, diferencia);
        validar(reserva, diferencia, "APROBAR");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.CONFIRMADA);
        comprobarSaldo("200", "0", "0", "0");
        assertThat(primero.getEstado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(primero.getMonto()).isEqualByComparingTo("100.00");
        assertThat(primero.getReservacion().getIdReservacion()).isEqualTo(reserva.getIdReservacion());
    }

    @Test
    void reducirUnaReservaAlTotalYaPagadoLaConfirmaSinCrearOtroPago() throws Exception {
        cambiarNoches(2);
        Pago primero = registrar(reserva, "100", propio());
        procesar(reserva, primero);
        validar(reserva, primero, "APROBAR");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.PENDIENTE);

        cambiarNoches(1);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReservacion.CONFIRMADA);
        comprobarSaldo("100", "0", "0", "0");
        assertThat(pagos.findByReservacion_IdReservacionOrderByFechaDescIdPagoDesc(reserva.getIdReservacion()))
                .extracting(Pago::getIdPago).containsExactly(primero.getIdPago());
    }

    private void cambiarNoches(int noches) throws Exception {
        cambiarPeriodo(reserva.getFechaEntrada(), noches);
    }

    private void cambiarPeriodo(LocalDate entrada, int noches) throws Exception {
        mvc.perform(post("/reservaciones/{id}/editar", reserva.getIdReservacion()).with(propio()).with(csrf())
                        .param("habitacionId", habitacion.getIdHabitacion().toString())
                        .param("entrada", entrada.toString())
                        .param("salida", entrada.plusDays(noches).toString())
                        .param("personas", "1"))
                .andExpect(redirectedUrl("/reservaciones/" + reserva.getIdReservacion()));
    }

    private void comprobarSaldo(String pagado, String pendiente, String enTramite, String disponible) throws Exception {
        var respuesta = mvc.perform(get(ruta(reserva)).with(propio())).andExpect(status().isOk()).andReturn();
        Historial historial = (Historial) respuesta.getModelAndView().getModel().get("historial");
        assertThat(historial.saldo().pagado()).isEqualByComparingTo(pagado);
        assertThat(historial.saldo().pendiente()).isEqualByComparingTo(pendiente);
        assertThat(historial.saldo().enTramite()).isEqualByComparingTo(enTramite);
        assertThat(historial.saldo().disponible()).isEqualByComparingTo(disponible);
    }

    private Pago registrar(Reservacion reserva, String monto, RequestPostProcessor usuario) throws Exception {
        UUID intento = UUID.randomUUID();
        mvc.perform(formulario(reserva, monto, intento).with(usuario).with(csrf())).andExpect(redirectedUrl(ruta(reserva)));
        return pagos.findByReservacion_IdReservacionAndClaveIntento(reserva.getIdReservacion(), intento.toString()).orElseThrow();
    }

    private void procesar(Reservacion reserva, Pago pago) throws Exception {
        mvc.perform(accion(reserva, pago, "procesar").with(recepcion()).with(csrf()))
                .andExpect(redirectedUrl(ruta(reserva))).andExpect(flash().attributeExists("mensajeExito"));
    }

    private void validar(Reservacion reserva, Pago pago, String decision) throws Exception {
        mvc.perform(accion(reserva, pago, "validar").param("decision", decision).with(recepcion()).with(csrf()))
                .andExpect(redirectedUrl(ruta(reserva))).andExpect(flash().attributeExists("mensajeExito"));
    }

    private MockHttpServletRequestBuilder formulario(Reservacion reserva, String monto, UUID intento) {
        return post(ruta(reserva)).param("monto", monto).param("metodo", "EFECTIVO").param("intento", intento.toString());
    }

    private MockHttpServletRequestBuilder accion(Reservacion reserva, Pago pago, String accion) {
        return post(ruta(reserva) + "/" + pago.getIdPago() + "/" + accion);
    }

    private String ruta(Reservacion reserva) {
        return "/reservaciones/" + reserva.getIdReservacion() + "/pagos";
    }

    private Huesped huesped(String nombre, String correo) {
        Huesped huesped = new Huesped(nombre, null, correo);
        entidades.persist(huesped);
        entidades.persist(Cuenta.paraHuesped(huesped, "hash de prueba para autenticar en MockMvc"));
        return huesped;
    }

    private Reservacion reservacion(Huesped huesped, EstadoReservacion estado) {
        LocalDate entrada = LocalDate.now().plusDays(2);
        return reservaciones.save(new Reservacion(huesped, habitacion, entrada, entrada.plusDays(1), 1, TOTAL, TOTAL, estado));
    }

    private RequestPostProcessor propio() {
        return user(titular.getCorreo()).roles("HUESPED");
    }

    private RequestPostProcessor recepcion() {
        return user("recepcion@example.test").roles("RECEPCIONISTA");
    }
}

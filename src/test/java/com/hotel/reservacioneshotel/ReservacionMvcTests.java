package com.hotel.reservacioneshotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.pagos.Pago;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
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
class ReservacionMvcTests {

    private static final String NUEVA = "/reservaciones/nueva";
    private static final BigDecimal TARIFA = new BigDecimal("850.75");
    private static final BigDecimal TARIFA_SUITE = new BigDecimal("1020.35");
    private final LocalDate entrada = LocalDate.now().plusDays(10);
    private Huesped titular;
    private Huesped otro;
    private Habitacion doble;
    private Habitacion suite;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;
    @Autowired
    private ReservacionRepository reservaciones;

    @BeforeEach
    void preparar() {
        titular = huesped("Titular ficticio", "titular@example.test");
        otro = huesped("Visitante ficticio", "visitante-qa@example.test");
        doble = habitacion("QA-202", 2, TARIFA);
        suite = habitacion("QA-301", 4, TARIFA_SUITE);
    }

    @Test
    void registraComoHuespedYConservaLaTarifaAunqueCambieElInventario() throws Exception {
        long id = crearComo(propio(), doble.getIdHabitacion(), entrada, entrada.plusDays(3), 2, null);
        doble.actualizarDatos(doble.getNumero(), 1, new BigDecimal("999.00"), doble.getTipoHabitacion());
        entidades.flush();
        entidades.clear();
        Reservacion guardada = guardada(id);
        assertThat(guardada.getHuesped().getIdHuesped()).isEqualTo(titular.getIdHuesped());
        assertThat(guardada.getEstado()).isEqualTo(EstadoReservacion.PENDIENTE);
        assertThat(guardada.getPrecioNocheAplicado()).isEqualByComparingTo(TARIFA);
        assertThat(guardada.getCostoTotal()).isEqualByComparingTo("2552.25");
        mvc.perform(get(ruta(id)).with(propio()))
                .andExpect(content().string(containsString("QA-202")))
                .andExpect(content().string(containsString("Pendiente")))
                .andExpect(content().string(not(containsString("titular@example.test"))));
    }

    @Test
    void registraComoRecepcionParaUnHuespedSeleccionado() throws Exception {
        long id = crearComo(recepcion(), suite.getIdHabitacion(), entrada, entrada.plusDays(1), 4, otro.getIdHuesped());
        assertThat(guardada(id).getHuesped().getIdHuesped()).isEqualTo(otro.getIdHuesped());
    }

    @Test
    void pideSesionYCortaElAccesoAlAdministrador() throws Exception {
        mvc.perform(get(NUEVA)).andExpect(redirectedUrl("/login"));
        mvc.perform(formulario(NUEVA, doble.getIdHabitacion(), entrada, entrada.plusDays(1), 2).with(csrf()))
                .andExpect(redirectedUrl("/login"));
        RequestPostProcessor admin = user("admin@example.test").roles("ADMINISTRADOR");
        mvc.perform(get(NUEVA).with(admin)).andExpect(status().isForbidden());
        mvc.perform(post("/reservaciones/1/cancelar").with(admin).with(csrf())).andExpect(status().isForbidden());
        assertThat(reservaciones.count()).isZero();
    }

    @Test
    void impideSuplantarAlTitularYNoMuestraElSelectorAlHuesped() throws Exception {
        mvc.perform(get(NUEVA).with(propio())).andExpect(content().string(not(containsString("name=\"huespedId\""))));
        mvc.perform(formulario(NUEVA, doble.getIdHabitacion(), entrada, entrada.plusDays(1), 1)
                        .param("huespedId", otro.getIdHuesped().toString()).with(propio()).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(reservaciones.count()).isZero();
    }

    @Test
    void validaFechasCantidadHabitacionCapacidadEImporte() throws Exception {
        validarAlta(doble.getIdHabitacion(), entrada, entrada, 1, "posterior a la fecha de entrada");
        validarAlta(doble.getIdHabitacion(), LocalDate.now().minusDays(1), entrada, 1, "hoy o posterior");
        validarAlta(doble.getIdHabitacion(), entrada, entrada.plusDays(366), 1, "hasta 365 noches");
        validarAlta(doble.getIdHabitacion(), entrada, entrada.plusDays(2), 0, "mayor que cero");
        validarAlta(doble.getIdHabitacion(), entrada, entrada.plusDays(2), 3, "capacidad de la habitación es insuficiente");
        validarAlta(Long.MAX_VALUE, entrada, entrada.plusDays(2), 1, "habitación seleccionada ya no existe");
        Habitacion cara = habitacion("QA-LIMITE", 2, new BigDecimal("9999999999.99"));
        validarAlta(cara.getIdHabitacion(), entrada, entrada.plusDays(2), 1, "importe supera el máximo");
        assertThat(reservaciones.count()).isZero();
    }

    @Test
    void rechazaSolapamientosYPermiteReservacionesConsecutivas() throws Exception {
        guardar(titular, doble, EstadoReservacion.CONFIRMADA, entrada, entrada.plusDays(3));
        validarAlta(doble.getIdHabitacion(), entrada.plusDays(2), entrada.plusDays(4), 1, "ya no está disponible");
        crearComo(propio(), doble.getIdHabitacion(), entrada.plusDays(3), entrada.plusDays(5), 1, null);
        assertThat(reservaciones.count()).isEqualTo(2);
    }

    @Test
    void listaSoloLasPropiasYRecepcionBuscaPorNombreCorreoEId() throws Exception {
        Reservacion propia = guardar(titular, doble, EstadoReservacion.PENDIENTE, entrada, entrada.plusDays(2));
        Reservacion ajena = guardar(otro, suite, EstadoReservacion.PENDIENTE, entrada, entrada.plusDays(2));
        mvc.perform(get("/reservaciones").param("q", otro.getCorreo()).with(propio()))
                .andExpect(content().string(containsString("QA-202")))
                .andExpect(content().string(not(containsString("QA-301"))))
                .andExpect(content().string(not(containsString(otro.getCorreo()))));
        mvc.perform(get("/reservaciones").with(recepcion()))
                .andExpect(content().string(containsString("QA-202")))
                .andExpect(content().string(containsString("QA-301")));
        for (String q : new String[] {"visitante ficticio", "VISITANTE-QA@", "#" + ajena.getIdReservacion()}) {
            mvc.perform(get("/reservaciones").param("q", q).with(recepcion()))
                    .andExpect(content().string(containsString("QA-301")))
                    .andExpect(content().string(not(containsString("QA-202"))));
        }
        mvc.perform(get(ruta(propia.getIdReservacion())).with(recepcion())).andExpect(status().isOk());
    }

    @Test
    void distingueAccesoAjenoDeReservacionInexistente() throws Exception {
        String ajena = ruta(guardar(otro, doble, EstadoReservacion.PENDIENTE, entrada, entrada.plusDays(2)).getIdReservacion());
        for (String sufijo : new String[] {"", "/editar", "/cancelar"}) {
            mvc.perform(get(ajena + sufijo).with(propio())).andExpect(status().isForbidden());
            mvc.perform(get(ruta(Long.MAX_VALUE) + sufijo).with(propio())).andExpect(status().isNotFound());
        }
        mvc.perform(get("/reservaciones/ruta-inexistente").with(propio())).andExpect(status().isNotFound());
        mvc.perform(formulario(ajena + "/editar", doble.getIdHabitacion(), entrada, entrada.plusDays(2), 1).with(propio()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(ajena + "/cancelar").with(propio()).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void editaExcluyendoLaPropiaYRecalculaSoloSiCambianFechasOHabitacion() throws Exception {
        Long id = crearComo(propio(), doble.getIdHabitacion(), entrada, entrada.plusDays(3), 2, null);
        Pago pagoPrevio = new Pago(guardada(id), LocalDateTime.now(), new BigDecimal("90.00"), "SIMULADO", EstadoPago.PAGADO);
        entidades.persist(pagoPrevio);
        Pago complemento = new Pago(guardada(id), LocalDateTime.now(), new BigDecimal("1950.70"), "SIMULADO", EstadoPago.PAGADO);
        entidades.persist(complemento);
        Pago rechazado = new Pago(guardada(id), LocalDateTime.now(), new BigDecimal("3000.00"), "SIMULADO", EstadoPago.RECHAZADO);
        entidades.persist(rechazado);
        String editar = ruta(id) + "/editar";
        doble.actualizarDatos(doble.getNumero(), 1, new BigDecimal("999.00"), doble.getTipoHabitacion());
        mvc.perform(formulario(editar, doble.getIdHabitacion(), entrada, entrada.plusDays(3), 1).with(propio()).with(csrf()))
                .andExpect(redirectedUrl(ruta(id)));
        assertThat(guardada(id).getCostoTotal()).isEqualByComparingTo("2552.25");
        mvc.perform(formulario(editar, suite.getIdHabitacion(), entrada.plusDays(1), entrada.plusDays(3), 4).with(propio()).with(csrf()))
                .andExpect(redirectedUrl(ruta(id)));
        entidades.flush();
        entidades.clear();
        Reservacion editada = guardada(id);
        assertThat(editada.getHabitacion().getIdHabitacion()).isEqualTo(suite.getIdHabitacion());
        assertThat(editada.getHuesped().getIdHuesped()).isEqualTo(titular.getIdHuesped());
        assertThat(editada.getPrecioNocheAplicado()).isEqualByComparingTo(TARIFA_SUITE);
        assertThat(editada.getCostoTotal()).isEqualByComparingTo("2040.70");
        assertThat(editada.getFechaEntrada()).isEqualTo(entrada.plusDays(1));
        assertThat(editada.getNumeroHuespedes()).isEqualTo(4);
        Pago pagoGuardado = entidades.find(Pago.class, pagoPrevio.getIdPago());
        assertThat(pagoGuardado.getMonto()).isEqualByComparingTo("90.00");
        assertThat(pagoGuardado.getEstado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(entidades.find(Pago.class, complemento.getIdPago()).getMonto()).isEqualByComparingTo("1950.70");
        assertThat(entidades.find(Pago.class, rechazado.getIdPago()).getEstado()).isEqualTo(EstadoPago.RECHAZADO);
    }

    @Test
    void rechazaUnTotalMenorQueLaSumaCobradaYConservaLosRegistros() throws Exception {
        Reservacion reserva = guardar(titular, doble, EstadoReservacion.CONFIRMADA, entrada, entrada.plusDays(2));
        Pago primero = new Pago(reserva, LocalDateTime.now(), new BigDecimal("450.00"), "SIMULADO", EstadoPago.PAGADO);
        Pago segundo = new Pago(reserva, LocalDateTime.now(), new BigDecimal("450.00"), "SIMULADO", EstadoPago.PAGADO);
        entidades.persist(primero);
        entidades.persist(segundo);
        String editar = ruta(reserva.getIdReservacion()) + "/editar";

        mvc.perform(formulario(editar, doble.getIdHabitacion(), entrada, entrada.plusDays(1), 1).with(propio()).with(csrf()))
                .andExpect(content().string(containsString("El nuevo total no puede ser menor que el importe ya pagado.")));

        entidades.clear();
        Reservacion guardada = guardada(reserva.getIdReservacion());
        assertThat(guardada.getCostoTotal()).isEqualByComparingTo("1701.50");
        assertThat(guardada.getFechaSalida()).isEqualTo(entrada.plusDays(2));
        assertThat(entidades.find(Pago.class, primero.getIdPago()).getMonto()).isEqualByComparingTo("450.00");
        assertThat(entidades.find(Pago.class, segundo.getIdPago()).getEstado()).isEqualTo(EstadoPago.PAGADO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoPago.class, names = {"PENDIENTE", "PROCESANDO"})
    void rechazaCambiosDeTotalDuranteUnPago(EstadoPago estado) throws Exception {
        Reservacion reserva = guardar(titular, doble, EstadoReservacion.CONFIRMADA, entrada, entrada.plusDays(2));
        Pago pago = new Pago(reserva, LocalDateTime.now(), new BigDecimal("1701.50"), "SIMULADO", estado);
        entidades.persist(pago);
        String editar = ruta(reserva.getIdReservacion()) + "/editar";

        mvc.perform(formulario(editar, doble.getIdHabitacion(), entrada, entrada.plusDays(2), 2).with(propio()).with(csrf()))
                .andExpect(redirectedUrl(ruta(reserva.getIdReservacion())));
        mvc.perform(formulario(editar, doble.getIdHabitacion(), entrada, entrada.plusDays(3), 2).with(propio()).with(csrf()))
                .andExpect(content().string(containsString("El total no puede cambiar mientras haya un pago pendiente o en proceso.")));

        entidades.clear();
        assertThat(guardada(reserva.getIdReservacion()).getCostoTotal()).isEqualByComparingTo("1701.50");
        assertThat(entidades.find(Pago.class, pago.getIdPago()).getMonto()).isEqualByComparingTo("1701.50");
        assertThat(entidades.find(Pago.class, pago.getIdPago()).getEstado()).isEqualTo(estado);
    }

    @Test
    void laEdicionConConflictoNoCambiaLosImportesNiElTitular() throws Exception {
        Reservacion movil = guardar(titular, doble, EstadoReservacion.CONFIRMADA, entrada, entrada.plusDays(2));
        guardar(otro, suite, EstadoReservacion.CONFIRMADA, entrada, entrada.plusDays(2));
        String editar = ruta(movil.getIdReservacion()) + "/editar";
        mvc.perform(formulario(editar, suite.getIdHabitacion(), entrada, entrada.plusDays(2), 2).with(recepcion()).with(csrf()))
                .andExpect(content().string(containsString("ya no está disponible")));
        mvc.perform(formulario(editar, doble.getIdHabitacion(), entrada, entrada.plusDays(2), 2)
                        .param("huespedId", otro.getIdHuesped().toString()).with(recepcion()).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(guardada(movil.getIdReservacion()).getHabitacion().getIdHabitacion()).isEqualTo(doble.getIdHabitacion());
        assertThat(guardada(movil.getIdReservacion()).getCostoTotal()).isEqualByComparingTo("1701.50");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReservacion.class, names = {"PENDIENTE", "CONFIRMADA"})
    void cancelaConConfirmacionConservaElPagoYLiberaFechas(EstadoReservacion estado) throws Exception {
        Reservacion reserva = guardar(titular, doble, estado, entrada, entrada.plusDays(2));
        Pago pago = new Pago(reserva, LocalDateTime.now(), new BigDecimal("80.00"), "SIMULADO", EstadoPago.PAGADO);
        entidades.persist(pago);
        String detalle = ruta(reserva.getIdReservacion());
        String cancelar = detalle + "/cancelar";
        mvc.perform(get(cancelar).with(propio())).andExpect(content().string(containsString("Confirmar cancelación")));
        assertThat(guardada(reserva.getIdReservacion()).getEstado()).isEqualTo(estado);
        mvc.perform(post(cancelar).with(propio()).with(csrf()))
                .andExpect(redirectedUrl(detalle)).andExpect(flash().attributeExists("mensajeExito"));
        entidades.flush();
        entidades.clear();
        assertThat(guardada(reserva.getIdReservacion()).getEstado()).isEqualTo(EstadoReservacion.CANCELADA);
        Pago pagoConservado = entidades.find(Pago.class, pago.getIdPago());
        assertThat(pagoConservado.getMonto()).isEqualByComparingTo("80.00");
        assertThat(pagoConservado.getEstado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(pagoConservado.getReservacion().getIdReservacion()).isEqualTo(reserva.getIdReservacion());
        mvc.perform(get("/disponibilidad").param("entrada", entrada.toString()).param("salida", entrada.plusDays(2).toString())
                        .param("huespedes", "2"))
                .andExpect(content().string(containsString("Habitación QA-202")));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReservacion.class, names = {"CANCELADA", "EN_CURSO", "FINALIZADA"})
    void protegeLasReservacionesCerradasOEnCurso(EstadoReservacion estado) throws Exception {
        Reservacion reserva = guardar(titular, doble, estado, entrada, entrada.plusDays(2));
        String ruta = ruta(reserva.getIdReservacion());
        mvc.perform(formulario(ruta + "/editar", suite.getIdHabitacion(), entrada, entrada.plusDays(3), 1)
                        .with(propio()).with(csrf()))
                .andExpect(content().string(containsString("Solo se pueden cambiar o cancelar")));
        mvc.perform(post(ruta + "/cancelar").with(propio()).with(csrf())).andExpect(flash().attributeExists("mensajeError"));
        assertThat(guardada(reserva.getIdReservacion()).getEstado()).isEqualTo(estado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/nueva", "/1/editar", "/1/cancelar"})
    void exigeTokenCsrfEnCadaEscritura(String sufijo) throws Exception {
        mvc.perform(post("/reservaciones" + sufijo).with(propio())).andExpect(status().isForbidden());
    }

    private long crearComo(RequestPostProcessor usuario, Long habitacion, LocalDate entrada, LocalDate salida,
                           int personas, Long huesped) throws Exception {
        MockHttpServletRequestBuilder peticion = formulario(NUEVA, habitacion, entrada, salida, personas).with(usuario).with(csrf());
        if (huesped != null) {
            peticion.param("huespedId", huesped.toString());
        }
        String url = mvc.perform(peticion).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(url.substring(url.lastIndexOf('/') + 1));
    }

    private void validarAlta(Long habitacion, LocalDate entrada, LocalDate salida, int personas, String mensaje) throws Exception {
        mvc.perform(formulario(NUEVA, habitacion, entrada, salida, personas).with(propio()).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(mensaje)));
    }

    private MockHttpServletRequestBuilder formulario(String ruta, Long habitacion, LocalDate entrada, LocalDate salida, int personas) {
        return post(ruta).param("habitacionId", habitacion.toString()).param("entrada", entrada.toString())
                .param("salida", salida.toString()).param("personas", Integer.toString(personas));
    }

    private Huesped huesped(String nombre, String correo) {
        Huesped huesped = new Huesped(nombre, null, correo);
        entidades.persist(huesped);
        entidades.persist(Cuenta.paraHuesped(huesped, "hash de prueba para autenticación simulada"));
        return huesped;
    }

    private Habitacion habitacion(String numero, int capacidad, BigDecimal tarifa) {
        TipoHabitacion tipo = new TipoHabitacion("Tipo " + numero, capacidad, null);
        entidades.persist(tipo);
        Habitacion habitacion = new Habitacion(numero, 1, EstadoHabitacion.DISPONIBLE, tarifa, tipo);
        entidades.persist(habitacion);
        return habitacion;
    }

    private Reservacion guardar(Huesped huesped, Habitacion habitacion, EstadoReservacion estado, LocalDate entrada, LocalDate salida) {
        BigDecimal tarifa = habitacion.getPrecioPorNoche();
        return reservaciones.save(new Reservacion(huesped, habitacion, entrada, salida, 1, tarifa,
                tarifa.multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(entrada, salida))), estado));
    }

    private Reservacion guardada(long id) {
        return reservaciones.findById(id).orElseThrow();
    }

    private String ruta(long id) {
        return "/reservaciones/" + id;
    }

    private RequestPostProcessor propio() {
        return user(titular.getCorreo()).roles("HUESPED");
    }

    private RequestPostProcessor recepcion() {
        return user("recepcion@example.test").roles("RECEPCIONISTA");
    }
}

package com.hotel.reservacioneshotel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.service.DisponibilidadService.HabitacionDisponible;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=${hotel.test.profile:test}")
@AutoConfigureMockMvc
@Transactional
class DisponibilidadMvcTests {

    private static final String RUTA = "/disponibilidad";
    private static final BigDecimal TARIFA = new BigDecimal("850.75");
    private final LocalDate hoy = LocalDate.now();
    private Huesped huespedDePrueba;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;

    @Test
    void muestraFormularioPublicoSinBuscarAlAbrirLaPagina() throws Exception {
        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Buscar habitaciones")))
                .andExpect(content().string(containsString("method=\"get\"")))
                .andExpect(model().attributeDoesNotExist("resultados"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HUESPED", "RECEPCIONISTA", "ADMINISTRADOR"})
    void permiteConsultarConCualquierRol(String rol) throws Exception {
        mvc.perform(consulta(hoy, hoy.plusDays(1), 1).with(user("rol@example.test").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("resultados"));
    }

    @Test
    void ofreceHabitacionesLibresYCalculaNochesYCostoSinRedondeos() throws Exception {
        Habitacion segunda = habitacion("QA-201", 2, EstadoHabitacion.DISPONIBLE);
        Habitacion primera = habitacion("QA-101", 2, EstadoHabitacion.DISPONIBLE);
        String entrada = hoy.plusDays(3).toString();
        String salida = hoy.plusDays(6).toString();

        var respuesta = mvc.perform(get(RUTA).param("entrada", entrada).param("salida", salida).param("huespedes", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El costo es una estimación")))
                .andExpect(content().string(containsString("value=\"" + entrada + "\"")))
                .andExpect(content().string(containsString("value=\"" + salida + "\"")))
                .andExpect(content().string(containsString("value=\"2\"")))
                .andReturn();
        assertThat(respuesta.getModelAndView().getModel().get("resultados"))
                .asList().containsExactly(
                        new HabitacionDisponible(primera.getIdHabitacion(), "QA-101", "Tipo QA-101", 2, TARIFA, 3, new BigDecimal("2552.25")),
                        new HabitacionDisponible(segunda.getIdHabitacion(), "QA-201", "Tipo QA-201", 2, TARIFA, 3, new BigDecimal("2552.25")));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReservacion.class, names = {"CONFIRMADA", "PENDIENTE"})
    void bloqueaReservacionesActivasQueSeSuperponen(EstadoReservacion estado) throws Exception {
        Habitacion habitacion = habitacion("QA-101", 2, EstadoHabitacion.DISPONIBLE);
        reservar(habitacion, 10, 15, estado);
        esperarSinResultados(consulta(hoy.plusDays(10), hoy.plusDays(15), 2));
    }

    @ParameterizedTest
    @CsvSource({"8, 12", "14, 18", "11, 13", "8, 18"})
    void bloqueaCrucesParcialesYContencionCompleta(int entrada, int salida) throws Exception {
        reservar(habitacion("QA-101", 2, EstadoHabitacion.DISPONIBLE), 10, 15, EstadoReservacion.CONFIRMADA);
        esperarSinResultados(consulta(hoy.plusDays(entrada), hoy.plusDays(salida), 2));
    }

    @ParameterizedTest
    @CsvSource({"8, 10", "15, 18"})
    void admiteDosEstanciasQueCompartenElDiaDeSalidaOEntrada(int entrada, int salida) throws Exception {
        reservar(habitacion("QA-101", 2, EstadoHabitacion.DISPONIBLE), 10, 15, EstadoReservacion.CONFIRMADA);
        esperarHabitacion(consulta(hoy.plusDays(entrada), hoy.plusDays(salida), 2), "QA-101");
    }

    @Test
    void reservaCanceladaNoBloqueaNiExponeDatosDeLaEstancia() throws Exception {
        reservar(habitacion("QA-101", 2, EstadoHabitacion.DISPONIBLE), 10, 15, EstadoReservacion.CANCELADA);
        mvc.perform(consulta(hoy.plusDays(11), hoy.plusDays(14), 2))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Habitación QA-101")))
                .andExpect(content().string(not(containsString("info-privada@example.test"))))
                .andExpect(content().string(not(containsString("Identidad privada de prueba"))))
                .andExpect(content().string(not(containsString("7777"))))
                .andExpect(content().string(not(containsString("CANCELADA"))));
        assertThat(entidades.createQuery("select count(r) from Reservacion r", Long.class).getSingleResult()).isEqualTo(1);
    }

    @Test
    void reservaFinalizadaNoBloqueaConsultaAunqueSusFechasOriginalesSeCruzan() throws Exception {
        reservar(habitacion("QA-101", 2, EstadoHabitacion.DISPONIBLE), -3, 2, EstadoReservacion.FINALIZADA);
        esperarHabitacion(consulta(hoy, hoy.plusDays(2), 2), "QA-101");
    }

    @Test
    void ocupadaConEstanciaVigenteSeOfreceDesdeSuSalida() throws Exception {
        reservar(habitacion("QA-101", 2, EstadoHabitacion.OCUPADA), -2, 3, EstadoReservacion.EN_CURSO);
        esperarSinResultados(consulta(hoy.plusDays(2), hoy.plusDays(5), 2));
        esperarHabitacion(consulta(hoy.plusDays(3), hoy.plusDays(5), 2), "QA-101");
    }

    @Test
    void admiteLaEntradaElDiaDeSalidaPrevistaDeUnaEstanciaEnCurso() throws Exception {
        reservar(habitacion("QA-101", 2, EstadoHabitacion.OCUPADA), -2, 0, EstadoReservacion.EN_CURSO);
        esperarHabitacion(consulta(hoy, hoy.plusDays(1), 2), "QA-101");
    }

    @Test
    void noOfreceOcupadasSinSalidaFiableNiConEstanciasContradictorias() throws Exception {
        habitacion("QA-SIN-ESTANCIA", 2, EstadoHabitacion.OCUPADA);
        reservar(habitacion("QA-VENCIDA", 2, EstadoHabitacion.OCUPADA), -5, -2, EstadoReservacion.EN_CURSO);
        reservar(habitacion("QA-ENTRADA-FUTURA", 2, EstadoHabitacion.OCUPADA), 1, 3, EstadoReservacion.EN_CURSO);
        Habitacion dobleEstancia = habitacion("QA-DUPLICADA", 2, EstadoHabitacion.OCUPADA);
        reservar(dobleEstancia, -2, 3, EstadoReservacion.EN_CURSO);
        reservar(dobleEstancia, -1, 4, EstadoReservacion.EN_CURSO);
        reservar(habitacion("QA-DISP-VENCIDA", 2, EstadoHabitacion.DISPONIBLE), -4, -1, EstadoReservacion.EN_CURSO);
        esperarSinResultados(consulta(hoy.plusDays(10), hoy.plusDays(13), 2));
    }

    @Test
    void respetaLaEstanciaEnCursoAunqueElInventarioIndiqueDisponible() throws Exception {
        reservar(habitacion("QA-101", 2, EstadoHabitacion.DISPONIBLE), -1, 2, EstadoReservacion.EN_CURSO);
        esperarSinResultados(consulta(hoy, hoy.plusDays(2), 2));
        esperarHabitacion(consulta(hoy.plusDays(2), hoy.plusDays(3), 2), "QA-101");
    }

    @Test
    void capacidadInsuficienteDevuelveListadoVacio() throws Exception {
        habitacion("QA-101", 2, EstadoHabitacion.DISPONIBLE);
        esperarSinResultados(consulta(hoy, hoy.plusDays(2), 3));
    }

    @Test
    void pideLosTresCamposAlEnviarElFormularioVacio() throws Exception {
        mvc.perform(get(RUTA).param("entrada", "").param("salida", "").param("huespedes", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("disponibilidadForm", "entrada", "salida", "huespedes"))
                .andExpect(content().string(containsString("Indica la fecha de entrada.")))
                .andExpect(content().string(containsString("Indica la fecha de salida.")))
                .andExpect(content().string(containsString("Indica el número de huéspedes.")))
                .andExpect(model().attributeDoesNotExist("resultados"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void rechazaSalidaAnteriorOIgualALaEntrada(int dias) throws Exception {
        mvc.perform(consulta(hoy, hoy.plusDays(dias), 1))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La fecha de salida debe ser posterior a la fecha de entrada.")))
                .andExpect(model().attributeDoesNotExist("resultados"));
    }

    @Test
    void rechazaEntradaAnteriorALaFechaDelServidor() throws Exception {
        mvc.perform(consulta(hoy.minusDays(1), hoy.plusDays(1), 1))
                .andExpect(content().string(containsString("La fecha de entrada debe ser hoy o posterior.")))
                .andExpect(model().attributeDoesNotExist("resultados"));
    }

    @Test
    void rechazaRangoMayorAlLimiteYFechasFueraDelTipoDateDeMysql() throws Exception {
        mvc.perform(consulta(hoy, hoy.plusDays(366), 1))
                .andExpect(content().string(containsString("La consulta admite estancias de hasta 365 noches.")))
                .andExpect(model().attributeDoesNotExist("resultados"));
        mvc.perform(consulta(LocalDate.of(9999, 12, 31), LocalDate.of(10000, 1, 1), 1))
                .andExpect(content().string(containsString("La fecha de salida debe corresponder a un año de hasta cuatro cifras.")))
                .andExpect(model().attributeDoesNotExist("resultados"));
    }

    @Test
    void rechazaFechasDeCalendarioInexistentesYCantidadNoNumerica() throws Exception {
        mvc.perform(get(RUTA).param("entrada", "2028-02-30").param("salida", "mañana").param("huespedes", "dos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribe una fecha de entrada válida.")))
                .andExpect(content().string(containsString("Escribe una fecha de salida válida.")))
                .andExpect(content().string(containsString("Escribe un número entero de huéspedes válido.")))
                .andExpect(model().attributeDoesNotExist("resultados"));
    }

    @Test
    void rechazaCeroHuespedes() throws Exception {
        mvc.perform(consulta(hoy, hoy.plusDays(1), 0))
                .andExpect(content().string(containsString("El número de huéspedes debe ser mayor que cero.")))
                .andExpect(model().attributeDoesNotExist("resultados"));
    }

    private void esperarSinResultados(MockHttpServletRequestBuilder peticion) throws Exception {
        mvc.perform(peticion)
                .andExpect(status().isOk())
                .andExpect(model().attribute("resultados", List.of()))
                .andExpect(content().string(containsString("No hay habitaciones disponibles")));
    }

    private void esperarHabitacion(MockHttpServletRequestBuilder peticion, String numero) throws Exception {
        mvc.perform(peticion)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Habitación " + numero)));
    }

    private MockHttpServletRequestBuilder consulta(LocalDate entrada, LocalDate salida, int huespedes) {
        return get(RUTA).param("entrada", entrada.toString()).param("salida", salida.toString())
                .param("huespedes", Integer.toString(huespedes));
    }

    private Habitacion habitacion(String numero, int capacidad, EstadoHabitacion estado) {
        TipoHabitacion tipo = new TipoHabitacion("Tipo " + numero, capacidad, "Descripción interna de prueba");
        entidades.persist(tipo);
        Habitacion habitacion = new Habitacion(numero, 1, estado, TARIFA, tipo);
        entidades.persist(habitacion);
        return habitacion;
    }

    private void reservar(Habitacion habitacion, int entrada, int salida, EstadoReservacion estado) {
        if (huespedDePrueba == null) {
            huespedDePrueba = new Huesped("Identidad privada de prueba", "000-000-7777", "info-privada@example.test");
            entidades.persist(huespedDePrueba);
        }
        BigDecimal tarifaHistorica = new BigDecimal("233.00");
        entidades.persist(new Reservacion(huespedDePrueba, habitacion, hoy.plusDays(entrada), hoy.plusDays(salida), 1,
                tarifaHistorica, tarifaHistorica.multiply(BigDecimal.valueOf(salida - entrada)), estado));
    }
}

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
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.HabitacionRepository;
import com.hotel.reservacioneshotel.repository.TipoHabitacionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
class InventarioCrudMvcTests {

    private static final String HABITACIONES = "/habitaciones";
    private static final String TIPOS = "/tipos-habitacion";
    private static final BigDecimal PRECIO = new BigDecimal("850.75");
    private static final BigDecimal COSTO_DOS_NOCHES = new BigDecimal("1701.50");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;
    @Autowired
    private HabitacionRepository habitaciones;
    @Autowired
    private TipoHabitacionRepository tipos;

    @Test
    void registraUnaHabitacionConTipoTarifaYEstado() throws Exception {
        TipoHabitacion tipo = guardarTipo("Doble", 2);
        mvc.perform(formularioHabitacion(HABITACIONES, " QA-101 ", "0", tipo.getIdTipo(), "850.75")
                        .param("estado", "OCUPADA"))
                .andExpect(redirectedUrl(HABITACIONES))
                .andExpect(flash().attribute("mensajeExito", "Habitación registrada correctamente."));
        entidades.clear();
        assertThat(habitaciones.findAll()).singleElement().satisfies(habitacion -> {
            assertThat(habitacion.getNumero()).isEqualTo("QA-101");
            assertThat(habitacion.getPiso()).isZero();
            assertThat(habitacion.getTipoHabitacion().getIdTipo()).isEqualTo(tipo.getIdTipo());
            assertThat(habitacion.getPrecioPorNoche()).isEqualByComparingTo(PRECIO);
            assertThat(habitacion.getEstadoOperativo()).isEqualTo(EstadoHabitacion.OCUPADA);
        });
    }

    @Test
    void rechazaDuplicadosEnAltaYEdicionAunqueCambienMayusculas() throws Exception {
        TipoHabitacion tipo = guardarTipo("Doble", 2);
        guardarHabitacion("QA-101", tipo);
        Habitacion otra = guardarHabitacion("QA-102", tipo);
        for (String ruta : List.of(HABITACIONES, ruta(otra, "editar"))) {
            mvc.perform(formularioHabitacion(ruta, " qa-101 ", "1", tipo.getIdTipo(), "850.75")
                            .param("estado", "DISPONIBLE"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Ese número de habitación ya está registrado.")));
        }
        assertThat(habitaciones.count()).isEqualTo(2);
        assertThat(habitaciones.findById(otra.getIdHabitacion()).orElseThrow().getNumero()).isEqualTo("QA-102");
    }

    @Test
    void validaNumeroPisoPrecioYEstadoEnElServidor() throws Exception {
        TipoHabitacion tipo = guardarTipo("Doble", 2);
        mvc.perform(formularioHabitacion(HABITACIONES, " ", "-1", tipo.getIdTipo(), "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribe el número de habitación.")))
                .andExpect(content().string(containsString("cero o un número entero positivo")))
                .andExpect(content().string(containsString("El precio por noche debe ser mayor que cero.")))
                .andExpect(content().string(containsString("Selecciona un estado operativo.")));
        mvc.perform(formularioHabitacion(HABITACIONES, "A".repeat(21), "2", tipo.getIdTipo(), "650.501")
                        .param("estado", "DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El número admite hasta 20 caracteres.")))
                .andExpect(content().string(containsString("Usa hasta 10 cifras enteras y 2 decimales.")));
        mvc.perform(formularioHabitacion(HABITACIONES, "QA-101", "2.5", tipo.getIdTipo(), "650.50")
                        .param("estado", "RESERVADA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El piso debe ser un número entero válido.")))
                .andExpect(content().string(containsString("Selecciona un estado operativo válido.")));
        assertThat(habitaciones.count()).isZero();
    }

    @Test
    void elTipoSeleccionadoDebeExistir() throws Exception {
        mvc.perform(formularioHabitacion(HABITACIONES, "QA-111", "1", Long.MAX_VALUE, "800")
                        .param("estado", "DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El tipo de habitación seleccionado ya no existe.")));
        assertThat(habitaciones.count()).isZero();
    }

    @Test
    void consultaElInventarioYFiltraPorNumeroYTipo() throws Exception {
        mvc.perform(get(HABITACIONES).with(admin()))
                .andExpect(content().string(containsString("Aún no hay habitaciones registradas.")));
        TipoHabitacion doble = guardarTipo("Doble", 2);
        TipoHabitacion suite = guardarTipo("Suite", 4);
        guardarHabitacion("QA-310", doble);
        guardarHabitacion("QA-311", suite);
        mvc.perform(get(HABITACIONES).with(admin()))
                .andExpect(content().string(containsString("QA-310")))
                .andExpect(content().string(containsString("QA-311")))
                .andExpect(content().string(containsString("850.75")));
        mvc.perform(get(HABITACIONES).with(admin()).param("q", " qa-31 ").param("tipo", suite.getIdTipo().toString()))
                .andExpect(content().string(containsString("QA-311")))
                .andExpect(content().string(not(containsString("QA-310"))));
        mvc.perform(get(HABITACIONES).with(admin()).param("q", "%"))
                .andExpect(content().string(containsString("No se encontraron habitaciones")));
    }

    @Test
    void modificaDatosSinAlterarEstadoNiImportesDeLaReservacion() throws Exception {
        TipoHabitacion tipo = guardarTipo("Doble", 2);
        TipoHabitacion suite = guardarTipo("Suite", 4);
        Habitacion habitacion = guardarHabitacion("QA-101", tipo);
        Reservacion reservacion = guardarReservacion(habitacion, EstadoReservacion.CONFIRMADA, 2);
        String editar = ruta(habitacion, "editar");
        mvc.perform(get(editar).with(admin()))
                .andExpect(content().string(containsString("QA-101")))
                .andExpect(content().string(not(containsString("name=\"estado\""))));
        mvc.perform(formularioHabitacion(editar, "QA-205", "2", suite.getIdTipo(), "950.00")
                        .param("estado", "OCUPADA"))
                .andExpect(redirectedUrl(HABITACIONES));
        entidades.clear();
        Habitacion guardada = habitaciones.findById(habitacion.getIdHabitacion()).orElseThrow();
        assertThat(guardada.getNumero()).isEqualTo("QA-205");
        assertThat(guardada.getPiso()).isEqualTo(2);
        assertThat(guardada.getTipoHabitacion().getIdTipo()).isEqualTo(suite.getIdTipo());
        assertThat(guardada.getPrecioPorNoche()).isEqualByComparingTo("950.00");
        assertThat(guardada.getEstadoOperativo()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        Reservacion historica = entidades.find(Reservacion.class, reservacion.getIdReservacion());
        assertThat(historica.getPrecioNocheAplicado()).isEqualByComparingTo(PRECIO);
        assertThat(historica.getCostoTotal()).isEqualByComparingTo(COSTO_DOS_NOCHES);
    }

    @Test
    void duranteUnaEstanciaSoloPermiteCambiarLaTarifa() throws Exception {
        TipoHabitacion tipo = guardarTipo("Doble", 2);
        Habitacion habitacion = guardarHabitacion("QA-101", tipo);
        guardarReservacion(habitacion, EstadoReservacion.EN_CURSO, 2);
        String editar = ruta(habitacion, "editar");
        mvc.perform(formularioHabitacion(editar, "QA-999", "1", tipo.getIdTipo(), "950.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("En este momento solo se puede modificar la tarifa.")));
        mvc.perform(formularioHabitacion(editar, "QA-101", "1", tipo.getIdTipo(), "950.00"))
                .andExpect(redirectedUrl(HABITACIONES));
        assertThat(habitaciones.findById(habitacion.getIdHabitacion()).orElseThrow().getNumero()).isEqualTo("QA-101");
    }

    @Test
    void protegeElCupoDeLasReservacionesVigentesAlCambiarHabitacionOTipo() throws Exception {
        TipoHabitacion doble = guardarTipo("Doble", 2);
        TipoHabitacion individual = guardarTipo("Individual", 1);
        Habitacion habitacion = guardarHabitacion("QA-101", doble);
        guardarReservacion(habitacion, EstadoReservacion.PENDIENTE, 2);
        mvc.perform(formularioHabitacion(ruta(habitacion, "editar"), "QA-101", "1", individual.getIdTipo(), "850.75"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La capacidad del tipo es insuficiente")));
        mvc.perform(formularioTipo(ruta(doble, "editar"), "Doble", "1", "De prueba"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La nueva capacidad es insuficiente")));
        assertThat(tipos.findById(doble.getIdTipo()).orElseThrow().getCapacidad()).isEqualTo(2);
    }

    @Test
    void eliminaHabitacionSinReservacionesSoloTrasConfirmar() throws Exception {
        Habitacion habitacion = guardarHabitacion("QA-101", guardarTipo("Doble", 2));
        String eliminar = ruta(habitacion, "eliminar");
        mvc.perform(get(eliminar).with(admin())).andExpect(content().string(containsString("Confirmar eliminación")));
        assertThat(habitaciones.existsById(habitacion.getIdHabitacion())).isTrue();
        mvc.perform(post(eliminar).with(admin()).with(csrf())).andExpect(redirectedUrl(HABITACIONES));
        assertThat(habitaciones.existsById(habitacion.getIdHabitacion())).isFalse();
    }

    @Test
    void conservaHabitacionConHistorialCanceladoYSusPagos() throws Exception {
        Habitacion habitacion = guardarHabitacion("QA-101", guardarTipo("Doble", 2));
        Reservacion reservacion = guardarReservacion(habitacion, EstadoReservacion.CANCELADA, 2);
        Pago pago = new Pago(reservacion, LocalDateTime.of(2027, 1, 12, 10, 0), PRECIO, "SIMULADO", EstadoPago.RECHAZADO);
        entidades.persist(pago);
        entidades.flush();
        comprobarEliminacionBloqueada(ruta(habitacion, "eliminar"), "La habitación tiene reservaciones asociadas.");
        entidades.clear();
        assertThat(habitaciones.existsById(habitacion.getIdHabitacion())).isTrue();
        assertThat(entidades.find(Reservacion.class, reservacion.getIdReservacion())).isNotNull();
        assertThat(entidades.find(Pago.class, pago.getIdPago())).isNotNull();
    }

    @Test
    void registraConsultaYEditaUnTipoInclusoConUnaHabitacionAsociada() throws Exception {
        mvc.perform(get(TIPOS).with(admin()))
                .andExpect(content().string(containsString("Aún no hay tipos de habitación registrados.")));
        mvc.perform(formularioTipo(TIPOS, " Suite ", "4", " Capacidad extra. "))
                .andExpect(redirectedUrl(TIPOS))
                .andExpect(flash().attribute("mensajeExito", "Tipo de habitación registrado correctamente."));
        TipoHabitacion tipo = tipos.findAll().getFirst();
        assertThat(tipo.getNombre()).isEqualTo("Suite");
        assertThat(tipo.getCapacidad()).isEqualTo(4);
        assertThat(tipo.getDescripcion()).isEqualTo("Capacidad extra.");
        guardarHabitacion("QA-101", tipo);
        mvc.perform(get(TIPOS).with(admin())).andExpect(content().string(containsString("Capacidad extra.")));
        mvc.perform(get(ruta(tipo, "editar")).with(admin())).andExpect(status().isOk());
        mvc.perform(formularioTipo(ruta(tipo, "editar"), "Suite grande", "5", "Nueva descripción."))
                .andExpect(redirectedUrl(TIPOS));
        entidades.clear();
        TipoHabitacion actualizado = tipos.findById(tipo.getIdTipo()).orElseThrow();
        assertThat(actualizado.getNombre()).isEqualTo("Suite grande");
        assertThat(actualizado.getCapacidad()).isEqualTo(5);
        assertThat(actualizado.getDescripcion()).isEqualTo("Nueva descripción.");
    }

    @Test
    void rechazaTiposDuplicadosEnAltaYEdicion() throws Exception {
        guardarTipo("Doble", 2);
        TipoHabitacion suite = guardarTipo("Suite", 4);
        for (String ruta : List.of(TIPOS, ruta(suite, "editar"))) {
            mvc.perform(formularioTipo(ruta, " doble ", "4", ""))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Ese nombre de tipo")));
        }
        assertThat(tipos.count()).isEqualTo(2);
    }

    @Test
    void validaNombreCapacidadYDescripcionDelTipo() throws Exception {
        mvc.perform(formularioTipo(TIPOS, "", "0", "d".repeat(256)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribe el nombre del tipo de habitación.")))
                .andExpect(content().string(containsString("La capacidad debe ser mayor que cero.")))
                .andExpect(content().string(containsString("La descripción admite hasta 255 caracteres.")));
        assertThat(tipos.count()).isZero();
    }

    @Test
    void eliminaTipoLibreYConservaElQueTieneHabitaciones() throws Exception {
        TipoHabitacion tipoLibre = guardarTipo("Individual", 1);
        String eliminar = ruta(tipoLibre, "eliminar");
        mvc.perform(get(eliminar).with(admin())).andExpect(content().string(containsString("Confirmar eliminación")));
        assertThat(tipos.existsById(tipoLibre.getIdTipo())).isTrue();
        mvc.perform(post(eliminar).with(admin()).with(csrf())).andExpect(redirectedUrl(TIPOS));
        assertThat(tipos.existsById(tipoLibre.getIdTipo())).isFalse();

        TipoHabitacion tipoAsignado = guardarTipo("Doble", 2);
        Habitacion habitacion = guardarHabitacion("QA-101", tipoAsignado);
        comprobarEliminacionBloqueada(ruta(tipoAsignado, "eliminar"), "El tipo tiene habitaciones asociadas.");
        assertThat(tipos.existsById(tipoAsignado.getIdTipo())).isTrue();
        assertThat(habitaciones.existsById(habitacion.getIdHabitacion())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {HABITACIONES, TIPOS})
    void devuelve404ParaIdentificadoresOAccionesInexistentes(String modulo) throws Exception {
        for (String accion : List.of("editar", "eliminar")) {
            String ruta = modulo + "/" + Long.MAX_VALUE + "/" + accion;
            mvc.perform(get(ruta).with(admin()))
                    .andExpect(status().isNotFound()).andExpect(content().string(containsString("Registro no encontrado")));
            mvc.perform(post(ruta).with(admin()).with(csrf())).andExpect(status().isNotFound());
        }
        mvc.perform(get(modulo + "/1/no-existe").with(admin())).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"HUESPED", "RECEPCIONISTA"})
    void deniegaLasRutasDeInventarioAOtrosRoles(String rol) throws Exception {
        RequestPostProcessor usuario = user("otro@example.test").roles(rol);
        for (String modulo : List.of(HABITACIONES, TIPOS)) {
            for (String ruta : List.of(modulo, modulo + "/1/editar", modulo + "/1/eliminar")) {
                mvc.perform(get(ruta).with(usuario)).andExpect(status().isForbidden());
                mvc.perform(post(ruta).with(usuario).with(csrf())).andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void requiereSesionYTokenCsrfParaAmbosModulos() throws Exception {
        TipoHabitacion tipo = guardarTipo("Individual", 1);
        TipoHabitacion tipoLibre = guardarTipo("Suite", 4);
        Habitacion habitacion = guardarHabitacion("QA-101", tipo);
        for (String ruta : List.of(HABITACIONES, TIPOS, ruta(habitacion, "editar"), ruta(habitacion, "eliminar"),
                ruta(tipoLibre, "editar"), ruta(tipoLibre, "eliminar"))) {
            mvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
            mvc.perform(post(ruta).with(csrf())).andExpect(redirectedUrl("/login"));
            mvc.perform(post(ruta).with(admin())).andExpect(status().isForbidden());
            mvc.perform(get(ruta).with(admin())).andExpect(content().string(containsString("name=\"_csrf\"")));
        }
        assertThat(habitaciones.existsById(habitacion.getIdHabitacion())).isTrue();
        assertThat(tipos.existsById(tipoLibre.getIdTipo())).isTrue();
    }

    private TipoHabitacion guardarTipo(String nombre, int capacidad) {
        return tipos.saveAndFlush(new TipoHabitacion(nombre, capacidad, "Categoría de prueba"));
    }

    private Habitacion guardarHabitacion(String numero, TipoHabitacion tipo) {
        return habitaciones.saveAndFlush(new Habitacion(numero, 1, EstadoHabitacion.DISPONIBLE, PRECIO, tipo));
    }

    private Reservacion guardarReservacion(Habitacion habitacion, EstadoReservacion estado, int personas) {
        Huesped huesped = new Huesped("Ficha de prueba", null, "inventario@example.test");
        entidades.persist(huesped);
        LocalDate entrada = LocalDate.of(2027, 1, 10);
        Reservacion reservacion = new Reservacion(huesped, habitacion, entrada, entrada.plusDays(2),
                personas, PRECIO, COSTO_DOS_NOCHES, estado);
        entidades.persist(reservacion);
        entidades.flush();
        return reservacion;
    }

    private void comprobarEliminacionBloqueada(String eliminar, String motivo) throws Exception {
        mvc.perform(get(eliminar).with(admin())).andExpect(content().string(containsString(motivo)))
                .andExpect(content().string(not(containsString("Confirmar eliminación"))));
        mvc.perform(post(eliminar).with(admin()).with(csrf()))
                .andExpect(redirectedUrl(eliminar))
                .andExpect(flash().attribute("mensajeError", containsString(motivo)));
    }

    private String ruta(Habitacion habitacion, String accion) {
        return HABITACIONES + "/" + habitacion.getIdHabitacion() + "/" + accion;
    }

    private String ruta(TipoHabitacion tipo, String accion) {
        return TIPOS + "/" + tipo.getIdTipo() + "/" + accion;
    }

    private MockHttpServletRequestBuilder formularioHabitacion(
            String ruta, String numero, String piso, Long tipo, String precio) {
        return post(ruta).with(admin()).with(csrf()).param("numero", numero).param("piso", piso)
                .param("idTipo", tipo.toString()).param("precio", precio);
    }

    private MockHttpServletRequestBuilder formularioTipo(String ruta, String nombre, String capacidad, String descripcion) {
        return post(ruta).with(admin()).with(csrf()).param("nombre", nombre).param("capacidad", capacidad)
                .param("descripcion", descripcion);
    }

    private RequestPostProcessor admin() {
        return user("administracion@example.test").roles("ADMINISTRADOR");
    }
}

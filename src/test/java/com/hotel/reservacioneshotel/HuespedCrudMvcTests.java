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
import com.hotel.reservacioneshotel.domain.usuarios.Recepcionista;
import com.hotel.reservacioneshotel.repository.CuentaRepository;
import com.hotel.reservacioneshotel.repository.HuespedRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=${hotel.test.profile:test}")
@AutoConfigureMockMvc
@Transactional
class HuespedCrudMvcTests {

    private static final String CORREO = "recepcion-alta@example.test";
    private static final String TELEFONO = "000-010-0000";
    private static final String TELEFONO_NUEVO = "000-010-0001";
    private static final String CLAVE = UUID.randomUUID().toString();
    private static final String DUPLICADO = "Ese correo electrónico ya está registrado.";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entidades;
    @Autowired
    private HuespedRepository huespedes;
    @Autowired
    private CuentaRepository cuentas;
    @Autowired
    private PasswordEncoder codificador;

    @Test
    void creaUnaFichaSinAccesoYReservaElCorreo() throws Exception {
        mvc.perform(ficha("/huespedes", "  Alta de recepción  ", " RECEPCION-ALTA@example.test ", TELEFONO))
                .andExpect(redirectedUrl("/huespedes"))
                .andExpect(flash().attribute("mensajeExito", "Huésped registrado correctamente."));

        entidades.clear();
        Cuenta cuenta = cuentas.findByCorreo(CORREO).orElseThrow();
        assertThat(cuenta.isActiva()).isFalse();
        assertThat(cuenta.getContrasenaHash()).isNull();
        assertThat(cuenta.getHuesped().getNombre()).isEqualTo("Alta de recepción");
        assertThat(cuenta.getHuesped().getTelefono()).isEqualTo(TELEFONO);
        assertThat(cuenta.getHuesped().getCorreo()).isEqualTo(CORREO);
        mvc.perform(post("/login").with(csrf()).param("correo", CORREO).param("password", CLAVE))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        mvc.perform(post("/registro").with(csrf()).param("nombre", "Registro de prueba")
                        .param("correo", CORREO).param("telefono", TELEFONO)
                        .param("contrasena", CLAVE).param("confirmacion", CLAVE))
                .andExpect(status().isOk()).andExpect(content().string(containsString(DUPLICADO)));
    }

    @Test
    void validaCamposObligatoriosYFormato() throws Exception {
        mvc.perform(ficha("/huespedes", " ", "no-es-correo", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribe tu nombre completo.")))
                .andExpect(content().string(containsString("Escribe un correo electrónico válido.")))
                .andExpect(content().string(containsString("Escribe tu teléfono.")));
        mvc.perform(ficha("/huespedes", "Nombre", "", TELEFONO))
                .andExpect(content().string(containsString("Escribe tu correo electrónico.")));
        assertThat(huespedes.count()).isZero();
    }

    @Test
    void limitaLasLongitudesEnElServidor() throws Exception {
        String correoLargo = "a".repeat(64) + "@" + ("b".repeat(63) + ".").repeat(3) + "test";
        mvc.perform(ficha("/huespedes", "a".repeat(121), correoLargo, "0".repeat(33)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("como máximo 120 caracteres")))
                .andExpect(content().string(containsString("como máximo 254 caracteres")))
                .andExpect(content().string(containsString("como máximo 32 caracteres")));
        assertThat(huespedes.count()).isZero();
    }

    @Test
    void rechazaDuplicadosDeFichasHistoricasYCuentasDeEmpleado() throws Exception {
        guardar("Ficha histórica", CORREO);
        var empleado = new Recepcionista("Empleado de prueba", "empleado_prueba", codificador.encode(CLAVE), true);
        entidades.persist(empleado);
        String correoEmpleado = "empleado@example.test";
        cuentas.saveAndFlush(Cuenta.paraEmpleado(correoEmpleado, empleado));

        for (String correo : List.of(" RECEPCION-ALTA@example.test ", correoEmpleado)) {
            mvc.perform(ficha("/huespedes", "Duplicado", correo, TELEFONO))
                    .andExpect(status().isOk()).andExpect(content().string(containsString(DUPLICADO)));
        }
        assertThat(huespedes.count()).isEqualTo(1);
        assertThat(cuentas.count()).isEqualTo(1);
    }

    @Test
    void listaLosDatosRealesYPermiteBuscarPorNombreCorreoEId() throws Exception {
        mvc.perform(get("/huespedes").with(recepcion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aún no hay huéspedes registrados.")));
        Huesped primero = guardar("Registro Álamo", CORREO);
        guardar("Registro Colina", "colina@example.test");
        mvc.perform(get("/huespedes").with(recepcion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(primero.getIdHuesped().toString())))
                .andExpect(content().string(containsString("Registro Álamo")))
                .andExpect(content().string(containsString(TELEFONO)))
                .andExpect(content().string(containsString(CORREO)))
                .andExpect(content().string(containsString("Registro Colina")));

        for (String texto : List.of(" ÁLAMO ", "RECEPCION-ALTA@", primero.getIdHuesped().toString())) {
            mvc.perform(get("/huespedes").with(recepcion()).param("q", texto))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Registro Álamo")))
                    .andExpect(content().string(not(containsString("Registro Colina"))));
        }
        mvc.perform(get("/huespedes").with(recepcion()).param("q", "%"))
                .andExpect(content().string(containsString("No se encontraron huéspedes")));
    }

    @Test
    void actualizaLaFichaYElCorreoDeSuCuentaInactiva() throws Exception {
        Huesped huesped = guardar("Nombre anterior", CORREO);
        cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, null));
        String editar = ruta(huesped, "editar");
        String nuevo = "nuevo@example.test";
        mvc.perform(get(editar).with(recepcion()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Nombre anterior")));
        mvc.perform(ficha(editar, "Nombre nuevo", nuevo, TELEFONO_NUEVO))
                .andExpect(redirectedUrl("/huespedes"))
                .andExpect(flash().attribute("mensajeExito", "Datos del huésped actualizados."));

        entidades.clear();
        Cuenta guardada = cuentas.findByCorreo(nuevo).orElseThrow();
        assertThat(guardada.getHuesped().getIdHuesped()).isEqualTo(huesped.getIdHuesped());
        assertThat(cuentas.count()).isEqualTo(1);
        assertThat(guardada.isActiva()).isFalse();
        assertThat(guardada.getHuesped().getCorreo()).isEqualTo(nuevo);
        assertThat(guardada.getHuesped().getNombre()).isEqualTo("Nombre nuevo");
        assertThat(guardada.getHuesped().getTelefono()).isEqualTo(TELEFONO_NUEVO);
        assertThat(cuentas.findByCorreo(CORREO)).isEmpty();
    }

    @Test
    void rechazaActualizarConUnCorreoOcupadoYConservaLaFicha() throws Exception {
        Huesped huesped = guardar("Nombre anterior", CORREO);
        cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, null));
        Huesped otro = guardar("Otra ficha", "otro@example.test");
        mvc.perform(ficha(ruta(huesped, "editar"), "Nombre nuevo", otro.getCorreo(), TELEFONO_NUEVO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(DUPLICADO)));
        entidades.clear();
        Cuenta guardada = cuentas.findByCorreo(CORREO).orElseThrow();
        assertThat(guardada.getHuesped().getCorreo()).isEqualTo(CORREO);
        assertThat(guardada.getHuesped().getNombre()).isEqualTo("Nombre anterior");
    }

    @Test
    void puedeEditarUnaFichaHistoricaYReservaElCorreoNuevo() throws Exception {
        Huesped huesped = guardar("Nombre anterior", CORREO);
        String editar = ruta(huesped, "editar");
        mvc.perform(ficha(editar, "Nombre nuevo", CORREO, TELEFONO_NUEVO))
                .andExpect(redirectedUrl("/huespedes"));
        assertThat(cuentas.findByHuesped_IdHuesped(huesped.getIdHuesped())).isEmpty();

        String nuevo = "historico@example.test";
        mvc.perform(ficha(editar, "Nombre nuevo", nuevo, TELEFONO_NUEVO))
                .andExpect(redirectedUrl("/huespedes"));
        assertThat(cuentas.findByCorreo(nuevo).orElseThrow().isActiva()).isFalse();
    }

    @Test
    void protegeElCorreoActivoYPermiteEditarLosDemasCampos() throws Exception {
        Huesped huesped = guardar("Nombre anterior", CORREO);
        String hash = codificador.encode(CLAVE);
        cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, hash));
        String editar = ruta(huesped, "editar");
        mvc.perform(get(editar).with(recepcion()))
                .andExpect(content().string(containsString("readonly")))
                .andExpect(content().string(containsString("pertenece a una cuenta activa")));
        mvc.perform(ficha(editar, "Nombre nuevo", "cambio@example.test", TELEFONO_NUEVO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Su cambio requiere verificar la nueva dirección.")));
        entidades.clear();
        assertThat(huespedes.findById(huesped.getIdHuesped()).orElseThrow().getNombre()).isEqualTo("Nombre anterior");

        mvc.perform(ficha(editar, "Nombre nuevo", CORREO, TELEFONO_NUEVO))
                .andExpect(redirectedUrl("/huespedes"));
        entidades.clear();
        Cuenta guardada = cuentas.findByCorreo(CORREO).orElseThrow();
        assertThat(guardada.getContrasenaHash()).isEqualTo(hash);
        assertThat(guardada.getHuesped().getCorreo()).isEqualTo(CORREO);
        assertThat(guardada.getHuesped().getNombre()).isEqualTo("Nombre nuevo");
        assertThat(guardada.getHuesped().getTelefono()).isEqualTo(TELEFONO_NUEVO);
        assertThat(cuentas.findByCorreo("cambio@example.test")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void eliminaUnaFichaSinRelacionesConConfirmacion(boolean reservarCorreo) throws Exception {
        Huesped huesped = guardar("Ficha eliminable", CORREO);
        if (reservarCorreo) {
            cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, null));
        }
        String eliminar = ruta(huesped, "eliminar");
        mvc.perform(get(eliminar).with(recepcion()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Confirmar eliminación")));
        assertThat(huespedes.existsById(huesped.getIdHuesped())).isTrue();
        mvc.perform(post(eliminar).with(recepcion()).with(csrf()))
                .andExpect(redirectedUrl("/huespedes"))
                .andExpect(flash().attribute("mensajeExito", "Huésped eliminado correctamente."));
        entidades.clear();
        assertThat(huespedes.existsById(huesped.getIdHuesped())).isFalse();
        assertThat(cuentas.findByCorreo(CORREO)).isEmpty();
    }

    @Test
    void conservaUnaFichaConReservacionesYPagosHistoricos() throws Exception {
        Huesped huesped = guardar("Ficha con historial", CORREO);
        cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, null));
        TipoHabitacion tipo = new TipoHabitacion("Doble de prueba", 2, null);
        entidades.persist(tipo);
        var precio = new BigDecimal("200.00");
        Habitacion habitacion = new Habitacion("P-201", 2, EstadoHabitacion.DISPONIBLE, precio, tipo);
        entidades.persist(habitacion);
        LocalDate entrada = LocalDate.of(2027, 1, 10);
        Reservacion reservacion = new Reservacion(huesped, habitacion, entrada, entrada.plusDays(1),
                1, precio, precio, EstadoReservacion.CANCELADA);
        entidades.persist(reservacion);
        Pago pago = new Pago(reservacion, LocalDateTime.of(2027, 1, 10, 14, 30), precio, "SIMULADO", EstadoPago.RECHAZADO);
        entidades.persist(pago);
        entidades.flush();

        comprobarEliminacionBloqueada(huesped, "El huésped tiene reservaciones asociadas. Se debe conservar su historial.");
        entidades.clear();
        assertThat(entidades.find(Reservacion.class, reservacion.getIdReservacion())).isNotNull();
        assertThat(entidades.find(Pago.class, pago.getIdPago())).isNotNull();
        assertThat(cuentas.findByCorreo(CORREO)).isPresent();
    }

    @Test
    void conservaUnaFichaConCuentaActiva() throws Exception {
        Huesped huesped = guardar("Ficha con sesión", CORREO);
        String hash = codificador.encode(CLAVE);
        cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, hash));
        comprobarEliminacionBloqueada(huesped,
                "El huésped tiene una cuenta activa. La baja requiere un procedimiento de cierre de cuenta.");
        entidades.clear();
        assertThat(cuentas.findByCorreo(CORREO).orElseThrow().getContrasenaHash()).isEqualTo(hash);
    }

    @ParameterizedTest
    @ValueSource(strings = {"editar", "eliminar"})
    void muestraUn404SiNoExisteLaFicha(String operacion) throws Exception {
        String ruta = "/huespedes/" + Long.MAX_VALUE + "/" + operacion;
        mvc.perform(get(ruta).with(recepcion()))
                .andExpect(status().isNotFound()).andExpect(content().string(containsString("Huésped no encontrado")));
        mvc.perform(ficha(ruta, "Ficha inexistente", CORREO, TELEFONO))
                .andExpect(status().isNotFound()).andExpect(content().string(containsString("Huésped no encontrado")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HUESPED", "ADMINISTRADOR"})
    void deniegaLecturaYEscrituraALosOtrosRoles(String rol) throws Exception {
        for (String ruta : List.of("/huespedes", "/huespedes/1/editar", "/huespedes/1/eliminar")) {
            mvc.perform(get(ruta).with(user("otro-rol@example.test").roles(rol)))
                    .andExpect(status().isForbidden());
            mvc.perform(post(ruta).with(user("otro-rol@example.test").roles(rol)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void exigeSesionYProtegeLosTresFormulariosConCsrf() throws Exception {
        Huesped huesped = guardar("Ficha protegida", CORREO);
        for (String ruta : List.of("/huespedes", ruta(huesped, "editar"), ruta(huesped, "eliminar"))) {
            mvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
            mvc.perform(post(ruta).with(csrf())).andExpect(redirectedUrl("/login"));
            mvc.perform(post(ruta).with(recepcion())).andExpect(status().isForbidden());
            mvc.perform(get(ruta).with(recepcion())).andExpect(content().string(containsString("name=\"_csrf\"")));
        }
        assertThat(huespedes.existsById(huesped.getIdHuesped())).isTrue();
    }

    private void comprobarEliminacionBloqueada(Huesped huesped, String motivo) throws Exception {
        String eliminar = ruta(huesped, "eliminar");
        mvc.perform(get(eliminar).with(recepcion()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(motivo)))
                .andExpect(content().string(not(containsString("Confirmar eliminación"))));
        mvc.perform(post(eliminar).with(recepcion()).with(csrf()))
                .andExpect(redirectedUrl(eliminar)).andExpect(flash().attribute("mensajeError", motivo));
        assertThat(huespedes.existsById(huesped.getIdHuesped())).isTrue();
    }

    private Huesped guardar(String nombre, String correo) {
        return huespedes.saveAndFlush(new Huesped(nombre, TELEFONO, correo));
    }

    private String ruta(Huesped huesped, String operacion) {
        return "/huespedes/" + huesped.getIdHuesped() + "/" + operacion;
    }

    private MockHttpServletRequestBuilder ficha(String ruta, String nombre, String correo, String telefono) {
        return post(ruta).with(recepcion()).with(csrf())
                .param("nombre", nombre).param("correo", correo).param("telefono", telefono);
    }

    private RequestPostProcessor recepcion() {
        return user("recepcion@example.test").roles("RECEPCIONISTA");
    }
}

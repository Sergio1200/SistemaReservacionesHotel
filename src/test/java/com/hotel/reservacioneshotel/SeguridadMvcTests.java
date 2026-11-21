package com.hotel.reservacioneshotel;

import java.util.UUID;

import com.hotel.reservacioneshotel.domain.usuarios.Administrador;
import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.domain.usuarios.Recepcionista;
import com.hotel.reservacioneshotel.domain.usuarios.Rol;
import com.hotel.reservacioneshotel.repository.CuentaRepository;
import com.hotel.reservacioneshotel.repository.EmpleadoRepository;
import com.hotel.reservacioneshotel.repository.HuespedRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=${hotel.test.profile:test}")
@AutoConfigureMockMvc
@Transactional
class SeguridadMvcTests {

    private static final String CORREO_HUESPED = "registro@example.test";
    private static final String CLAVE_PRUEBA = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CuentaRepository cuentas;
    @Autowired
    private HuespedRepository huespedes;
    @Autowired
    private EmpleadoRepository empleados;
    @Autowired
    private PasswordEncoder codificador;

    @Test
    void registraUnHuespedConCorreoNormalizadoYHashBCrypt() throws Exception {
        mvc.perform(registro("  REGISTRO@example.test  ", CLAVE_PRUEBA, CLAVE_PRUEBA).with(csrf())
                        .param("rol", "ADMINISTRADOR").param("idEmpleado", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrado"));

        Cuenta cuenta = cuentas.findByCorreo(CORREO_HUESPED).orElseThrow();
        assertThat(cuenta.getRol()).isEqualTo(Rol.HUESPED);
        assertThat(cuenta.getEmpleado()).isNull();
        assertThat(cuenta.getHuesped().getNombre()).isEqualTo("Huésped de prueba");
        assertThat(cuenta.getHuesped().getTelefono()).isEqualTo("+52 555 010 0000");
        assertThat(cuenta.getContrasenaHash()).startsWith("$2").isNotEqualTo(CLAVE_PRUEBA);
        assertThat(codificador.matches(CLAVE_PRUEBA, cuenta.getContrasenaHash())).isTrue();
    }

    @Test
    void rechazaCorreoDuplicadoAunqueCambieElUsoDeMayusculas() throws Exception {
        crearHuesped();
        mvc.perform(registro("REGISTRO@example.test", CLAVE_PRUEBA, CLAVE_PRUEBA).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ese correo electrónico ya está registrado.")));
        assertThat(cuentas.findByCorreo(CORREO_HUESPED)).isPresent();
    }

    @Test
    void unCorreoDeEmpleadoNoPuedeRegistrarseComoHuesped() throws Exception {
        String correo = "recepcion-prueba@example.test";
        var empleado = empleados.save(new Recepcionista("Prueba", "recepcion_prueba", codificador.encode(CLAVE_PRUEBA), true));
        cuentas.saveAndFlush(Cuenta.paraEmpleado(correo, empleado));

        mvc.perform(registro(correo, CLAVE_PRUEBA, CLAVE_PRUEBA).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ese correo electrónico ya está registrado.")));
        assertThat(huespedes.existeCorreoNormalizado(correo)).isFalse();
    }

    @Test
    void validaCamposObligatoriosYFormatoDelCorreo() throws Exception {
        mvc.perform(post("/registro").with(csrf()).param("correo", "no-es-correo")
                        .param("contrasena", CLAVE_PRUEBA).param("confirmacion", CLAVE_PRUEBA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribe tu nombre completo.")))
                .andExpect(content().string(containsString("Escribe un correo electrónico válido.")))
                .andExpect(content().string(containsString("Escribe tu teléfono.")));
    }

    @Test
    void validaCoincidenciaYLimiteDeBCryptSinDevolverContrasenas() throws Exception {
        String demasiadoLarga = "ñ".repeat(37);
        mvc.perform(registro(CORREO_HUESPED, demasiadoLarga, "confirmacion-diferente").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("al menos 8 caracteres")))
                .andExpect(content().string(containsString("Las contraseñas no coinciden.")))
                .andExpect(content().string(not(containsString(demasiadoLarga))))
                .andExpect(content().string(not(containsString("confirmacion-diferente"))));
        assertThat(cuentas.findByCorreo(CORREO_HUESPED)).isEmpty();

        mvc.perform(registro(CORREO_HUESPED, "corta", "corta").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("al menos 8 caracteres")));
    }

    @Test
    void autenticaPorCorreoYEliminaCredencialesDeLaSesion() throws Exception {
        crearHuesped();
        MockHttpSession sesion = iniciarSesion("REGISTRO@example.test", CORREO_HUESPED);
        SecurityContext contexto = (SecurityContext) sesion.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(contexto.getAuthentication().getCredentials()).isNull();
        assertThat(((UserDetails) contexto.getAuthentication().getPrincipal()).getPassword()).isNull();
        mvc.perform(get("/panel").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Huésped")))
                .andExpect(content().string(not(containsString("href=\"/habitaciones\""))));
    }

    @Test
    void autenticaEmpleadosConSuCorreoYVerificaElEstadoActivo() throws Exception {
        var activo = empleados.save(new Administrador("Prueba", "admin_prueba", codificador.encode(CLAVE_PRUEBA), true));
        var inactivo = empleados.save(new Recepcionista("Prueba", "recepcion_inactivo", activo.getContrasenaHash(), false));
        cuentas.save(Cuenta.paraEmpleado("admin@example.test", activo));
        cuentas.saveAndFlush(Cuenta.paraEmpleado("inactivo@example.test", inactivo));
        MockHttpSession sesion = iniciarSesion("admin@example.test", "admin@example.test");
        mvc.perform(get("/habitaciones").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/pagos").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(login("inactivo@example.test", CLAVE_PRUEBA).with(csrf()))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    @Test
    void rechazaContrasenaIncorrectaYAcotaLasContrasenasExcesivas() throws Exception {
        crearHuesped();
        mvc.perform(login(CORREO_HUESPED, "clave incorrecta").with(csrf()))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        mvc.perform(login(CORREO_HUESPED, "ñ".repeat(37)).with(csrf()))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        mvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Correo o contraseña incorrectos.")));
    }

    @Test
    void cierraSesionSoloConPostYTokenValido() throws Exception {
        crearHuesped();
        MockHttpSession sesion = iniciarSesion(CORREO_HUESPED, CORREO_HUESPED);
        mvc.perform(get("/logout").session(sesion));
        assertThat(sesion.isInvalid()).isFalse();
        mvc.perform(post("/logout").session(sesion)).andExpect(status().isForbidden());
        assertThat(sesion.isInvalid()).isFalse();
        mvc.perform(post("/logout").session(sesion).with(csrf()))
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
        assertThat(sesion.isInvalid()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"/panel", "/reservaciones", "/huespedes", "/habitaciones", "/tipos-habitacion", "/pagos",
            "/check-in-check-out", "/reservaciones/nueva"})
    void exigeAutenticacionParaAccesoDirecto(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
    }

    @ParameterizedTest
    @CsvSource({
            "HUESPED, /reservaciones/nueva", "HUESPED, /reservaciones",
            "RECEPCIONISTA, /huespedes", "RECEPCIONISTA, /reservaciones", "RECEPCIONISTA, /pagos",
            "RECEPCIONISTA, /check-in-check-out", "ADMINISTRADOR, /habitaciones", "ADMINISTRADOR, /tipos-habitacion"
    })
    void permiteLasRutasDelRol(String rol, String ruta) throws Exception {
        mvc.perform(get(ruta).with(user("rol@example.test").roles(rol))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({
            "HUESPED, /huespedes", "HUESPED, /habitaciones", "HUESPED, /tipos-habitacion",
            "HUESPED, /pagos", "HUESPED, /check-in-check-out",
            "RECEPCIONISTA, /habitaciones", "RECEPCIONISTA, /tipos-habitacion",
            "ADMINISTRADOR, /reservaciones", "ADMINISTRADOR, /check-in-check-out"
    })
    void deniegaLasRutasDeOtrosRolesConRespuesta403(String rol, String ruta) throws Exception {
        mvc.perform(get(ruta).with(user("rol@example.test").roles(rol)))
                .andExpect(status().isForbidden()).andExpect(forwardedUrl("/acceso-denegado"));
    }

    @Test
    void protegeRegistroYLoginConCsrfYRenderizaLosTokens() throws Exception {
        mvc.perform(registro(CORREO_HUESPED, CLAVE_PRUEBA, CLAVE_PRUEBA)).andExpect(status().isForbidden());
        assertThat(cuentas.findByCorreo(CORREO_HUESPED)).isEmpty();
        mvc.perform(login(CORREO_HUESPED, CLAVE_PRUEBA)).andExpect(status().isForbidden());
        for (String ruta : new String[]{"/login", "/registro"}) {
            mvc.perform(get(ruta)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("name=\"_csrf\"")));
        }
        mvc.perform(get("/acceso-denegado")).andExpect(status().isForbidden())
                .andExpect(content().string(containsString("<h1 id=\"titulo-acceso\">Acceso denegado</h1>")));
    }

    private void crearHuesped() {
        var huesped = huespedes.save(new Huesped("Huésped de prueba", "+52 555 010 0000", CORREO_HUESPED));
        cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, codificador.encode(CLAVE_PRUEBA)));
    }

    private MockHttpSession iniciarSesion(String correo, String correoEsperado) throws Exception {
        MvcResult respuesta = mvc.perform(login(correo, CLAVE_PRUEBA).with(csrf()))
                .andExpect(redirectedUrl("/panel"))
                .andExpect(authenticated().withUsername(correoEsperado))
                .andReturn();
        return (MockHttpSession) respuesta.getRequest().getSession(false);
    }

    private MockHttpServletRequestBuilder registro(String correo, String contrasena, String confirmacion) {
        return post("/registro").param("nombre", "Huésped de prueba").param("correo", correo)
                .param("telefono", "+52 555 010 0000").param("contrasena", contrasena).param("confirmacion", confirmacion);
    }

    private MockHttpServletRequestBuilder login(String correo, String contrasena) {
        return post("/login").param("correo", correo).param("password", contrasena);
    }
}

package com.hotel.reservacioneshotel;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.hotel.reservacioneshotel.controller.HabitacionController;
import com.hotel.reservacioneshotel.controller.DisponibilidadController;
import com.hotel.reservacioneshotel.controller.HuespedController;
import com.hotel.reservacioneshotel.controller.ReservacionController;
import com.hotel.reservacioneshotel.controller.TipoHabitacionController;
import com.hotel.reservacioneshotel.controller.VistasController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class VistasControllerTests {

    private static final List<String> RUTAS = List.of(
            "/", "/login", "/registro", "/panel", "/acerca-de", "/huespedes", "/habitaciones", "/tipos-habitacion",
            "/disponibilidad", "/reservaciones/nueva", "/reservaciones", "/pagos", "/check-in-check-out");

    private static final Pattern ENLACES = Pattern.compile("<a\\b[^>]*\\bhref=\"([^\"]+)\"");
    private static final Pattern PIE = Pattern.compile("<footer\\b[^>]*>(.*?)</footer>", Pattern.DOTALL);

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @CsvSource({
            "/login, login, Inicio de sesión",
            "/panel, panel, Panel principal",
            "/huespedes, huespedes, Gestión de huéspedes",
            "/habitaciones, habitaciones, Gestión de habitaciones",
            "/tipos-habitacion, tipos-habitacion, Tipos de habitación",
            "/disponibilidad, disponibilidad, Consultar disponibilidad",
            "/reservaciones/nueva, nueva-reservacion, Nueva reservación",
            "/reservaciones, reservaciones, Consultar reservaciones",
            "/pagos, pagos, Gestión de pagos",
            "/check-in-check-out, check-in-check-out, Check-in y check-out"
    })
    void renderizaLasVistasDelPortal(String ruta, String plantilla, String encabezado) throws Exception {
        Class<?> controlador = switch (ruta) {
            case "/huespedes" -> HuespedController.class;
            case "/habitaciones" -> HabitacionController.class;
            case "/tipos-habitacion" -> TipoHabitacionController.class;
            case "/disponibilidad" -> DisponibilidadController.class;
            case "/reservaciones", "/reservaciones/nueva" -> ReservacionController.class;
            default -> VistasController.class;
        };
        mvc.perform(peticionVista(ruta))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(controlador))
                .andExpect(view().name(plantilla))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString(">" + encabezado + "</h1>")));
    }

    @Test
    void renderizaAcercaDelProyectoConLogotiposYDatosAcademicos() throws Exception {
        mvc.perform(get("/acerca-de"))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(VistasController.class))
                .andExpect(view().name("acerca-de"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<h1>Acerca del proyecto</h1>")))
                .andExpect(content().string(containsString("Sistema de Gestión de Reservaciones de Hotel")))
                .andExpect(content().string(containsString("Sergio Garduño Arriaga")))
                .andExpect(content().string(containsString("ICO 30")))
                .andExpect(content().string(containsString("Paradigmas de Programación I")))
                .andExpect(content().string(containsString("Centro Universitario UAEMéx Atlacomulco")))
                .andExpect(content().string(containsString("2026B")))
                .andExpect(content().string(containsString("Tecnologías utilizadas")))
                .andExpect(content().string(containsString("src=\"/img/logo-uaemex.png\"")))
                .andExpect(content().string(containsString("src=\"/img/logo-uaem-atlacomulco.png\"")));
    }

    @Test
    void losEnlacesConducenAVistasExistentes() throws Exception {
        Set<String> rutasEnlazadas = new HashSet<>();
        for (String ruta : RUTAS) {
            String html = mvc.perform(peticionVista(ruta))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(html).contains("id=\"contenido\"");
            var pie = PIE.matcher(html);
            assertThat(pie.find()).as("Pie compartido de %s", ruta).isTrue();
            assertThat(pie.group(1)).contains(
                    "Desarrollado por Sergio Garduño Arriaga",
                    "ICO 30 · Paradigmas de Programación I",
                    "Centro Universitario UAEMéx Atlacomulco");
            ENLACES.matcher(html).results()
                    .map(resultado -> resultado.group(1))
                    .filter(enlace -> !enlace.equals("#contenido"))
                    .forEach(rutasEnlazadas::add);
        }
        assertThat(rutasEnlazadas).containsExactlyInAnyOrderElementsOf(RUTAS);
    }

    @Test
    void cargaLosRecursosLocales() throws Exception {
        mvc.perform(get("/css/estilos.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/css")));
        mvc.perform(get("/js/interfaz.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-menu-toggle")));
        mvc.perform(get("/img/vestibulo-hotel.webp"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("image/webp")));
        mvc.perform(get("/img/icono-hotel.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
        mvc.perform(get("/img/logo-uaemex.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
        mvc.perform(get("/img/logo-uaem-atlacomulco.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
    }

    @Test
    void losFormulariosNoAceptanEnvios() throws Exception {
        for (String ruta : List.of("/disponibilidad",
                "/pagos")) {
            mvc.perform(conAcceso(post(ruta), ruta).with(csrf())).andExpect(status().isMethodNotAllowed());
        }
    }

    private MockHttpServletRequestBuilder peticionVista(String ruta) {
        return conAcceso(get(ruta), ruta);
    }

    private MockHttpServletRequestBuilder conAcceso(MockHttpServletRequestBuilder peticion, String ruta) {
        return switch (ruta) {
            case "/panel", "/reservaciones", "/reservaciones/nueva" ->
                peticion.with(user("visitante@example.test").roles("HUESPED"));
            case "/huespedes", "/pagos", "/check-in-check-out" ->
                peticion.with(user("recepcion@example.test").roles("RECEPCIONISTA"));
            case "/habitaciones", "/tipos-habitacion" ->
                peticion.with(user("admin@example.test").roles("ADMINISTRADOR"));
            default -> peticion;
        };
    }
}

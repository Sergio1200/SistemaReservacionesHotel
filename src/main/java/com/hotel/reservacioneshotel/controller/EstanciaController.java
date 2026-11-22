package com.hotel.reservacioneshotel.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.service.EstanciaService;
import com.hotel.reservacioneshotel.service.EstanciaService.Conflicto;
import com.hotel.reservacioneshotel.service.EstanciaService.Movimiento;
import com.hotel.reservacioneshotel.service.ReservacionService.NoEncontrada;
import jakarta.persistence.LockTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/check-in-check-out")
public class EstanciaController {

    private static final Logger LOG = LoggerFactory.getLogger(EstanciaController.class);
    private static final String REDIRIGIR = "redirect:/check-in-check-out";

    private final EstanciaService estancias;
    private final Clock relojHotel;
    private final SesionVistaAdvice sesionVista;

    public EstanciaController(EstanciaService estancias, Clock relojHotel, SesionVistaAdvice sesionVista) {
        this.estancias = estancias;
        this.relojHotel = relojHotel;
        this.sesionVista = sesionVista;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") @Size(max = 120) String q, Model modelo) {
        LocalDate hoy = LocalDate.now(relojHotel);
        List<Movimiento> movimientos = estancias.listar(q, hoy);
        modelo.addAttribute("q", q.strip());
        modelo.addAttribute("hoy", hoy);
        modelo.addAttribute("zona", relojHotel.getZone().getId());
        modelo.addAttribute("grupos", List.of(
                grupo("llegadas", "Llegadas pendientes", EstadoReservacion.CONFIRMADA, movimientos),
                grupo("en-curso", "Estancias en curso", EstadoReservacion.EN_CURSO, movimientos),
                grupo("finalizadas", "Reservaciones finalizadas", EstadoReservacion.FINALIZADA, movimientos)));
        return "check-in-check-out";
    }

    @PostMapping("/{id}/check-in")
    public String registrarEntrada(@PathVariable @Positive Long id, RedirectAttributes redireccion) {
        estancias.registrarEntrada(id);
        redireccion.addFlashAttribute("mensajeExito", "Check-in registrado para la reservación #" + id + ".");
        redireccion.addAttribute("q", id);
        return REDIRIGIR;
    }

    @PostMapping("/{id}/check-out")
    public String registrarSalida(@PathVariable @Positive Long id, RedirectAttributes redireccion) {
        estancias.registrarSalida(id);
        redireccion.addFlashAttribute("mensajeExito", "Check-out registrado para la reservación #" + id + ".");
        redireccion.addAttribute("q", id);
        return REDIRIGIR;
    }

    @ExceptionHandler(NoEncontrada.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrada(HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo, "La reservación solicitada no existe.");
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String solicitudInvalida(HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo, "Introduce un número de reservación válido o una búsqueda de hasta 120 caracteres.");
    }

    @ExceptionHandler(Conflicto.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflicto(Conflicto exception, HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo, exception.getMessage());
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, LockTimeoutException.class,
            DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String bloqueo(HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo,
                "La reservación o su habitación cambió o está en uso. Consulta su estado y vuelve a intentarlo.");
    }

    @ExceptionHandler({TransactionException.class, DataAccessException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String almacenamiento(RuntimeException exception, HttpServletRequest solicitud, Model modelo) {
        LOG.warn("No fue posible consultar o actualizar la estancia.", exception);
        return error(solicitud, modelo, "No se pudo completar la operación. Consulta el estado de la estancia antes de reintentar.");
    }

    private String error(HttpServletRequest solicitud, Model modelo, String mensaje) {
        modelo.addAttribute("sesion", sesionVista.sesion(solicitud));
        modelo.addAttribute("mensajeError", mensaje);
        return "estancia-error";
    }

    private Grupo grupo(String id, String titulo, EstadoReservacion estado, List<Movimiento> movimientos) {
        return new Grupo(id, titulo, movimientos.stream().filter(movimiento -> movimiento.estado() == estado).toList());
    }

    public record Grupo(String id, String titulo, List<Movimiento> movimientos) {
    }
}

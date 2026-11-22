package com.hotel.reservacioneshotel.controller;

import java.time.LocalDate;

import com.hotel.reservacioneshotel.service.DisponibilidadService;
import com.hotel.reservacioneshotel.service.DisponibilidadService.PeriodoInvalido;
import com.hotel.reservacioneshotel.service.ReservacionService;
import com.hotel.reservacioneshotel.service.ReservacionService.Detalle;
import com.hotel.reservacioneshotel.service.ReservacionService.NoEncontrada;
import com.hotel.reservacioneshotel.service.ReservacionService.OperacionNoPermitida;
import com.hotel.reservacioneshotel.service.ReservacionService.Operador;
import jakarta.persistence.LockTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservaciones")
public class ReservacionController {

    private final ReservacionService reservaciones;
    private final DisponibilidadService disponibilidad;
    private final SesionVistaAdvice sesionVista;

    public ReservacionController(ReservacionService reservaciones, DisponibilidadService disponibilidad,
                                 SesionVistaAdvice sesionVista) {
        this.reservaciones = reservaciones;
        this.disponibilidad = disponibilidad;
        this.sesionVista = sesionVista;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String q, HttpServletRequest solicitud, Model modelo) {
        Operador operador = operador(solicitud);
        modelo.addAttribute("q", operador.recepcion() ? q.strip() : "");
        modelo.addAttribute("reservaciones", reservaciones.listar(operador, q));
        return "reservaciones";
    }

    @GetMapping("/nueva")
    public String nueva(@ModelAttribute("reservacionForm") ReservacionForm formulario, BindingResult errores,
                        HttpServletRequest solicitud, Model modelo) {
        return nuevaVista(operador(solicitud), modelo);
    }

    @PostMapping("/nueva")
    public String crear(@Valid @ModelAttribute("reservacionForm") ReservacionForm formulario, BindingResult errores,
                        HttpServletRequest solicitud, Model modelo, RedirectAttributes redireccion) {
        Operador operador = operador(solicitud);
        if (!errores.hasErrors()) {
            try {
                Long id = reservaciones.crear(operador, formulario.huespedId(), formulario.habitacionId(),
                        formulario.entrada(), formulario.salida(), formulario.personas());
                redireccion.addAttribute("id", id);
                redireccion.addFlashAttribute("mensajeExito", "La reservación se registró como pendiente.");
                return "redirect:/reservaciones/{id}";
            } catch (PeriodoInvalido exception) {
                errores.rejectValue(exception.getCampo(), "reservacion.periodo", exception.getMessage());
            } catch (OperacionNoPermitida exception) {
                rechazar(errores, exception);
            }
        }
        return nuevaVista(operador, modelo);
    }

    @GetMapping("/{id:[0-9]+}")
    public String detalle(@PathVariable Long id, HttpServletRequest solicitud, Model modelo) {
        modelo.addAttribute("reservacion", reservaciones.consultar(operador(solicitud), id));
        return "reservacion-detalle";
    }

    @GetMapping("/{id:[0-9]+}/editar")
    public String editar(@PathVariable Long id, HttpServletRequest solicitud, Model modelo) {
        Detalle detalle = reservaciones.consultar(operador(solicitud), id);
        modelo.addAttribute("reservacionForm", new ReservacionForm(
                detalle.habitacionId(), detalle.entrada(), detalle.salida(), detalle.personas(), null));
        return editarVista(detalle, modelo);
    }

    @PostMapping("/{id:[0-9]+}/editar")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("reservacionForm") ReservacionForm formulario,
                             BindingResult errores, HttpServletRequest solicitud, Model modelo, RedirectAttributes redireccion) {
        Operador operador = operador(solicitud);
        if (!errores.hasErrors()) {
            try {
                reservaciones.actualizar(operador, id, formulario.huespedId(), formulario.habitacionId(),
                        formulario.entrada(), formulario.salida(), formulario.personas());
                redireccion.addFlashAttribute("mensajeExito", "La reservación se actualizó.");
                return "redirect:/reservaciones/{id}";
            } catch (PeriodoInvalido exception) {
                errores.rejectValue(exception.getCampo(), "reservacion.periodo", exception.getMessage());
            } catch (OperacionNoPermitida exception) {
                rechazar(errores, exception);
            }
        }
        return editarVista(reservaciones.consultar(operador, id), modelo);
    }

    @GetMapping("/{id:[0-9]+}/cancelar")
    public String confirmarCancelacion(@PathVariable Long id, HttpServletRequest solicitud, Model modelo) {
        modelo.addAttribute("reservacion", reservaciones.consultar(operador(solicitud), id));
        return "reservacion-cancelar";
    }

    @PostMapping("/{id:[0-9]+}/cancelar")
    public String cancelar(@PathVariable Long id, HttpServletRequest solicitud, RedirectAttributes redireccion) {
        try {
            reservaciones.cancelar(operador(solicitud), id);
            redireccion.addFlashAttribute("mensajeExito", "La reservación se canceló.");
        } catch (OperacionNoPermitida exception) {
            redireccion.addFlashAttribute("mensajeError", exception.getMessage());
        }
        return "redirect:/reservaciones/{id}";
    }

    @ModelAttribute("hoy")
    public LocalDate hoy() {
        return disponibilidad.hoy();
    }

    @ExceptionHandler(NoEncontrada.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrada(HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo, "Reservación no encontrada", "La reservación solicitada no existe.");
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, LockTimeoutException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String bloqueo(HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo, "Operación en conflicto",
                "Otra operación está usando la habitación o la reservación. Vuelve a consultar e inténtalo de nuevo.");
    }

    private String nuevaVista(Operador operador, Model modelo) {
        modelo.addAttribute("habitaciones", reservaciones.opcionesHabitaciones());
        modelo.addAttribute("huespedes", reservaciones.opcionesHuespedes(operador));
        return "nueva-reservacion";
    }

    private String editarVista(Detalle detalle, Model modelo) {
        modelo.addAttribute("reservacion", detalle);
        modelo.addAttribute("habitaciones", reservaciones.opcionesHabitaciones());
        return "reservacion-editar";
    }

    private void rechazar(BindingResult errores, OperacionNoPermitida exception) {
        if (exception.getCampo() == null) {
            errores.reject("reservacion.operacion", exception.getMessage());
        } else {
            errores.rejectValue(exception.getCampo(), "reservacion.operacion", exception.getMessage());
        }
    }

    private String error(HttpServletRequest solicitud, Model modelo, String titulo, String mensaje) {
        modelo.addAttribute("sesion", sesionVista.sesion(solicitud));
        modelo.addAttribute("tituloError", titulo);
        modelo.addAttribute("mensajeError", mensaje);
        return "reservacion-error";
    }

    private Operador operador(HttpServletRequest solicitud) {
        return new Operador(solicitud.getUserPrincipal().getName(), solicitud.isUserInRole("RECEPCIONISTA"));
    }
}

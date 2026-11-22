package com.hotel.reservacioneshotel.controller;

import java.util.UUID;

import com.hotel.reservacioneshotel.domain.pagos.MetodoPago;
import com.hotel.reservacioneshotel.service.PagoService;
import com.hotel.reservacioneshotel.service.PagoService.DecisionPago;
import com.hotel.reservacioneshotel.service.PagoService.Historial;
import com.hotel.reservacioneshotel.service.PagoService.OperacionPagoNoPermitida;
import com.hotel.reservacioneshotel.service.PagoService.PagoNoEncontrado;
import com.hotel.reservacioneshotel.service.PagoService.Registro;
import com.hotel.reservacioneshotel.service.ReservacionService;
import com.hotel.reservacioneshotel.service.ReservacionService.NoEncontrada;
import com.hotel.reservacioneshotel.service.ReservacionService.Operador;
import jakarta.persistence.LockTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PagoController {

    private static final String HISTORIAL = "/reservaciones/{id:[0-9]+}/pagos";
    private static final String REDIRIGIR_HISTORIAL = "redirect:/reservaciones/{id}/pagos";

    private final PagoService pagos;
    private final ReservacionService reservaciones;
    private final SesionVistaAdvice sesionVista;

    public PagoController(PagoService pagos, ReservacionService reservaciones, SesionVistaAdvice sesionVista) {
        this.pagos = pagos;
        this.reservaciones = reservaciones;
        this.sesionVista = sesionVista;
    }

    @GetMapping("/pagos")
    public String listar(@RequestParam(defaultValue = "") String q, HttpServletRequest solicitud, Model modelo) {
        modelo.addAttribute("q", q.strip());
        modelo.addAttribute("reservaciones", reservaciones.listar(operador(solicitud), q));
        return "pagos";
    }

    @GetMapping(HISTORIAL)
    public String historial(@PathVariable Long id, HttpServletRequest solicitud, Model modelo) {
        modelo.addAttribute("pagoForm", new PagoForm(null, null, UUID.randomUUID()));
        return mostrarHistorial(pagos.consultar(operador(solicitud), id), modelo);
    }

    @PostMapping(HISTORIAL)
    public String registrar(@PathVariable Long id, @Valid @ModelAttribute("pagoForm") PagoForm formulario,
                            BindingResult errores, HttpServletRequest solicitud, Model modelo, RedirectAttributes redireccion) {
        Operador operador = operador(solicitud);
        if (!errores.hasErrors()) {
            try {
                Registro registro = pagos.registrar(operador, id, formulario.monto(), formulario.metodo(), formulario.intento());
                redireccion.addFlashAttribute("mensajeExito", registro.nuevo()
                        ? "El pago simulado #" + registro.id() + " quedó pendiente de procesamiento."
                        : "El intento ya estaba registrado. Consulta su estado en el historial.");
                return REDIRIGIR_HISTORIAL;
            } catch (OperacionPagoNoPermitida exception) {
                if (exception.getCampo() == null) {
                    errores.reject("pago.operacion", exception.getMessage());
                } else {
                    errores.rejectValue(exception.getCampo(), "pago.valor", exception.getMessage());
                }
            }
        }
        return mostrarHistorial(pagos.consultar(operador, id), modelo);
    }

    @PostMapping(HISTORIAL + "/{pagoId:[0-9]+}/procesar")
    public String procesar(@PathVariable Long id, @PathVariable Long pagoId,
                           HttpServletRequest solicitud, RedirectAttributes redireccion) {
        try {
            pagos.procesar(operador(solicitud), id, pagoId);
            redireccion.addFlashAttribute("mensajeExito", "El pago simulado está en procesamiento.");
        } catch (OperacionPagoNoPermitida exception) {
            redireccion.addFlashAttribute("mensajeError", exception.getMessage());
        }
        return REDIRIGIR_HISTORIAL;
    }

    @PostMapping(HISTORIAL + "/{pagoId:[0-9]+}/validar")
    public String validar(@PathVariable Long id, @PathVariable Long pagoId, @RequestParam DecisionPago decision,
                          HttpServletRequest solicitud, RedirectAttributes redireccion) {
        try {
            pagos.validar(operador(solicitud), id, pagoId, decision);
            redireccion.addFlashAttribute("mensajeExito", decision == DecisionPago.APROBAR
                    ? "El pago simulado fue aprobado." : "El pago simulado fue rechazado.");
        } catch (OperacionPagoNoPermitida exception) {
            redireccion.addFlashAttribute("mensajeError", exception.getMessage());
        }
        return REDIRIGIR_HISTORIAL;
    }

    @ExceptionHandler({NoEncontrada.class, PagoNoEncontrado.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo, "Registro no encontrado", "La reservación o el pago solicitado no existe.");
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, LockTimeoutException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String bloqueo(HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo, "Operación en conflicto",
                "Otra operación está usando la reservación. Consulta el estado antes de reintentar.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String integridad(HttpServletRequest solicitud, Model modelo) {
        return error(solicitud, modelo, "Operación en conflicto",
                "El pago no pudo guardarse. Consulta el historial antes de reintentar.");
    }

    private String mostrarHistorial(Historial historial, Model modelo) {
        modelo.addAttribute("historial", historial);
        modelo.addAttribute("metodos", MetodoPago.values());
        return "pagos-reservacion";
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

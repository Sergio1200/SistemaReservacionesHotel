package com.hotel.reservacioneshotel.controller;

import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.service.CorreoOcupadoException;
import com.hotel.reservacioneshotel.service.HuespedService;
import com.hotel.reservacioneshotel.service.HuespedService.Ficha;
import com.hotel.reservacioneshotel.service.HuespedService.HuespedNoEncontrado;
import com.hotel.reservacioneshotel.service.HuespedService.OperacionNoPermitida;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/huespedes")
public class HuespedController {

    private final HuespedService huespedes;
    private final SesionVistaAdvice sesionVista;

    public HuespedController(HuespedService huespedes, SesionVistaAdvice sesionVista) {
        this.huespedes = huespedes;
        this.sesionVista = sesionVista;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String q, Model modelo) {
        modelo.addAttribute("formulario", new RegistroHuespedForm("", "", ""));
        return directorio(q.strip(), modelo);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("formulario") RegistroHuespedForm formulario,
                        BindingResult errores, Model modelo, RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            return directorio("", modelo);
        }
        try {
            huespedes.crear(formulario.nombre(), formulario.telefono(), formulario.correo());
        } catch (CorreoOcupadoException exception) {
            rechazarCorreoDuplicado(errores);
            return directorio("", modelo);
        }
        redireccion.addFlashAttribute("mensajeExito", "Huésped registrado correctamente.");
        return "redirect:/huespedes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        Ficha ficha = huespedes.consultar(id);
        Huesped huesped = ficha.huesped();
        modelo.addAttribute("formulario",
                new RegistroHuespedForm(huesped.getNombre(), huesped.getCorreo(), huesped.getTelefono()));
        modelo.addAttribute("ficha", ficha);
        return "huesped-editar";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("formulario") RegistroHuespedForm formulario,
                             BindingResult errores, Model modelo, RedirectAttributes redireccion) {
        if (!errores.hasErrors()) {
            try {
                huespedes.actualizar(id, formulario.nombre(), formulario.telefono(), formulario.correo());
                redireccion.addFlashAttribute("mensajeExito", "Datos del huésped actualizados.");
                return "redirect:/huespedes";
            } catch (CorreoOcupadoException exception) {
                rechazarCorreoDuplicado(errores);
            } catch (OperacionNoPermitida exception) {
                errores.rejectValue("correo", "huesped.correo.protegido", exception.getMessage());
            }
        }
        modelo.addAttribute("ficha", huespedes.consultar(id));
        return "huesped-editar";
    }

    @GetMapping("/{id}/eliminar")
    public String confirmarEliminacion(@PathVariable Long id, Model modelo) {
        modelo.addAttribute("ficha", huespedes.consultar(id));
        return "huesped-eliminar";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redireccion) {
        try {
            huespedes.eliminar(id);
        } catch (OperacionNoPermitida exception) {
            redireccion.addFlashAttribute("mensajeError", exception.getMessage());
            return "redirect:/huespedes/{id}/eliminar";
        }
        redireccion.addFlashAttribute("mensajeExito", "Huésped eliminado correctamente.");
        return "redirect:/huespedes";
    }

    @ExceptionHandler(HuespedNoEncontrado.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String huespedNoEncontrado(HttpServletRequest solicitud, Model modelo) {
        modelo.addAttribute("sesion", sesionVista.sesion(solicitud));
        return "huesped-no-encontrado";
    }

    private String directorio(String q, Model modelo) {
        modelo.addAttribute("q", q);
        modelo.addAttribute("huespedes", huespedes.buscar(q));
        return "huespedes";
    }

    private void rechazarCorreoDuplicado(BindingResult errores) {
        errores.rejectValue("correo", "huesped.correo.ocupado", "Ese correo electrónico ya está registrado.");
    }
}

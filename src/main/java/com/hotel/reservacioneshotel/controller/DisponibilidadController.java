package com.hotel.reservacioneshotel.controller;

import java.time.LocalDate;

import com.hotel.reservacioneshotel.service.DisponibilidadService;
import com.hotel.reservacioneshotel.service.DisponibilidadService.PeriodoInvalido;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class DisponibilidadController {

    private final DisponibilidadService disponibilidad;

    public DisponibilidadController(DisponibilidadService disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @GetMapping(path = "/disponibilidad", params = {"!entrada", "!salida", "!huespedes"})
    public String formulario(Model modelo) {
        modelo.addAttribute("disponibilidadForm", new DisponibilidadForm(null, null, null));
        return "disponibilidad";
    }

    @GetMapping("/disponibilidad")
    public String consultar(@Valid @ModelAttribute("disponibilidadForm") DisponibilidadForm formulario,
                            BindingResult errores, Model modelo) {
        if (!errores.hasErrors()) {
            try {
                modelo.addAttribute("resultados", disponibilidad.consultar(
                        formulario.entrada(), formulario.salida(), formulario.huespedes()));
            } catch (PeriodoInvalido exception) {
                errores.rejectValue(exception.getCampo(), "disponibilidad.periodo", exception.getMessage());
            }
        }
        return "disponibilidad";
    }

    @ModelAttribute("hoy")
    public LocalDate hoy() {
        return disponibilidad.hoy();
    }
}

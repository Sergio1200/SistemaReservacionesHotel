package com.hotel.reservacioneshotel.controller;

import com.hotel.reservacioneshotel.service.CorreoOcupadoException;
import com.hotel.reservacioneshotel.service.PoliticaContrasenas;
import com.hotel.reservacioneshotel.service.RegistroHuespedService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistroController {

    private final RegistroHuespedService registro;

    public RegistroController(RegistroHuespedService registro) {
        this.registro = registro;
    }

    @GetMapping("/registro")
    public String formulario(Model modelo) {
        modelo.addAttribute("registro", new RegistroHuespedForm("", "", ""));
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroHuespedForm formulario,
                            BindingResult errores,
                            @RequestParam(defaultValue = "") String contrasena,
                            @RequestParam(defaultValue = "") String confirmacion) {
        if (!PoliticaContrasenas.valida(contrasena)) {
            errores.reject("registro.contrasena", PoliticaContrasenas.MENSAJE);
        }
        if (!contrasena.equals(confirmacion)) {
            errores.reject("registro.confirmacion", "Las contraseñas no coinciden.");
        }
        if (errores.hasErrors()) {
            return "registro";
        }
        try {
            registro.registrar(formulario.nombre(), formulario.correo(), formulario.telefono(), contrasena);
        } catch (CorreoOcupadoException exception) {
            errores.rejectValue("correo", "registro.correo.ocupado", "Ese correo electrónico ya está registrado.");
            return "registro";
        }
        return "redirect:/login?registrado";
    }
}

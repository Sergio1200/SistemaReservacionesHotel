package com.hotel.reservacioneshotel.controller;

import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.service.InventarioException;
import com.hotel.reservacioneshotel.service.TipoHabitacionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tipos-habitacion")
public class TipoHabitacionController {

    private final TipoHabitacionService tipos;

    public TipoHabitacionController(TipoHabitacionService tipos) {
        this.tipos = tipos;
    }

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("tipoHabitacionForm", new TipoHabitacionForm("", null, ""));
        return catalogo(modelo);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("tipoHabitacionForm") TipoHabitacionForm formulario,
                        BindingResult errores, Model modelo, RedirectAttributes redireccion) {
        if (!errores.hasErrors()) {
            try {
                tipos.crear(formulario.nombre(), formulario.capacidad(), formulario.descripcion());
                redireccion.addFlashAttribute("mensajeExito", "Tipo de habitación registrado correctamente.");
                return "redirect:/tipos-habitacion";
            } catch (InventarioException exception) {
                errores.rejectValue(exception.getCampo(), "inventario.valor", exception.getMessage());
            }
        }
        return catalogo(modelo);
    }

    @GetMapping("/{id:[0-9]+}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        TipoHabitacion tipo = tipos.consultar(id);
        modelo.addAttribute("tipoHabitacionForm", new TipoHabitacionForm(tipo.getNombre(), tipo.getCapacidad(), tipo.getDescripcion()));
        modelo.addAttribute("tipoHabitacion", tipo);
        return "tipo-habitacion-editar";
    }

    @PostMapping("/{id:[0-9]+}/editar")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("tipoHabitacionForm") TipoHabitacionForm formulario,
                             BindingResult errores, Model modelo, RedirectAttributes redireccion) {
        if (!errores.hasErrors()) {
            try {
                tipos.actualizar(id, formulario.nombre(), formulario.capacidad(), formulario.descripcion());
                redireccion.addFlashAttribute("mensajeExito", "Tipo de habitación actualizado correctamente.");
                return "redirect:/tipos-habitacion";
            } catch (InventarioException exception) {
                errores.rejectValue(exception.getCampo(), "inventario.valor", exception.getMessage());
            }
        }
        modelo.addAttribute("tipoHabitacion", tipos.consultar(id));
        return "tipo-habitacion-editar";
    }

    @GetMapping("/{id:[0-9]+}/eliminar")
    public String confirmar(@PathVariable Long id, Model modelo) {
        TipoHabitacionService.Confirmacion ficha = tipos.consultarEliminacion(id);
        modelo.addAttribute("titulo", "Eliminar tipo de habitación");
        modelo.addAttribute("detalle", ficha.tipo().getNombre());
        modelo.addAttribute("modulo", "/tipos-habitacion");
        modelo.addAttribute("id", id);
        modelo.addAttribute("motivo", ficha.motivoParaConservar());
        return "inventario-eliminar";
    }

    @PostMapping("/{id:[0-9]+}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redireccion) {
        try {
            tipos.eliminar(id);
        } catch (InventarioException exception) {
            redireccion.addFlashAttribute("mensajeError", exception.getMessage());
            return "redirect:/tipos-habitacion/{id}/eliminar";
        }
        redireccion.addFlashAttribute("mensajeExito", "Tipo de habitación eliminado correctamente.");
        return "redirect:/tipos-habitacion";
    }

    private String catalogo(Model modelo) {
        modelo.addAttribute("tipos", tipos.listar());
        return "tipos-habitacion";
    }
}

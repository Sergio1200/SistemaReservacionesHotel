package com.hotel.reservacioneshotel.controller;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.service.HabitacionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/habitaciones")
public class HabitacionController {

    private final HabitacionService habitaciones;
    private final TipoHabitacionService tipos;

    public HabitacionController(HabitacionService habitaciones, TipoHabitacionService tipos) {
        this.habitaciones = habitaciones;
        this.tipos = tipos;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Long tipo, Model modelo) {
        modelo.addAttribute("habitacionForm", new HabitacionForm("", null, null, null, EstadoHabitacion.DISPONIBLE));
        return inventario(q.strip(), tipo, modelo);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("habitacionForm") HabitacionForm formulario,
                        BindingResult errores, Model modelo, RedirectAttributes redireccion) {
        if (formulario.estado() == null && !errores.hasFieldErrors("estado")) {
            errores.rejectValue("estado", "inventario.estado.requerido", "Selecciona un estado operativo.");
        }
        if (!errores.hasErrors()) {
            try {
                habitaciones.crear(formulario.numero(), formulario.piso(), formulario.idTipo(),
                        formulario.precio(), formulario.estado());
                redireccion.addFlashAttribute("mensajeExito", "Habitación registrada correctamente.");
                return "redirect:/habitaciones";
            } catch (InventarioException exception) {
                mostrarError(errores, exception);
            }
        }
        return inventario("", null, modelo);
    }

    @GetMapping("/{id:[0-9]+}/editar")
    public String editar(@PathVariable Long id, Model modelo) {
        Habitacion habitacion = habitaciones.consultar(id);
        modelo.addAttribute("habitacionForm", new HabitacionForm(habitacion.getNumero(), habitacion.getPiso(),
                habitacion.getTipoHabitacion().getIdTipo(), habitacion.getPrecioPorNoche(), null));
        return vistaEdicion(habitacion, modelo);
    }

    @PostMapping("/{id:[0-9]+}/editar")
    public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("habitacionForm") HabitacionForm formulario,
                             BindingResult errores, Model modelo, RedirectAttributes redireccion) {
        if (!errores.hasErrors()) {
            try {
                habitaciones.actualizar(id, formulario.numero(), formulario.piso(), formulario.idTipo(), formulario.precio());
                redireccion.addFlashAttribute("mensajeExito", "Habitación actualizada correctamente.");
                return "redirect:/habitaciones";
            } catch (InventarioException exception) {
                mostrarError(errores, exception);
            }
        }
        return vistaEdicion(habitaciones.consultar(id), modelo);
    }

    @GetMapping("/{id:[0-9]+}/eliminar")
    public String confirmar(@PathVariable Long id, Model modelo) {
        HabitacionService.Confirmacion ficha = habitaciones.consultarEliminacion(id);
        modelo.addAttribute("titulo", "Eliminar habitación");
        modelo.addAttribute("detalle", "Habitación " + ficha.habitacion().getNumero());
        modelo.addAttribute("modulo", "/habitaciones");
        modelo.addAttribute("id", id);
        modelo.addAttribute("motivo", ficha.motivoParaConservar());
        return "inventario-eliminar";
    }

    @PostMapping("/{id:[0-9]+}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redireccion) {
        try {
            habitaciones.eliminar(id);
        } catch (InventarioException exception) {
            redireccion.addFlashAttribute("mensajeError", exception.getMessage());
            return "redirect:/habitaciones/{id}/eliminar";
        }
        redireccion.addFlashAttribute("mensajeExito", "Habitación eliminada correctamente.");
        return "redirect:/habitaciones";
    }

    private String inventario(String q, Long tipo, Model modelo) {
        modelo.addAttribute("habitaciones", habitaciones.buscar(q, tipo));
        modelo.addAttribute("tipos", tipos.listar());
        modelo.addAttribute("estados", EstadoHabitacion.values());
        modelo.addAttribute("q", q);
        modelo.addAttribute("tipoFiltro", tipo);
        return "habitaciones";
    }

    private String vistaEdicion(Habitacion habitacion, Model modelo) {
        modelo.addAttribute("habitacion", habitacion);
        modelo.addAttribute("tipos", tipos.listar());
        return "habitacion-editar";
    }

    private void mostrarError(BindingResult errores, InventarioException exception) {
        if (exception.getCampo() == null) {
            errores.reject("inventario.operacion", exception.getMessage());
        } else {
            errores.rejectValue(exception.getCampo(), "inventario.valor", exception.getMessage());
        }
    }
}

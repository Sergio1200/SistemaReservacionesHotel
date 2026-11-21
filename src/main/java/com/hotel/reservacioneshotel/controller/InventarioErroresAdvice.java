package com.hotel.reservacioneshotel.controller;

import com.hotel.reservacioneshotel.service.InventarioException.NoEncontrado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = {HabitacionController.class, TipoHabitacionController.class})
public class InventarioErroresAdvice {

    private final SesionVistaAdvice sesionVista;

    public InventarioErroresAdvice(SesionVistaAdvice sesionVista) {
        this.sesionVista = sesionVista;
    }

    @ExceptionHandler(NoEncontrado.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(NoEncontrado exception, HttpServletRequest solicitud, Model modelo) {
        modelo.addAttribute("sesion", sesionVista.sesion(solicitud));
        modelo.addAttribute("mensaje", exception.getMessage());
        return "inventario-no-encontrado";
    }
}

package com.hotel.reservacioneshotel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VistasController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/panel")
    public String panel() {
        return "panel";
    }

    @GetMapping("/acerca-de")
    public String acercaDe() {
        return "acerca-de";
    }

    @GetMapping("/huespedes")
    public String huespedes() {
        return "huespedes";
    }

    @GetMapping("/habitaciones")
    public String habitaciones() {
        return "habitaciones";
    }

    @GetMapping("/tipos-habitacion")
    public String tiposHabitacion() {
        return "tipos-habitacion";
    }

    @GetMapping("/disponibilidad")
    public String disponibilidad() {
        return "disponibilidad";
    }

    @GetMapping("/reservaciones/nueva")
    public String nuevaReservacion() {
        return "nueva-reservacion";
    }

    @GetMapping("/reservaciones")
    public String reservaciones() {
        return "reservaciones";
    }

    @GetMapping("/pagos")
    public String pagos() {
        return "pagos";
    }

    @GetMapping("/check-in-check-out")
    public String checkInCheckOut() {
        return "check-in-check-out";
    }
}

package com.hotel.reservacioneshotel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class VistasController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @RequestMapping("/acceso-denegado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesoDenegado() {
        return "acceso-denegado";
    }

    @GetMapping("/panel")
    public String panel() {
        return "panel";
    }

    @GetMapping("/acerca-de")
    public String acercaDe() {
        return "acerca-de";
    }

    @GetMapping("/check-in-check-out")
    public String checkInCheckOut() {
        return "check-in-check-out";
    }
}

package com.hotel.reservacioneshotel.service;

public class InventarioException extends RuntimeException {

    private final String campo;

    public InventarioException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public InventarioException(String mensaje) {
        this(null, mensaje);
    }

    public String getCampo() {
        return campo;
    }

    public static class NoEncontrado extends RuntimeException {
        public NoEncontrado(String mensaje) {
            super(mensaje);
        }
    }
}

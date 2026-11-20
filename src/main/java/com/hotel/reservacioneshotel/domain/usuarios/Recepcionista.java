package com.hotel.reservacioneshotel.domain.usuarios;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("RECEPCIONISTA")
public class Recepcionista extends Empleado {

    protected Recepcionista() {
    }

    public Recepcionista(String nombre, String usuario, String contrasenaHash, boolean activo) {
        super(nombre, usuario, contrasenaHash, activo);
    }
}

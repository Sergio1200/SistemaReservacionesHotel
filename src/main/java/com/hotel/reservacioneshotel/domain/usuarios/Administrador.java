package com.hotel.reservacioneshotel.domain.usuarios;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMINISTRADOR")
public class Administrador extends Empleado {

    protected Administrador() {
    }

    public Administrador(String nombre, String usuario, String contrasenaHash, boolean activo) {
        super(nombre, usuario, contrasenaHash, activo);
    }

    @Override
    public Rol getRol() {
        return Rol.ADMINISTRADOR;
    }
}

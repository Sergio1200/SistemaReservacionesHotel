package com.hotel.reservacioneshotel.domain.usuarios;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    private Long idCuenta;

    @Column(nullable = false, length = 254)
    private String correo;

    @Column(name = "contrasena_hash", length = 255)
    private String contrasenaHash;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_huesped")
    private Huesped huesped;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;

    protected Cuenta() {
    }

    public static Cuenta paraHuesped(Huesped huesped, String contrasenaHash) {
        Cuenta cuenta = new Cuenta();
        cuenta.correo = normalizarCorreo(huesped.getCorreo());
        cuenta.huesped = huesped;
        cuenta.contrasenaHash = contrasenaHash;
        return cuenta;
    }

    public static Cuenta paraEmpleado(String correo, Empleado empleado) {
        Cuenta cuenta = new Cuenta();
        cuenta.correo = normalizarCorreo(correo);
        cuenta.empleado = empleado;
        return cuenta;
    }

    public static String normalizarCorreo(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    public String getCorreo() {
        return correo;
    }

    public Huesped getHuesped() {
        return huesped;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public Rol getRol() {
        return empleado == null ? Rol.HUESPED : empleado.getRol();
    }

    public String getContrasenaHash() {
        return empleado == null ? contrasenaHash : empleado.getContrasenaHash();
    }

    public boolean isActiva() {
        return empleado == null ? contrasenaHash != null : empleado.isActivo();
    }
}

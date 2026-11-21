package com.hotel.reservacioneshotel.config;

import com.hotel.reservacioneshotel.domain.usuarios.Administrador;
import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.domain.usuarios.Empleado;
import com.hotel.reservacioneshotel.domain.usuarios.Recepcionista;
import com.hotel.reservacioneshotel.domain.usuarios.Rol;
import com.hotel.reservacioneshotel.repository.CuentaRepository;
import com.hotel.reservacioneshotel.repository.EmpleadoRepository;
import com.hotel.reservacioneshotel.repository.HuespedRepository;
import com.hotel.reservacioneshotel.service.PoliticaContrasenas;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class CuentasDemo implements ApplicationRunner {

    private final CuentaRepository cuentas;
    private final EmpleadoRepository empleados;
    private final HuespedRepository huespedes;
    private final PasswordEncoder codificador;
    private final Environment entorno;

    public CuentasDemo(CuentaRepository cuentas, EmpleadoRepository empleados, HuespedRepository huespedes,
                       PasswordEncoder codificador, Environment entorno) {
        this.cuentas = cuentas;
        this.empleados = empleados;
        this.huespedes = huespedes;
        this.codificador = codificador;
        this.entorno = entorno;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        crearSiFalta("administrador@example.test", "demo_admin", Rol.ADMINISTRADOR, "HOTEL_DEMO_ADMIN_PASSWORD");
        crearSiFalta("recepcionista@example.test", "demo_recepcion", Rol.RECEPCIONISTA,
                "HOTEL_DEMO_RECEPCION_PASSWORD");
    }

    private void crearSiFalta(String correo, String usuario, Rol rol, String variable) {
        var cuentaAnterior = cuentas.findByCorreo(correo);
        if (cuentaAnterior.isPresent()) {
            Cuenta cuenta = cuentaAnterior.get();
            if (cuenta.getRol() != rol || !usuario.equals(cuenta.getEmpleado().getUsuario())) {
                throw new IllegalStateException("El correo de demostración ya pertenece a otra cuenta: " + correo);
            }
            return;
        }
        if (huespedes.existeCorreoNormalizado(correo) || empleados.existsByUsuario(usuario)) {
            throw new IllegalStateException("Los datos de demostración coinciden con un registro previo: " + correo);
        }
        String contrasena = entorno.getProperty(variable);
        if (!PoliticaContrasenas.valida(contrasena)) {
            throw new IllegalStateException("Configura " + variable + ". " + PoliticaContrasenas.MENSAJE);
        }
        String hash = codificador.encode(contrasena);
        String nombre = rol.getEtiqueta() + " de demostración";
        Empleado empleado = rol == Rol.ADMINISTRADOR
                ? new Administrador(nombre, usuario, hash, true)
                : new Recepcionista(nombre, usuario, hash, true);
        cuentas.save(Cuenta.paraEmpleado(correo, empleados.save(empleado)));
    }
}

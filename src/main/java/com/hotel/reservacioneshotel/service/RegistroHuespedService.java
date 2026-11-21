package com.hotel.reservacioneshotel.service;

import java.util.Locale;

import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.CuentaRepository;
import com.hotel.reservacioneshotel.repository.HuespedRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroHuespedService {

    private final CuentaRepository cuentas;
    private final HuespedRepository huespedes;
    private final PasswordEncoder codificador;

    public RegistroHuespedService(CuentaRepository cuentas, HuespedRepository huespedes, PasswordEncoder codificador) {
        this.cuentas = cuentas;
        this.huespedes = huespedes;
        this.codificador = codificador;
    }

    @Transactional
    public void registrar(String nombre, String correo, String telefono, String contrasena) {
        String normalizado = Cuenta.normalizarCorreo(correo);
        if (cuentas.existsByCorreo(normalizado) || huespedes.existeCorreoNormalizado(normalizado)) {
            throw new CorreoOcupadoException();
        }
        String hash = codificador.encode(contrasena);
        try {
            Huesped huesped = huespedes.save(new Huesped(nombre, telefono, normalizado));
            cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, hash));
        } catch (DataIntegrityViolationException exception) {
            // El índice único también protege si otra transacción registra el correo entre la consulta y el INSERT.
            if (correspondeACorreoDuplicado(exception)) {
                throw new CorreoOcupadoException();
            }
            throw exception;
        }
    }

    private boolean correspondeACorreoDuplicado(DataIntegrityViolationException exception) {
        for (Throwable causa = exception.getCause(); causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException restriccion && restriccion.getConstraintName() != null) {
                String nombre = restriccion.getConstraintName().toLowerCase(Locale.ROOT);
                if (nombre.contains("uk_huespedes_correo") || nombre.contains("uk_cuentas_correo")) {
                    return true;
                }
            }
        }
        return false;
    }
}

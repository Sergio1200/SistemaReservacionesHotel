package com.hotel.reservacioneshotel.service;

import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.repository.CuentaRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentasUserDetailsService implements UserDetailsService {

    private final CuentaRepository cuentas;

    public CuentasUserDetailsService(CuentaRepository cuentas) {
        this.cuentas = cuentas;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        String normalizado = Cuenta.normalizarCorreo(correo);
        if (normalizado.isEmpty() || normalizado.length() > 254) {
            throw new UsernameNotFoundException("Cuenta no disponible.");
        }
        Cuenta cuenta = cuentas.findByCorreo(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Cuenta no disponible."));
        String hash = cuenta.getContrasenaHash();
        if (hash == null) {
            throw new UsernameNotFoundException("Cuenta no disponible.");
        }
        return User.withUsername(cuenta.getCorreo())
                .password(hash)
                .roles(cuenta.getRol().name())
                .disabled(!cuenta.isActiva())
                .build();
    }
}

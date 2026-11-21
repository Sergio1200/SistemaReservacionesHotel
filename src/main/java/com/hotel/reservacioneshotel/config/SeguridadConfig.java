package com.hotel.reservacioneshotel.config;

import com.hotel.reservacioneshotel.service.PoliticaContrasenas;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static com.hotel.reservacioneshotel.domain.usuarios.Rol.ADMINISTRADOR;
import static com.hotel.reservacioneshotel.domain.usuarios.Rol.HUESPED;
import static com.hotel.reservacioneshotel.domain.usuarios.Rol.RECEPCIONISTA;

@Configuration
public class SeguridadConfig {

    @Bean
    PasswordEncoder codificadorContrasenas() {
        return new BCryptPasswordEncoder(12) {
            @Override
            protected boolean matchesNonNull(String contrasena, String hash) {
                return PoliticaContrasenas.dentroDelLimite(contrasena) && super.matchesNonNull(contrasena, hash);
            }
        };
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain seguridad(HttpSecurity http, UserDetailsService cuentas, PasswordEncoder codificador)
            throws Exception {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(cuentas);
        proveedor.setPasswordEncoder(codificador);

        return http
                .authenticationProvider(proveedor)
                .authorizeHttpRequests(reglas -> reglas
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/login", "/registro", "/acerca-de", "/disponibilidad",
                                "/acceso-denegado", "/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/panel").hasAnyRole(HUESPED.name(), RECEPCIONISTA.name(), ADMINISTRADOR.name())
                        .requestMatchers("/reservaciones", "/reservaciones/nueva")
                                .hasAnyRole(HUESPED.name(), RECEPCIONISTA.name())
                        .requestMatchers("/huespedes", "/pagos", "/check-in-check-out").hasRole(RECEPCIONISTA.name())
                        .requestMatchers("/habitaciones", "/tipos-habitacion").hasRole(ADMINISTRADOR.name())
                        .anyRequest().denyAll())
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .usernameParameter("correo")
                        .defaultSuccessUrl("/panel"))
                .logout(cierre -> cierre
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(errores -> errores.accessDeniedPage("/acceso-denegado"))
                .build();
    }
}

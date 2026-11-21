package com.hotel.reservacioneshotel.service;

import java.util.List;
import java.util.Optional;

import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import com.hotel.reservacioneshotel.repository.CuentaRepository;
import com.hotel.reservacioneshotel.repository.HuespedRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HuespedService {

    private static final String CORREO_PROTEGIDO = "El correo pertenece a una cuenta activa. Su cambio requiere verificar la nueva dirección.";
    private static final String CON_RESERVACIONES = "El huésped tiene reservaciones asociadas. Se debe conservar su historial.";
    private static final String CON_CUENTA_ACTIVA = "El huésped tiene una cuenta activa. La baja requiere un procedimiento de cierre de cuenta.";

    private final HuespedRepository huespedes;
    private final CuentaRepository cuentas;
    private final ReservacionRepository reservaciones;

    public HuespedService(HuespedRepository huespedes, CuentaRepository cuentas, ReservacionRepository reservaciones) {
        this.huespedes = huespedes;
        this.cuentas = cuentas;
        this.reservaciones = reservaciones;
    }

    @Transactional(readOnly = true)
    public List<Huesped> buscar(String texto) {
        return texto.isEmpty() ? huespedes.findAll(Sort.by("nombre", "idHuesped")) : huespedes.buscar(texto);
    }

    @Transactional(readOnly = true)
    public Ficha consultar(Long id) {
        Huesped huesped = huespedes.findById(id).orElseThrow(HuespedNoEncontrado::new);
        boolean cuentaActiva = cuentaActiva(cuentas.findByHuesped_IdHuesped(id));
        return new Ficha(huesped, cuentaActiva, motivoParaConservar(id, cuentaActiva));
    }

    @Transactional
    public void crear(String nombre, String telefono, String correo) {
        String normalizado = Cuenta.normalizarCorreo(correo);
        if (cuentas.existsByCorreo(normalizado) || huespedes.existeCorreoNormalizado(normalizado)) {
            throw new CorreoOcupadoException();
        }
        try {
            Huesped huesped = huespedes.save(new Huesped(nombre, telefono, normalizado));
            cuentas.saveAndFlush(Cuenta.paraHuesped(huesped, null));
        } catch (DataIntegrityViolationException exception) {
            throw traducirDuplicado(exception);
        }
    }

    @Transactional
    public void actualizar(Long id, String nombre, String telefono, String correo) {
        Huesped huesped = huespedes.buscarParaActualizar(id).orElseThrow(HuespedNoEncontrado::new);
        Optional<Cuenta> cuenta = cuentas.buscarDeHuespedParaActualizar(id);
        String normalizado = Cuenta.normalizarCorreo(correo);
        boolean cambiaCorreo = !Cuenta.normalizarCorreo(huesped.getCorreo()).equals(normalizado);
        if (cambiaCorreo) {
            if (cuentaActiva(cuenta)) {
                throw new OperacionNoPermitida(CORREO_PROTEGIDO);
            }
            if (huespedes.existeCorreoDeOtroHuesped(normalizado, id) || perteneceAOtraCuenta(normalizado, id)) {
                throw new CorreoOcupadoException();
            }
        }
        try {
            huesped.actualizarDatos(nombre, telefono, cambiaCorreo ? normalizado : huesped.getCorreo());
            if (cambiaCorreo) {
                if (cuenta.isPresent()) {
                    cuenta.get().actualizarCorreoDelHuesped();
                } else {
                    cuentas.save(Cuenta.paraHuesped(huesped, null));
                }
            }
            huespedes.flush();
        } catch (DataIntegrityViolationException exception) {
            throw traducirDuplicado(exception);
        }
    }

    @Transactional
    public void eliminar(Long id) {
        Huesped huesped = huespedes.buscarParaActualizar(id).orElseThrow(HuespedNoEncontrado::new);
        Optional<Cuenta> cuenta = cuentas.buscarDeHuespedParaActualizar(id);
        String motivo = motivoParaConservar(id, cuentaActiva(cuenta));
        if (motivo != null) {
            throw new OperacionNoPermitida(motivo);
        }
        try {
            if (cuenta.isPresent()) {
                cuentas.delete(cuenta.get());
                cuentas.flush();
            }
            huespedes.delete(huesped);
            huespedes.flush();
        } catch (DataIntegrityViolationException exception) {
            if (RestriccionesPersistencia.coincide(exception, "fk_reservaciones_huesped")) {
                throw new OperacionNoPermitida(CON_RESERVACIONES);
            }
            if (RestriccionesPersistencia.coincide(exception, "fk_cuentas_huesped")) {
                throw new OperacionNoPermitida("El huésped conserva una cuenta vinculada. Vuelve a consultar su ficha.");
            }
            throw exception;
        }
    }

    private boolean perteneceAOtraCuenta(String correo, Long id) {
        return cuentas.findByCorreo(correo)
                .filter(cuenta -> cuenta.getHuesped() == null || !id.equals(cuenta.getHuesped().getIdHuesped()))
                .isPresent();
    }

    private boolean cuentaActiva(Optional<Cuenta> cuenta) {
        return cuenta.map(Cuenta::isActiva).orElse(false);
    }

    private String motivoParaConservar(Long id, boolean cuentaActiva) {
        if (reservaciones.existsByHuesped_IdHuesped(id)) {
            return CON_RESERVACIONES;
        }
        return cuentaActiva ? CON_CUENTA_ACTIVA : null;
    }

    private RuntimeException traducirDuplicado(DataIntegrityViolationException exception) {
        return RestriccionesPersistencia.correoDuplicado(exception) ? new CorreoOcupadoException() : exception;
    }

    public record Ficha(Huesped huesped, boolean correoProtegido, String motivoParaConservar) {
    }

    public static class HuespedNoEncontrado extends RuntimeException {
    }

    public static class OperacionNoPermitida extends RuntimeException {
        public OperacionNoPermitida(String mensaje) {
            super(mensaje);
        }
    }
}

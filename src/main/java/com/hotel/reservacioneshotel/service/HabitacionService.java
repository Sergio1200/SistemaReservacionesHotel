package com.hotel.reservacioneshotel.service;

import java.math.BigDecimal;
import java.util.List;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.repository.HabitacionRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import com.hotel.reservacioneshotel.repository.TipoHabitacionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HabitacionService {

    private static final String CON_RESERVACIONES =
            "La habitación tiene reservaciones asociadas. Se debe conservar su historial.";

    private final HabitacionRepository habitaciones;
    private final TipoHabitacionRepository tipos;
    private final ReservacionRepository reservaciones;

    public HabitacionService(HabitacionRepository habitaciones, TipoHabitacionRepository tipos,
                             ReservacionRepository reservaciones) {
        this.habitaciones = habitaciones;
        this.tipos = tipos;
        this.reservaciones = reservaciones;
    }

    @Transactional(readOnly = true)
    public List<Habitacion> buscar(String numero, Long idTipo) {
        return habitaciones.buscar(numero, idTipo);
    }

    @Transactional(readOnly = true)
    public Habitacion consultar(Long id) {
        return habitaciones.findOneByIdHabitacion(id).orElseThrow(this::noEncontrada);
    }

    @Transactional(readOnly = true)
    public Confirmacion consultarEliminacion(Long id) {
        Habitacion habitacion = consultar(id);
        return new Confirmacion(habitacion, motivoParaConservar(id));
    }

    @Transactional
    public void crear(String numero, int piso, Long idTipo, BigDecimal precio, EstadoHabitacion estado) {
        TipoHabitacion tipo = buscarTipo(idTipo);
        comprobarNumeroDisponible(numero, null);
        guardar(new Habitacion(numero, piso, estado, precio, tipo));
    }

    @Transactional
    public void actualizar(Long id, String numero, int piso, Long idTipo, BigDecimal precio) {
        Habitacion habitacion = habitaciones.buscarParaActualizar(id).orElseThrow(this::noEncontrada);
        TipoHabitacion tipo = buscarTipo(idTipo);
        boolean cambiaTipo = !habitacion.getTipoHabitacion().getIdTipo().equals(idTipo);
        boolean cambiaIdentificacion = cambiaTipo || habitacion.getPiso() != piso || !habitacion.getNumero().equals(numero);
        if (cambiaIdentificacion && reservaciones.existsByHabitacion_IdHabitacionAndEstado(id, EstadoReservacion.EN_CURSO)) {
            throw new InventarioException(
                    "La habitación tiene una estancia en curso. En este momento solo se puede modificar la tarifa.");
        }
        if (cambiaTipo && reservaciones.existsByHabitacion_IdHabitacionAndEstadoInAndNumeroHuespedesGreaterThan(
                id, EstadoReservacion.QUE_REQUIEREN_CUPO, tipo.getCapacidad())) {
            throw new InventarioException("idTipo", "La capacidad del tipo es insuficiente para una reservación vigente.");
        }
        comprobarNumeroDisponible(numero, id);
        habitacion.actualizarDatos(numero, piso, precio, tipo);
        guardar(habitacion);
    }

    @Transactional
    public void eliminar(Long id) {
        Habitacion habitacion = habitaciones.buscarParaActualizar(id).orElseThrow(this::noEncontrada);
        String motivo = motivoParaConservar(id);
        if (motivo != null) {
            throw new InventarioException(motivo);
        }
        try {
            habitaciones.delete(habitacion);
            habitaciones.flush();
        } catch (DataIntegrityViolationException exception) {
            if (RestriccionesPersistencia.coincide(exception, "fk_reservaciones_habitacion")) {
                throw new InventarioException(CON_RESERVACIONES);
            }
            throw exception;
        }
    }

    private TipoHabitacion buscarTipo(Long idTipo) {
        return tipos.buscarParaActualizar(idTipo).orElseThrow(
                () -> new InventarioException("idTipo", "El tipo de habitación seleccionado ya no existe."));
    }

    private void comprobarNumeroDisponible(String numero, Long id) {
        if (habitaciones.existeOtroNumero(numero, id)) {
            throw numeroOcupado();
        }
    }

    private void guardar(Habitacion habitacion) {
        try {
            habitaciones.saveAndFlush(habitacion);
        } catch (DataIntegrityViolationException exception) {
            if (RestriccionesPersistencia.coincide(exception, "uk_habitaciones_numero")) {
                throw numeroOcupado();
            }
            throw exception;
        }
    }

    private String motivoParaConservar(Long id) {
        return reservaciones.existsByHabitacion_IdHabitacion(id) ? CON_RESERVACIONES : null;
    }

    private InventarioException numeroOcupado() {
        return new InventarioException("numero", "Ese número de habitación ya está registrado.");
    }

    private InventarioException.NoEncontrado noEncontrada() {
        return new InventarioException.NoEncontrado("La habitación solicitada ya no está en el inventario.");
    }

    public record Confirmacion(Habitacion habitacion, String motivoParaConservar) {
    }
}

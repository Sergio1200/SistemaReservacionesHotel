package com.hotel.reservacioneshotel.service;

import java.util.List;

import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.repository.HabitacionRepository;
import com.hotel.reservacioneshotel.repository.ReservacionRepository;
import com.hotel.reservacioneshotel.repository.TipoHabitacionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoHabitacionService {

    private static final String CON_HABITACIONES =
            "El tipo tiene habitaciones asociadas. Reasigna las habitaciones antes de eliminarlo.";

    private final TipoHabitacionRepository tipos;
    private final HabitacionRepository habitaciones;
    private final ReservacionRepository reservaciones;

    public TipoHabitacionService(TipoHabitacionRepository tipos, HabitacionRepository habitaciones,
                                 ReservacionRepository reservaciones) {
        this.tipos = tipos;
        this.habitaciones = habitaciones;
        this.reservaciones = reservaciones;
    }

    @Transactional(readOnly = true)
    public List<TipoHabitacion> listar() {
        return tipos.findAll(Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public TipoHabitacion consultar(Long id) {
        return tipos.findById(id).orElseThrow(this::noEncontrado);
    }

    @Transactional(readOnly = true)
    public Confirmacion consultarEliminacion(Long id) {
        TipoHabitacion tipo = consultar(id);
        return new Confirmacion(tipo, motivoParaConservar(id));
    }

    @Transactional
    public void crear(String nombre, int capacidad, String descripcion) {
        comprobarNombreDisponible(nombre, null);
        guardar(new TipoHabitacion(nombre, capacidad, descripcion));
    }

    @Transactional
    public void actualizar(Long id, String nombre, int capacidad, String descripcion) {
        TipoHabitacion tipo = tipos.buscarParaActualizar(id).orElseThrow(this::noEncontrado);
        comprobarNombreDisponible(nombre, id);
        if (capacidad < tipo.getCapacidad()
                && reservaciones.existsByHabitacion_TipoHabitacion_IdTipoAndEstadoInAndNumeroHuespedesGreaterThan(
                        id, EstadoReservacion.QUE_REQUIEREN_CUPO, capacidad)) {
            throw new InventarioException("capacidad", "La nueva capacidad es insuficiente para una reservación vigente.");
        }
        tipo.actualizarDatos(nombre, capacidad, descripcion);
        guardar(tipo);
    }

    @Transactional
    public void eliminar(Long id) {
        TipoHabitacion tipo = tipos.buscarParaActualizar(id).orElseThrow(this::noEncontrado);
        String motivo = motivoParaConservar(id);
        if (motivo != null) {
            throw new InventarioException(motivo);
        }
        try {
            tipos.delete(tipo);
            tipos.flush();
        } catch (DataIntegrityViolationException exception) {
            if (RestriccionesPersistencia.coincide(exception, "fk_habitaciones_tipo")) {
                throw new InventarioException(CON_HABITACIONES);
            }
            throw exception;
        }
    }

    private void comprobarNombreDisponible(String nombre, Long id) {
        if (tipos.existeOtroNombre(nombre, id)) {
            throw nombreOcupado();
        }
    }

    private void guardar(TipoHabitacion tipo) {
        try {
            tipos.saveAndFlush(tipo);
        } catch (DataIntegrityViolationException exception) {
            if (RestriccionesPersistencia.coincide(exception, "uk_tipos_habitacion_nombre")) {
                throw nombreOcupado();
            }
            throw exception;
        }
    }

    private String motivoParaConservar(Long id) {
        return habitaciones.existsByTipoHabitacion_IdTipo(id) ? CON_HABITACIONES : null;
    }

    private InventarioException nombreOcupado() {
        return new InventarioException("nombre", "Ese nombre de tipo de habitación ya está registrado.");
    }

    private InventarioException.NoEncontrado noEncontrado() {
        return new InventarioException.NoEncontrado("El tipo solicitado ya no está en el catálogo.");
    }

    public record Confirmacion(TipoHabitacion tipo, String motivoParaConservar) {
    }
}

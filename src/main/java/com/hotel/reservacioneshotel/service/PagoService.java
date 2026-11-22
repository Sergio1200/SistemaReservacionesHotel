package com.hotel.reservacioneshotel.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.pagos.MetodoPago;
import com.hotel.reservacioneshotel.domain.pagos.Pago;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import com.hotel.reservacioneshotel.repository.PagoRepository;
import com.hotel.reservacioneshotel.service.ReservacionService.Operador;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoService {

    private static final BigDecimal CERO = new BigDecimal("0.00");

    private final PagoRepository pagos;
    private final ReservacionService reservaciones;

    public PagoService(PagoRepository pagos, ReservacionService reservaciones) {
        this.pagos = pagos;
        this.reservaciones = reservaciones;
    }

    @Transactional(readOnly = true)
    public Historial consultar(Operador operador, Long id) {
        Reservacion reservacion = reservaciones.buscarReservacion(operador, id);
        List<Pago> historial = pagos.findByReservacion_IdReservacionOrderByFechaDescIdPagoDesc(id);
        BigDecimal pagado = CERO;
        BigDecimal enTramite = CERO;
        for (Pago pago : historial) {
            if (pago.getEstado() == EstadoPago.PAGADO) {
                pagado = pagado.add(pago.getMonto());
            } else if (EstadoPago.EN_TRAMITE.contains(pago.getEstado())) {
                enTramite = enTramite.add(pago.getMonto());
            }
        }
        Saldo saldo = calcularSaldo(reservacion, pagado, enTramite);
        return new Historial(id, reservacion.getEstado(), saldo, historial.stream().map(this::detalle).toList());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Registro registrar(Operador operador, Long id, BigDecimal monto, MetodoPago metodo, UUID intento) {
        Reservacion reservacion = reservaciones.bloquearReservacion(operador, id);
        var previo = pagos.findByReservacion_IdReservacionAndClaveIntento(id, intento.toString());
        if (previo.isPresent()) {
            Pago pago = previo.get();
            if (pago.getMonto().compareTo(monto) != 0 || !pago.getMetodoPago().equals(metodo.name())) {
                throw new OperacionPagoNoPermitida("El formulario ya se envió con otros datos. Abre un nuevo intento.");
            }
            return new Registro(pago.getIdPago(), false);
        }
        exigirReservacionActiva(reservacion);
        if (monto.signum() <= 0 || monto.scale() > 2) {
            throw new OperacionPagoNoPermitida("monto", "El monto debe ser mayor que cero y tener hasta dos decimales.");
        }
        if (monto.compareTo(saldoActual(reservacion).disponible()) > 0) {
            throw new OperacionPagoNoPermitida("monto", "El monto supera el saldo disponible para un nuevo intento.");
        }
        Pago pago = pagos.save(Pago.simulado(reservacion, LocalDateTime.now(), monto.setScale(2), metodo, intento));
        return new Registro(pago.getIdPago(), true);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void procesar(Operador operador, Long id, Long pagoId) {
        exigirRecepcion(operador);
        Reservacion reservacion = reservaciones.bloquearReservacion(operador, id);
        Pago pago = bloquearPago(id, pagoId, EstadoPago.PENDIENTE);
        exigirReservacionActiva(reservacion);
        pago.procesar();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void validar(Operador operador, Long id, Long pagoId, DecisionPago decision) {
        exigirRecepcion(operador);
        Reservacion reservacion = reservaciones.bloquearReservacion(operador, id);
        Pago pago = bloquearPago(id, pagoId, EstadoPago.PROCESANDO);
        exigirReservacionActiva(reservacion);
        boolean aprobar = decision == DecisionPago.APROBAR;
        if (aprobar) {
            Saldo saldo = saldoActual(reservacion);
            if (pago.getMonto().compareTo(saldo.pendiente()) > 0 || saldo.enTramite().compareTo(saldo.pendiente()) > 0) {
                throw new OperacionPagoNoPermitida("La aprobación supera el saldo. Rechaza el intento o revisa los otros pagos.");
            }
            reservacion.actualizarConfirmacion(saldo.pagado().add(pago.getMonto()));
        }
        pago.finalizar(aprobar);
    }

    private Pago bloquearPago(Long reservacionId, Long pagoId, EstadoPago esperado) {
        Pago pago = pagos.buscarParaValidar(reservacionId, pagoId).orElseThrow(PagoNoEncontrado::new);
        if (pago.getEstado() != esperado) {
            throw new OperacionPagoNoPermitida("El pago ya cambió de estado. Consulta el historial antes de volver a actuar.");
        }
        return pago;
    }

    private void exigirRecepcion(Operador operador) {
        if (!operador.recepcion()) {
            throw new AccessDeniedException("Solo recepción puede validar pagos.");
        }
    }

    private void exigirReservacionActiva(Reservacion reservacion) {
        if (!reservacion.getEstado().admitePagos()) {
            throw new OperacionPagoNoPermitida("La reservación ya no admite pagos.");
        }
    }

    private Saldo saldoActual(Reservacion reservacion) {
        Long id = reservacion.getIdReservacion();
        return calcularSaldo(reservacion, pagos.sumarMontos(id, List.of(EstadoPago.PAGADO)), pagos.sumarMontos(id, EstadoPago.EN_TRAMITE));
    }

    private Saldo calcularSaldo(Reservacion reservacion, BigDecimal pagado, BigDecimal enTramite) {
        BigDecimal total = reservacion.getCostoTotal();
        BigDecimal pendiente = total.subtract(pagado).max(CERO);
        BigDecimal disponible = reservacion.getEstado().admitePagos() ? pendiente.subtract(enTramite).max(CERO) : CERO;
        return new Saldo(total, pagado, pendiente, enTramite, disponible, pagado.compareTo(total) > 0);
    }

    private DetallePago detalle(Pago pago) {
        String metodo = pago.getMetodoPago();
        for (MetodoPago permitido : MetodoPago.values()) {
            if (permitido.name().equals(metodo)) {
                metodo = permitido.getEtiqueta();
                break;
            }
        }
        return new DetallePago(pago.getIdPago(), pago.getFecha(), pago.getMonto(), metodo, pago.getEstado());
    }

    public record Historial(Long id, EstadoReservacion estado, Saldo saldo, List<DetallePago> pagos) {
        public boolean permiteRegistrar() {
            return estado.admitePagos() && saldo.disponible.signum() > 0;
        }

        public boolean requiereConciliacion() {
            return estado == EstadoReservacion.CANCELADA && (saldo.pagado.signum() > 0 || saldo.enTramite.signum() > 0);
        }
    }

    public record Saldo(BigDecimal total, BigDecimal pagado, BigDecimal pendiente,
                        BigDecimal enTramite, BigDecimal disponible, boolean excedido) {
    }

    public record DetallePago(Long id, LocalDateTime fecha, BigDecimal monto, String metodo, EstadoPago estado) {
    }

    public record Registro(Long id, boolean nuevo) {
    }

    public enum DecisionPago {
        APROBAR,
        RECHAZAR
    }

    public static class PagoNoEncontrado extends RuntimeException {
    }

    public static class OperacionPagoNoPermitida extends RuntimeException {
        private final String campo;

        public OperacionPagoNoPermitida(String mensaje) {
            this(null, mensaje);
        }

        public OperacionPagoNoPermitida(String campo, String mensaje) {
            super(mensaje);
            this.campo = campo;
        }

        public String getCampo() {
            return campo;
        }
    }
}

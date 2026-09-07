package com.project.advanced.project.domain.valueobject

public enum class EstadoReserva {
    CANCELADA(false),
    CONFIRMADA(true),
    EN_CURSO(true),
    FINALIZADA(false),
    NO_SHOW(false),
    PENDIENTE(true);

    private final boolean esActiva;

    EstadoReserva(boolean esActiva) {
      this.esActiva = esActiva;
    }

    public boolean esActiva() {
      return esActiva;
    }

    public boolean esTerminal() {
      return !esActiva;
    }
}

package com.project.advanced.project.domain.valueobject

enum class EstadoReserva(
    private val esActiva: Boolean,
) {
    CANCELADA(false),
    CONFIRMADA(true),
    EN_CURSO(true),
    FINALIZADA(false),
    NO_SHOW(false),
    PENDIENTE(true),
    ;

    fun esActiva(): Boolean = esActiva

    fun esTerminal(): Boolean = !esActiva
}

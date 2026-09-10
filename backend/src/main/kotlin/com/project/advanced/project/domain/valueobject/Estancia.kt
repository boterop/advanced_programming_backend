package com.project.advanced.project.domain.valueobject

import com.project.advanced.project.domain.exception.ReglaDominioException
import java.time.LocalDate

data class Estancia(
    val entrada: LocalDate,
    val salida: LocalDate,
) {
    init {
        if (entrada.isAfter(salida)) {
            throw ReglaDominioException("La entrada no puede ser posterior a la salida")
        }
        if (entrada.isEqual(salida)) {
            throw ReglaDominioException("La entrada no puede ser igual a la salida")
        }
    }

    fun solapa(
        entrada: LocalDate,
        salida: LocalDate,
    ): Boolean {
        if (entrada.isAfter(this.entrada) && entrada.isBefore(this.salida)) {
            return true
        }

        if (salida.isAfter(this.entrada) && salida.isBefore(this.salida)) {
            return true
        }

        if (this.entrada.isAfter(entrada) && this.entrada.isBefore(salida)) {
            return true
        }

        if (this.salida.isAfter(entrada) && this.salida.isBefore(salida)) {
            return true
        }

        return false
    }

    fun solapa(estancia: Estancia): Boolean = solapa(estancia.entrada, estancia.salida)
}

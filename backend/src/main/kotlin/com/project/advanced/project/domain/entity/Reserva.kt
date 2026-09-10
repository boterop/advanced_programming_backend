package com.project.advanced.project.domain.entity

import com.project.advanced.project.domain.entity.Apartamento
import com.project.advanced.project.domain.entity.Estancia
import com.project.advanced.project.domain.entity.Ocupante
import com.project.advanced.project.domain.exception.ReglaDominioException
import com.project.advanced.project.domain.valueobject.CanalOrigen
import com.project.advanced.project.domain.valueobject.CodigoReserva
import com.project.advanced.project.domain.valueobject.EstadoReserva
import com.project.advanced.project.domain.valueobject.FechaCreacion

class Reserva private constructor(
    val codigo: CodigoReserva,
    val canalOrigen: CanalOrigen,
    val fechaCreacion: FechaCreacion,
    val titular: Ocupante,
    var estado: EstadoReserva,
    var apartamento: Apartamento,
    var estancia: Estancia,
    var ocupantes: MutableList<Ocupante>,
) {
    init {
        estado = EstadoReserva.PENDIENTE
    }

    fun crear(
        codigo: CodigoReserva,
        canalOrigen: CanalOrigen,
        fechaCreacion: fechaCreacion,
        titular: Ocupante,
        apartamento: Apartamento,
        estancia: Estancia,
        ocupantes: MutableList<Ocupante>,
    ): Reserva {
        val message = "La reserva debe tener "
        if (this.codigo == null) throw ReglaDominioException("$message un codigo")
        if (this.canalOrigen == null) throw ReglaDominioException("$message un canal de origen")
        if (this.fechaCreacion == null) throw ReglaDominioException("$message una fecha de creacion")
        if (this.titular == null) throw ReglaDominioException("$message un titular")
        if (this.apartamento == null) throw ReglaDominioException("$message un apartamento")
        if (this.estancia == null) throw ReglaDominioException("$message una estancia")
        if (this.ocupantes == null) throw ReglaDominioException("$message ocupantes")

        return Reserva(codigo, canalOrigen, fechaCreacion, titular, EstadoReserva.PENDIENTE, apartamento, estancia, ocupantes)
    }

    override fun hashCode(): Int = codigo.hashCode()

    override fun equals(other: Any?): Boolean = other is Reserva && other.codigo == this.codigo
}

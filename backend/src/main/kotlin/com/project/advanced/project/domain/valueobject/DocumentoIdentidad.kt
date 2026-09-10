package com.project.advanced.project.domain.valueobject

import com.project.advanced.project.domain.exception.ReglaDominioException

data class DocumentoIdentidad(
    val numero: String,
) {
    init {
        if (numero.isBlank() || numero.isEmpty()) {
            throw ReglaDominioException("El Documento Identidad no es valido")
        }
    }
}

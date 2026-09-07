package com.project.advanced.project.domain.valueobject

public record DocumentoIdentidad(String numero: String) {
  public DocumentoIdentidad{
    if(numero == null || numero.isBlank() || numero.isEmpty){
      throw ReglaDominioException("El Documento Identidad no es valido")
    }

    numero = numero.trim()
  }
}

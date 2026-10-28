package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record UsuarioSeloRequestDTO(
        LocalDate dataColeta,
        int idUsuario,
        int idSelo
) {
}

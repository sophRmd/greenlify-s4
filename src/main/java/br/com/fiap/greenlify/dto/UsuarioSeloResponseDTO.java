package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.models.Selo;
import java.time.LocalDate;

public record UsuarioSeloResponseDTO(
        int idUsuarioSelo,
        LocalDate dataColeta,
        UsuarioResumoDTO usuario,
        Selo selo
) {
}

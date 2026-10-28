package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.models.Grupo;
import java.time.LocalDate;

public record UsuarioCadastroResponseDTO(
        int idUsuario,
        String nome,
        String email,
        int nivelAcesso,
        StatusContaEnum statusConta,
        LocalDate dataNascimento,
        Grupo grupo
) {
}

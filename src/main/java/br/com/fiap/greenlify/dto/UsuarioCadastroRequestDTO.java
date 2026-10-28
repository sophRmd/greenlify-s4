package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import java.time.LocalDate;

public record UsuarioCadastroRequestDTO(
        String nome,
        String email,
        String senha,
        int nivelAcesso,
        StatusContaEnum statusConta,
        String cpf,
        LocalDate dataNascimento,
        Integer idGrupo
) {
}

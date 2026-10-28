package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record GrupoResponseDTO(
        int idGrupo,
        String nome,
        LocalDate dataCriacao,
        int qtdUsuarios
) {
}

package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record GrupoRequestDTO(
        String nome,
        LocalDate dataCriacao,
        int qtdUsuarios
) {
}

package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record MissaoMensalResumidaRequestDTO(
        String nome,
        String descricao,
        int meta,
        LocalDate dataFim
) {
}

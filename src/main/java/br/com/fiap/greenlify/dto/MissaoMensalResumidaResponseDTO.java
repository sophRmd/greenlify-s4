package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record MissaoMensalResumidaResponseDTO(
        int idMissao,
        String nome,
        String descricao,
        int meta,
        LocalDate dataFim
) {
}

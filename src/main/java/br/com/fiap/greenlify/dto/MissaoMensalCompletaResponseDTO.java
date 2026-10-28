package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.CategoriaEnum;
import java.time.LocalDate;

public record MissaoMensalCompletaResponseDTO(
        int idMissao,
        String nome,
        String descricao,
        String comoComprovar,
        String requisitos,
        CategoriaEnum categoria,
        int meta,
        LocalDate dataInicio,
        LocalDate dataFim
) {
}

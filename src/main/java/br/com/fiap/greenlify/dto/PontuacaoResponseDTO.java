package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.DificuldadeEnum;
import br.com.fiap.greenlify.enums.ImpactoEnum;

public record PontuacaoResponseDTO(
        int idPontuacao,
        ImpactoEnum impacto,
        DificuldadeEnum dificuldade,
        int valorBase
) {
}
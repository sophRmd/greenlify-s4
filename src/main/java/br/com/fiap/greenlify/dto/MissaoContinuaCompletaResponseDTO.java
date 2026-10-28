package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.CategoriaEnum;
import br.com.fiap.greenlify.enums.TipoMetricaEnum;
import br.com.fiap.greenlify.models.Pontuacao;

public record MissaoContinuaCompletaResponseDTO(
        int idMissao,
        String nome,
        String descricao,
        String comoComprovar,
        String requisitos,
        CategoriaEnum categoria,
        int limiteRealizacoes,
        TipoMetricaEnum tipoMetrica,
        int valorMetrica,
        Pontuacao pontuacao
) {
}

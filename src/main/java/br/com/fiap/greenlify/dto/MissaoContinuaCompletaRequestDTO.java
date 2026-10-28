package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.CategoriaEnum;
import br.com.fiap.greenlify.enums.TipoMetricaEnum;

public record MissaoContinuaCompletaRequestDTO(
        String nome,
        String descricao,
        String comoComprovar,
        String requisitos,
        CategoriaEnum categoria,
        int limiteRealizacoes,
        TipoMetricaEnum tipoMetrica,
        int valorMetrica,
        int idPontuacao
) {
}

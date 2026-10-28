package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.TipoMetricaEnum;

public record MissaoContinuaResumidaRequestDTO(
        String nome,
        TipoMetricaEnum tipoMetrica,
        int valorMetrica,
        int idPontuacao
) {
}

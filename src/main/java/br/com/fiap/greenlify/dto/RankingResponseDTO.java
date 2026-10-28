package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.StatusRankingEnum;
import br.com.fiap.greenlify.enums.TipoRankingEnum;
import java.time.LocalDate;

public record RankingResponseDTO(
        int idRanking,
        LocalDate dataInicio,
        LocalDate dataFim,
        TipoRankingEnum tipo,
        StatusRankingEnum status
) {
}

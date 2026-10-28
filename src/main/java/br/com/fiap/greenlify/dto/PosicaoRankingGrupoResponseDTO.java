package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.models.Grupo;
import br.com.fiap.greenlify.models.Ranking;

public record PosicaoRankingGrupoResponseDTO(
        int idPosicao,
        int posicao,
        int pontuacao,
        Grupo grupo,
        Ranking ranking
) {
}

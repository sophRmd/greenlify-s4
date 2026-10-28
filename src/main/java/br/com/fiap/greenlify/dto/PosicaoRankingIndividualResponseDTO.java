package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.models.Ranking;

public record PosicaoRankingIndividualResponseDTO(
        int idPosicao,
        int posicao,
        int pontuacao,
        UsuarioResumoDTO usuario,
        Ranking ranking
) {
}

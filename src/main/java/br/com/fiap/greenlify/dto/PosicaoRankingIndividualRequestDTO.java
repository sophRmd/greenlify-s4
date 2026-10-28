package br.com.fiap.greenlify.dto;

public record PosicaoRankingIndividualRequestDTO(
        int posicao,
        int pontuacao,
        int idUsuario,
        int idRanking
) {
}

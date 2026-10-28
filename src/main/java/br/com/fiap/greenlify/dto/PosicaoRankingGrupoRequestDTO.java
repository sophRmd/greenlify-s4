package br.com.fiap.greenlify.dto;

public record PosicaoRankingGrupoRequestDTO(
        int posicao,
        int pontuacao,
        int idGrupo,
        int idRanking
) {
}

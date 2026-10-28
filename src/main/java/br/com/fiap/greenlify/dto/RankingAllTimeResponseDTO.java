package br.com.fiap.greenlify.dto;

public record RankingAllTimeResponseDTO(
        int posicao,
        UsuarioResumoDTO usuario,
        int totalMissoes
) {
}

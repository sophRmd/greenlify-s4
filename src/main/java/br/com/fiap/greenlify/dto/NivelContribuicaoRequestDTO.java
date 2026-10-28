package br.com.fiap.greenlify.dto;

public record NivelContribuicaoRequestDTO(
        int nivel,
        String nome,
        int xpMinimo,
        int xpMaximo
) {
}

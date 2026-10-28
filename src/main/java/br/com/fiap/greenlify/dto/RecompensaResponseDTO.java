package br.com.fiap.greenlify.dto;

public record RecompensaResponseDTO(
        int idRecompensa,
        int bonusPercentual,
        int bonusPontos,
        String descricao
) {
}

package br.com.fiap.greenlify.dto;

public record RecompensaRequestDTO(
        int idRecompensa,
        int bonusPercentual,
        int bonusPontos,
        String descricao
) {
}
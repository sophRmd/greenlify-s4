package br.com.fiap.greenlify.dto;

public record PerfilSustentavelRequestDTO(
        int idPerfilSustentavel,
        int xpSustentavel,
        int totalMissoesConcluidas,
        double totalAguaEconomizada,
        double totalEnergiaEconomizada,
        double totalCarbonoEconomizado,
        int idUsuario,
        int idNivel
) {
}

package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.models.NivelContribuicao;

public record PerfilSustentavelResponseDTO(
        int idPerfilSustentavel,
        int xpSustentavel,
        int totalMissoesConcluidas,
        double totalAguaEconomizada,
        double totalEnergiaEconomizada,
        double totalCarbonoEconomizado,
        UsuarioResumoDTO usuario,
        NivelContribuicao nivel
) {
}

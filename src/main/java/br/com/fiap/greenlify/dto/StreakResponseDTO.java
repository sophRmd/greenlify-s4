package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record StreakResponseDTO(
        int idStreak,
        int maiorSequencia,
        int sequenciaAtual,
        LocalDate ultimaAtividade,
        LocalDate primeiraAtividade,
        UsuarioResumoDTO usuario
) {
}

package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record ResgateRequestDTO(
        LocalDate dataConcessao,
        int idUsuario,
        int idRecompensa
) {
}

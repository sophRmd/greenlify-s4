package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record FingerprintRequestDTO(
        String hashVideo,
        LocalDate dataGeracao,
        int idUpload
) {
}

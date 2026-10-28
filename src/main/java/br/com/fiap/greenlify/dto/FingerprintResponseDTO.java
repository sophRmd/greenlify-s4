package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record FingerprintResponseDTO(
        int idFingerprint,
        String hashVideo,
        LocalDate dataGeracao,
        int idUpload
) {
}

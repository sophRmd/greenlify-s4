package br.com.fiap.greenlify.dto;

public record FingerprintFrameResponseDTO(
        int idFingerprintFrame,
        int numeroFrame,
        int tempoSegundo,
        String pHash,
        String embedding,
        int idFingerprint
) {
}

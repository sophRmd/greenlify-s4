package br.com.fiap.greenlify.dto;

public record FingerprintFrameRequestDTO(
        int numeroFrame,
        int tempoSegundo,
        String pHash,
        String embedding,
        int idFingerprint
) {
}

package br.com.fiap.greenlify.dto;

import java.util.List;

public record UploadRequestDTO(
        String arquivo,
        double tamanhoMB,
        int idMissao,
        int idUsuario,
        String hashVideo,
        List<FingerprintFrameRequestDTO> frames
) {
}

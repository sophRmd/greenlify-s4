package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.models.Missao;
import java.time.LocalDate;

public record UploadResponseDTO(
        int idUpload,
        StatusUploadEnum status,
        LocalDate dataEnvio,
        LocalDate dataAnalise,
        String arquivo,
        String analise,
        int confiancaDuplicidade,
        Missao missao,
        UsuarioResumoDTO usuario
) {
}

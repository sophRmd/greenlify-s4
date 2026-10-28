package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.UploadRequestDTO;
import br.com.fiap.greenlify.dto.UploadResponseDTO;
import br.com.fiap.greenlify.models.Upload;

public class UploadMapper {
    public static Upload dtoToEntity (UploadRequestDTO dto){
        Upload upload = new Upload();
        // status, datas, parecer e confiança são definidos pelo UploadService, nunca pelo cliente.
        upload.setArquivo(dto.arquivo());
        upload.setMissao(ReferenciaMapper.missao(dto.idMissao()));
        upload.setUsuario(ReferenciaMapper.usuarioComum(dto.idUsuario()));
        return upload;
    }

    public static UploadResponseDTO toDto (Upload upload){
        UploadResponseDTO dto = new UploadResponseDTO(
                upload.getIdUpload(),
                upload.getStatus(),
                upload.getDataEnvio(),
                upload.getDataAnalise(),
                upload.getArquivo(),
                upload.getAnalise(),
                upload.getConfiancaDuplicidade(),
                upload.getMissao(),
                UsuarioResumoMapper.toDto(upload.getUsuario())
        );
        return dto;
    }
}
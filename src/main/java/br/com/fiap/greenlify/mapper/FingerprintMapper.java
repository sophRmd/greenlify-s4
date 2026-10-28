package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.FingerprintRequestDTO;
import br.com.fiap.greenlify.dto.FingerprintResponseDTO;
import br.com.fiap.greenlify.models.Fingerprint;

public class FingerprintMapper {
    public static Fingerprint dtoToEntity (FingerprintRequestDTO dto){
        Fingerprint fingerprint = new Fingerprint();
        fingerprint.setHashVideo(dto.hashVideo());
        fingerprint.setDataGeracao(dto.dataGeracao());
        fingerprint.setUpload(ReferenciaMapper.upload(dto.idUpload()));
        return fingerprint;
    }

    public static FingerprintResponseDTO toDto (Fingerprint fingerprint){
        return new FingerprintResponseDTO(
                fingerprint.getIdFingerprint(),
                fingerprint.getHashVideo(),
                fingerprint.getDataGeracao(),
                fingerprint.getUpload() != null ? fingerprint.getUpload().getIdUpload() : 0
        );
    }
}

package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.FingerprintFrameRequestDTO;
import br.com.fiap.greenlify.dto.FingerprintFrameResponseDTO;
import br.com.fiap.greenlify.models.FingerprintFrame;

public class FingerprintFrameMapper {
    public static FingerprintFrame dtoToEntity (FingerprintFrameRequestDTO dto){
        FingerprintFrame frame = new FingerprintFrame();
        frame.setNumeroFrame(dto.numeroFrame());
        frame.setTempoSegundo(dto.tempoSegundo());
        frame.setpHash(dto.pHash());
        frame.setEmbedding(dto.embedding());
        frame.setFingerprint(ReferenciaMapper.fingerprint(dto.idFingerprint()));
        return frame;
    }

    public static FingerprintFrameResponseDTO toDto (FingerprintFrame frame){
        return new FingerprintFrameResponseDTO(
                frame.getIdFingerprintFrame(),
                frame.getNumeroFrame(),
                frame.getTempoSegundo(),
                frame.getpHash(),
                frame.getEmbedding(),
                frame.getFingerprint() != null ? frame.getFingerprint().getIdFingerprint() : 0
        );
    }
}

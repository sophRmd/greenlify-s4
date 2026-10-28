package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.MissaoContinuaResumidaResponseDTO;
import br.com.fiap.greenlify.dto.MissaoMensalResumidaRequestDTO;
import br.com.fiap.greenlify.dto.MissaoMensalResumidaResponseDTO;
import br.com.fiap.greenlify.models.MissaoMensal;

public class MissaoMensalResumidaMapper {
    public static MissaoMensal dtoToEntity (MissaoMensalResumidaRequestDTO dto){
        MissaoMensal missaoMensal = new MissaoMensal();
        missaoMensal.setNome(dto.nome());
        missaoMensal.setDescricao(dto.descricao());
        missaoMensal.setMeta(dto.meta());
        missaoMensal.setDataFim(dto.dataFim());
        return missaoMensal;
    }

    public static MissaoMensalResumidaResponseDTO toDto (MissaoMensal missaoMensal){
        MissaoMensalResumidaResponseDTO dto = new MissaoMensalResumidaResponseDTO(
                missaoMensal.getIdMissao(),
                missaoMensal.getNome(),
                missaoMensal.getDescricao(),
                missaoMensal.getMeta(),
                missaoMensal.getDataFim()
        );
        return dto;
    }
}

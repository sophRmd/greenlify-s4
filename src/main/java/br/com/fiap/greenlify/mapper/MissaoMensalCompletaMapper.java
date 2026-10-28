package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.MissaoMensalCompletaRequestDTO;
import br.com.fiap.greenlify.dto.MissaoMensalCompletaResponseDTO;
import br.com.fiap.greenlify.models.MissaoMensal;

public class MissaoMensalCompletaMapper {
    public static MissaoMensal dtoToEntity(MissaoMensalCompletaRequestDTO dto){
        MissaoMensal missaoMensal = new MissaoMensal();
        missaoMensal.setNome(dto.nome());
        missaoMensal.setDescricao(dto.descricao());
        missaoMensal.setComoComprovar(dto.comoComprovar());
        missaoMensal.setRequisitos(dto.requisitos());
        missaoMensal.setCategoria(dto.categoria());
        missaoMensal.setMeta(dto.meta());
        missaoMensal.setDataInicio(dto.dataInicio());
        missaoMensal.setDataFim(dto.dataFim());
        return missaoMensal;
    }

    public static MissaoMensalCompletaResponseDTO toDto (MissaoMensal missaoMensal){
        MissaoMensalCompletaResponseDTO dto = new MissaoMensalCompletaResponseDTO(
                missaoMensal.getIdMissao(),
                missaoMensal.getNome(),
                missaoMensal.getDescricao(),
                missaoMensal.getComoComprovar(),
                missaoMensal.getRequisitos(),
                missaoMensal.getCategoria(),
                missaoMensal.getMeta(),
                missaoMensal.getDataInicio(),
                missaoMensal.getDataFim()
        );
        return dto;

    }
}

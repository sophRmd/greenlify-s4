package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.NivelContribuicaoRequestDTO;
import br.com.fiap.greenlify.dto.NivelContribuicaoResponseDTO;
import br.com.fiap.greenlify.models.NivelContribuicao;

public class NivelContribuicaoMapper {
    public static NivelContribuicao dtoToEntity (NivelContribuicaoRequestDTO dto){
        NivelContribuicao nivel = new NivelContribuicao();
        nivel.setNivel(dto.nivel());
        nivel.setNome(dto.nome());
        nivel.setXpMinimo(dto.xpMinimo());
        nivel.setXpMaximo(dto.xpMaximo());
        return nivel;
    }

    public static NivelContribuicaoResponseDTO toDto (NivelContribuicao nivel){
        NivelContribuicaoResponseDTO dto = new NivelContribuicaoResponseDTO(
                nivel.getIdNivel(),
                nivel.getNivel(),
                nivel.getNome(),
                nivel.getXpMinimo(),
                nivel.getXpMaximo()
        );
        return dto;
    }
}

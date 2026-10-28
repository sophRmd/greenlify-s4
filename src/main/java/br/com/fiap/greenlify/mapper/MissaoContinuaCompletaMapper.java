package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.MissaoContinuaCompletaRequestDTO;
import br.com.fiap.greenlify.dto.MissaoContinuaCompletaResponseDTO;
import br.com.fiap.greenlify.models.MissaoContinua;

public class MissaoContinuaCompletaMapper {
    public static MissaoContinua dtoToEntity(MissaoContinuaCompletaRequestDTO dto){
        MissaoContinua missaoContinua = new MissaoContinua();
        missaoContinua.setNome(dto.nome());
        missaoContinua.setDescricao(dto.descricao());
        missaoContinua.setComoComprovar(dto.comoComprovar());
        missaoContinua.setRequisitos(dto.requisitos());
        missaoContinua.setCategoria(dto.categoria());
        missaoContinua.setLimiteRealizacoes(dto.limiteRealizacoes());
        missaoContinua.setTipoMetrica(dto.tipoMetrica());
        missaoContinua.setValorMetrica(dto.valorMetrica());
        missaoContinua.setPontuacao(ReferenciaMapper.pontuacao(dto.idPontuacao()));
        return missaoContinua;
    }

    public static MissaoContinuaCompletaResponseDTO toDto (MissaoContinua missaoContinua){
        return new MissaoContinuaCompletaResponseDTO(
                missaoContinua.getIdMissao(),
                missaoContinua.getNome(),
                missaoContinua.getDescricao(),
                missaoContinua.getComoComprovar(),
                missaoContinua.getRequisitos(),
                missaoContinua.getCategoria(),
                missaoContinua.getLimiteRealizacoes(),
                missaoContinua.getTipoMetrica(),
                missaoContinua.getValorMetrica(),
                missaoContinua.getPontuacao()
        );
    }
}

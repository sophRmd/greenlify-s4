package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.MissaoContinuaResumidaRequestDTO;
import br.com.fiap.greenlify.dto.MissaoContinuaResumidaResponseDTO;
import br.com.fiap.greenlify.models.MissaoContinua;

public class MissaoContinuaResumidaMapper {
    public static MissaoContinua dtoToEntity(MissaoContinuaResumidaRequestDTO dto){
        MissaoContinua missaoContinua = new MissaoContinua();
        missaoContinua.setNome(dto.nome());
        missaoContinua.setTipoMetrica(dto.tipoMetrica());
        missaoContinua.setValorMetrica(dto.valorMetrica());
        missaoContinua.setPontuacao(ReferenciaMapper.pontuacao(dto.idPontuacao()));
        return missaoContinua;
    }

    public static MissaoContinuaResumidaResponseDTO toDto (MissaoContinua missaoContinua){
        return new MissaoContinuaResumidaResponseDTO(
                missaoContinua.getIdMissao(),
                missaoContinua.getNome(),
                missaoContinua.getTipoMetrica(),
                missaoContinua.getValorMetrica(),
                missaoContinua.getPontuacao()
        );
    }
}

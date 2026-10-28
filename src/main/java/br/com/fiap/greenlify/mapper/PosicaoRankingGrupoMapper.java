package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.PosicaoRankingGrupoRequestDTO;
import br.com.fiap.greenlify.dto.PosicaoRankingGrupoResponseDTO;
import br.com.fiap.greenlify.models.PosicaoRanking;

public class PosicaoRankingGrupoMapper {
    public static PosicaoRanking dtoToEntity (PosicaoRankingGrupoRequestDTO dto){
        PosicaoRanking posicaoRanking = new PosicaoRanking();
        posicaoRanking.setPosicao(dto.posicao());
        posicaoRanking.setPontuacao(dto.pontuacao());
        posicaoRanking.setGrupo(ReferenciaMapper.grupo(dto.idGrupo()));
        posicaoRanking.setRanking(ReferenciaMapper.ranking(dto.idRanking()));
        return posicaoRanking;
    }

    public static PosicaoRankingGrupoResponseDTO toDto (PosicaoRanking posicaoRanking){
        PosicaoRankingGrupoResponseDTO dto = new PosicaoRankingGrupoResponseDTO(
                posicaoRanking.getIdPosicao(),
                posicaoRanking.getPosicao(),
                posicaoRanking.getPontuacao(),
                posicaoRanking.getGrupo(),
                posicaoRanking.getRanking()
        );
        return dto;
    }
}

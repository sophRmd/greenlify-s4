package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.PosicaoRankingIndividualRequestDTO;
import br.com.fiap.greenlify.dto.PosicaoRankingIndividualResponseDTO;
import br.com.fiap.greenlify.models.PosicaoRanking;

public class PosicaoRankingIndividualMapper {
    public static PosicaoRanking dtoToEntity (PosicaoRankingIndividualRequestDTO dto){
        PosicaoRanking posicaoRanking = new PosicaoRanking();
        posicaoRanking.setPosicao(dto.posicao());
        posicaoRanking.setPontuacao(dto.pontuacao());
        posicaoRanking.setUsuario(ReferenciaMapper.usuarioComum(dto.idUsuario()));
        posicaoRanking.setRanking(ReferenciaMapper.ranking(dto.idRanking()));
        return posicaoRanking;
    }

    public static PosicaoRankingIndividualResponseDTO toDto (PosicaoRanking posicaoRanking){
        PosicaoRankingIndividualResponseDTO dto = new PosicaoRankingIndividualResponseDTO(
                posicaoRanking.getIdPosicao(),
                posicaoRanking.getPosicao(),
                posicaoRanking.getPontuacao(),
                UsuarioResumoMapper.toDto(posicaoRanking.getUsuario()),
                posicaoRanking.getRanking()
        );
        return dto;
    }
}
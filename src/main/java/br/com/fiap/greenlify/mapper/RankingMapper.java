package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.RankingRequestDTO;
import br.com.fiap.greenlify.dto.RankingResponseDTO;
import br.com.fiap.greenlify.models.Ranking;

public class RankingMapper {
    public static Ranking dtoToEntity (RankingRequestDTO dto){
        Ranking ranking = new Ranking();
        ranking.setDataInicio(dto.dataInicio());
        ranking.setDataFim(dto.dataFim());
        ranking.setTipo(dto.tipo());
        ranking.setStatus(dto.status());
        return ranking;
    }

    public static RankingResponseDTO toDto (Ranking ranking){
        RankingResponseDTO dto = new RankingResponseDTO(
                ranking.getIdRanking(),
                ranking.getDataInicio(),
                ranking.getDataFim(),
                ranking.getTipo(),
                ranking.getStatus()
        );
        return dto;
    }
}
package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.RecompensaRequestDTO;
import br.com.fiap.greenlify.dto.RecompensaResponseDTO;
import br.com.fiap.greenlify.models.Recompensa;

public class RecompensaMapper {
    public static Recompensa dtoToEntity (RecompensaRequestDTO dto){
        Recompensa recompensa = new Recompensa();
        recompensa.setIdRecompensa(dto.idRecompensa());
        recompensa.setBonusPercentual(dto.bonusPercentual());
        recompensa.setBonusPontos(dto.bonusPontos());
        recompensa.setDescricao(dto.descricao());
        return recompensa;
    }

    public static RecompensaResponseDTO toDto (Recompensa recompensa){
        RecompensaResponseDTO dto = new RecompensaResponseDTO(
                recompensa.getIdRecompensa(),
                recompensa.getBonusPercentual(),
                recompensa.getBonusPontos(),
                recompensa.getDescricao()
        );
        return dto;
    }
}
package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.SeloRequestDTO;
import br.com.fiap.greenlify.dto.SeloResponseDTO;
import br.com.fiap.greenlify.models.Selo;

public class SeloMapper {
    public static Selo dtoToEntity (SeloRequestDTO dto){
        Selo selo = new Selo();
        selo.setIdSelo(dto.idSelo());
        selo.setNome(dto.nome());
        selo.setValeEnergia(dto.valeEnergia());
        selo.setCreditoCarbono(dto.creditoCarbono());
        selo.setPontosNecessarios(dto.pontosNecessarios());
        return selo;
    }

    public static SeloResponseDTO toDto (Selo selo){
        SeloResponseDTO dto = new SeloResponseDTO(
                selo.getIdSelo(),
                selo.getNome(),
                selo.getValeEnergia(),
                selo.getCreditoCarbono(),
                selo.getPontosNecessarios()
        );
        return dto;
    }
}
package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.ResgateRequestDTO;
import br.com.fiap.greenlify.dto.ResgateResponseDTO;
import br.com.fiap.greenlify.models.Resgate;

public class ResgateMapper {
    public static Resgate dtoToEntity (ResgateRequestDTO dto){
        Resgate resgate = new Resgate();
        resgate.setDataConcessao(dto.dataConcessao());
        resgate.setUsuario(ReferenciaMapper.usuario(dto.idUsuario()));
        resgate.setRecompensa(ReferenciaMapper.recompensa(dto.idRecompensa()));
        return resgate;
    }

    public static ResgateResponseDTO toDto (Resgate resgate){
        ResgateResponseDTO dto = new ResgateResponseDTO(
                resgate.getIdResgate(),
                resgate.getDataConcessao(),
                UsuarioResumoMapper.toDto(resgate.getUsuario()),
                resgate.getRecompensa()
        );
        return dto;
    }
}
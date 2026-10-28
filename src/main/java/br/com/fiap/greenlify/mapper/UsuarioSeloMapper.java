package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.UsuarioSeloRequestDTO;
import br.com.fiap.greenlify.dto.UsuarioSeloResponseDTO;
import br.com.fiap.greenlify.models.UsuarioSelo;

public class UsuarioSeloMapper {
    public static UsuarioSelo dtoToEntity (UsuarioSeloRequestDTO dto){
        UsuarioSelo usuarioSelo = new UsuarioSelo();
        usuarioSelo.setDataColeta(dto.dataColeta());
        usuarioSelo.setUsuario(ReferenciaMapper.usuario(dto.idUsuario()));
        usuarioSelo.setSelo(ReferenciaMapper.selo(dto.idSelo()));
        return usuarioSelo;
    }

    public static UsuarioSeloResponseDTO toDto (UsuarioSelo usuarioSelo){
        UsuarioSeloResponseDTO dto = new UsuarioSeloResponseDTO(
                usuarioSelo.getIdUsuarioSelo(),
                usuarioSelo.getDataColeta(),
                UsuarioResumoMapper.toDto(usuarioSelo.getUsuario()),
                usuarioSelo.getSelo()
        );
        return dto;
    }
}
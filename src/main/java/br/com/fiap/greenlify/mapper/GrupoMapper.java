package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.GrupoRequestDTO;
import br.com.fiap.greenlify.dto.GrupoResponseDTO;
import br.com.fiap.greenlify.models.Grupo;

public class GrupoMapper {
    public static Grupo dtoToEntity(GrupoRequestDTO dto){
        Grupo grupo = new Grupo();
        grupo.setNome(dto.nome());
        grupo.setDataCriacao(dto.dataCriacao());
        grupo.setQtdUsuarios(dto.qtdUsuarios());
        return grupo;
    }

    public static GrupoResponseDTO toDto (Grupo grupo){
        return new GrupoResponseDTO(
                grupo.getIdGrupo(),
                grupo.getNome(),
                grupo.getDataCriacao(),
                grupo.getQtdUsuarios()
        );
    }
}

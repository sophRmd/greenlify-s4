package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.PerfilSustentavelRequestDTO;
import br.com.fiap.greenlify.dto.PerfilSustentavelResponseDTO;
import br.com.fiap.greenlify.models.NivelContribuicao;
import br.com.fiap.greenlify.models.PerfilSustentavel;
import br.com.fiap.greenlify.models.UsuarioComum;

public class PerfilSustentavelMapper {
    public static PerfilSustentavel dtoToEntity (PerfilSustentavelRequestDTO dto){
        PerfilSustentavel perfil = new PerfilSustentavel();
        perfil.setIdPerfilSustentavel(dto.idPerfilSustentavel());
        perfil.setXpSustentavel(dto.xpSustentavel());
        perfil.setTotalMissoesConcluidas(dto.totalMissoesConcluidas());
        perfil.setTotalAguaEconomizada(dto.totalAguaEconomizada());
        perfil.setTotalEnergiaEconomizada(dto.totalEnergiaEconomizada());
        perfil.setTotalCarbonoEconomizado(dto.totalCarbonoEconomizado());
        perfil.setUsuario(ReferenciaMapper.usuarioComum(dto.idUsuario()));
        perfil.setNivel(ReferenciaMapper.nivel(dto.idNivel()));
        return perfil;
    }

    public static PerfilSustentavelResponseDTO toDto (PerfilSustentavel perfil){
        PerfilSustentavelResponseDTO dto = new PerfilSustentavelResponseDTO(
                perfil.getIdPerfilSustentavel(),
                perfil.getXpSustentavel(),
                perfil.getTotalMissoesConcluidas(),
                perfil.getTotalAguaEconomizada(),
                perfil.getTotalEnergiaEconomizada(),
                perfil.getTotalCarbonoEconomizado(),
                UsuarioResumoMapper.toDto(perfil.getUsuario()),
                perfil.getNivel()
        );
        return dto;
    }
}
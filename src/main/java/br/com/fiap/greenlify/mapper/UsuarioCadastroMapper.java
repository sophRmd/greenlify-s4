package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.UsuarioCadastroRequestDTO;
import br.com.fiap.greenlify.dto.UsuarioCadastroResponseDTO;
import br.com.fiap.greenlify.models.UsuarioComum;

public class UsuarioCadastroMapper {
    public static UsuarioComum dtoToEntity (UsuarioCadastroRequestDTO dto){
        UsuarioComum usuario = new UsuarioComum();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(dto.senha());
        usuario.setNivelAcesso(dto.nivelAcesso());
        usuario.setStatusConta(dto.statusConta());
        usuario.setCpf(dto.cpf());
        usuario.setDataNascimento(dto.dataNascimento());
        if (dto.idGrupo() != null) {
            usuario.setGrupo(ReferenciaMapper.grupo(dto.idGrupo()));
        }
        return usuario;
    }

    /** Nunca inclui senha nem CPF na resposta. */
    public static UsuarioCadastroResponseDTO toDto (UsuarioComum usuario){
        return new UsuarioCadastroResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getNivelAcesso(),
                usuario.getStatusConta(),
                usuario.getDataNascimento(),
                usuario.getGrupo()
        );
    }
}

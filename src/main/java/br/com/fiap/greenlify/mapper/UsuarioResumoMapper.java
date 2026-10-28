package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.UsuarioResumoDTO;
import br.com.fiap.greenlify.models.Usuario;

public class UsuarioResumoMapper {

    private UsuarioResumoMapper() {
    }

    /**
     * Converte um usuário em sua versão pública (id e nome), sem senha, CPF,
     * e-mail ou data de nascimento. Use sempre este método ao expor um usuário
     * dentro de outro DTO de resposta.
     */
    public static UsuarioResumoDTO toDto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumoDTO(usuario.getIdUsuario(), usuario.getNome());
    }
}

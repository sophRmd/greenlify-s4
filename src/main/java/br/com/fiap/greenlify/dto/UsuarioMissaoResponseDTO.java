package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.models.Missao;

public record UsuarioMissaoResponseDTO(
        int idUsuarioMissao,
        int tentativas,
        int progressoAtual,
        UsuarioResumoDTO usuario,
        Missao missao
) {
}

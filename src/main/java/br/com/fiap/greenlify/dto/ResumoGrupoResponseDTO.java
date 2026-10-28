package br.com.fiap.greenlify.dto;

import java.util.List;

/**
 * Resumo de um grupo (modal do grupo): selos coletados e integrantes ordenados
 * pela quantidade de selos.
 */
public record ResumoGrupoResponseDTO(
        int idGrupo,
        String nomeGrupo,
        int totalSelos,
        int tiposColetados,
        int totalTipos,
        List<SeloQuantidade> selos,
        List<Integrante> integrantes
) {
    public record SeloQuantidade(int idSelo, String nome, int quantidade) {
    }

    public record Integrante(int posicao, int idUsuario, String nome, int selos) {
    }
}

package br.com.fiap.greenlify.dto;

/**
 * Resumo das missões de um usuário (tela de módulos de missões), separado em
 * missões contínuas e mensais vigentes.
 */
public record ResumoMissoesResponseDTO(
        int idUsuario,
        GrupoMissoes continuas,
        GrupoMissoes mensais
) {
    /**
     * @param total            missões consideradas (contínuas: todas; mensais: as vigentes)
     * @param emAndamento      missões que o usuário iniciou e ainda não concluiu
     * @param concluidas       missões com progresso igual ou acima da meta
     * @param percentualConcluido concluídas / total, em %
     * @param pontosMaximos    soma dos pontos base das missões (só contínuas; 0 nas mensais)
     * @param diasParaExpirar  dias até a missão mensal não concluída que expira primeiro (-1 se não houver)
     */
    public record GrupoMissoes(
            int total,
            int emAndamento,
            int concluidas,
            int percentualConcluido,
            int pontosMaximos,
            int diasParaExpirar
    ) {
    }
}

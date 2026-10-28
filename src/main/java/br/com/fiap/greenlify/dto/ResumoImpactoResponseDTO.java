package br.com.fiap.greenlify.dto;

/**
 * Resumo "Meu Impacto" de um usuário. {@code pontos} é o saldo atual da
 * carteira (o banco não guarda histórico de pontos por mês) e
 * {@code posicaoRanking} é 0 quando o usuário ainda não está no ranking.
 */
public record ResumoImpactoResponseDTO(
        int idUsuario,
        String nome,
        int pontos,
        int posicaoRanking,
        int mesesDeStreak,
        int missoesConcluidas
) {
}

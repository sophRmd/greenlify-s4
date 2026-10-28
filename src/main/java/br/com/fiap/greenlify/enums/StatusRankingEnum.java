package br.com.fiap.greenlify.enums;

/**
 * Estado do período de um ranking mensal. O único caminho válido é {@code ABERTO -> FECHADO}:
 * um ranking fechado é histórico (posições e prêmios preservados) e nunca volta a ser aberto.
 * O fechamento é decisão do administrador, só depois do fim do período.
 */
public enum StatusRankingEnum {
    /** Em andamento: aceita atualização das posições. */
    ABERTO,
    /** Encerrado pelo administrador: posições congeladas. */
    FECHADO
}
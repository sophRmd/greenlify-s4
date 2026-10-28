package br.com.fiap.greenlify.dto;

/**
 * Pedido de cadastro/atualização de carteira. Não carrega dados: pontos e valor em reais pertencem
 * exclusivamente ao servidor (são creditados pelo sistema, e o valor é calculado a partir dos
 * pontos). Qualquer campo enviado no corpo é ignorado; envie {@code {}}.
 */
public record CarteiraRequestDTO() {
}
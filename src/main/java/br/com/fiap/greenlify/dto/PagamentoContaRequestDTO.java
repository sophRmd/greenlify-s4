package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record PagamentoContaRequestDTO(
        LocalDate dataPagamento,
        double valorPago,
        int idUsuario,
        int idContaLuz
) {
}

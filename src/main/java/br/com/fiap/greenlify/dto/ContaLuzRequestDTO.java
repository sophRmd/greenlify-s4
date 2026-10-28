package br.com.fiap.greenlify.dto;

import java.time.LocalDate;

public record ContaLuzRequestDTO(
        String codigoBarras,
        double valorConta,
        LocalDate dataPagamento
) {
}

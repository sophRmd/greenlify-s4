package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.models.ContaLuz;
import java.time.LocalDate;

public record PagamentoContaResponseDTO(
        int idPagamento,
        LocalDate dataPagamento,
        double valorPago,
        UsuarioResumoDTO usuario,
        ContaLuz contaLuz
) {
}

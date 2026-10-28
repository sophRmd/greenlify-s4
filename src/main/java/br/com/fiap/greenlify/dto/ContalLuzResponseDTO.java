package br.com.fiap.greenlify.dto;

public record ContalLuzResponseDTO(
        int idContaLuz,
        String codigoBarras,
        double valorConta
) {
}

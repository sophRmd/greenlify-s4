package br.com.fiap.greenlify.dto;

public record SeloResponseDTO(
        int idSelo,
        String nome,
        int valeEnergia,
        int creditoCarbono,
        int pontosNecessarios
) {
}
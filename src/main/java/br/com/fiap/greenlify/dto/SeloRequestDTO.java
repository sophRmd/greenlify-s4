package br.com.fiap.greenlify.dto;

public record SeloRequestDTO(
        int idSelo,
        String nome,
        int valeEnergia,
        int creditoCarbono,
        int pontosNecessarios
) {
}
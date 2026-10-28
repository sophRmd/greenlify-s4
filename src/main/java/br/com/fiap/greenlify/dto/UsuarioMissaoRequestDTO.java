package br.com.fiap.greenlify.dto;

/**
 * Pedido para iniciar uma missão. Só a missão vem do cliente: o usuário é o autenticado e
 * tentativas e progresso são controlados pelo servidor (começam em 0).
 */
public record UsuarioMissaoRequestDTO(
        int idMissao
) {
}
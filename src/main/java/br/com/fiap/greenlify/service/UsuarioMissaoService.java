package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.dao.UsuarioMissaoDAO;
import br.com.fiap.greenlify.dto.UsuarioMissaoRequestDTO;
import br.com.fiap.greenlify.dto.UsuarioMissaoResponseDTO;
import br.com.fiap.greenlify.mapper.UsuarioMissaoMapper;
import br.com.fiap.greenlify.models.UsuarioMissao;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioMissaoService {
    private final UsuarioMissaoDAO usuarioMissaoDAO;
    private final UsuarioComumDAO usuarioDAO;

    public UsuarioMissaoService() {
        this.usuarioMissaoDAO = new UsuarioMissaoDAO();
        this.usuarioDAO = new UsuarioComumDAO();
    }

    public UsuarioMissaoResponseDTO buscarPorId(int id) {
        UsuarioMissao usuarioMissao = usuarioMissaoDAO.buscarPorId(id);
        if (usuarioMissao == null) {
            throw new IllegalArgumentException("Registro de usuário e missão não encontrado");
        }
        return UsuarioMissaoMapper.toDto(usuarioMissao);
    }

    public List<UsuarioMissaoResponseDTO> listar() {
        return usuarioMissaoDAO.listar()
                .stream()
                .map(UsuarioMissaoMapper::toDto)
                .toList();
    }

    public List<UsuarioMissaoResponseDTO> listarPorUsuario(int idUsuario) {
        return usuarioMissaoDAO.listarPorUsuario(idUsuario)
                .stream()
                .map(UsuarioMissaoMapper::toDto)
                .toList();
    }

    /**
     * Inicia a missão para o usuário autenticado: tentativas e progresso sempre começam em 0.
     * A conferência de duplicidade e de missão encerrada é feita pelo DAO, dentro da transação.
     */
    public void cadastrar(UsuarioMissaoRequestDTO dto, int idUsuario) {
        validar(dto, idUsuario);

        UsuarioMissao novo = UsuarioMissaoMapper.dtoToEntity(dto, idUsuario);
        usuarioMissaoDAO.cadastrar(novo);
    }

    public void excluir(int id) {
        UsuarioMissao usuarioMissao = usuarioMissaoDAO.buscarPorId(id);
        if (usuarioMissao == null) {
            throw new IllegalArgumentException("Registro de usuário e missão não encontrado");
        }
        usuarioMissaoDAO.excluir(id);
    }

    /**
     * Registra um passo de progresso em uma missão mensal do usuário autenticado. O servidor é
     * quem avança o progresso (um passo por chamada, só dentro do período da missão) e quem
     * decide a conclusão. O DAO grava, na mesma transação, o avanço e, se a meta foi atingida,
     * o encerramento (data de conclusão e streak). A missão mensal não pontua no ranking.
     *
     * @throws SecurityException        se o registro pertence a outro usuário
     * @throws IllegalArgumentException se o registro não existe, a missão está fora do período ou já foi concluída
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public UsuarioMissaoResponseDTO registrarProgresso(int id, int idUsuario) {
        UsuarioMissao existente = usuarioMissaoDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Registro de usuário e missão não encontrado");
        }
        if (existente.getUsuario().getIdUsuario() != idUsuario) {
            throw new SecurityException("Você só pode registrar progresso nas suas próprias missões.");
        }

        usuarioMissaoDAO.avancarProgressoMensal(idUsuario, existente.getMissao().getIdMissao(), LocalDate.now());
        return buscarPorId(id);
    }

    private void validar(UsuarioMissaoRequestDTO dto, int idUsuario) {
        if (idUsuario <= 0) {
            System.out.println("ERRO: O usuário é obrigatório.");
            throw new IllegalArgumentException("O usuário é obrigatório.");
        }
        if (dto.idMissao() <= 0) {
            System.out.println("ERRO: A missão é obrigatória.");
            throw new IllegalArgumentException("A missão é obrigatória.");
        }
        if (usuarioDAO.buscarPorId(idUsuario) == null) {
            System.out.println("ERRO: Usuário informado não existe.");
            throw new IllegalArgumentException("O usuário informado não existe.");
        }
    }
}
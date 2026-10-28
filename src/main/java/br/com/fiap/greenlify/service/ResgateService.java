package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.RecompensaDAO;
import br.com.fiap.greenlify.dao.ResgateDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.dto.ResgateRequestDTO;
import br.com.fiap.greenlify.dto.ResgateResponseDTO;
import br.com.fiap.greenlify.mapper.ResgateMapper;
import br.com.fiap.greenlify.models.Resgate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ResgateService {
    private final ResgateDAO resgateDAO;
    private final UsuarioComumDAO usuarioDAO;
    private final RecompensaDAO recompensaDAO;

    public ResgateService() {
        this.resgateDAO = new ResgateDAO();
        this.usuarioDAO = new UsuarioComumDAO();
        this.recompensaDAO = new RecompensaDAO();
    }

    public ResgateResponseDTO buscarPorId(int id) {
        Resgate resgate = resgateDAO.buscarPorId(id);
        if (resgate == null) {
            throw new IllegalArgumentException("Resgate não encontrado");
        }
        return ResgateMapper.toDto(resgate);
    }

    public List<ResgateResponseDTO> listar() {
        return resgateDAO.listar()
                .stream()
                .map(ResgateMapper::toDto)
                .toList();
    }

    public void cadastrar(ResgateRequestDTO dto) {
        validar(dto);
        validarDuplicidade(dto, 0);
        resgateDAO.cadastrar(ResgateMapper.dtoToEntity(dto));
    }

    public void excluir(int id) {
        Resgate resgate = resgateDAO.buscarPorId(id);
        if (resgate == null) {
            throw new IllegalArgumentException("Resgate não encontrado");
        }
        resgateDAO.excluir(id);
    }

    public void alterar(ResgateRequestDTO dto, int id) {
        Resgate existente = resgateDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Resgate não encontrado");
        }
        validar(dto);
        validarDuplicidade(dto, id);

        Resgate resgate = ResgateMapper.dtoToEntity(dto);
        resgate.setIdResgate(id);
        resgateDAO.alterar(resgate);
    }

    /**
     * Usuário + data identifica o prêmio de um ranking (ver {@link Resgate#dataDoRanking}): um
     * resgate manual não pode repetir essa dupla e burlar a regra de um prêmio por ranking.
     */
    private void validarDuplicidade(ResgateRequestDTO dto, int idResgateAtual) {
        if (resgateDAO.existePorUsuarioEData(dto.idUsuario(), dto.dataConcessao(), idResgateAtual)) {
            System.out.println("ERRO: O usuário já tem resgate nessa data.");
            throw new IllegalArgumentException("O usuário já tem um resgate nessa data de concessão.");
        }
    }

    private void validar(ResgateRequestDTO dto) {
        if (dto.dataConcessao() == null) {
            System.out.println("ERRO: A data de concessão é obrigatória.");
            throw new IllegalArgumentException("A data de concessão é obrigatória.");
        }
        if (!Resgate.dataConcessaoValida(dto.dataConcessao(), LocalDate.now())) {
            System.out.println("ERRO: A data de concessão não pode ser futura.");
            throw new IllegalArgumentException("A data de concessão não pode ser futura.");
        }
        if (dto.idUsuario() <= 0) {
            System.out.println("ERRO: O usuário é obrigatório.");
            throw new IllegalArgumentException("O usuário é obrigatório.");
        }
        if (dto.idRecompensa() <= 0) {
            System.out.println("ERRO: A recompensa é obrigatória.");
            throw new IllegalArgumentException("A recompensa é obrigatória.");
        }
        if (usuarioDAO.buscarPorId(dto.idUsuario()) == null) {
            System.out.println("ERRO: Usuário informado não existe.");
            throw new IllegalArgumentException("O usuário informado não existe.");
        }
        if (recompensaDAO.buscarPorId(dto.idRecompensa()) == null) {
            System.out.println("ERRO: Recompensa informada não existe.");
            throw new IllegalArgumentException("A recompensa informada não existe.");
        }
    }
}
package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.RecompensaDAO;
import br.com.fiap.greenlify.dto.RecompensaRequestDTO;
import br.com.fiap.greenlify.dto.RecompensaResponseDTO;
import br.com.fiap.greenlify.mapper.RecompensaMapper;
import br.com.fiap.greenlify.models.PosicaoRanking;
import br.com.fiap.greenlify.models.Recompensa;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecompensaService {
    private final RecompensaDAO recompensaDAO;

    public RecompensaService() {
        this.recompensaDAO = new RecompensaDAO();
    }

    public RecompensaResponseDTO buscarPorId(int id) {
        Recompensa recompensa = recompensaDAO.buscarPorId(id);
        if (recompensa == null) {
            throw new IllegalArgumentException("Recompensa não encontrada");
        }
        return RecompensaMapper.toDto(recompensa);
    }

    public List<RecompensaResponseDTO> listar() {
        return recompensaDAO.listar()
                .stream()
                .map(RecompensaMapper::toDto)
                .toList();
    }

    public void cadastrar(RecompensaRequestDTO dto) {
        validar(dto);
        if (dto.idRecompensa() <= 0) {
            System.out.println("ERRO: O id da recompensa deve ser maior que zero.");
            throw new IllegalArgumentException("O id da recompensa deve ser maior que zero.");
        }
        if (recompensaDAO.buscarPorId(dto.idRecompensa()) != null) {
            System.out.println("ERRO: Já existe uma recompensa com esse id.");
            throw new IllegalArgumentException("Já existe uma recompensa com esse id.");
        }
        recompensaDAO.cadastrar(RecompensaMapper.dtoToEntity(dto));
    }

    public void excluir(int id) {
        Recompensa recompensa = recompensaDAO.buscarPorId(id);
        if (recompensa == null) {
            throw new IllegalArgumentException("Recompensa não encontrada");
        }
        // As recompensas 1 a 6 são as faixas de premiação do ranking: sem elas o fechamento não concede prêmio.
        if (PosicaoRanking.recompensaDeFaixa(id)) {
            System.out.println("ERRO: Recompensa de faixa de ranking não pode ser excluída.");
            throw new IllegalArgumentException("As recompensas de 1 a 6 são as faixas de premiação do ranking e não podem ser excluídas.");
        }
        recompensaDAO.excluir(id);
    }

    public void alterar(RecompensaRequestDTO dto, int id) {
        Recompensa existente = recompensaDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Recompensa não encontrada");
        }
        if (dto.idRecompensa() != id) {
            System.out.println("ERRO: O id informado está incorreto.");
            throw new IllegalArgumentException("O id informado está incorreto.");
        }
        validar(dto);
        recompensaDAO.alterar(RecompensaMapper.dtoToEntity(dto));
    }

    private void validar(RecompensaRequestDTO dto) {
        if (dto.bonusPercentual() < 0 || dto.bonusPercentual() > 100) {
            System.out.println("ERRO: O bônus percentual deve estar entre 0 e 100.");
            throw new IllegalArgumentException("O bônus percentual deve estar entre 0 e 100.");
        }
        if (dto.bonusPontos() < 0) {
            System.out.println("ERRO: O bônus de pontos não pode ser negativo.");
            throw new IllegalArgumentException("O bônus de pontos não pode ser negativo.");
        }
        if (dto.descricao() == null || dto.descricao().isBlank()) {
            System.out.println("ERRO: A descrição é obrigatória.");
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }
    }
}
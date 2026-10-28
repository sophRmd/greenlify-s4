package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.PosicaoRankingDAO;
import br.com.fiap.greenlify.dto.PosicaoRankingGrupoResponseDTO;
import br.com.fiap.greenlify.dto.PosicaoRankingIndividualResponseDTO;
import br.com.fiap.greenlify.mapper.PosicaoRankingGrupoMapper;
import br.com.fiap.greenlify.mapper.PosicaoRankingIndividualMapper;
import br.com.fiap.greenlify.models.PosicaoRanking;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class PosicaoRankingService {
    private final PosicaoRankingDAO posicaoDAO;

    public PosicaoRankingService() {
        this.posicaoDAO = new PosicaoRankingDAO();
    }

    // ---------- Grupo ----------

    public PosicaoRankingGrupoResponseDTO buscarGrupoPorId(int id) {
        PosicaoRanking posicao = posicaoDAO.buscarPorId(id);
        if (posicao == null || posicao.getGrupo() == null) {
            throw new IllegalArgumentException("Posição de ranking de grupo não encontrada");
        }
        return PosicaoRankingGrupoMapper.toDto(posicao);
    }

    /**
     * Lista o ranking de grupos atual (o mais recente), ordenado pela posição.
     */
    public List<PosicaoRankingGrupoResponseDTO> listarGrupos() {
        int idRankingAtual = posicaoDAO.obterUltimoIdRankingGrupo();
        return posicaoDAO.listarPorRanking(idRankingAtual)
                .stream()
                .filter(p -> p.getGrupo() != null
                        && p.getRanking() != null
                        && p.getRanking().getIdRanking() == idRankingAtual)
                .sorted(Comparator.comparingInt(PosicaoRanking::getPosicao))
                .map(PosicaoRankingGrupoMapper::toDto)
                .toList();
    }

    // ---------- Individual ----------

    public PosicaoRankingIndividualResponseDTO buscarIndividualPorId(int id) {
        PosicaoRanking posicao = posicaoDAO.buscarPorId(id);
        if (posicao == null || posicao.getUsuario() == null) {
            throw new IllegalArgumentException("Posição de ranking individual não encontrada");
        }
        return PosicaoRankingIndividualMapper.toDto(posicao);
    }

    /**
     * Lista o ranking individual atual (o mais recente), ordenado pela posição.
     */
    public List<PosicaoRankingIndividualResponseDTO> listarIndividuais() {
        int idRankingAtual = posicaoDAO.obterUltimoIdRankingUsuario();
        return posicaoDAO.listarPorRanking(idRankingAtual)
                .stream()
                .filter(p -> p.getUsuario() != null
                        && p.getRanking() != null
                        && p.getRanking().getIdRanking() == idRankingAtual)
                .sorted(Comparator.comparingInt(PosicaoRanking::getPosicao))
                .map(PosicaoRankingIndividualMapper::toDto)
                .toList();
    }
}

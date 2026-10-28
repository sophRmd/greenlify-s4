package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.RankingRedisDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.dao.UsuarioMissaoDAO;
import br.com.fiap.greenlify.dto.RankingAllTimeResponseDTO;
import br.com.fiap.greenlify.mapper.UsuarioResumoMapper;
import br.com.fiap.greenlify.models.UsuarioComum;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Ranking all time: cada usuário vale a pontuação elegível das missões contínuas que concluiu
 * (regra única de {@code Pontuacao.pontosElegiveis}; missão mensal não soma). Não depende da
 * carteira, então pagar uma conta não faz ninguém cair.
 *
 * <p>O Oracle é a fonte da verdade e o Redis (Sorted Set {@value RankingRedisDAO#CHAVE}) é só uma
 * projeção rápida dela: o Redis responde a consulta; se estiver fora do ar ou vazio, o Oracle
 * responde. A conclusão incrementa o Redis uma única vez por evento, e a {@link #reconciliar()}
 * recalcula os totais no Oracle e corrige qualquer divergência (incremento perdido, Redis
 * reiniciado).</p>
 *
 * <p>Não cria, atualiza nem fecha rankings mensais: isso é ação do administrador
 * ({@link RankingService}).</p>
 */
@Service
public class RankingAllTimeService {
    private final UsuarioMissaoDAO usuarioMissaoDAO;
    private final UsuarioComumDAO usuarioDAO;
    private final RankingRedisDAO redisDAO;

    public RankingAllTimeService() {
        this.usuarioMissaoDAO = new UsuarioMissaoDAO();
        this.usuarioDAO = new UsuarioComumDAO();
        this.redisDAO = new RankingRedisDAO();
    }

    /**
     * Lista o ranking all time, do usuário com mais pontos elegíveis para o com menos
     * (empate: o cadastro mais antigo vem primeiro).
     */
    public List<RankingAllTimeResponseDTO> listar() {
        Map<Integer, Integer> totais = redisDAO.listar();
        if (totais == null) {
            // Redis fora do ar: o Oracle responde.
            totais = usuarioMissaoDAO.somarPontosElegiveisPorUsuario();
        } else if (totais.isEmpty()) {
            // Redis vazio: o Oracle responde e o Redis é corrigido sem ser apagado.
            totais = usuarioMissaoDAO.somarPontosElegiveisPorUsuario();
            if (!totais.isEmpty()) {
                redisDAO.reconciliar(totais);
            }
        }
        final Map<Integer, Integer> pontos = totais;

        List<UsuarioComum> usuarios = usuarioDAO.listar();
        List<RankingAllTimeResponseDTO> ranking = new ArrayList<>();
        if (usuarios == null) {
            return ranking;
        }

        usuarios.sort(Comparator
                .comparingInt((UsuarioComum u) -> pontos.getOrDefault(u.getIdUsuario(), 0)).reversed()
                .thenComparingInt(UsuarioComum::getIdUsuario));

        int posicao = 1;
        for (UsuarioComum usuario : usuarios) {
            ranking.add(new RankingAllTimeResponseDTO(
                    posicao++,
                    UsuarioResumoMapper.toDto(usuario),
                    pontos.getOrDefault(usuario.getIdUsuario(), 0)));
        }
        return ranking;
    }

    /**
     * Chamado depois que a conclusão de uma missão contínua já foi gravada no Oracle: soma no
     * Sorted Set a pontuação elegível do usuário. Uma falha do Redis só é registrada (a conclusão
     * não é desfeita) e a reconciliação recupera o incremento. Repetir a chamada para a mesma
     * conclusão (mesmo usuário, missão e mês) não soma de novo.
     *
     * @param pontos pontuação elegível do evento, calculada por {@code Pontuacao.pontosElegiveis}
     */
    public void registrarMissaoConcluida(int idUsuario, int idMissao, LocalDate dia, int pontos) {
        String idEvento = idUsuario + ":" + idMissao + ":" + YearMonth.from(dia);
        try {
            if (redisDAO.incrementar(idUsuario, idEvento, dia, pontos)
                    == RankingRedisDAO.Resultado.REDIS_INDISPONIVEL) {
                System.err.println("Redis fora do ar: o incremento do usuário " + idUsuario
                        + " será recuperado na próxima reconciliação.");
            }
        } catch (RuntimeException e) {
            System.err.println("Não foi possível atualizar o ranking all time: " + e.getMessage());
        }
    }

    /**
     * Recalcula os totais a partir do Oracle e corrige o Redis membro a membro (sem apagar a chave).
     *
     * @return o resumo da correção, ou {@code null} se o Redis estiver fora do ar
     * @throws IllegalStateException se o Oracle falhar (nada é alterado no Redis)
     */
    public RankingRedisDAO.Reconciliacao reconciliar() {
        return redisDAO.reconciliar(usuarioMissaoDAO.somarPontosElegiveisPorUsuario());
    }

    /** Reconciliação periódica; o intervalo pode ser ajustado em ranking.all-time.reconciliacao-ms. */
    @Scheduled(fixedDelayString = "${ranking.all-time.reconciliacao-ms:600000}",
            initialDelayString = "${ranking.all-time.reconciliacao-inicial-ms:60000}")
    public void reconciliarAgendado() {
        try {
            reconciliar();
        } catch (RuntimeException e) {
            System.err.println("Não foi possível reconciliar o ranking all time: " + e.getMessage());
        }
    }
}
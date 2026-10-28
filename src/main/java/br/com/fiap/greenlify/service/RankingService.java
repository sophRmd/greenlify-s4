package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.PosicaoRankingDAO;
import br.com.fiap.greenlify.dao.RankingDAO;
import br.com.fiap.greenlify.dto.RankingRequestDTO;
import br.com.fiap.greenlify.dto.RankingResponseDTO;
import br.com.fiap.greenlify.mapper.RankingMapper;
import br.com.fiap.greenlify.enums.StatusRankingEnum;
import br.com.fiap.greenlify.enums.TipoRankingEnum;
import br.com.fiap.greenlify.models.PosicaoRanking;
import br.com.fiap.greenlify.models.Ranking;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Ciclo dos rankings mensais, em três etapas separadas, todas de iniciativa do administrador:
 * <ol>
 *   <li><b>criar</b> o ranking ({@link #cadastrar}, {@link #criarRankingsDoMes()});</li>
 *   <li><b>atualizar</b> as posições dos rankings abertos ({@link #atualizarRankings()});</li>
 *   <li><b>fechar</b> de forma definitiva, depois do fim do período, e conceder as recompensas
 *       ({@link #fecharRanking(int)}).</li>
 * </ol>
 * Nenhuma etapa dispara a outra, e nenhuma conclusão de missão mexe nos rankings mensais. O ranking
 * all time (Redis) é outra coisa: ver {@link RankingAllTimeService}.
 */
@Service
public class RankingService {
    private final RankingDAO rankingDAO;
    private final PosicaoRankingDAO posicaoDAO;

    /**
     * Trava única (vale para todas as instâncias do serviço) para quem cria, atualiza e fecha
     * rankings: evita que duas operações simultâneas dupliquem posições ou prêmios. Entre
     * servidores diferentes quem garante é o banco (linha do ranking travada e prêmio único por
     * usuário + data final).
     */
    private static final Object TRAVA_POSICOES = new Object();

    public RankingService() {
        this.rankingDAO = new RankingDAO();
        this.posicaoDAO = new PosicaoRankingDAO();
    }

    // ------------------------------------------------------------------
    // 1) Criação
    // ------------------------------------------------------------------

    /**
     * Cria os rankings do mês atual (individual e de grupos) que ainda não existirem. Só cria:
     * não calcula posições e não fecha nada. Repetir não duplica (um período que já cruza o mês
     * é respeitado).
     *
     * @return quantos rankings foram criados agora (0 a 2)
     * @throws IllegalStateException se o banco não confirmar a criação
     */
    public int criarRankingsDoMes() {
        synchronized (TRAVA_POSICOES) {
            LocalDate hoje = LocalDate.now();
            LocalDate inicio = hoje.withDayOfMonth(1);
            LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());

            int criados = 0;
            for (TipoRankingEnum tipo : TipoRankingEnum.values()) {
                if (rankingDAO.existeSobreposicao(tipo, inicio, fim, 0)) {
                    continue;
                }
                Ranking novo = new Ranking();
                novo.setTipo(tipo);
                novo.setDataInicio(inicio);
                novo.setDataFim(fim);
                novo.setStatus(StatusRankingEnum.ABERTO);
                rankingDAO.cadastrar(novo);
                if (novo.getIdRanking() <= 0) {
                    throw new IllegalStateException("Não foi possível criar o ranking do mês.");
                }
                criados++;
            }
            return criados;
        }
    }

    // ------------------------------------------------------------------
    // 2) Atualização das posições
    // ------------------------------------------------------------------

    /**
     * Recalcula e regrava as posições de todos os rankings ABERTOS, com as regras do model
     * {@link Ranking} (individual: missões contínuas concluídas no período e, no empate, cadastro
     * mais antigo; grupal: Selos Verdes resgatados pelos integrantes). Só atualiza: não cria
     * rankings, não fecha nenhum e não concede recompensas. Rankings fechados são histórico e não
     * são tocados.
     *
     * @throws IllegalStateException se o banco falhar (as posições anteriores são mantidas)
     */
    public void atualizarRankings() {
        synchronized (TRAVA_POSICOES) {
            for (Ranking ranking : rankingDAO.listar()) {
                if (ranking.aceitaAtualizacao()) {
                    posicaoDAO.substituirPorRanking(ranking.getIdRanking(), ranking.calcularPosicoes());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 3) Fechamento definitivo e recompensas
    // ------------------------------------------------------------------

    /**
     * Resumo do fechamento: prêmios concedidos agora, já concedidos antes e não concedidos;
     * {@code fechadoAgora} diz se esta chamada foi a que encerrou o ranking.
     */
    public record ResultadoFechamento(int concedidas, int jaConcedidas, int naoConcedidas, boolean fechadoAgora) {
    }

    /**
     * Fecha definitivamente um ranking já encerrado (nunca um vigente) e, no individual, concede as
     * recompensas das posições elegíveis (quem concluiu ao menos uma missão no período).
     *
     * <p>Passos, nesta ordem: (1) se o ranking ainda está ABERTO, as posições finais são calculadas
     * e gravadas; se já está FECHADO, as posições gravadas são mantidas (histórico) e só os prêmios
     * pendentes são reaplicados; (2) cada prêmio é concedido por
     * {@link PosicaoRanking#concederRecompensa}, que grava prêmio e resgate na mesma transação e
     * nunca repete o prêmio do mesmo usuário no mesmo ranking; (3) só no fim o ranking passa a
     * FECHADO, por troca atômica de status. Se algum passo lançar erro, o ranking continua ABERTO e
     * a operação pode ser repetida sem duplicar nada. Prêmios não concedidos (ex.: 1º lugar sem
     * conta de luz pendente) aparecem no resumo e podem ser reaplicados repetindo o fechamento.</p>
     *
     * @param idRanking identificador do ranking a fechar
     * @return o resumo dos prêmios
     * @throws IllegalArgumentException ranking inexistente ou que ainda não terminou
     * @throws IllegalStateException    se o banco falhar (o ranking não é fechado)
     */
    public ResultadoFechamento fecharRanking(int idRanking) {
        Ranking ranking = rankingDAO.buscarPorId(idRanking);
        if (ranking == null) {
            throw new IllegalArgumentException("Ranking não encontrado");
        }
        if (!ranking.terminou(LocalDate.now())) {
            throw new IllegalArgumentException("O ranking ainda não terminou: só se fecha ranking encerrado.");
        }

        synchronized (TRAVA_POSICOES) {
            boolean aberto = ranking.aceitaAtualizacao();

            List<PosicaoRanking> posicoes;
            if (aberto) {
                posicoes = ranking.calcularPosicoes();
                posicaoDAO.substituirPorRanking(idRanking, posicoes);
            } else {
                posicoes = posicaoDAO.listarPorRanking(idRanking);
            }

            int concedidas = 0;
            int jaConcedidas = 0;
            int naoConcedidas = 0;
            if (ranking.getTipo() == TipoRankingEnum.INDIVIDUAL) {
                for (PosicaoRanking posicao : posicoes) {
                    if (posicao.getPontuacao() <= 0 || posicao.getUsuario() == null) {
                        continue;
                    }
                    switch (posicao.concederRecompensa(posicao.getUsuario())) {
                        case CONCEDIDA -> concedidas++;
                        case JA_CONCEDIDA -> jaConcedidas++;
                        default -> naoConcedidas++;
                    }
                }
            }

            boolean fechadoAgora = aberto && rankingDAO.fechar(idRanking);
            return new ResultadoFechamento(concedidas, jaConcedidas, naoConcedidas, fechadoAgora);
        }
    }

    // ------------------------------------------------------------------
    // Consulta e cadastro manual
    // ------------------------------------------------------------------

    public RankingResponseDTO buscarPorId(int id) {
        Ranking ranking = rankingDAO.buscarPorId(id);
        if (ranking == null) {
            throw new IllegalArgumentException("Ranking não encontrado");
        }
        return RankingMapper.toDto(ranking);
    }

    public List<RankingResponseDTO> listar() {
        return rankingDAO.listar()
                .stream()
                .map(RankingMapper::toDto)
                .toList();
    }

    /** Cria um ranking ABERTO com o período informado, sem cruzar outro do mesmo tipo. */
    public void cadastrar(RankingRequestDTO dto) {
        validar(dto);
        if (dto.status() != StatusRankingEnum.ABERTO) {
            throw new IllegalArgumentException("Um ranking novo começa ABERTO: o fechamento é uma etapa própria.");
        }
        if (rankingDAO.existeSobreposicao(dto.tipo(), dto.dataInicio(), dto.dataFim(), 0)) {
            throw new IllegalArgumentException("Já existe um ranking desse tipo cujo período cruza o informado.");
        }
        rankingDAO.cadastrar(RankingMapper.dtoToEntity(dto));
    }

    /** Ranking encerrado é histórico: não se exclui (posições e prêmios ficam preservados). */
    public void excluir(int id) {
        Ranking ranking = rankingDAO.buscarPorId(id);
        if (ranking == null) {
            throw new IllegalArgumentException("Ranking não encontrado");
        }
        if (ranking.getStatus() == StatusRankingEnum.FECHADO) {
            throw new IllegalArgumentException("Ranking encerrado não pode ser excluído: o histórico é preservado.");
        }
        rankingDAO.excluir(id);
    }

    /**
     * Altera o período de um ranking ABERTO. Ranking encerrado não muda, e o status não é alterado
     * por aqui: fechar é a etapa {@link #fecharRanking(int)}.
     */
    public void alterar(RankingRequestDTO dto, int id) {
        Ranking existente = rankingDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Ranking não encontrado");
        }
        if (existente.getStatus() == StatusRankingEnum.FECHADO) {
            throw new IllegalArgumentException("Ranking encerrado não pode ser alterado: o histórico é preservado.");
        }
        validar(dto);
        if (dto.status() != StatusRankingEnum.ABERTO) {
            throw new IllegalArgumentException("O fechamento é uma etapa própria: use o fechamento do ranking.");
        }
        if (rankingDAO.existeSobreposicao(dto.tipo(), dto.dataInicio(), dto.dataFim(), id)) {
            throw new IllegalArgumentException("Já existe um ranking desse tipo cujo período cruza o informado.");
        }

        Ranking ranking = RankingMapper.dtoToEntity(dto);
        ranking.setIdRanking(id);
        rankingDAO.alterar(ranking);
    }

    private void validar(RankingRequestDTO dto) {
        if (dto.dataInicio() == null) {
            System.out.println("ERRO: A data de início é obrigatória.");
            throw new IllegalArgumentException("A data de início é obrigatória.");
        }
        if (dto.dataFim() == null) {
            System.out.println("ERRO: A data de fim é obrigatória.");
            throw new IllegalArgumentException("A data de fim é obrigatória.");
        }
        if (dto.dataFim().isBefore(dto.dataInicio())) {
            System.out.println("ERRO: A data de fim não pode ser anterior à data de início.");
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início.");
        }
        if (dto.tipo() == null) {
            System.out.println("ERRO: O tipo é obrigatório.");
            throw new IllegalArgumentException("O tipo é obrigatório.");
        }
        if (dto.status() == null) {
            System.out.println("ERRO: O status é obrigatório.");
            throw new IllegalArgumentException("O status é obrigatório.");
        }
    }
}
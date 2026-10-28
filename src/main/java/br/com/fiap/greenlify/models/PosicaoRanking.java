package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.dao.ContaLuzDAO;
import br.com.fiap.greenlify.dao.RecompensaDAO;
import br.com.fiap.greenlify.dao.ResgateDAO;
import br.com.fiap.greenlify.enums.TipoRankingEnum;

import java.time.LocalDate;
import java.util.List;

/**
 * Representa a posição de um usuário ou grupo em um {@link Ranking},
 * incluindo sua pontuação e o processo de concessão de recompensas com base
 * na colocação obtida.
 */
public class PosicaoRanking {

    /** Maior id de recompensa usado nas faixas de premiação (ids 1 a 6). */
    public static final int ID_ULTIMA_RECOMPENSA_FAIXA = 6;

    private int idPosicao;
    private int posicao;
    private int pontuacao;
    private UsuarioComum usuario;
    private Ranking ranking;
    private Grupo grupo;
    /** Missões concluídas pelo usuário dentro do período do ranking (calculado, não é gravado). */
    private int missoesRealizadas;

    /** Resultado da tentativa de conceder a recompensa de uma posição. */
    public enum Resultado {
        /** Prêmio concedido agora. */
        CONCEDIDA,
        /** O usuário já tinha recebido o prêmio deste ranking (nada foi feito). */
        JA_CONCEDIDA,
        /** Não foi possível conceder (ex.: 1º lugar sem conta de luz pendente, falha no banco); pode ser repetido depois. */
        NAO_CONCEDIDA
    }

    /**
     * Concede ao usuário informado a recompensa correspondente à colocação
     * desta posição no ranking e registra o resgate (com a data final do
     * ranking, o que identifica usuário + ranking), no máximo uma vez por ranking.
     * <p>
     * Regras de premiação:
     * <ul>
     *   <li>1º lugar: pagamento INTEGRAL da conta de luz que o usuário cadastrou
     *       (sem crédito nem débito na carteira);</li>
     *   <li>2º e 3º lugares: bônus da recompensa 2 e 3 sobre o saldo de pontos da carteira;</li>
     *   <li>4º ao 10º: recompensa 4; 11º ao 20º: recompensa 5; demais: recompensa 6,
     *       todas com o bônus cadastrado (percentual do saldo mais pontos fixos, ver
     *       {@link Recompensa#calcularBonus(long)}).</li>
     * </ul>
     * A concessão e o registro do resgate acontecem na mesma transação do banco, e repetir a chamada
     * nunca concede duas vezes (usuário + data final do ranking).
     *
     * @param usuarioResgatador usuário que deve receber a recompensa
     * @return o que aconteceu: concedida, já concedida antes ou não concedida
     */
    public Resultado concederRecompensa(UsuarioComum usuarioResgatador) {

        if (usuarioResgatador == null || ranking == null || ranking.getDataFim() == null) {
            System.out.println("Erro ao conceder recompensa.");
            return Resultado.NAO_CONCEDIDA;
        }

        if (ranking.getTipo() != TipoRankingEnum.INDIVIDUAL) {
            System.out.println("Só o ranking individual concede recompensa a usuários.");
            return Resultado.NAO_CONCEDIDA;
        }
        // O prêmio é da colocação desta posição: não pode ir para outro usuário.
        if (usuario == null || usuario.getIdUsuario() != usuarioResgatador.getIdUsuario()) {
            System.out.println("O usuário informado não é o dono desta posição: recompensa não concedida.");
            return Resultado.NAO_CONCEDIDA;
        }
        ResgateDAO resgateDAO = new ResgateDAO();
        // O prêmio do ranking é gravado com a data final do período: usuário + data = usuário + ranking.
        LocalDate dataConcessao = Resgate.dataDoRanking(ranking);
        if (resgateDAO.existePorUsuarioEData(usuarioResgatador.getIdUsuario(), dataConcessao)) {
            return Resultado.JA_CONCEDIDA;
        }

        int idRecompensa = idRecompensaPorPosicao(this.posicao);
        if (idRecompensa == 0) {
            System.out.println("Posição inválida: recompensa não concedida.");
            return Resultado.NAO_CONCEDIDA;
        }

        Recompensa recompensa = new RecompensaDAO().buscarPorId(idRecompensa);
        if (recompensa == null) {
            System.out.println("Recompensa não encontrada.");
            return Resultado.NAO_CONCEDIDA;
        }

        Resgate resgate = new Resgate();
        resgate.setDataConcessao(dataConcessao);
        resgate.setUsuario(usuarioResgatador);
        resgate.setRecompensa(recompensa);

        Resultado resultado;
        if (this.posicao == 1) {
            ContaLuz conta = new ContaLuzDAO().buscarPendenteDoUsuario(usuarioResgatador.getIdUsuario());
            if (conta == null || conta.getValorConta() <= 0) {
                System.out.println("1º lugar sem conta de luz pendente: prêmio não concedido.");
                return Resultado.NAO_CONCEDIDA;
            }
            PagamentoConta pagamento = new PagamentoConta();
            pagamento.setDataPagamento(LocalDate.now());
            pagamento.setValorPago(conta.getValorConta());
            pagamento.setUsuario(usuarioResgatador);
            pagamento.setContaLuz(conta);
            resultado = resgateDAO.concederPagamentoConta(resgate, pagamento);
        } else {
            resultado = resgateDAO.concederBonus(resgate);
        }

        if (resultado == Resultado.CONCEDIDA) {
            System.out.println("Recompensa concedida: " + recompensa.getDescricao());
        }
        return resultado;
    }

    /**
     * Faixa de premiação (fonte única): 1º, 2º e 3º lugares têm recompensa própria (ids 1, 2 e 3);
     * 4º ao 10º, recompensa 4; 11º ao 20º, recompensa 5; demais, recompensa
     * {@value #ID_ULTIMA_RECOMPENSA_FAIXA}.
     *
     * @return o id da recompensa cadastrada, ou 0 se a posição for inválida (menor que 1)
     */
    public static int idRecompensaPorPosicao(int posicao) {
        if (posicao < 1) {
            return 0;
        }
        if (posicao <= 3) {
            return posicao;
        }
        if (posicao <= 10) {
            return 4;
        }
        if (posicao <= 20) {
            return 5;
        }
        return ID_ULTIMA_RECOMPENSA_FAIXA;
    }

    /** Se o id é de uma recompensa de faixa de premiação (1 a 6): essas não podem ser apagadas. */
    public static boolean recompensaDeFaixa(int idRecompensa) {
        return idRecompensa >= 1 && idRecompensa <= ID_ULTIMA_RECOMPENSA_FAIXA;
    }

    /**
     * Consistência com o tipo do ranking: individual tem usuário e não tem grupo;
     * grupal tem grupo e não tem usuário.
     */
    public boolean compativelCom(TipoRankingEnum tipo) {
        if (tipo == null) {
            return false;
        }
        return switch (tipo) {
            case INDIVIDUAL -> usuario != null && grupo == null;
            case GRUPAL -> grupo != null && usuario == null;
        };
    }

    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(int pontuacao) {
        this.pontuacao = pontuacao;
    }

    public UsuarioComum getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioComum usuario) {
        this.usuario = usuario;
    }

    public int getIdPosicao() {
        return idPosicao;
    }

    public void setIdPosicao(int idPosicao) {
        this.idPosicao = idPosicao;
    }

    public Ranking getRanking() {
        return ranking;
    }

    public void setRanking(Ranking ranking) {
        this.ranking = ranking;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public int getMissoesRealizadas() {
        return missoesRealizadas;
    }

    public void setMissoesRealizadas(int missoesRealizadas) {
        this.missoesRealizadas = missoesRealizadas;
    }
}
package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.models.Grupo;
import br.com.fiap.greenlify.models.PagamentoConta;
import br.com.fiap.greenlify.models.PosicaoRanking;
import br.com.fiap.greenlify.models.Recompensa;
import br.com.fiap.greenlify.models.Resgate;
import br.com.fiap.greenlify.models.UsuarioComum;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO responsável pela persistência da entidade Resgate, que representa
 * a concessão de uma recompensa a um usuário (prêmio do ranking individual).
 *
 * <p>Não tem relação com os Selos Verdes (tabela Usuario_Selo): resgates de prêmio não entram na
 * pontuação do ranking de grupos. Falhas de escrita lançam {@link IllegalStateException}.</p>
 */
public class ResgateDAO {

    /**
     * Cadastra um novo resgate, vinculando um usuário a uma recompensa
     * em uma determinada data de concessão.
     *
     * @param resgate dados do resgate a ser gravado
     */
    public void cadastrar(Resgate resgate) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Resgate (dt_concessao, id_usuario, id_recompensa)" +
                    "VALUES (?, ?, ?)";
            ps = conexao.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(resgate.getDataConcessao()));
            ps.setInt(2, resgate.getUsuario().getIdUsuario());
            ps.setInt(3, resgate.getRecompensa().getIdRecompensa());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o resgate.", e);
        } finally {
            try {
                if (conexao != null && !conexao.isClosed()) {
                    conexao.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Atualiza um resgate já existente.
     *
     * @param resgate dados atualizados do resgate (o id é usado para localizar o registro)
     */
    public void alterar(Resgate resgate) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Resgate SET dt_concessao = ?, id_usuario = ?, id_recompensa = ? " +
                    "WHERE id_resgate = ?";
            ps = conexao.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(resgate.getDataConcessao()));
            ps.setInt(2, resgate.getUsuario().getIdUsuario());
            ps.setInt(3, resgate.getRecompensa().getIdRecompensa());
            ps.setInt(4, resgate.getIdResgate());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o resgate.", e);
        } finally {
            try {
                if (conexao != null && !conexao.isClosed()) {
                    conexao.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Exclui um resgate pelo id.
     *
     * @param id id do resgate a ser removido
     */
    public void excluir(int id) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "DELETE FROM Resgate WHERE id_resgate = ?";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, id);

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o resgate.", e);
        } finally {
            try {
                if (conexao != null && !conexao.isClosed()) {
                    conexao.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /** Resgate + Recompensa (rc_) + Usuário (u_) em uma única consulta. */
    private static final String SELECT_COMPLETO =
            "SELECT r.id_resgate, r.dt_concessao, "
                    + "       rc.id_recompensa AS rc_id, rc.nr_bonus_percentual AS rc_percentual, "
                    + "       rc.nr_pontos_bonus AS rc_pontos, rc.ds_recompensa AS rc_descricao, "
                    + "       u.id_usuario AS u_id, u.nm_usuario AS u_nome, u.ds_cpf AS u_cpf, "
                    + "       u.dt_nascimento AS u_nascimento, u.ds_email AS u_email, u.ds_senha AS u_senha, "
                    + "       u.nr_nivel_acesso AS u_nivel, u.ds_status_conta AS u_status, u.id_grupo AS u_grupo "
                    + "FROM Resgate r "
                    + "LEFT JOIN Recompensa rc ON rc.id_recompensa = r.id_recompensa "
                    + "LEFT JOIN Usuario u ON u.id_usuario = r.id_usuario AND u.nr_nivel_acesso = 0 ";

    /**
     * Busca um resgate pelo id, já carregando o usuário e a recompensa relacionados,
     * em uma única consulta.
     *
     * @param id id do resgate procurado
     * @return o resgate encontrado (com usuário e recompensa preenchidos), ou {@code null} se não existir
     */
    public Resgate buscarPorId(int id) {
        Resgate resgate = null;

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "WHERE r.id_resgate = ?")) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resgate = mapear(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resgate;
    }

    /**
     * Retorna todos os resgates cadastrados, cada um já com o usuário
     * e a recompensa correspondentes carregados, em uma única consulta.
     *
     * @return lista de resgates (pode vir vazia, nunca nula)
     */
    public List<Resgate> listar() {
        List<Resgate> lista = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "ORDER BY r.id_resgate");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Prêmios de ranking (uma transação por prêmio)
    //
    // O prêmio de um ranking é gravado com dt_concessao = último dia do período
    // do ranking. Assim a combinação usuário + ranking é identificada pela
    // dupla (id_usuario, dt_concessao), sem alterar a tabela Resgate.
    // ------------------------------------------------------------------

    /** Taxa de conversão pontos -> reais, a mesma de Carteira. */
    private static final double REAIS_POR_PONTO = 0.009052;

    /**
     * Informa se o usuário já tem resgate na data de concessão (a data final do ranking).
     */
    public boolean existePorUsuarioEData(int idUsuario, LocalDate data) {
        try (Connection con = ConnectionFactory.obterConexao()) {
            return existe(con, idUsuario, data);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Informa se já existe OUTRO resgate do usuário na data (usuário + data identifica o prêmio de
     * um ranking). Usado ao cadastrar/alterar à mão, para não duplicar o prêmio.
     *
     * @param ignorarIdResgate resgate a desconsiderar (o que está sendo alterado); use 0 para nenhum
     * @throws IllegalStateException se o banco falhar (na dúvida, não se libera o cadastro)
     */
    public boolean existePorUsuarioEData(int idUsuario, LocalDate data, int ignorarIdResgate) {
        String sql = "SELECT COUNT(*) FROM Resgate WHERE id_usuario = ? AND dt_concessao = ? AND id_resgate <> ?";
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setDate(2, Date.valueOf(data));
            ps.setInt(3, ignorarIdResgate);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível verificar o resgate do usuário.", e);
        }
    }

    private boolean existe(Connection con, int idUsuario, LocalDate data) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Resgate WHERE id_usuario = ? AND dt_concessao = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setDate(2, Date.valueOf(data));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Concede o bônus da recompensa do resgate (percentual do saldo mais pontos fixos, ver
     * {@link Recompensa#calcularBonus(long)}) à carteira e registra o resgate na MESMA transação:
     * ou grava os dois ou nenhum. Repetir não concede de novo (JA_CONCEDIDA).
     *
     * @return CONCEDIDA, JA_CONCEDIDA (já havia resgate na data) ou NAO_CONCEDIDA (sem carteira ou erro)
     */
    public PosicaoRanking.Resultado concederBonus(Resgate resgate) {
        return executarEmTransacao(resgate, con -> {
            long saldo;
            String sqlSaldo = "SELECT nr_saldo_pontos FROM Carteira WHERE id_usuario = ? FOR UPDATE";
            try (PreparedStatement ps = con.prepareStatement(sqlSaldo)) {
                ps.setInt(1, resgate.getUsuario().getIdUsuario());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return PosicaoRanking.Resultado.NAO_CONCEDIDA;
                    }
                    saldo = rs.getInt(1);
                }
            }

            long novoSaldo = saldo + resgate.getRecompensa().calcularBonus(saldo);
            String sqlCarteira = "UPDATE Carteira SET nr_saldo_pontos = ?, vl_pontos_convertidos = ? "
                    + "WHERE id_usuario = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlCarteira)) {
                ps.setLong(1, novoSaldo);
                ps.setDouble(2, novoSaldo * REAIS_POR_PONTO);
                ps.setInt(3, resgate.getUsuario().getIdUsuario());
                ps.executeUpdate();
            }

            inserir(con, resgate);
            return PosicaoRanking.Resultado.CONCEDIDA;
        });
    }

    /**
     * Quita a conta de luz (pagamento integral) e registra o resgate na MESMA
     * transação. A carteira não é tocada. Dentro da transação a conta é travada e o valor
     * restante é conferido de novo: se outro pagamento já reduziu a dívida, o prêmio não é
     * concedido (nada é pago a mais) e pode ser repetido depois com o valor atualizado.
     *
     * @return CONCEDIDA, JA_CONCEDIDA ou NAO_CONCEDIDA (conta já paga em parte/inexistente ou erro de banco)
     */
    public PosicaoRanking.Resultado concederPagamentoConta(Resgate resgate, PagamentoConta pagamento) {
        return executarEmTransacao(resgate, con -> {
            int idConta = pagamento.getContaLuz().getIdContaLuz();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_conta_luz FROM Conta_Luz WHERE id_conta_luz = ? FOR UPDATE")) {
                ps.setInt(1, idConta);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return PosicaoRanking.Resultado.NAO_CONCEDIDA;
                    }
                }
            }
            String sqlRestante = "SELECT c.vl_conta - NVL((SELECT SUM(p.vl_pago) FROM Pagamento_Conta p "
                    + "WHERE p.id_conta_luz = c.id_conta_luz), 0) FROM Conta_Luz c WHERE c.id_conta_luz = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlRestante)) {
                ps.setInt(1, idConta);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getDouble(1) + 0.005 < pagamento.getValorPago()) {
                        return PosicaoRanking.Resultado.NAO_CONCEDIDA;
                    }
                }
            }

            new PagamentoContaDAO().cadastrar(con, pagamento);
            inserir(con, resgate);
            return PosicaoRanking.Resultado.CONCEDIDA;
        });
    }

    /** Operação de banco que usa a conexão da transação. */
    private interface OperacaoTransacional {
        PosicaoRanking.Resultado executar(Connection con) throws SQLException;
    }

    /**
     * Abre a transação, confirma se tudo der certo e desfaz tudo se algo falhar.
     * Primeiro trava a linha do usuário (FOR UPDATE) e só então confere se o
     * resgate já existe: duas chamadas simultâneas para o mesmo usuário ficam em
     * fila, e a segunda enxerga o resgate da primeira, então o prêmio nunca sai duas vezes.
     */
    private PosicaoRanking.Resultado executarEmTransacao(Resgate resgate, OperacaoTransacional operacao) {
        int idUsuario = resgate.getUsuario().getIdUsuario();
        try (Connection con = ConnectionFactory.obterConexao()) {
            try {
                con.setAutoCommit(false);

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT id_usuario FROM Usuario WHERE id_usuario = ? FOR UPDATE")) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return PosicaoRanking.Resultado.NAO_CONCEDIDA;
                        }
                    }
                }

                if (existe(con, idUsuario, resgate.getDataConcessao())) {
                    con.rollback();
                    return PosicaoRanking.Resultado.JA_CONCEDIDA;
                }

                PosicaoRanking.Resultado resultado = operacao.executar(con);
                if (resultado == PosicaoRanking.Resultado.CONCEDIDA) {
                    con.commit();
                } else {
                    con.rollback();
                }
                return resultado;
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                return PosicaoRanking.Resultado.NAO_CONCEDIDA;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return PosicaoRanking.Resultado.NAO_CONCEDIDA;
        }
    }

    private void inserir(Connection con, Resgate resgate) throws SQLException {
        String sql = "INSERT INTO Resgate (dt_concessao, id_usuario, id_recompensa) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(resgate.getDataConcessao()));
            ps.setInt(2, resgate.getUsuario().getIdUsuario());
            ps.setInt(3, resgate.getRecompensa().getIdRecompensa());
            ps.executeUpdate();
        }
    }

    private Resgate mapear(ResultSet rs) throws SQLException {
        Resgate resgate = new Resgate();
        resgate.setIdResgate(rs.getInt("id_resgate"));

        Date dtConcessao = rs.getDate("dt_concessao");
        if (dtConcessao != null) {
            resgate.setDataConcessao(dtConcessao.toLocalDate());
        }

        resgate.setUsuario(mapearUsuario(rs));
        resgate.setRecompensa(mapearRecompensa(rs));
        return resgate;
    }

    /** Monta a recompensa a partir das colunas rc_*; devolve null se não existir. */
    private Recompensa mapearRecompensa(ResultSet rs) throws SQLException {
        int idRecompensa = rs.getInt("rc_id");
        if (rs.wasNull()) {
            return null;
        }
        Recompensa recompensa = new Recompensa();
        recompensa.setIdRecompensa(idRecompensa);
        recompensa.setBonusPercentual(rs.getInt("rc_percentual"));
        recompensa.setBonusPontos(rs.getInt("rc_pontos"));
        recompensa.setDescricao(rs.getString("rc_descricao"));
        return recompensa;
    }

    /** Monta o usuário comum a partir das colunas u_*; devolve null se não existir. */
    private UsuarioComum mapearUsuario(ResultSet rs) throws SQLException {
        int idUsuario = rs.getInt("u_id");
        if (rs.wasNull()) {
            return null;
        }
        UsuarioComum usuario = new UsuarioComum();
        usuario.setIdUsuario(idUsuario);
        usuario.setNome(rs.getString("u_nome"));
        usuario.setCpf(rs.getString("u_cpf"));
        usuario.setDataNascimento(rs.getDate("u_nascimento").toLocalDate());
        usuario.setEmail(rs.getString("u_email"));
        usuario.setSenha(rs.getString("u_senha"));
        usuario.setNivelAcesso(rs.getInt("u_nivel"));
        usuario.setStatusConta(StatusContaEnum.valueOf(rs.getString("u_status")));

        // Igual ao UsuarioComumDAO.buscarPorId: só guarda o id do grupo do usuário.
        int idGrupo = rs.getInt("u_grupo");
        if (idGrupo > 0) {
            Grupo grupo = new Grupo();
            grupo.setIdGrupo(idGrupo);
            usuario.setGrupo(grupo);
        }
        return usuario;
    }
}
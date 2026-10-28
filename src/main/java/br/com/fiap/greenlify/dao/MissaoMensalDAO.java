package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.CategoriaEnum;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;
import br.com.fiap.greenlify.models.Missao;
import br.com.fiap.greenlify.models.MissaoMensal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link MissaoMensal}, envolvendo as tabelas
 * {@code Missao} e {@code Missao_Mensal}.
 */
public class MissaoMensalDAO {
    /**
     * Cadastra uma nova missão mensal, inserindo os dados básicos na
     * tabela {@code Missao} e os dados específicos na tabela
     * {@code Missao_Mensal}, dentro de uma mesma transação.
     *
     * @param missao dados básicos da missão a ser cadastrada
     * @param mensal dados específicos da missão mensal
     * @throws IllegalArgumentException se as datas de início e fim não foram informadas
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public void cadastrarMissao(Missao missao, MissaoMensal mensal) {
        if (mensal.getDataInicio() == null || mensal.getDataFim() == null) {
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias.");
        }

        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement psMissao = null;
        PreparedStatement psMensal = null;

        try {
            conexao.setAutoCommit(false);

            psMissao = conexao.prepareStatement("INSERT INTO Missao (nm_missao, ds_missao, ds_requisitos, " +
                            "ds_comprovacao, ds_categoria, ds_tipo_missao) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[]{"ID_MISSAO"});
            psMissao.setString(1, missao.getNome());
            psMissao.setString(2, missao.getDescricao());
            psMissao.setString(3, missao.getRequisitos());
            psMissao.setString(4, missao.getComoComprovar());
            psMissao.setString(5, missao.getCategoria().toString());
            psMissao.setString(6, missao.getTipoMissao().toString());
            psMissao.executeUpdate();

            ResultSet rsGerado = psMissao.getGeneratedKeys();
            if (rsGerado.next()) {
                missao.setIdMissao(rsGerado.getInt(1));
                mensal.setIdMissao(rsGerado.getInt(1));
            }
            rsGerado.close();

            psMensal = conexao.prepareStatement("INSERT INTO Missao_Mensal (id_missao, vl_meta, dt_inicio, dt_fim) VALUES (?, ?, ?, ?)");
            psMensal.setInt(1, missao.getIdMissao());
            psMensal.setInt(2, mensal.getMeta());
            psMensal.setDate(3, Date.valueOf(mensal.getDataInicio()));
            psMensal.setDate(4, Date.valueOf(mensal.getDataFim()));
            psMensal.executeUpdate();

            conexao.commit();

            psMissao.close();
            psMensal.close();

        } catch (SQLException e) {
            try {
                if (conexao != null) {
                    conexao.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível cadastrar a missão mensal.", e);
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
     * Atualiza os dados de uma missão mensal já cadastrada, alterando
     * tanto os dados básicos da missão quanto os dados específicos da
     * missão mensal, dentro de uma mesma transação.
     *
     * @param mensal dados específicos da missão mensal a serem atualizados
     * @param missao dados básicos da missão a serem atualizados
     * @throws IllegalArgumentException se a missão não existe, as datas não foram informadas ou a
     *                                  meta ou o início mudam (ou o fim encurta) depois que
     *                                  usuários já começaram a missão
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public void alterar(MissaoMensal mensal, Missao missao) {
        if (mensal.getDataInicio() == null || mensal.getDataFim() == null) {
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias.");
        }

        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement psMissao = null;
        PreparedStatement psMensal = null;

        try {
            conexao.setAutoCommit(false);

            // Trava a missão e confere, na mesma transação, se já há usuários nela: meta e período
            // definem o que conta como progresso e conclusão, então não podem mudar com registros
            // feitos. Prorrogar o fim é permitido (não invalida nada do que já foi feito).
            if (!new UsuarioMissaoDAO().travarMissao(conexao, missao.getIdMissao())) {
                throw new IllegalArgumentException("Missão mensal não encontrada");
            }
            MissaoMensal atual = lerRegras(conexao, missao.getIdMissao());
            if (atual == null) {
                throw new IllegalArgumentException("Missão mensal não encontrada");
            }
            boolean mudouRegra = atual.getMeta() != mensal.getMeta()
                    || !mensal.getDataInicio().equals(atual.getDataInicio())
                    || (atual.getDataFim() != null && mensal.getDataFim().isBefore(atual.getDataFim()));
            if (mudouRegra && temParticipacao(conexao, missao.getIdMissao())) {
                throw new IllegalArgumentException(
                        "A meta e a data de início não podem ser alteradas, e a data de fim só pode ser "
                                + "prorrogada: já existem usuários nesta missão.");
            }

            psMissao = conexao.prepareStatement("UPDATE Missao SET nm_missao = ?, ds_missao = ?, ds_requisitos = ?, ds_comprovacao = ?," +
                    " ds_categoria = ?, ds_tipo_missao = ? WHERE id_missao = ?");
            psMissao.setString(1, missao.getNome());
            psMissao.setString(2, missao.getDescricao());
            psMissao.setString(3, missao.getRequisitos());
            psMissao.setString(4, missao.getComoComprovar());
            psMissao.setString(5, missao.getCategoria().toString());
            psMissao.setString(6, missao.getTipoMissao().toString());
            psMissao.setInt(7, missao.getIdMissao());
            psMissao.executeUpdate();

            psMensal = conexao.prepareStatement("UPDATE Missao_Mensal SET vl_meta = ?, dt_inicio = ?, dt_fim = ? WHERE id_missao = ?");
            psMensal.setInt(1, mensal.getMeta());
            psMensal.setDate(2, Date.valueOf(mensal.getDataInicio()));
            psMensal.setDate(3, Date.valueOf(mensal.getDataFim()));
            psMensal.setInt(4, missao.getIdMissao());
            psMensal.executeUpdate();

            conexao.commit();

            psMissao.close();
            psMensal.close();

        } catch (IllegalArgumentException e) {
            try {
                if (conexao != null) {
                    conexao.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw e;
        } catch (SQLException e) {
            try {
                if (conexao != null) {
                    conexao.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível alterar a missão mensal.", e);
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
     * Remove uma missão mensal, excluindo tanto o registro específico na
     * tabela {@code Missao_Mensal} quanto o registro básico na tabela
     * {@code Missao}, dentro de uma mesma transação.
     *
     * @param mensal missão mensal a ser removida
     * @throws IllegalArgumentException se já há usuários na missão (a exclusão apagaria o histórico)
     * @throws IllegalStateException    se o banco falhar (nada é removido)
     */
    public void excluir(MissaoMensal mensal) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement psMissao = null;
        PreparedStatement psMensal = null;

        try {
            conexao.setAutoCommit(false);

            if (new UsuarioMissaoDAO().travarMissao(conexao, mensal.getIdMissao())
                    && temParticipacao(conexao, mensal.getIdMissao())) {
                throw new IllegalArgumentException(
                        "A missão não pode ser excluída: já existem usuários nela.");
            }

            psMensal = conexao.prepareStatement("DELETE FROM Missao_Mensal WHERE id_missao = ?");
            psMensal.setInt(1, mensal.getIdMissao());
            psMensal.executeUpdate();

            psMissao = conexao.prepareStatement("DELETE FROM Missao WHERE id_missao = ?");
            psMissao.setInt(1, mensal.getIdMissao());
            psMissao.executeUpdate();

            conexao.commit();

            psMensal.close();
            psMissao.close();

        } catch (IllegalArgumentException e) {
            try {
                if (conexao != null) {
                    conexao.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw e;
        } catch (SQLException e) {
            try {
                if (conexao != null) {
                    conexao.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir a missão mensal.", e);
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

    /** Meta e período atuais da missão mensal (só os campos de regra), ou {@code null} se não for mensal. */
    private MissaoMensal lerRegras(Connection con, int idMissao) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT vl_meta, dt_inicio, dt_fim FROM Missao_Mensal WHERE id_missao = ?")) {
            ps.setInt(1, idMissao);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                MissaoMensal regras = new MissaoMensal();
                regras.setMeta(rs.getInt("vl_meta"));
                if (rs.getDate("dt_inicio") != null) {
                    regras.setDataInicio(rs.getDate("dt_inicio").toLocalDate());
                }
                if (rs.getDate("dt_fim") != null) {
                    regras.setDataFim(rs.getDate("dt_fim").toLocalDate());
                }
                return regras;
            }
        }
    }

    /** Indica se já há usuário (Usuario_Missao) ligado à missão. */
    private boolean temParticipacao(Connection con, int idMissao) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM Usuario_Missao WHERE id_missao = ?")) {
            ps.setInt(1, idMissao);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Consulta única: Missao + Missao_Mensal (LEFT JOIN, como o antigo buscarPorId). */
    private static final String SELECT_MENSAL =
            "SELECT m.id_missao, m.nm_missao, m.ds_missao, m.ds_requisitos, m.ds_comprovacao, "
                    + "       m.ds_categoria, m.ds_tipo_missao, mm.vl_meta, mm.dt_inicio, mm.dt_fim "
                    + "FROM Missao m "
                    + "LEFT JOIN Missao_Mensal mm ON mm.id_missao = m.id_missao ";

    /**
     * Busca uma missão mensal pelo seu identificador, combinando os
     * dados das tabelas {@code Missao} e {@code Missao_Mensal} em uma única consulta.
     *
     * @param idMissao identificador da missão
     * @return a missão mensal encontrada; os campos permanecem com os
     *         valores padrão caso o identificador não seja encontrado
     */
    public MissaoMensal buscarPorId(Integer idMissao) {
        MissaoMensal mensal = new MissaoMensal();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_MENSAL + "WHERE m.id_missao = ?")) {

            ps.setInt(1, idMissao);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    mensal = mapear(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mensal;
    }

    /**
     * Lista todas as missões mensais cadastradas no banco de dados,
     * usando uma única consulta (uma única conexão).
     *
     * @return lista de missões mensais cadastradas
     */
    public List<MissaoMensal> listar() {
        List<MissaoMensal> mensais = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(
                     SELECT_MENSAL + "WHERE m.ds_tipo_missao = 'MISSAO_MENSAL' ORDER BY m.id_missao");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                mensais.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mensais;
    }

    /** Monta a missão mensal a partir da linha atual do JOIN (sem novas consultas). */
    private MissaoMensal mapear(ResultSet rs) throws SQLException {
        MissaoMensal mensal = new MissaoMensal();
        mensal.setIdMissao(rs.getInt("id_missao"));
        mensal.setNome(rs.getString("nm_missao"));
        mensal.setDescricao(rs.getString("ds_missao"));
        mensal.setRequisitos(rs.getString("ds_requisitos"));
        mensal.setComoComprovar(rs.getString("ds_comprovacao"));
        mensal.setCategoria(CategoriaEnum.valueOf(rs.getString("ds_categoria")));
        mensal.setTipoMissao(TipoMissaoEnum.valueOf(rs.getString("ds_tipo_missao")));
        mensal.setMeta(rs.getInt("vl_meta"));
        if (rs.getDate("dt_inicio") != null) {
            mensal.setDataInicio(rs.getDate("dt_inicio").toLocalDate());
        }
        if (rs.getDate("dt_fim") != null) {
            mensal.setDataFim(rs.getDate("dt_fim").toLocalDate());
        }
        return mensal;
    }
}
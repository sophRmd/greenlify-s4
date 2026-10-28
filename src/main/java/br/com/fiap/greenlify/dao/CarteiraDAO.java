package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.models.Carteira;
import br.com.fiap.greenlify.models.Grupo;
import br.com.fiap.greenlify.models.UsuarioComum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link Carteira} na tabela {@code Carteira}.
 */
public class CarteiraDAO {
    /**
     * Cadastra uma nova carteira, vinculada ao usuário informado.
     *
     * @param carteira carteira a ser cadastrada
     */
    public void cadastrar(Carteira carteira) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Carteira (id_carteira, nr_saldo_pontos, vl_pontos_convertidos, id_usuario) " +
                    "VALUES (?, ?, ?, ?)";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, carteira.getUsuario().getIdUsuario());
            ps.setInt(2, carteira.getQtdPontos());
            ps.setDouble(3, carteira.getValorReal());
            ps.setInt(4, carteira.getUsuario().getIdUsuario());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
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
     * Atualiza os dados de uma carteira já cadastrada.
     *
     * @param carteira carteira com os dados atualizados
     */
    public void alterar(Carteira carteira) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            if (carteira.getUsuario() != null) {
                String sql = "UPDATE Carteira SET nr_saldo_pontos = ?, vl_pontos_convertidos = ?, id_usuario = ? " +
                        "WHERE id_carteira = ?";
                ps = conexao.prepareStatement(sql);
                ps.setInt(1, carteira.getQtdPontos());
                ps.setDouble(2, carteira.getValorReal());
                ps.setInt(3, carteira.getUsuario().getIdUsuario());
                ps.setInt(4, carteira.getIdCarteira());
            } else {
                String sql = "UPDATE Carteira SET nr_saldo_pontos = ?, vl_pontos_convertidos = ? " +
                        "WHERE id_carteira = ?";
                ps = conexao.prepareStatement(sql);
                ps.setInt(1, carteira.getQtdPontos());
                ps.setDouble(2, carteira.getValorReal());
                ps.setInt(3, carteira.getIdCarteira());
            }

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
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
     * Remove uma carteira do banco de dados.
     *
     * @param idCarteira identificador da carteira a ser removida
     */
    public void excluir(int idCarteira) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Carteira WHERE id_carteira = ?");
            ps.setInt(1, idCarteira);

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
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

    /** Carteira + Usuário (u_) em uma única consulta. */
    private static final String SELECT_COMPLETO =
            "SELECT c.id_carteira, c.nr_saldo_pontos, c.vl_pontos_convertidos, "
                    + "       u.id_usuario AS u_id, u.nm_usuario AS u_nome, u.ds_cpf AS u_cpf, "
                    + "       u.dt_nascimento AS u_nascimento, u.ds_email AS u_email, u.ds_senha AS u_senha, "
                    + "       u.nr_nivel_acesso AS u_nivel, u.ds_status_conta AS u_status, u.id_grupo AS u_grupo "
                    + "FROM Carteira c "
                    + "LEFT JOIN Usuario u ON u.id_usuario = c.id_usuario AND u.nr_nivel_acesso = 0 ";

    /**
     * Busca uma carteira pelo seu identificador, em uma única consulta.
     *
     * @param idCarteira identificador da carteira
     * @return a carteira encontrada, ou {@code null} caso não exista
     */
    public Carteira buscarPorId(int idCarteira) {
        return buscarUma(SELECT_COMPLETO + "WHERE c.id_carteira = ?", idCarteira);
    }

    /**
     * Lista todas as carteiras cadastradas no banco de dados, em uma única consulta.
     *
     * @return lista de carteiras cadastradas
     */
    public List<Carteira> listar() {
        List<Carteira> lista = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "ORDER BY c.id_carteira");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Busca a carteira associada a um determinado usuário, em uma única consulta.
     *
     * @param idUsuario identificador do usuário
     * @return a carteira do usuário, ou {@code null} caso não exista
     */
    public Carteira buscarPorIdUsuario(int idUsuario) {
        return buscarUma(SELECT_COMPLETO + "WHERE c.id_usuario = ?", idUsuario);
    }

    private Carteira buscarUma(String sql, int parametro) {
        Carteira carteira = null;

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    carteira = mapear(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return carteira;
    }

    private Carteira mapear(ResultSet rs) throws SQLException {
        Carteira carteira = new Carteira();
        carteira.setIdCarteira(rs.getInt("id_carteira"));
        carteira.setQtdPontos(rs.getInt("nr_saldo_pontos"));
        carteira.setValorReal(rs.getDouble("vl_pontos_convertidos"));
        carteira.setUsuario(mapearUsuario(rs));
        return carteira;
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

    /**
     * Atualiza apenas o saldo de pontos e o respectivo valor convertido em
     * reais de uma carteira, sem alterar o vínculo com o usuário.
     *
     * @param carteira carteira com o novo saldo de pontos
     */
    public void atualizarSaldoPontos(Carteira carteira) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Carteira SET nr_saldo_pontos = ?, vl_pontos_convertidos = ? " +
                    "WHERE id_carteira = ?";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, carteira.getQtdPontos());
            ps.setDouble(2, carteira.getValorReal());
            ps.setInt(3, carteira.getIdCarteira());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
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

    /** Taxa de conversão pontos -> reais, a mesma de Carteira. */
    private static final double REAIS_POR_PONTO = 0.009052;

    /**
     * Soma pontos ao saldo da carteira do usuário em uma única instrução SQL
     * (o saldo novo é calculado pelo próprio banco a partir do saldo atual), então
     * duas operações simultâneas nunca se sobrescrevem. Se o usuário ainda não tem
     * carteira, ela é criada já com os pontos (o id da carteira é o id do usuário,
     * como em {@link #cadastrar(Carteira)}).
     * <p>
     * Usa a conexão recebida, para participar da transação de quem chamou: não faz
     * commit nem fecha a conexão, e erros de SQL sobem para o chamador decidir o rollback.
     *
     * @param conexao   conexão (e transação) já aberta
     * @param idUsuario usuário que recebe os pontos
     * @param pontos    pontos a somar
     */
    public void creditarPontos(Connection conexao, int idUsuario, int pontos) throws SQLException {
        String sql = "UPDATE Carteira SET nr_saldo_pontos = nr_saldo_pontos + ?, "
                + "vl_pontos_convertidos = (nr_saldo_pontos + ?) * ? WHERE id_usuario = ?";
        int atualizadas;
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, pontos);
            ps.setInt(2, pontos);
            ps.setDouble(3, REAIS_POR_PONTO);
            ps.setInt(4, idUsuario);
            atualizadas = ps.executeUpdate();
        }

        if (atualizadas == 0) {
            String insert = "INSERT INTO Carteira (id_carteira, nr_saldo_pontos, vl_pontos_convertidos, id_usuario) "
                    + "VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conexao.prepareStatement(insert)) {
                ps.setInt(1, idUsuario);
                ps.setInt(2, pontos);
                ps.setDouble(3, pontos * REAIS_POR_PONTO);
                ps.setInt(4, idUsuario);
                ps.executeUpdate();
            }
        }
    }

    /**
     * Mesma operação de {@link #creditarPontos(Connection, int, int)}, com conexão
     * própria (uma única instrução, confirmada na hora).
     */
    public void creditarPontos(int idUsuario, int pontos) {
        try (Connection con = ConnectionFactory.obterConexao()) {
            creditarPontos(con, idUsuario, pontos);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
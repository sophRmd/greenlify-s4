package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.models.MissaoMensal;
import br.com.fiap.greenlify.models.Selo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da entidade Selo. Um selo representa uma conquista que o usuário
 * pode obter ao acumular pontos, dando direito a benefícios como
 * vale-entrega e crédito de carbono.
 *
 * <p>Cada registro de Usuario_Selo é um Selo Verde resgatado e conta na pontuação do grupo do
 * usuário. Por isso um selo já resgatado não deve ser apagado (ver {@link #contarResgates}).
 * Falhas de escrita lançam {@link IllegalStateException}.</p>
 */
public class SeloDAO{

    /**
     * Cadastra um novo selo.
     *
     * @param selo dados do selo a ser inserido
     */
    public void cadastrar(Selo selo) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Selo (id_selo, nm_selo, vl_vale_entrega, nr_credito_carbono, nr_pontos_necessarios) " +
                    "VALUES (?, ?, ?, ?, ?)";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, selo.getIdSelo());
            ps.setString(2, selo.getNome());
            ps.setDouble(3, selo.getValeEnergia());
            ps.setDouble(4, selo.getCreditoCarbono());
            ps.setInt(5, selo.getPontosNecessarios());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o selo.", e);
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
     * Atualiza os dados de um selo existente.
     *
     * @param selo objeto com os dados atualizados (o id é usado como referência)
     */
    public void alterar(Selo selo) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Selo SET nm_selo = ?, vl_vale_entrega = ?, nr_credito_carbono = ?, nr_pontos_necessarios = ? " +
                    "WHERE id_selo = ?";
            ps = conexao.prepareStatement(sql);
            ps.setString(1, selo.getNome());
            ps.setDouble(2, selo.getValeEnergia());
            ps.setDouble(3, selo.getCreditoCarbono());
            ps.setInt(4, selo.getPontosNecessarios());
            ps.setInt(5, selo.getIdSelo());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o selo.", e);
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
     * Exclui um selo pelo id.
     *
     * @param idSelo id do selo a ser removido
     */
    public void excluir(int idSelo) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Selo WHERE id_selo = ?");
            ps.setInt(1, idSelo);
            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o selo.", e);
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
     * Quantos resgates (registros de Usuario_Selo) o selo já tem. Enquanto for maior que zero, o selo
     * não pode ser excluído: apagá-lo faria os resgates dos usuários sumirem da pontuação dos grupos.
     *
     * @param idSelo id do selo
     * @throws IllegalStateException se o banco falhar (na dúvida, não se libera a exclusão)
     */
    public int contarResgates(int idSelo) {
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM Usuario_Selo WHERE id_selo = ?")) {

            ps.setInt(1, idSelo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível verificar os resgates do selo.", e);
        }
    }

    /**
     * Busca um selo pelo id.
     *
     * @param idSelo id do selo procurado
     * @return o selo encontrado, ou {@code null} caso não exista
     */
    public Selo buscarPorId(int idSelo) {
        Selo selo = null;

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM Selo WHERE id_selo = ?")) {

            ps.setInt(1, idSelo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    selo = mapear(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return selo;
    }

    /**
     * Lista todos os selos cadastrados, em uma única consulta (uma única conexão).
     *
     * @return lista com todos os selos (pode vir vazia, nunca nula)
     */
    public List<Selo> listar() {
        List<Selo> selos = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM Selo");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                selos.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return selos;
    }

    /** Monta o selo a partir da linha atual (colunas na ordem da tabela). */
    private Selo mapear(ResultSet rs) throws SQLException {
        Selo selo = new Selo();
        selo.setIdSelo(rs.getInt(1));
        selo.setNome(rs.getString(2));
        selo.setValeEnergia(rs.getInt(3));
        selo.setCreditoCarbono(rs.getInt(4));
        selo.setPontosNecessarios(rs.getInt(5));
        return selo;
    }
}
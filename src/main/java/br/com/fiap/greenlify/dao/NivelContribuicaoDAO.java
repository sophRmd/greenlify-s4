package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.models.NivelContribuicao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link NivelContribuicao} na tabela
 * {@code Nivel_Contribuicao}.
 */
public class NivelContribuicaoDAO {

    /**
     * Cadastra um novo nível de contribuição.
     *
     * @param nivel nível de contribuição a ser cadastrado
     */
    public void cadastrar(NivelContribuicao nivel) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Nivel_Contribuicao (id_nivel, nr_nivel, nm_nivel, nr_xp_minimo, nr_xp_maximo) " +
                    "VALUES (?, ?, ?, ?, ?)";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, nivel.getIdNivel());
            ps.setInt(2, nivel.getNivel());
            ps.setString(3, nivel.getNome());
            ps.setInt(4, nivel.getXpMinimo());
            ps.setInt(5, nivel.getXpMaximo());

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
     * Atualiza os dados de um nível de contribuição já cadastrado.
     *
     * @param nivel nível de contribuição com os dados atualizados
     */
    public void alterar(NivelContribuicao nivel) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Nivel_Contribuicao SET nr_nivel = ?, nm_nivel = ?, nr_xp_minimo = ?, nr_xp_maximo = ? " +
                    "WHERE id_nivel = ?";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, nivel.getNivel());
            ps.setString(2, nivel.getNome());
            ps.setInt(3, nivel.getXpMinimo());
            ps.setInt(4, nivel.getXpMaximo());
            ps.setInt(5, nivel.getIdNivel());

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
     * Remove um nível de contribuição do banco de dados.
     *
     * @param idNivel identificador do nível de contribuição a ser removido
     */
    public void excluir(int idNivel) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Nivel_Contribuicao WHERE id_nivel = ?");
            ps.setInt(1, idNivel);
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
     * Busca um nível de contribuição pelo seu identificador.
     *
     * @param idNivel identificador do nível de contribuição
     * @return o nível de contribuição encontrado, ou {@code null} caso não exista
     */
    public NivelContribuicao buscarPorId(int idNivel) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        NivelContribuicao nivel = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Nivel_Contribuicao WHERE id_nivel = ?");
            ps.setInt(1, idNivel);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                nivel = new NivelContribuicao();
                nivel.setIdNivel(rs.getInt("id_nivel"));
                nivel.setNivel(rs.getInt("nr_nivel"));
                nivel.setNome(rs.getString("nm_nivel"));
                nivel.setXpMinimo(rs.getInt("nr_xp_minimo"));
                nivel.setXpMaximo(rs.getInt("nr_xp_maximo"));
            }

            rs.close();
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
        return nivel;
    }

    /**
     * Lista todos os níveis de contribuição cadastrados, ordenados de
     * forma crescente pelo número do nível.
     *
     * @return lista de níveis de contribuição cadastrados
     */
    public List<NivelContribuicao> listar() {
        Connection conexao = ConnectionFactory.obterConexao();
        List<NivelContribuicao> niveis = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Nivel_Contribuicao ORDER BY nr_nivel ASC");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                NivelContribuicao nivel = new NivelContribuicao();
                nivel.setIdNivel(rs.getInt("id_nivel"));
                nivel.setNivel(rs.getInt("nr_nivel"));
                nivel.setNome(rs.getString("nm_nivel"));
                nivel.setXpMinimo(rs.getInt("nr_xp_minimo"));
                nivel.setXpMaximo(rs.getInt("nr_xp_maximo"));

                niveis.add(nivel);
            }

            rs.close();
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
        return niveis;
    }
}
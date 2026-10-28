package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.models.Grupo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link Grupo} na tabela {@code Grupo}.
 *
 * <p>A coluna {@code nr_usuarios} não é fonte confiável do tamanho do grupo (ninguém a atualiza ao
 * entrar/sair): o número de integrantes vem de {@link #contarIntegrantes()}, a partir de
 * {@code Usuario.id_grupo}. Integrantes não se confundem com selos resgatados, que formam a
 * pontuação do ranking de grupos.</p>
 */
public class GrupoDAO {
    /**
     * Cadastra um novo grupo, preenchendo o identificador gerado
     * automaticamente pelo banco de dados no próprio objeto.
     *
     * @param grupo grupo a ser cadastrado
     */
    public void cadastrar(Grupo grupo) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Grupo (nm_grupo, dt_criacao, nr_status_grupo) " +
                    "VALUES (?, ?, ?)";
            ps = conexao.prepareStatement(sql, new String[]{"ID_GRUPO"});
            ps.setString(1, grupo.getNome());
            ps.setDate(2, Date.valueOf(String.valueOf(grupo.getDataCriacao())));
            ps.setInt(3, grupo.getStatusGrupo());

            ps.executeUpdate();

            ResultSet rsGerado = ps.getGeneratedKeys();
            if (rsGerado.next()) {
                grupo.setIdGrupo(rsGerado.getInt(1));
            }
            rsGerado.close();

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
     * Atualiza os dados de um grupo já cadastrado.
     *
     * @param grupo grupo com os dados atualizados
     */
    public void alterar(Grupo grupo) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Grupo SET nm_grupo = ?, dt_criacao = ?, nr_usuarios = ?, nr_status_grupo = ? " +
                    "WHERE id_grupo = ?";
            ps = conexao.prepareStatement(sql);
            ps.setString(1, grupo.getNome());
            ps.setDate(2, Date.valueOf(String.valueOf(grupo.getDataCriacao())));
            ps.setInt(3, grupo.getQtdUsuarios());
            ps.setInt(4, grupo.getStatusGrupo());
            ps.setInt(5, grupo.getIdGrupo());

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
     * Remove um grupo do banco de dados. Na mesma transação, os integrantes ficam sem grupo
     * (id_grupo nulo: deixam de somar selos para ele) e as posições do grupo nos rankings são
     * apagadas; sem isso as chaves estrangeiras impediam a exclusão. Ou sai tudo, ou nada.
     *
     * @param idGrupo identificador do grupo a ser removido
     * @throws IllegalStateException se o banco falhar (nada é alterado)
     */
    public void excluir(int idGrupo) {
        String[] comandos = {
                "UPDATE Usuario SET id_grupo = NULL WHERE id_grupo = ?",
                "DELETE FROM Posicao_Ranking WHERE id_grupo = ?",
                "DELETE FROM Grupo WHERE id_grupo = ?"
        };

        try (Connection conexao = ConnectionFactory.obterConexao()) {
            boolean confirmado = false;
            try {
                conexao.setAutoCommit(false);
                for (String sql : comandos) {
                    try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                        ps.setInt(1, idGrupo);
                        ps.executeUpdate();
                    }
                }
                conexao.commit();
                confirmado = true;
            } finally {
                // Desfaz antes de devolver a conexão: no Oracle, ligar o autocommit com a
                // transação aberta confirmaria tudo.
                if (!confirmado) {
                    conexao.rollback();
                }
                conexao.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir o grupo.", e);
        }
    }

    /**
     * Conta os integrantes de cada grupo (usuários comuns com {@code id_grupo}), em uma consulta.
     * É o tamanho do grupo; não tem relação com a quantidade de selos resgatados.
     *
     * @return mapa idGrupo -> integrantes (grupos vazios não aparecem)
     */
    public Map<Integer, Integer> contarIntegrantes() {
        String sql = "SELECT id_grupo, COUNT(*) AS total FROM Usuario "
                + "WHERE nr_nivel_acesso = 0 AND id_grupo IS NOT NULL GROUP BY id_grupo";
        Map<Integer, Integer> total = new HashMap<>();

        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                total.put(rs.getInt("id_grupo"), rs.getInt("total"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível contar os integrantes dos grupos.", e);
        }
        return total;
    }

    /**
     * Busca um grupo pelo seu identificador.
     *
     * @param idGrupo identificador do grupo
     * @return o grupo encontrado, ou {@code null} caso não exista
     */
    public Grupo buscarPorId(int idGrupo) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        Grupo grupo = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Grupo WHERE id_grupo = ?");
            ps.setInt(1, idGrupo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                grupo = new Grupo();
                grupo.setIdGrupo(rs.getInt(1));
                grupo.setNome(rs.getString(2));
                if (rs.getDate(3) != null) {
                    grupo.setDataCriacao(rs.getDate(3).toLocalDate());
                }
                grupo.setQtdUsuarios(rs.getInt(4));
                grupo.setStatusGrupo(rs.getInt(5));
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
        return grupo;
    }

    /**
     * Lista todos os grupos cadastrados no banco de dados.
     *
     * @return lista de grupos cadastrados
     */
    public List<Grupo> listar() {
        Connection conexao = ConnectionFactory.obterConexao();
        List<Grupo> lista = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Grupo");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Grupo grupo = new Grupo();
                grupo.setIdGrupo(rs.getInt(1));
                grupo.setNome(rs.getString(2));
                if (rs.getDate(3) != null) {
                    grupo.setDataCriacao(rs.getDate(3).toLocalDate());
                }
                grupo.setQtdUsuarios(rs.getInt(4));
                grupo.setStatusGrupo(rs.getInt(5));

                lista.add(grupo);
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
        return lista;
    }
}
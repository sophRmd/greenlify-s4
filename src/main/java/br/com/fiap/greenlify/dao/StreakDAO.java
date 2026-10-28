package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.models.Streak;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência na tabela Sequencia (relacionamento 1 para 1 com Usuario,
 * onde id_sequencia é igual ao id_usuario).
 *
 * <p>A coluna {@code nr_meses_ativos} armazena a sequência atual de meses
 * consecutivos com atividade (equivalente a {@link Streak#getSequenciaAtual()}).</p>
 */
public class StreakDAO {

    /**
     * Cadastra um novo registro na tabela Sequencia para o usuário.
     *
     * @param streak    dados do streak a ser cadastrado
     * @param idUsuario identificador do usuário (usado para id_sequencia e id_usuario)
     * @throws IllegalStateException se o banco falhar
     */
    public void cadastrar(Streak streak, int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Sequencia (id_sequencia, nr_maior_sequencia, nr_meses_ativos, " +
                    "dt_primeira_atividade, dt_ultima_atividade, id_usuario) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            ps = conexao.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ps.setInt(2, streak.getMaiorSequencia());
            ps.setInt(3, streak.getSequenciaAtual());
            ps.setDate(4, streak.getPrimeiraAtividade() != null ? Date.valueOf(streak.getPrimeiraAtividade()) : null);
            ps.setDate(5, streak.getUltimaAtividade() != null ? Date.valueOf(streak.getUltimaAtividade()) : null);
            ps.setInt(6, idUsuario);

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível cadastrar a sequência.", e);
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
     * Atualiza os dados da sequência de um usuário.
     *
     * @param streak    dados atualizados da sequência
     * @param idUsuario identificador do usuário
     * @throws IllegalStateException se o banco falhar
     */
    public void atualizar(Streak streak, int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Sequencia SET nr_maior_sequencia = ?, nr_meses_ativos = ?, " +
                    "dt_primeira_atividade = ?, dt_ultima_atividade = ? " +
                    "WHERE id_sequencia = ?";

            ps = conexao.prepareStatement(sql);
            ps.setInt(1, streak.getMaiorSequencia());
            ps.setInt(2, streak.getSequenciaAtual());
            ps.setDate(3, streak.getPrimeiraAtividade() != null ? Date.valueOf(streak.getPrimeiraAtividade()) : null);
            ps.setDate(4, streak.getUltimaAtividade() != null ? Date.valueOf(streak.getUltimaAtividade()) : null);
            ps.setInt(5, idUsuario);

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível atualizar a sequência.", e);
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
     * Busca a sequência pelo identificador do usuário.
     *
     * @param idUsuario identificador do usuário
     * @return o objeto Streak preenchido, ou null caso não seja encontrado
     */
    public Streak buscarPorIdUsuario(int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        Streak streak = null;
        PreparedStatement ps = null;

        try {
            String sql = "SELECT * FROM Sequencia WHERE id_sequencia = ?";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                streak = new Streak();
                streak.setIdStreak(rs.getInt("id_sequencia"));
                streak.setMaiorSequencia(rs.getInt("nr_maior_sequencia"));
                streak.setSequenciaAtual(rs.getInt("nr_meses_ativos"));

                if (rs.getDate("dt_primeira_atividade") != null) {
                    streak.setPrimeiraAtividade(rs.getDate("dt_primeira_atividade").toLocalDate());
                }
                if (rs.getDate("dt_ultima_atividade") != null) {
                    streak.setUltimaAtividade(rs.getDate("dt_ultima_atividade").toLocalDate());
                }
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
        return streak;
    }

    /**
     * Busca a sequência do usuário; caso não exista registro, cria um novo zerado e o retorna.
     *
     * @param idUsuario identificador do usuário
     * @return a sequência existente ou recém-criada
     */
    public Streak buscarOuCriar(int idUsuario) {
        Streak streak = buscarPorIdUsuario(idUsuario);

        if (streak == null) {
            streak = new Streak();
            streak.setIdStreak(idUsuario);
            streak.setMaiorSequencia(0);
            streak.setSequenciaAtual(0);

            try {
                cadastrar(streak, idUsuario);
            } catch (IllegalStateException e) {
                // outra chamada pode ter criado a sequência ao mesmo tempo: usa a que já existe
                Streak criada = buscarPorIdUsuario(idUsuario);
                if (criada == null) {
                    throw e;
                }
                return criada;
            }
        }

        return streak;
    }

    /**
     * Registra a conclusão de uma missão mensal na sequência do usuário, na conexão (transação) de
     * quem chamou: não faz commit nem fecha a conexão, e qualquer erro de SQL sobe para o rollback.
     * É o único caminho que atualiza o streak por evento (a conclusão mensal), então a regra
     * fica só no model {@link Streak#registrarAtividadeMensal}. Trava a linha da sequência
     * (FOR UPDATE) e cria o registro zerado se ainda não existir.
     *
     * @return {@code true} se a sequência mudou; {@code false} se o mês já estava contado
     */
    public boolean registrarAtividadeMensal(Connection con, int idUsuario, LocalDate dia) throws SQLException {
        Streak streak = null;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT nr_maior_sequencia, nr_meses_ativos, dt_primeira_atividade, dt_ultima_atividade "
                        + "FROM Sequencia WHERE id_sequencia = ? FOR UPDATE")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    streak = new Streak();
                    streak.setIdStreak(idUsuario);
                    streak.setMaiorSequencia(rs.getInt("nr_maior_sequencia"));
                    streak.setSequenciaAtual(rs.getInt("nr_meses_ativos"));
                    if (rs.getDate("dt_primeira_atividade") != null) {
                        streak.setPrimeiraAtividade(rs.getDate("dt_primeira_atividade").toLocalDate());
                    }
                    if (rs.getDate("dt_ultima_atividade") != null) {
                        streak.setUltimaAtividade(rs.getDate("dt_ultima_atividade").toLocalDate());
                    }
                }
            }
        }

        if (streak == null) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Sequencia (id_sequencia, nr_maior_sequencia, nr_meses_ativos, "
                            + "dt_primeira_atividade, dt_ultima_atividade, id_usuario) VALUES (?, 0, 0, NULL, NULL, ?)")) {
                ps.setInt(1, idUsuario);
                ps.setInt(2, idUsuario);
                ps.executeUpdate();
            }
            streak = new Streak();
            streak.setIdStreak(idUsuario);
        }

        if (!streak.registrarAtividadeMensal(dia)) {
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Sequencia SET nr_maior_sequencia = ?, nr_meses_ativos = ?, "
                        + "dt_primeira_atividade = ?, dt_ultima_atividade = ? WHERE id_sequencia = ?")) {
            ps.setInt(1, streak.getMaiorSequencia());
            ps.setInt(2, streak.getSequenciaAtual());
            ps.setDate(3, streak.getPrimeiraAtividade() != null ? Date.valueOf(streak.getPrimeiraAtividade()) : null);
            ps.setDate(4, streak.getUltimaAtividade() != null ? Date.valueOf(streak.getUltimaAtividade()) : null);
            ps.setInt(5, idUsuario);
            ps.executeUpdate();
        }
        return true;
    }
}
package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.DificuldadeEnum;
import br.com.fiap.greenlify.enums.ImpactoEnum;
import br.com.fiap.greenlify.models.Pontuacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link Pontuacao} na tabela {@code Pontuacao}.
 */
public class PontuacaoDAO {

    /**
     * Cadastra uma nova pontuação. O valor gravado precisa corresponder ao impacto e à dificuldade.
     *
     * @param pontuacao pontuação a ser cadastrada
     * @throws IllegalArgumentException se o valor não corresponde ao impacto e à dificuldade
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public void cadastrar(Pontuacao pontuacao) {
        exigirCorrespondencia(pontuacao);
        String sql = "INSERT INTO Pontuacao (id_pontuacao, ds_impacto, ds_dificuldade, nr_pontuacao) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pontuacao.getIdPontuacao());
            ps.setString(2, pontuacao.getImpacto().toString());
            ps.setString(3, pontuacao.getDificuldade().toString());
            ps.setInt(4, pontuacao.getValorBase());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível cadastrar a pontuação.", e);
        }
    }

    /**
     * Atualiza os dados de uma pontuação já cadastrada. O valor gravado precisa corresponder ao
     * impacto e à dificuldade.
     *
     * @param pontuacao pontuação com os dados atualizados
     * @throws IllegalArgumentException se a pontuação não existe ou o valor não corresponde ao cálculo
     * @throws IllegalStateException    se o banco falhar
     */
    public void alterar(Pontuacao pontuacao) {
        exigirCorrespondencia(pontuacao);
        String sql = "UPDATE Pontuacao SET ds_impacto = ?, ds_dificuldade = ?, nr_pontuacao = ? " +
                "WHERE id_pontuacao = ?";
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pontuacao.getImpacto().toString());
            ps.setString(2, pontuacao.getDificuldade().toString());
            ps.setInt(3, pontuacao.getValorBase());
            ps.setInt(4, pontuacao.getIdPontuacao());
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Pontuação não encontrada");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível alterar a pontuação.", e);
        }
    }

    /**
     * Remove uma pontuação do banco de dados, desde que nenhuma missão contínua a use.
     *
     * @param idPontuacao identificador da pontuação a ser removida
     * @throws IllegalArgumentException se alguma missão contínua usa a pontuação
     * @throws IllegalStateException    se o banco falhar
     */
    public void excluir(int idPontuacao) {
        try (Connection con = ConnectionFactory.obterConexao()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM Missao_Continua WHERE id_pontuacao = ?")) {
                ps.setInt(1, idPontuacao);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalArgumentException(
                                "A pontuação não pode ser excluída: há missões que a utilizam.");
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM Pontuacao WHERE id_pontuacao = ?")) {
                ps.setInt(1, idPontuacao);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir a pontuação.", e);
        }
    }

    private void exigirCorrespondencia(Pontuacao pontuacao) {
        if (!pontuacao.correspondeAoCalculo()) {
            throw new IllegalArgumentException(
                    "O valor da pontuação não corresponde ao impacto e à dificuldade informados.");
        }
    }

    /**
     * Monta a pontuação da linha atual (id, impacto, dificuldade, valor). O valor gravado é mantido
     * (é ele que credita pontos), mas se não bate com impacto e dificuldade o aviso fica registrado.
     */
    private Pontuacao mapear(ResultSet rs) throws SQLException {
        Pontuacao pontuacao = new Pontuacao();
        pontuacao.setIdPontuacao(rs.getInt(1));
        pontuacao.setImpacto(ImpactoEnum.valueOf(rs.getString(2)));
        pontuacao.setDificuldade(DificuldadeEnum.valueOf(rs.getString(3)));
        pontuacao.setValorBase(rs.getInt(4));
        if (!pontuacao.correspondeAoCalculo()) {
            System.err.println("ATENÇÃO: a pontuação " + pontuacao.getIdPontuacao()
                    + " tem valor " + pontuacao.getValorBase() + ", diferente do calculado para "
                    + pontuacao.getImpacto() + "/" + pontuacao.getDificuldade() + ".");
        }
        return pontuacao;
    }

    /**
     * Busca uma pontuação pelo seu identificador.
     *
     * @param idPontuacao identificador da pontuação
     * @return a pontuação encontrada, ou {@code null} caso não exista
     */
    public Pontuacao buscarPorId(int idPontuacao) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        Pontuacao pontuacao = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Pontuacao WHERE id_pontuacao = ?");
            ps.setInt(1, idPontuacao);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                pontuacao = mapear(rs);
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
        return pontuacao;
    }

    /**
     * Busca a pontuação correspondente a uma combinação específica de
     * impacto e dificuldade.
     *
     * @param impacto     nível de impacto ambiental da missão
     * @param dificuldade nível de dificuldade da missão
     * @return a pontuação encontrada, ou {@code null} caso não exista
     */
    public Pontuacao buscarPorImpactoEDificuldade(String impacto, String dificuldade) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        Pontuacao pontuacao = null;

        try {
            String sql = "SELECT * FROM Pontuacao WHERE ds_impacto = ? AND ds_dificuldade = ?";
            ps = conexao.prepareStatement(sql);
            ps.setString(1, impacto);
            ps.setString(2, dificuldade);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                pontuacao = mapear(rs);
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
        return pontuacao;
    }

    /**
     * Lista todas as pontuações cadastradas no banco de dados, em uma única
     * consulta (uma única conexão).
     *
     * @return lista de pontuações cadastradas
     */
    public List<Pontuacao> listar() {
        List<Pontuacao> pontuacoes = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM Pontuacao");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                pontuacoes.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pontuacoes;
    }
}
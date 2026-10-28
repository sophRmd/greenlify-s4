package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.models.ContaLuz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link ContaLuz} na tabela {@code Conta_Luz}.
 *
 * <p>Falhas de escrita lançam {@link IllegalStateException}, para quem chamou não responder
 * "sucesso" quando nada foi gravado.</p>
 */
public class ContaLuzDAO {

    /**
     * Cadastra uma nova conta de luz.
     *
     * @param conta conta de luz a ser cadastrada
     */
    public void cadastrar(ContaLuz conta) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Conta_Luz (id_conta_luz, ds_codigo_barra, vl_conta) VALUES (?, ?, ?)";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, conta.getIdContaLuz());
            ps.setString(2, conta.getCodigoBarras());
            ps.setDouble(3, conta.getValorConta());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível cadastrar a conta de luz.", e);
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
     * Atualiza os dados de uma conta de luz já cadastrada.
     *
     * @param conta conta de luz com os dados atualizados
     */
    public void alterar(ContaLuz conta) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Conta_Luz SET ds_codigo_barra = ?, vl_conta = ? WHERE id_conta_luz = ?";
            ps = conexao.prepareStatement(sql);
            ps.setString(1, conta.getCodigoBarras());
            ps.setDouble(2, conta.getValorConta());
            ps.setInt(3, conta.getIdContaLuz());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível alterar a conta de luz.", e);
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
     * Remove uma conta de luz do banco de dados.
     *
     * @param idContaLuz identificador da conta de luz a ser removida
     */
    public void excluir(int idContaLuz) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Conta_Luz WHERE id_conta_luz = ?");
            ps.setInt(1, idContaLuz);
            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir a conta de luz.", e);
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
     * Busca uma conta de luz pelo seu identificador.
     *
     * @param idContaLuz identificador da conta de luz
     * @return a conta de luz encontrada, ou {@code null} caso não exista
     */
    public ContaLuz buscarPorId(int idContaLuz) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        ContaLuz conta = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Conta_Luz WHERE id_conta_luz = ?");
            ps.setInt(1, idContaLuz);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                conta = new ContaLuz();
                conta.setIdContaLuz(rs.getInt(1));
                conta.setCodigoBarras(rs.getString(2));
                conta.setValorConta(rs.getDouble(3));
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
        return conta;
    }

    /**
     * Lista todas as contas de luz cadastradas no banco de dados.
     *
     * @return lista de contas de luz cadastradas
     */
    public List<ContaLuz> listar() {
        Connection conexao = ConnectionFactory.obterConexao();
        List<ContaLuz> contas = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Conta_Luz");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ContaLuz conta = new ContaLuz();
                conta.setIdContaLuz(rs.getInt(1));
                conta.setCodigoBarras(rs.getString(2));
                conta.setValorConta(rs.getDouble(3));

                contas.add(conta);
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
        return contas;
    }

    /**
     * Busca a conta de luz pendente de um usuário, usada no prêmio do 1º lugar
     * do ranking (pagamento integral da conta).
     * <p>
     * A tabela Conta_Luz não guarda o dono da conta; o vínculo usuário + conta
     * existe em Pagamento_Conta. Por isso, a conta pendente do usuário é a mais
     * recente conta em que ele já fez algum pagamento e cujo total pago (por
     * todos os pagamentos) ainda é menor que o valor da conta.
     * <p>
     * O objeto devolvido traz em {@code valorConta} o valor <b>restante</b>
     * (valor da conta menos o já pago), para que o prêmio quite a conta sem pagar
     * a mais. Nenhuma alteração é feita no banco.
     *
     * @param idUsuario identificador do usuário
     * @return a conta pendente (com o valor restante), ou {@code null} se não houver
     */
    public ContaLuz buscarPendenteDoUsuario(int idUsuario) {
        String sql = "SELECT * FROM ("
                + "  SELECT c.id_conta_luz, c.ds_codigo_barra, "
                + "         c.vl_conta - NVL((SELECT SUM(p.vl_pago) FROM Pagamento_Conta p "
                + "                           WHERE p.id_conta_luz = c.id_conta_luz), 0) AS vl_restante "
                + "  FROM Conta_Luz c "
                + "  WHERE EXISTS (SELECT 1 FROM Pagamento_Conta pu "
                + "                WHERE pu.id_conta_luz = c.id_conta_luz AND pu.id_usuario = ?) "
                + "    AND c.vl_conta > NVL((SELECT SUM(p2.vl_pago) FROM Pagamento_Conta p2 "
                + "                          WHERE p2.id_conta_luz = c.id_conta_luz), 0) "
                + "  ORDER BY c.id_conta_luz DESC"
                + ") WHERE ROWNUM = 1";
        ContaLuz conta = null;

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    conta = new ContaLuz();
                    conta.setIdContaLuz(rs.getInt(1));
                    conta.setCodigoBarras(rs.getString(2));
                    conta.setValorConta(rs.getDouble(3));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conta;
    }
}
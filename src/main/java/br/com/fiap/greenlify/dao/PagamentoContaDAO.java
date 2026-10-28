package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.models.ContaLuz;
import br.com.fiap.greenlify.models.PagamentoConta;
import br.com.fiap.greenlify.models.Usuario;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da entidade PagamentoConta. Cada pagamento está associado a um usuário
 * e a uma conta de luz (colunas id_usuario e id_conta_luz da tabela Pagamento_Conta).
 *
 * Falhas de escrita lançam {@link IllegalStateException}, para quem chamou não responder
 * "sucesso" quando nada foi gravado.
 *
 * Ordem das colunas assumida na tabela, usada na leitura por índice:
 * 1 id_pagamento, 2 dt_pagamento, 3 vl_pago, 4 id_usuario, 5 id_conta_luz.
 */
public class PagamentoContaDAO {

    /**
     * Cadastra um novo pagamento. O id_pagamento é gerado pelo banco (auto increment),
     * então fica de fora do INSERT e, ao final, é preenchido no próprio objeto.
     *
     * @param pagamento dados do pagamento a ser cadastrado
     */
    public void cadastrar(PagamentoConta pagamento) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Pagamento_Conta (dt_pagamento, vl_pago, id_usuario, id_conta_luz) " +
                    "VALUES (?, ?, ?, ?)";
            ps = conexao.prepareStatement(sql, new String[]{"ID_PAGAMENTO"});
            ps.setDate(1, Date.valueOf(pagamento.getDataPagamento()));
            ps.setDouble(2, pagamento.getValorPago());
            ps.setInt(3, pagamento.getUsuario().getIdUsuario());
            ps.setInt(4, pagamento.getContaLuz().getIdContaLuz());

            ps.executeUpdate();

            ResultSet rsGerado = ps.getGeneratedKeys();
            if (rsGerado.next()) {
                pagamento.setIdPagamento(rsGerado.getInt(1));
            }
            rsGerado.close();

            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o pagamento.", e);
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
     * Cadastra um pagamento usando uma conexão já aberta (para participar de uma
     * transação de quem chamou). Não faz commit nem fecha a conexão; erros de SQL
     * sobem para o chamador decidir o rollback.
     */
    public void cadastrar(Connection conexao, PagamentoConta pagamento) throws SQLException {
        String sql = "INSERT INTO Pagamento_Conta (dt_pagamento, vl_pago, id_usuario, id_conta_luz) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql, new String[]{"ID_PAGAMENTO"})) {
            ps.setDate(1, Date.valueOf(pagamento.getDataPagamento()));
            ps.setDouble(2, pagamento.getValorPago());
            ps.setInt(3, pagamento.getUsuario().getIdUsuario());
            ps.setInt(4, pagamento.getContaLuz().getIdContaLuz());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    pagamento.setIdPagamento(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Atualiza os dados de um pagamento já existente.
     *
     * @param pagamento dados atualizados do pagamento (o id é usado como referência)
     */
    public void alterar(PagamentoConta pagamento) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Pagamento_Conta SET dt_pagamento = ?, vl_pago = ?, " +
                    "id_usuario = ?, id_conta_luz = ? WHERE id_pagamento = ?";
            ps = conexao.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(pagamento.getDataPagamento()));
            ps.setDouble(2, pagamento.getValorPago());
            ps.setInt(3, pagamento.getUsuario().getIdUsuario());
            ps.setInt(4, pagamento.getContaLuz().getIdContaLuz());
            ps.setInt(5, pagamento.getIdPagamento());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível alterar o pagamento.", e);
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
     * Exclui um pagamento pelo id.
     *
     * @param idPagamento id do pagamento a ser removido
     */
    public void excluir(int idPagamento) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Pagamento_Conta WHERE id_pagamento = ?");
            ps.setInt(1, idPagamento);
            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir o pagamento.", e);
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
     * Busca um pagamento pelo id.
     *
     * @param idPagamento id do pagamento procurado
     * @return o pagamento encontrado, ou {@code null} caso não exista
     */
    public PagamentoConta buscarPorId(int idPagamento) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        PagamentoConta pagamento = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Pagamento_Conta WHERE id_pagamento = ?");
            ps.setInt(1, idPagamento);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                pagamento = new PagamentoConta();
                pagamento.setIdPagamento(rs.getInt(1));
                pagamento.setDataPagamento(rs.getDate(2).toLocalDate());
                pagamento.setValorPago(rs.getDouble(3));

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt(4));
                pagamento.setUsuario(usuario);

                ContaLuz contaLuz = new ContaLuz();
                contaLuz.setIdContaLuz(rs.getInt(5));
                pagamento.setContaLuz(contaLuz);
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
        return pagamento;
    }

    /**
     * Lista todos os pagamentos cadastrados, do mais recente para o mais antigo.
     *
     * @return lista de pagamentos (pode vir vazia, nunca nula)
     */
    public List<PagamentoConta> listar() {
        Connection conexao = ConnectionFactory.obterConexao();
        List<PagamentoConta> pagamentos = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Pagamento_Conta ORDER BY dt_pagamento DESC");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PagamentoConta pagamento = new PagamentoConta();

                pagamento.setIdPagamento(rs.getInt(1));
                pagamento.setDataPagamento(rs.getDate(2).toLocalDate());
                pagamento.setValorPago(rs.getDouble(3));

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt(4));
                pagamento.setUsuario(usuario);

                ContaLuz contaLuz = new ContaLuz();
                contaLuz.setIdContaLuz(rs.getInt(5));
                pagamento.setContaLuz(contaLuz);

                pagamentos.add(pagamento);
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
        return pagamentos;
    }

    /**
     * Lista todos os pagamentos feitos por um usuário, do mais recente para o mais antigo.
     *
     * @param idUsuario id do usuário dono dos pagamentos
     * @return lista de pagamentos do usuário (pode vir vazia, nunca nula)
     */
    public List<PagamentoConta> listarPorUsuario(int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        List<PagamentoConta> pagamentos = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement(
                    "SELECT * FROM Pagamento_Conta WHERE id_usuario = ? ORDER BY dt_pagamento DESC");
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                PagamentoConta pagamento = new PagamentoConta();

                pagamento.setIdPagamento(rs.getInt(1));
                pagamento.setDataPagamento(rs.getDate(2).toLocalDate());
                pagamento.setValorPago(rs.getDouble(3));

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt(4));
                pagamento.setUsuario(usuario);

                ContaLuz contaLuz = new ContaLuz();
                contaLuz.setIdContaLuz(rs.getInt(5));
                pagamento.setContaLuz(contaLuz);

                pagamentos.add(pagamento);
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
        return pagamentos;
    }
}
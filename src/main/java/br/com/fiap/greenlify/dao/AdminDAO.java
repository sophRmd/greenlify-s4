package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.models.Admin;
import br.com.fiap.greenlify.models.Grupo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link Admin} na tabela {@code Usuario}, filtrando
 * sempre pelo nível de acesso de administrador.
 * <p>
 * O modelo {@link Admin} não possui CPF nem data de nascimento (esses
 * atributos existem apenas em {@link br.com.fiap.greenlify.models.UsuarioComum}),
 * mas as colunas {@code ds_cpf} e {@code dt_nascimento} são NOT NULL (e
 * {@code ds_cpf} é UNIQUE) no banco. Por isso, ao cadastrar um admin, esse
 * DAO grava valores de preenchimento (placeholder) únicos por usuário,
 * derivados do próprio {@code id_usuario}, e nunca mais toca nessas
 * colunas depois — não há necessidade de atualizá-las em {@link #alterar}.
 */
public class AdminDAO {

    /**
     * Cadastra um novo administrador no banco de dados.
     *
     * @param usuario administrador a ser cadastrado
     */
    public void cadastrar(Admin usuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Usuario (id_usuario, nm_usuario, ds_cpf, dt_nascimento, " +
                    "ds_email, ds_senha, nr_nivel_acesso, ds_status_conta) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 1, ?)";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, usuario.getIdUsuario());
            ps.setString(2, usuario.getNome());
            ps.setString(3, "ADMIN-" + usuario.getIdUsuario());
            ps.setDate(4, Date.valueOf(LocalDate.of(1990, 1, 1)));
            ps.setString(5, usuario.getEmail());
            ps.setString(6, usuario.getSenha());
            ps.setString(7, usuario.getStatusConta().toString());

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
     * Atualiza os dados de um administrador já cadastrado.
     * <p>
     * Não altera {@code ds_cpf} nem {@code dt_nascimento}: como essas colunas
     * já foram preenchidas no cadastro (e não existem no modelo {@link Admin}),
     * não há motivo para tocá-las aqui — isso evita reintroduzir o problema de
     * índices de parâmetro e o risco de colisão na coluna UNIQUE.
     *
     * @param usuario administrador com os dados atualizados
     */
    public void alterar(Admin usuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Usuario SET nm_usuario = ?, ds_email = ?, ds_senha = ?, " +
                    "nr_nivel_acesso = 1, ds_status_conta = ?" +
                    "WHERE id_usuario = ? AND nr_nivel_acesso = 1";
            ps = conexao.prepareStatement(sql);
            ps.setString(1, usuario.getNome());
            ps.setString(2, usuario.getEmail());
            ps.setString(3, usuario.getSenha());
            ps.setString(4, usuario.getStatusConta().toString());
            ps.setInt(5, usuario.getIdUsuario());

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
     * Remove um administrador do banco de dados.
     *
     * @param idUsuario identificador do administrador a ser removido
     */
    public void excluir(int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Usuario WHERE id_usuario = ? AND nr_nivel_acesso = 1");
            ps.setInt(1, idUsuario);
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
     * Busca um administrador pelo seu identificador.
     *
     * @param idUsuario identificador do administrador
     * @return o administrador encontrado, ou {@code null} caso não exista
     */
    public Admin buscarPorId(int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        Admin usuario = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Usuario WHERE id_usuario = ? AND nr_nivel_acesso = 1");
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                usuario = new Admin();
                usuario.setIdUsuario(rs.getInt(1));
                usuario.setNome(rs.getString(2));

                usuario.setEmail(rs.getString(5));
                usuario.setSenha(rs.getString(6));
                usuario.setNivelAcesso(rs.getInt(7));
                usuario.setStatusConta(StatusContaEnum.valueOf(rs.getString(8)));
            }

            rs.close();
            ps.close();

        }catch (SQLException e) {
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
        return usuario;
    }

    /**
     * Busca um administrador pelas credenciais de acesso (e-mail e senha),
     * utilizado no processo de autenticação.
     *
     * @param email e-mail informado para login
     * @param senha senha informada para login
     * @return o administrador correspondente, ou {@code null} caso as
     *         credenciais não confiram com nenhum registro
     */
    public Admin buscarPorEmailESenha(String email, String senha) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        Admin usuario = null;

        try {
            String sql = "SELECT * FROM Usuario WHERE ds_email = ? AND ds_senha = ? AND nr_nivel_acesso = ?";
            ps = conexao.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, senha);
            ps.setInt(3, 1);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                usuario = new Admin();
                usuario.setIdUsuario(rs.getInt(1));
                usuario.setNome(rs.getString(2));
                usuario.setEmail(rs.getString(5));
                usuario.setSenha(rs.getString(6));
                usuario.setNivelAcesso(rs.getInt(7));
                usuario.setStatusConta(StatusContaEnum.valueOf(rs.getString(8)));
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
        return usuario;
    }

    /**
     * Lista todos os administradores cadastrados no banco de dados, em uma
     * única consulta (uma única conexão).
     *
     * @return lista de administradores cadastrados
     */
    public List<Admin> listar() {
        List<Admin> usuarios = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM Usuario WHERE nr_nivel_acesso = 1");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Admin usuario = new Admin();
                usuario.setIdUsuario(rs.getInt(1));
                usuario.setNome(rs.getString(2));
                usuario.setEmail(rs.getString(5));
                usuario.setSenha(rs.getString(6));
                usuario.setNivelAcesso(rs.getInt(7));
                usuario.setStatusConta(StatusContaEnum.valueOf(rs.getString(8)));
                usuarios.add(usuario);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarios;
    }

    public void alterarSenha(Admin admin){
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Usuario SET senha = ? WHERE id_usuario = ? AND nr_nivel_acesso = 1";
            ps = conexao.prepareStatement(sql);
            ps.setString(1, admin.getSenha());
            ps.setInt(2, admin.getIdUsuario());

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
}
package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.models.Grupo;
import br.com.fiap.greenlify.models.UsuarioComum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da entidade UsuarioComum. Todas as consultas aqui filtram por
 * {@code nr_nivel_acesso = 0}, que identifica um usuário comum
 * (diferenciando-o de contas administrativas na mesma tabela Usuario).
 */
public class UsuarioComumDAO {
    /**
     * Cadastra um novo usuário comum. Se o usuário tiver um grupo associado,
     * o id_grupo também é gravado; caso contrário, a coluna fica de fora do INSERT.
     * Ao final, o id gerado pelo banco é preenchido no próprio objeto.
     *
     * @param usuario dados do usuário a ser cadastrado
     */
    public void cadastrar(UsuarioComum usuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            if (usuario.getGrupo() != null) {
                String sql = "INSERT INTO Usuario (nm_usuario, ds_cpf, dt_nascimento, " +
                        "ds_email, ds_senha, nr_nivel_acesso, ds_status_conta, id_grupo) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                ps = conexao.prepareStatement(sql, new String[]{"ID_USUARIO"});
                ps.setString(1, usuario.getNome());
                ps.setString(2, usuario.getCpf());
                ps.setDate(3, Date.valueOf(usuario.getDataNascimento()));
                ps.setString(4, usuario.getEmail());
                ps.setString(5, usuario.getSenha());
                ps.setInt(6, 0);
                ps.setString(7, usuario.getStatusConta().toString());
                ps.setInt(8, usuario.getGrupo().getIdGrupo());

            } else {
                String sql = "INSERT INTO Usuario (nm_usuario, ds_cpf, dt_nascimento, " +
                        "ds_email, ds_senha, nr_nivel_acesso, ds_status_conta) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                ps = conexao.prepareStatement(sql, new String[]{"ID_USUARIO"});
                ps.setString(1, usuario.getNome());
                ps.setString(2, usuario.getCpf());
                ps.setDate(3, Date.valueOf(usuario.getDataNascimento()));
                ps.setString(4, usuario.getEmail());
                ps.setString(5, usuario.getSenha());
                ps.setInt(6, 0);
                ps.setString(7, usuario.getStatusConta().toString());
            }

            ps.executeUpdate();

            // Recupera o id_usuario gerado automaticamente (IDENTITY) e preenche no objeto
            ResultSet rsGerado = ps.getGeneratedKeys();
            if (rsGerado.next()) {
                usuario.setIdUsuario(rsGerado.getInt(1));
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
     * Atualiza os dados de um usuário comum já existente.
     * A cláusula WHERE reforça {@code nr_nivel_acesso = 0} para impedir
     * que esse método altere uma conta administrativa por engano.
     *
     * @param usuario dados atualizados do usuário (o id é usado como referência)
     */
    public void alterar(UsuarioComum usuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            if (usuario.getGrupo() != null) {
                String sql = "UPDATE Usuario SET nm_usuario = ?, ds_cpf = ?, dt_nascimento = ?, " +
                        "ds_email = ?, ds_senha = ?, nr_nivel_acesso = 0, ds_status_conta = ?, id_grupo = ? " +
                        "WHERE id_usuario = ? AND nr_nivel_acesso = 0";
                ps = conexao.prepareStatement(sql);
                ps.setString(1, usuario.getNome());
                ps.setString(2, usuario.getCpf());
                ps.setDate(3, Date.valueOf(usuario.getDataNascimento()));
                ps.setString(4, usuario.getEmail());
                ps.setString(5, usuario.getSenha());
                ps.setString(6, usuario.getStatusConta().toString());
                ps.setInt(7, usuario.getGrupo().getIdGrupo());
                ps.setInt(8, usuario.getIdUsuario());
            } else {
                String sql = "UPDATE Usuario SET nm_usuario = ?, ds_cpf = ?, dt_nascimento = ?, " +
                        "ds_email = ?, ds_senha = ?, nr_nivel_acesso = 0, ds_status_conta = ? " +
                        "WHERE id_usuario = ? AND nr_nivel_acesso = 0";
                ps = conexao.prepareStatement(sql);
                ps.setString(1, usuario.getNome());
                ps.setString(2, usuario.getCpf());
                ps.setDate(3, Date.valueOf(usuario.getDataNascimento()));
                ps.setString(4, usuario.getEmail());
                ps.setString(5, usuario.getSenha());
                ps.setString(6, usuario.getStatusConta().toString());
                ps.setInt(7, usuario.getIdUsuario());
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
     * Exclui um usuário comum pelo id. Assim como em {@link #alterar}, o filtro
     * {@code nr_nivel_acesso = 0} evita que uma conta administrativa seja apagada aqui.
     *
     * @param idUsuario id do usuário a ser removido
     */
    public void excluir(int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Usuario WHERE id_usuario = ? AND nr_nivel_acesso = 0");
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
     * Busca um usuário comum pelo id.
     *
     * @param idUsuario id do usuário procurado
     * @return o usuário encontrado, ou {@code null} caso não exista (ou não seja um usuário comum)
     */
    public UsuarioComum buscarPorId(int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        UsuarioComum usuario = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Usuario WHERE id_usuario = ? AND nr_nivel_acesso = 0");
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                usuario = new UsuarioComum();
                usuario.setIdUsuario(rs.getInt(1));
                usuario.setNome(rs.getString(2));
                usuario.setCpf(rs.getString(3));
                usuario.setDataNascimento(rs.getDate(4).toLocalDate());
                usuario.setEmail(rs.getString(5));
                usuario.setSenha(rs.getString(6));
                usuario.setNivelAcesso(rs.getInt(7));
                usuario.setStatusConta(StatusContaEnum.valueOf(rs.getString(8)));

                int idGrupo = rs.getInt(9);
                if (idGrupo > 0) {
                    Grupo grupo = new Grupo();
                    grupo.setIdGrupo(idGrupo);
                    usuario.setGrupo(grupo);
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
        return usuario;
    }

    /**
     * Lista todos os usuários comuns cadastrados.
     *
     * @return lista de usuários comuns (pode vir vazia, nunca nula)
     */
    public List<UsuarioComum> listar() {
        Connection conexao = ConnectionFactory.obterConexao();
        List<UsuarioComum> usuarios = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Usuario WHERE nr_nivel_acesso = 0");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UsuarioComum usuario = new UsuarioComum();

                usuario.setIdUsuario(rs.getInt(1));
                usuario.setNome(rs.getString(2));
                usuario.setCpf(rs.getString(3));
                usuario.setDataNascimento(rs.getDate(4).toLocalDate());
                usuario.setEmail(rs.getString(5));
                usuario.setSenha(rs.getString(6));
                usuario.setNivelAcesso(rs.getInt(7));
                usuario.setStatusConta(StatusContaEnum.valueOf(rs.getString(8)));

                int idGrupo = rs.getInt(9);
                if (idGrupo > 0) {
                    Grupo grupo = new Grupo();
                    grupo.setIdGrupo(idGrupo);
                    usuario.setGrupo(grupo);
                }

                usuarios.add(usuario);
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
        return usuarios;
    }

    /**
     * Busca um usuário comum pela combinação de e-mail e senha, usada no login.
     * A senha é comparada em texto puro, exatamente como está gravada na tabela.
     *
     * @param email e-mail informado no login
     * @param senha senha informada no login
     * @return o usuário correspondente, ou {@code null} se as credenciais não baterem
     */
    public UsuarioComum buscarPorEmailESenha(String email, String senha) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        UsuarioComum usuario = null;

        try {
            String sql = "SELECT * FROM Usuario WHERE ds_email = ? AND ds_senha = ? AND nr_nivel_acesso = ?";
            ps = conexao.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, senha);
            ps.setInt(3, 0);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                usuario = new UsuarioComum();
                usuario.setIdUsuario(rs.getInt(1));
                usuario.setNome(rs.getString(2));
                usuario.setCpf(rs.getString(3));
                usuario.setDataNascimento(rs.getDate(4).toLocalDate());
                usuario.setEmail(rs.getString(5));
                usuario.setSenha(rs.getString(6));
                usuario.setNivelAcesso(rs.getInt(7));
                usuario.setStatusConta(StatusContaEnum.valueOf(rs.getString(8)));

                int idGrupo = rs.getInt(9);
                if (idGrupo > 0) {
                    Grupo grupo = new Grupo();
                    grupo.setIdGrupo(idGrupo);
                    usuario.setGrupo(grupo);
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
        return usuario;
    }
}
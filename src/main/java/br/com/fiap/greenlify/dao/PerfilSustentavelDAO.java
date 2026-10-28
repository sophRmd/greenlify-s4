package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.models.Grupo;
import br.com.fiap.greenlify.models.NivelContribuicao;
import br.com.fiap.greenlify.models.PerfilSustentavel;
import br.com.fiap.greenlify.models.UsuarioComum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link PerfilSustentavel} na tabela
 * {@code Perfil_Sustentavel}.
 */
public class PerfilSustentavelDAO {

    /**
     * Cadastra um novo perfil sustentável, vinculado ao usuário e ao
     * nível de contribuição informados.
     *
     * @param perfil perfil sustentável a ser cadastrado
     */
    public void cadastrar(PerfilSustentavel perfil) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Perfil_Sustentavel (id_perfil, nr_xp, id_usuario, id_nivel) " +
                    "VALUES (?, ?, ?, ?)";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, perfil.getUsuario().getIdUsuario());
            ps.setInt(2, perfil.getXpSustentavel());
            ps.setInt(3, perfil.getUsuario().getIdUsuario());
            ps.setInt(4, perfil.getNivel().getIdNivel());

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
     * Atualiza os dados de um perfil sustentável já cadastrado.
     *
     * @param perfil perfil sustentável com os dados atualizados
     */
    public void alterar(PerfilSustentavel perfil) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Perfil_Sustentavel SET nr_xp = ?, nr_missoes_concluidas = ?, nr_agua_economizada = ?, nr_energia_economizada = ?, nr_carbono_economizada = ?, id_usuario = ?, id_nivel = ? " +
                    "WHERE id_perfil = ?";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, perfil.getXpSustentavel());
            ps.setInt(2, perfil.getTotalMissoesConcluidas());
            ps.setDouble(3, perfil.getTotalAguaEconomizada());
            ps.setDouble(4, perfil.getTotalEnergiaEconomizada());
            ps.setDouble(5, perfil.getTotalCarbonoEconomizado());
            ps.setInt(6, perfil.getUsuario().getIdUsuario());
            ps.setInt(7, perfil.getNivel().getIdNivel());
            ps.setInt(8, perfil.getIdPerfilSustentavel());

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
     * Remove um perfil sustentável do banco de dados.
     *
     * @param idPerfil identificador do perfil sustentável a ser removido
     */
    public void excluir(int idPerfil) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Perfil_Sustentavel WHERE id_perfil = ?");
            ps.setInt(1, idPerfil);

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

    /** Perfil + Nível (n_) + Usuário (u_) em uma única consulta. */
    private static final String SELECT_COMPLETO =
            "SELECT p.id_perfil, p.nr_xp, p.nr_missoes_concluidas, p.nr_agua_economizada, "
                    + "       p.nr_energia_economizada, p.nr_carbono_economizada, "
                    + "       n.id_nivel AS n_id, n.nr_nivel AS n_nivel, n.nm_nivel AS n_nome, "
                    + "       n.nr_xp_minimo AS n_xp_min, n.nr_xp_maximo AS n_xp_max, "
                    + "       u.id_usuario AS u_id, u.nm_usuario AS u_nome, u.ds_cpf AS u_cpf, "
                    + "       u.dt_nascimento AS u_nascimento, u.ds_email AS u_email, u.ds_senha AS u_senha, "
                    + "       u.nr_nivel_acesso AS u_nivel, u.ds_status_conta AS u_status, u.id_grupo AS u_grupo "
                    + "FROM Perfil_Sustentavel p "
                    + "LEFT JOIN Nivel_Contribuicao n ON n.id_nivel = p.id_nivel "
                    + "LEFT JOIN Usuario u ON u.id_usuario = p.id_usuario ";

    /**
     * Busca um perfil sustentável pelo seu identificador, carregando
     * também o usuário e o nível de contribuição associados, em uma única consulta.
     *
     * @param idPerfil identificador do perfil sustentável
     * @return o perfil sustentável encontrado, ou {@code null} caso não exista
     */
    public PerfilSustentavel buscarPorId(int idPerfil) {
        PerfilSustentavel perfil = null;

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "WHERE p.id_perfil = ?")) {

            ps.setInt(1, idPerfil);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    perfil = mapear(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return perfil;
    }

    /**
     * Lista todos os perfis sustentáveis cadastrados no banco de dados, em uma
     * única consulta.
     *
     * @return lista de perfis sustentáveis cadastrados
     */
    public List<PerfilSustentavel> listar() {
        List<PerfilSustentavel> perfis = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "ORDER BY p.id_perfil");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                perfis.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return perfis;
    }

    private PerfilSustentavel mapear(ResultSet rs) throws SQLException {
        PerfilSustentavel perfil = new PerfilSustentavel();
        perfil.setIdPerfilSustentavel(rs.getInt("id_perfil"));
        perfil.setXpSustentavel(rs.getInt("nr_xp"));
        perfil.setTotalMissoesConcluidas(rs.getInt("nr_missoes_concluidas"));
        perfil.setTotalAguaEconomizada(rs.getDouble("nr_agua_economizada"));
        perfil.setTotalEnergiaEconomizada(rs.getDouble("nr_energia_economizada"));
        perfil.setTotalCarbonoEconomizado(rs.getDouble("nr_carbono_economizada"));
        perfil.setUsuario(mapearUsuario(rs));
        perfil.setNivel(mapearNivel(rs));
        return perfil;
    }

    /** Monta o nível a partir das colunas n_*; devolve null se não existir. */
    private NivelContribuicao mapearNivel(ResultSet rs) throws SQLException {
        int idNivel = rs.getInt("n_id");
        if (rs.wasNull()) {
            return null;
        }
        NivelContribuicao nivel = new NivelContribuicao();
        nivel.setIdNivel(idNivel);
        nivel.setNivel(rs.getInt("n_nivel"));
        nivel.setNome(rs.getString("n_nome"));
        nivel.setXpMinimo(rs.getInt("n_xp_min"));
        nivel.setXpMaximo(rs.getInt("n_xp_max"));
        return nivel;
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
}
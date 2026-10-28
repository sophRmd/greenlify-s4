package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.models.Grupo;
import br.com.fiap.greenlify.models.Selo;
import br.com.fiap.greenlify.models.UsuarioComum;
import br.com.fiap.greenlify.models.UsuarioSelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO da entidade UsuarioSelo, que representa a coleta de um selo
 * por um usuário em uma determinada data.
 */
public class UsuarioSeloDAO {
    /**
     * Registra a coleta de um selo por um usuário, em uma transação: confere de novo (com o usuário
     * travado) se o selo ainda não foi coletado e só então grava.
     *
     * @param usuarioSelo dados da coleta (usuário, selo e data)
     * @throws IllegalArgumentException se o usuário não existir ou já tiver coletado o selo
     * @throws IllegalStateException    se o banco falhar (antes o erro era engolido e a API respondia sucesso)
     */
    public void cadastrar(UsuarioSelo usuarioSelo) {
        int idUsuario = usuarioSelo.getUsuario().getIdUsuario();
        int idSelo = usuarioSelo.getSelo().getIdSelo();

        try (Connection conexao = ConnectionFactory.obterConexao()) {
            boolean confirmado = false;
            try {
                conexao.setAutoCommit(false);

                // Trava a linha do usuário: duas coletas simultâneas do mesmo selo ficam em fila e a
                // segunda enxerga a primeira, então o selo nunca é gravado (nem contado no grupo) duas vezes.
                try (PreparedStatement trava = conexao.prepareStatement(
                        "SELECT id_usuario FROM Usuario WHERE id_usuario = ? FOR UPDATE")) {
                    trava.setInt(1, idUsuario);
                    try (ResultSet rs = trava.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("O usuário informado não existe.");
                        }
                    }
                }

                try (PreparedStatement existe = conexao.prepareStatement(
                        "SELECT COUNT(*) FROM Usuario_Selo WHERE id_usuario = ? AND id_selo = ?")) {
                    existe.setInt(1, idUsuario);
                    existe.setInt(2, idSelo);
                    try (ResultSet rs = existe.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new IllegalArgumentException("O usuário já coletou esse selo.");
                        }
                    }
                }

                String sql = "INSERT INTO Usuario_Selo (dt_coleta, id_usuario, id_selo) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                    ps.setDate(1, Date.valueOf(usuarioSelo.getDataColeta()));
                    ps.setInt(2, idUsuario);
                    ps.setInt(3, idSelo);
                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException("A coleta do selo não foi gravada.");
                    }
                }

                conexao.commit();
                confirmado = true;
            } finally {
                // Desfaz antes de fechar a conexão (no Oracle, ligar o autocommit com a
                // transação aberta confirmaria tudo).
                if (!confirmado) {
                    conexao.rollback();
                }
                conexao.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível registrar a coleta do selo.", e);
        }
    }

    /**
     * Atualiza um registro de coleta de selo já existente.
     *
     * @param usuarioSelo dados atualizados (o id é usado como referência)
     */
    public void alterar(UsuarioSelo usuarioSelo) {
        String sql = "UPDATE Usuario_Selo SET dt_coleta = ?, id_usuario = ?, id_selo = ? WHERE id_usuario_selo = ?";

        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(usuarioSelo.getDataColeta()));
            ps.setInt(2, usuarioSelo.getUsuario().getIdUsuario());
            ps.setInt(3, usuarioSelo.getSelo().getIdSelo());
            ps.setInt(4, usuarioSelo.getIdUsuarioSelo());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível alterar a coleta do selo.", e);
        }
    }

    /**
     * Remove um registro de coleta de selo pelo id.
     *
     * @param idUsuarioSelo id do registro a ser excluído
     */
    public void excluir(int idUsuarioSelo) {
        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement("DELETE FROM Usuario_Selo WHERE id_usuario_selo = ?")) {
            ps.setInt(1, idUsuarioSelo);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir a coleta do selo.", e);
        }
    }

    /** Colunas do selo (alias s_) e do usuário (alias u_) lidas junto com Usuario_Selo. */
    private static final String SELECT_COMPLETO =
            "SELECT us.id_usuario_selo, us.dt_coleta, "
                    + "       s.id_selo AS s_id, s.nm_selo AS s_nome, s.vl_vale_entrega AS s_vale, "
                    + "       s.nr_credito_carbono AS s_credito, s.nr_pontos_necessarios AS s_pontos, "
                    + "       u.id_usuario AS u_id, u.nm_usuario AS u_nome, u.ds_cpf AS u_cpf, "
                    + "       u.dt_nascimento AS u_nascimento, u.ds_email AS u_email, u.ds_senha AS u_senha, "
                    + "       u.nr_nivel_acesso AS u_nivel, u.ds_status_conta AS u_status, u.id_grupo AS u_grupo "
                    + "FROM Usuario_Selo us "
                    + "LEFT JOIN Selo s ON s.id_selo = us.id_selo "
                    + "LEFT JOIN Usuario u ON u.id_usuario = us.id_usuario AND u.nr_nivel_acesso = 0 ";

    /** Versão sem usuário (usada quando o id do usuário já é conhecido). */
    private static final String SELECT_SO_SELO =
            "SELECT us.id_usuario_selo, us.dt_coleta, "
                    + "       s.id_selo AS s_id, s.nm_selo AS s_nome, s.vl_vale_entrega AS s_vale, "
                    + "       s.nr_credito_carbono AS s_credito, s.nr_pontos_necessarios AS s_pontos "
                    + "FROM Usuario_Selo us "
                    + "LEFT JOIN Selo s ON s.id_selo = us.id_selo ";

    /**
     * Busca um registro de coleta de selo pelo seu id, já carregando
     * o usuário e o selo relacionados, em uma única consulta.
     *
     * @param idUsuarioSelo id do registro procurado
     * @return o registro encontrado, ou {@code null} se não existir
     */
    public UsuarioSelo buscarPorId(int idUsuarioSelo) {
        UsuarioSelo usuarioSelo = null;

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "WHERE us.id_usuario_selo = ?")) {

            ps.setInt(1, idUsuarioSelo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    usuarioSelo = mapearBase(rs);
                    usuarioSelo.setUsuario(mapearUsuario(rs));
                    usuarioSelo.setSelo(mapearSelo(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarioSelo;
    }

    /**
     * Lista os selos coletados por um usuário específico.
     * Diferente de {@link #buscarPorId}, aqui o objeto usuário não é carregado
     * dentro de cada UsuarioSelo (só o selo), já que o id já foi informado.
     *
     * @param idUsuario id do usuário cujos selos serão listados
     * @return lista de coletas de selo desse usuário (pode vir vazia, nunca nula)
     */
    public List<UsuarioSelo> buscarPorIdUsuario(int idUsuario) {
        List<UsuarioSelo> lista = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_SO_SELO + "WHERE us.id_usuario = ?")) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UsuarioSelo usuarioSelo = mapearBase(rs);
                    usuarioSelo.setSelo(mapearSelo(rs));
                    lista.add(usuarioSelo);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Lista todos os registros de coleta de selo cadastrados, cada um já
     * com o usuário e o selo correspondentes carregados, em uma única consulta.
     *
     * @return lista completa de coletas (pode vir vazia, nunca nula)
     */
    public List<UsuarioSelo> listar() {
        List<UsuarioSelo> lista = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "ORDER BY us.id_usuario_selo");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UsuarioSelo usuarioSelo = mapearBase(rs);
                usuarioSelo.setUsuario(mapearUsuario(rs));
                usuarioSelo.setSelo(mapearSelo(rs));
                lista.add(usuarioSelo);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Total de Selos Verdes resgatados, por grupo, para o ranking coletivo (id do grupo -> total).
     * Conta cada selo uma única vez por usuário e só entram resgates válidos:
     * <ul>
     *   <li>o selo ainda existe (JOIN em Selo) e a data de coleta está preenchida e não é futura;</li>
     *   <li>repetições do mesmo selo pelo mesmo usuário valem uma só (DISTINCT), então um registro
     *       duplicado nunca soma duas vezes;</li>
     *   <li>o resgatante é usuário comum e pertence, <b>hoje</b>, a um grupo existente: quem sai do
     *       grupo deixa de somar para ele (e quem entra passa a somar) na próxima atualização do
     *       ranking, e quem não tem grupo não pontua em nenhum.</li>
     * </ul>
     * Não usa a tabela Resgate (prêmios de ranking individual) nem a pontuação de missões. Não há
     * status de cancelamento: desfazer um resgate é excluir o registro de Usuario_Selo, e a
     * contagem sempre reflete as linhas existentes.
     *
     * @return mapa idGrupo -> total de selos (grupos sem selos não aparecem)
     * @throws IllegalStateException se o banco falhar
     */
    public Map<Integer, Integer> contarSelosPorGrupo() {
        String sql = "SELECT u.id_grupo AS id_grupo, COUNT(*) AS total FROM ("
                + "SELECT DISTINCT us.id_usuario AS id_usuario, us.id_selo AS id_selo "
                + "FROM Usuario_Selo us "
                + "JOIN Selo s ON s.id_selo = us.id_selo "
                + "WHERE us.dt_coleta IS NOT NULL AND us.dt_coleta <= TRUNC(SYSDATE)) v "
                + "JOIN Usuario u ON u.id_usuario = v.id_usuario "
                + "JOIN Grupo g ON g.id_grupo = u.id_grupo "
                + "WHERE u.nr_nivel_acesso = 0 "
                + "GROUP BY u.id_grupo";

        Map<Integer, Integer> totais = new HashMap<>();
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                totais.put(rs.getInt("id_grupo"), rs.getInt("total"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível contar os selos por grupo.", e);
        }
        return totais;
    }

    /**
     * Informa se o usuário já coletou o selo (um selo por usuário), sem carregar a tabela toda.
     *
     * @param ignorarIdUsuarioSelo registro a desconsiderar (o que está sendo alterado); use 0 para nenhum
     */
    public boolean existeColeta(int idUsuario, int idSelo, int ignorarIdUsuarioSelo) {
        String sql = "SELECT COUNT(*) FROM Usuario_Selo "
                + "WHERE id_usuario = ? AND id_selo = ? AND id_usuario_selo <> ?";
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setInt(2, idSelo);
            ps.setInt(3, ignorarIdUsuarioSelo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível verificar a coleta do selo.", e);
        }
    }

    private UsuarioSelo mapearBase(ResultSet rs) throws SQLException {
        UsuarioSelo usuarioSelo = new UsuarioSelo();
        usuarioSelo.setIdUsuarioSelo(rs.getInt(1));
        usuarioSelo.setDataColeta(rs.getDate(2).toLocalDate());
        return usuarioSelo;
    }

    /** Monta o selo a partir das colunas s_*; devolve null se o selo não existir. */
    private Selo mapearSelo(ResultSet rs) throws SQLException {
        int idSelo = rs.getInt(3);
        if (rs.wasNull()) {
            return null;
        }
        Selo selo = new Selo();
        selo.setIdSelo(idSelo);
        selo.setNome(rs.getString(4));
        selo.setValeEnergia(rs.getInt(5));
        selo.setCreditoCarbono(rs.getInt(6));
        selo.setPontosNecessarios(rs.getInt(7));
        return selo;
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

        int idGrupo = rs.getInt("u_grupo");
        if (idGrupo > 0) {
            Grupo grupo = new Grupo();
            grupo.setIdGrupo(idGrupo);
            usuario.setGrupo(grupo);
        }
        return usuario;
    }
}
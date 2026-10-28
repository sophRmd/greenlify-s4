package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.enums.StatusRankingEnum;
import br.com.fiap.greenlify.enums.TipoRankingEnum;
import br.com.fiap.greenlify.models.Grupo;
import br.com.fiap.greenlify.models.PosicaoRanking;
import br.com.fiap.greenlify.models.Ranking;
import br.com.fiap.greenlify.models.UsuarioComum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link PosicaoRanking} na tabela {@code Posicao_Ranking}.
 *
 * <p>Cada posição pertence a um usuário OU a um grupo (nunca aos dois), como
 * exige o CHECK da tabela.</p>
 *
 * <p>As consultas de leitura trazem posição, ranking, usuário e grupo em um
 * único SELECT (JOIN), usando apenas UMA conexão por chamada. Este DAO não
 * chama nenhum outro DAO.</p>
 *
 * <p>As posições de um ranking encerrado são histórico: {@link #substituirPorRanking} recusa
 * regravá-las.</p>
 *
 * <p>Estas posições são do ranking mensal (ligadas a um {@code Ranking} com período). O ranking
 * all time não é gravado aqui: vive no Redis. Falhas de escrita lançam
 * {@link IllegalStateException} em vez de passar em silêncio.</p>
 */
public class PosicaoRankingDAO {

    /** Consulta base com JOIN. Os aliases evitam colisão de nomes de colunas. */
    private static final String SELECT_BASE =
            "SELECT p.id_posicao, p.nr_posicao, p.nr_pontuacao, "
                    + "       r.id_ranking AS r_id, r.ds_tipo AS r_tipo, r.dt_inicio AS r_inicio, "
                    + "       r.dt_fim AS r_fim, r.ds_status AS r_status, "
                    + "       u.id_usuario AS u_id, u.nm_usuario AS u_nome, u.ds_cpf AS u_cpf, "
                    + "       u.dt_nascimento AS u_nascimento, u.ds_email AS u_email, u.ds_senha AS u_senha, "
                    + "       u.nr_nivel_acesso AS u_nivel, u.ds_status_conta AS u_status, u.id_grupo AS u_grupo, "
                    + "       g.id_grupo AS g_id, g.nm_grupo AS g_nome, g.dt_criacao AS g_criacao, "
                    + "       g.nr_usuarios AS g_qtd, g.nr_status_grupo AS g_status "
                    + "FROM Posicao_Ranking p "
                    + "JOIN Ranking r ON r.id_ranking = p.id_ranking "
                    + "LEFT JOIN Usuario u ON u.id_usuario = p.id_usuario AND u.nr_nivel_acesso = 0 "
                    + "LEFT JOIN Grupo g ON g.id_grupo = p.id_grupo ";

    /**
     * Cadastra uma nova posição de ranking e preenche o id gerado pelo banco.
     *
     * @param posicaoRanking posição de ranking a ser cadastrada (com usuário OU grupo)
     * @throws IllegalArgumentException se faltar o ranking ou não houver exatamente um de usuário/grupo
     * @throws IllegalStateException    se o banco falhar
     */
    public void cadastrar(PosicaoRanking posicaoRanking) {
        if (posicaoRanking == null || posicaoRanking.getRanking() == null) {
            throw new IllegalArgumentException("A posição precisa estar ligada a um ranking.");
        }
        validarDono(posicaoRanking);
        String sql = "INSERT INTO Posicao_Ranking (nr_posicao, nr_pontuacao, id_ranking, id_usuario, id_grupo) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql, new String[]{"ID_POSICAO"})) {

            ps.setInt(1, posicaoRanking.getPosicao());
            ps.setInt(2, posicaoRanking.getPontuacao());
            ps.setInt(3, posicaoRanking.getRanking().getIdRanking());
            definirInteiroOuNulo(ps, 4, posicaoRanking.getUsuario() != null
                    ? posicaoRanking.getUsuario().getIdUsuario() : null);
            definirInteiroOuNulo(ps, 5, posicaoRanking.getGrupo() != null
                    ? posicaoRanking.getGrupo().getIdGrupo() : null);

            ps.executeUpdate();

            try (ResultSet rsGerado = ps.getGeneratedKeys()) {
                if (rsGerado.next()) {
                    posicaoRanking.setIdPosicao(rsGerado.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar a posição do ranking.", e);
        }
    }

    /**
     * Troca todas as posições de um ranking pelas informadas, em UMA transação: apaga as antigas e
     * grava as novas e só então confirma. Quem consulta durante a atualização continua vendo as
     * posições anteriores (nunca o ranking vazio ou pela metade), e se qualquer etapa falhar nada
     * muda.
     *
     * @param idRanking identificador do ranking
     * @param posicoes  novas posições (usuário OU grupo em cada uma)
     * <p>A linha do ranking é travada (FOR UPDATE) durante a troca: atualizações e fechamentos
     * simultâneos do mesmo ranking ficam em fila. Se o ranking já estiver FECHADO, nada é regravado.</p>
     *
     * @throws IllegalArgumentException se alguma posição não tiver exatamente um de usuário/grupo
     *                                  ou se o ranking não existir
     * @throws IllegalStateException    se o ranking estiver encerrado ou se o banco falhar (as
     *                                  posições antigas são mantidas)
     */
    public void substituirPorRanking(int idRanking, List<PosicaoRanking> posicoes) {
        if (posicoes == null) {
            throw new IllegalArgumentException("A lista de posições é obrigatória.");
        }
        for (PosicaoRanking posicao : posicoes) {
            validarDono(posicao);
        }
        String sql = "INSERT INTO Posicao_Ranking (nr_posicao, nr_pontuacao, id_ranking, id_usuario, id_grupo) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conexao = ConnectionFactory.obterConexao()) {
            boolean confirmado = false;
            try {
                conexao.setAutoCommit(false);

                try (PreparedStatement trava = conexao.prepareStatement(
                        "SELECT ds_status FROM Ranking WHERE id_ranking = ? FOR UPDATE")) {
                    trava.setInt(1, idRanking);
                    try (ResultSet rs = trava.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Ranking não encontrado");
                        }
                        if (StatusRankingEnum.FECHADO.name().equals(rs.getString(1))) {
                            throw new IllegalStateException(
                                    "O ranking está encerrado: as posições do histórico são preservadas.");
                        }
                    }
                }

                try (PreparedStatement apagar = conexao.prepareStatement(
                        "DELETE FROM Posicao_Ranking WHERE id_ranking = ?")) {
                    apagar.setInt(1, idRanking);
                    apagar.executeUpdate();
                }

                try (PreparedStatement ps = conexao.prepareStatement(sql)) {
                    for (PosicaoRanking posicao : posicoes) {
                        ps.setInt(1, posicao.getPosicao());
                        ps.setInt(2, posicao.getPontuacao());
                        ps.setInt(3, idRanking);
                        definirInteiroOuNulo(ps, 4, posicao.getUsuario() != null
                                ? posicao.getUsuario().getIdUsuario() : null);
                        definirInteiroOuNulo(ps, 5, posicao.getGrupo() != null
                                ? posicao.getGrupo().getIdGrupo() : null);
                        ps.addBatch();
                    }
                    ps.executeBatch();
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
            throw new IllegalStateException("Não foi possível gravar as posições do ranking.", e);
        }
    }

    /**
     * Exclui uma posição de ranking pelo seu identificador.
     *
     * @param idPosicao identificador da posição
     * @throws IllegalStateException se o banco falhar
     */
    public void excluir(int idPosicao) {
        executarUpdate("DELETE FROM Posicao_Ranking WHERE id_posicao = ?", idPosicao);
    }

    /**
     * Exclui todas as posições de um ranking (usado ao recalcular o ranking).
     *
     * @param idRanking identificador do ranking
     * @throws IllegalStateException se o banco falhar
     */
    public void excluirPorRanking(int idRanking) {
        executarUpdate("DELETE FROM Posicao_Ranking WHERE id_ranking = ?", idRanking);
    }

    /**
     * Busca uma posição de ranking pelo identificador.
     *
     * @param idPosicao identificador da posição
     * @return a posição encontrada, ou {@code null} se não existir
     */
    public PosicaoRanking buscarPorId(int idPosicao) {
        List<PosicaoRanking> lista = consultar(SELECT_BASE + "WHERE p.id_posicao = ?", idPosicao);
        return lista.isEmpty() ? null : lista.get(0);
    }

    /**
     * Lista todas as posições de todos os rankings.
     *
     * @return lista de posições (pode vir vazia, nunca nula)
     */
    public List<PosicaoRanking> listar() {
        return consultar(SELECT_BASE + "ORDER BY p.id_ranking, p.nr_posicao", null);
    }

    /**
     * Lista as posições de um único ranking, em ordem de colocação.
     *
     * @param idRanking identificador do ranking
     * @return lista de posições (pode vir vazia, nunca nula)
     */
    public List<PosicaoRanking> listarPorRanking(int idRanking) {
        return consultar(SELECT_BASE + "WHERE p.id_ranking = ? ORDER BY p.nr_posicao", idRanking);
    }

    /**
     * Obtém o id do ranking de grupos mais recente que já tem posições.
     *
     * @return o id, ou 0 se não houver
     */
    public int obterUltimoIdRankingGrupo() {
        return obterMaximo("SELECT MAX(id_ranking) FROM Posicao_Ranking WHERE id_grupo IS NOT NULL");
    }

    /**
     * Obtém o id do ranking de usuários mais recente que já tem posições.
     *
     * @return o id, ou 0 se não houver
     */
    public int obterUltimoIdRankingUsuario() {
        return obterMaximo("SELECT MAX(id_ranking) FROM Posicao_Ranking WHERE id_usuario IS NOT NULL");
    }

    // ------------------------------------------------------------------
    // Métodos internos
    // ------------------------------------------------------------------

    private void executarUpdate(String sql, int parametro) {
        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, parametro);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir as posições do ranking.", e);
        }
    }

    /** O CHECK da tabela exige usuário OU grupo (nunca os dois, nunca nenhum): confere antes de ir ao banco. */
    private void validarDono(PosicaoRanking posicao) {
        if (posicao == null || (posicao.getUsuario() == null) == (posicao.getGrupo() == null)) {
            throw new IllegalArgumentException("A posição deve ter um usuário OU um grupo.");
        }
    }

    private int obterMaximo(String sql) {
        int id = 0;
        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
     * Executa o SELECT com JOIN usando uma única conexão. O parâmetro é
     * opcional (nulo quando a consulta não tem filtro).
     */
    private List<PosicaoRanking> consultar(String sql, Integer parametro) {
        List<PosicaoRanking> lista = new ArrayList<>();

        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            if (parametro != null) {
                ps.setInt(1, parametro);
            }

            try (ResultSet rs = ps.executeQuery()) {
                // Reaproveita o mesmo objeto Ranking / Grupo entre linhas iguais.
                Map<Integer, Ranking> cacheRankings = new HashMap<>();
                Map<Integer, Grupo> cacheGrupos = new HashMap<>();

                while (rs.next()) {
                    lista.add(mapear(rs, cacheRankings, cacheGrupos));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Monta a posição a partir da linha atual do JOIN. Não faz nenhuma
     * consulta adicional ao banco.
     */
    private PosicaoRanking mapear(ResultSet rs, Map<Integer, Ranking> cacheRankings,
                                  Map<Integer, Grupo> cacheGrupos) throws SQLException {
        PosicaoRanking posicaoRanking = new PosicaoRanking();
        posicaoRanking.setIdPosicao(rs.getInt("id_posicao"));
        posicaoRanking.setPosicao(rs.getInt("nr_posicao"));
        posicaoRanking.setPontuacao(rs.getInt("nr_pontuacao"));

        // Ranking (JOIN obrigatório)
        int idRanking = rs.getInt("r_id");
        if (idRanking > 0) {
            Ranking ranking = cacheRankings.get(idRanking);
            if (ranking == null) {
                ranking = new Ranking();
                ranking.setIdRanking(idRanking);
                ranking.setTipo(TipoRankingEnum.valueOf(rs.getString("r_tipo")));
                if (rs.getDate("r_inicio") != null) {
                    ranking.setDataInicio(rs.getDate("r_inicio").toLocalDate());
                }
                if (rs.getDate("r_fim") != null) {
                    ranking.setDataFim(rs.getDate("r_fim").toLocalDate());
                }
                ranking.setStatus(StatusRankingEnum.valueOf(rs.getString("r_status")));
                cacheRankings.put(idRanking, ranking);
            }
            posicaoRanking.setRanking(ranking);
        }

        // Usuário (LEFT JOIN: só vem preenchido nas posições de usuário)
        int idUsuario = rs.getInt("u_id");
        if (idUsuario > 0) {
            UsuarioComum usuario = new UsuarioComum();
            usuario.setIdUsuario(idUsuario);
            usuario.setNome(rs.getString("u_nome"));
            usuario.setCpf(rs.getString("u_cpf"));
            if (rs.getDate("u_nascimento") != null) {
                usuario.setDataNascimento(rs.getDate("u_nascimento").toLocalDate());
            }
            usuario.setEmail(rs.getString("u_email"));
            usuario.setSenha(rs.getString("u_senha"));
            usuario.setNivelAcesso(rs.getInt("u_nivel"));
            usuario.setStatusConta(StatusContaEnum.valueOf(rs.getString("u_status")));

            // Igual ao UsuarioComumDAO.buscarPorId: só guarda o id do grupo do usuário.
            int idGrupoUsuario = rs.getInt("u_grupo");
            if (idGrupoUsuario > 0) {
                Grupo grupoDoUsuario = new Grupo();
                grupoDoUsuario.setIdGrupo(idGrupoUsuario);
                usuario.setGrupo(grupoDoUsuario);
            }
            posicaoRanking.setUsuario(usuario);
        }

        // Grupo (LEFT JOIN: só vem preenchido nas posições de grupo)
        int idGrupo = rs.getInt("g_id");
        if (idGrupo > 0) {
            Grupo grupo = cacheGrupos.get(idGrupo);
            if (grupo == null) {
                grupo = new Grupo();
                grupo.setIdGrupo(idGrupo);
                grupo.setNome(rs.getString("g_nome"));
                if (rs.getDate("g_criacao") != null) {
                    grupo.setDataCriacao(rs.getDate("g_criacao").toLocalDate());
                }
                grupo.setQtdUsuarios(rs.getInt("g_qtd"));
                grupo.setStatusGrupo(rs.getInt("g_status"));
                cacheGrupos.put(idGrupo, grupo);
            }
            posicaoRanking.setGrupo(grupo);
        }
        return posicaoRanking;
    }

    private void definirInteiroOuNulo(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, valor);
        }
    }
}
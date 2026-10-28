package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusRankingEnum;
import br.com.fiap.greenlify.enums.TipoRankingEnum;
import br.com.fiap.greenlify.models.Ranking;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link Ranking} na tabela {@code Ranking}.
 *
 * <p>Esta tabela guarda só os períodos do ciclo mensal (controlado pelo administrador). O ranking
 * all time não tem período nem registro aqui: ele é calculado no Oracle e projetado no Redis
 * (ver {@code RankingAllTimeService}).</p>
 *
 * <p>Falhas de banco não passam mais em silêncio: cadastrar, alterar e excluir lançam
 * {@link IllegalStateException}, para quem chamou não responder "sucesso" quando nada foi gravado.</p>
 *
 * <p>Estados válidos: {@code ABERTO -> FECHADO}. Um ranking encerrado é histórico: não é alterado
 * nem excluído (as posições dele ficam preservadas), e o fechamento é uma troca atômica de status
 * ({@link #fechar(int)}), que só vale uma vez.</p>
 */
public class RankingDAO {

    private static final String COLUNAS = "id_ranking, ds_tipo, dt_inicio, dt_fim, ds_status";

    /**
     * Cadastra um novo período de ranking e preenche o id gerado pelo banco
     * (coluna IDENTITY) no próprio objeto.
     *
     * @param ranking ranking a ser cadastrado
     * @throws IllegalArgumentException se tipo, status ou período forem inválidos
     * @throws IllegalStateException    se o banco falhar (o id permanece 0)
     */
    public void cadastrar(Ranking ranking) {
        validar(ranking);
        String sql = "INSERT INTO Ranking (ds_tipo, dt_inicio, dt_fim, ds_status) VALUES (?, ?, ?, ?)";

        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql, new String[]{"ID_RANKING"})) {

            ps.setString(1, ranking.getTipo().name());
            ps.setDate(2, Date.valueOf(ranking.getDataInicio()));
            ps.setDate(3, Date.valueOf(ranking.getDataFim()));
            ps.setString(4, ranking.getStatus().name());

            ps.executeUpdate();

            try (ResultSet rsGerado = ps.getGeneratedKeys()) {
                if (rsGerado.next()) {
                    ranking.setIdRanking(rsGerado.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível cadastrar o ranking.", e);
        }
    }

    /**
     * Atualiza os dados de um período de ranking existente que ainda esteja ABERTO. Ranking
     * encerrado não é alterado (histórico).
     *
     * @param ranking ranking com os novos dados (o id define qual será atualizado)
     * @throws IllegalArgumentException se tipo, status ou período forem inválidos, ou se o ranking
     *                                  não existir ou já estiver encerrado
     * @throws IllegalStateException    se o banco falhar
     */
    public void alterar(Ranking ranking) {
        validar(ranking);
        String sql = "UPDATE Ranking SET ds_tipo = ?, dt_inicio = ?, dt_fim = ?, ds_status = ? "
                + "WHERE id_ranking = ? AND ds_status = 'ABERTO'";

        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, ranking.getTipo().name());
            ps.setDate(2, Date.valueOf(ranking.getDataInicio()));
            ps.setDate(3, Date.valueOf(ranking.getDataFim()));
            ps.setString(4, ranking.getStatus().name());
            ps.setInt(5, ranking.getIdRanking());

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("O ranking não existe ou já está encerrado e não pode ser alterado.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível alterar o ranking.", e);
        }
    }

    /**
     * Fecha o ranking de forma atômica: só troca ABERTO por FECHADO (única mudança de estado
     * permitida) e diz se foi esta chamada que fechou. Chamadas repetidas ou simultâneas fecham no
     * máximo uma vez, e não mexem nas demais colunas.
     *
     * @return {@code true} se estava aberto e foi fechado agora; {@code false} se já estava fechado
     *         ou não existe
     * @throws IllegalStateException se o banco falhar
     */
    public boolean fechar(int idRanking) {
        String sql = "UPDATE Ranking SET ds_status = 'FECHADO' WHERE id_ranking = ? AND ds_status = 'ABERTO'";
        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, idRanking);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível fechar o ranking.", e);
        }
    }

    /**
     * Informa se já existe ranking do mesmo tipo cujo período cruza o intervalo informado. Serve
     * para recusar períodos sobrepostos: o prêmio é identificado por usuário + data final, então
     * dois rankings individuais com a mesma data final se confundiriam.
     *
     * @param ignorarIdRanking ranking a desconsiderar (o que está sendo alterado); use 0 para nenhum
     * @throws IllegalStateException se o banco falhar
     */
    public boolean existeSobreposicao(TipoRankingEnum tipo, java.time.LocalDate inicio,
                                      java.time.LocalDate fim, int ignorarIdRanking) {
        String sql = "SELECT COUNT(*) FROM Ranking WHERE ds_tipo = ? AND dt_inicio <= ? AND dt_fim >= ? "
                + "AND id_ranking <> ?";
        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, tipo.name());
            ps.setDate(2, Date.valueOf(fim));
            ps.setDate(3, Date.valueOf(inicio));
            ps.setInt(4, ignorarIdRanking);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível verificar os períodos dos rankings.", e);
        }
    }

    /**
     * Exclui um período de ranking ABERTO pelo seu identificador. As posições do ranking são
     * apagadas antes, na mesma transação (a chave estrangeira impediria apagar o ranking com
     * posições). Ou sai tudo, ou nada. Ranking encerrado é histórico e não pode ser excluído.
     *
     * @param idRanking identificador do ranking a ser excluído
     * @throws IllegalArgumentException se o ranking já estiver encerrado
     * @throws IllegalStateException    se o banco falhar (nada é apagado)
     */
    public void excluir(int idRanking) {
        try (Connection conexao = ConnectionFactory.obterConexao()) {
            boolean confirmado = false;
            try {
                conexao.setAutoCommit(false);

                // Trava a linha e confere o estado: um fechamento simultâneo não é atropelado.
                try (PreparedStatement trava = conexao.prepareStatement(
                        "SELECT ds_status FROM Ranking WHERE id_ranking = ? FOR UPDATE")) {
                    trava.setInt(1, idRanking);
                    try (ResultSet rs = trava.executeQuery()) {
                        if (rs.next() && StatusRankingEnum.FECHADO.name().equals(rs.getString(1))) {
                            throw new IllegalArgumentException(
                                    "Ranking encerrado não pode ser excluído: o histórico é preservado.");
                        }
                    }
                }

                try (PreparedStatement posicoes = conexao.prepareStatement(
                        "DELETE FROM Posicao_Ranking WHERE id_ranking = ?")) {
                    posicoes.setInt(1, idRanking);
                    posicoes.executeUpdate();
                }
                try (PreparedStatement ranking = conexao.prepareStatement(
                        "DELETE FROM Ranking WHERE id_ranking = ?")) {
                    ranking.setInt(1, idRanking);
                    ranking.executeUpdate();
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
            throw new IllegalStateException("Não foi possível excluir o ranking.", e);
        }
    }

    /**
     * Busca um período de ranking pelo seu identificador.
     *
     * @param idRanking identificador do ranking
     * @return o ranking encontrado, ou {@code null} caso não exista
     */
    public Ranking buscarPorId(int idRanking) {
        String sql = "SELECT " + COLUNAS + " FROM Ranking WHERE id_ranking = ?";
        Ranking ranking = null;

        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setInt(1, idRanking);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ranking = mapear(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ranking;
    }

    /**
     * Lista todos os períodos de ranking cadastrados.
     *
     * @return lista de rankings (pode vir vazia, nunca nula)
     */
    public List<Ranking> listar() {
        String sql = "SELECT " + COLUNAS + " FROM Ranking ORDER BY id_ranking";
        List<Ranking> lista = new ArrayList<>();

        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /** Recusa, antes de ir ao banco, o ranking sem tipo, sem status ou com período inválido. */
    private void validar(Ranking ranking) {
        if (ranking == null || ranking.getTipo() == null || ranking.getStatus() == null) {
            throw new IllegalArgumentException("O tipo e o status do ranking são obrigatórios.");
        }
        if (!ranking.periodoValido()) {
            throw new IllegalArgumentException("O período do ranking é inválido.");
        }
    }

    /** Monta o ranking a partir da linha atual (colunas lidas pelo nome). */
    private Ranking mapear(ResultSet rs) throws SQLException {
        Ranking ranking = new Ranking();
        ranking.setIdRanking(rs.getInt("id_ranking"));
        ranking.setTipo(TipoRankingEnum.valueOf(rs.getString("ds_tipo")));
        if (rs.getDate("dt_inicio") != null) {
            ranking.setDataInicio(rs.getDate("dt_inicio").toLocalDate());
        }
        if (rs.getDate("dt_fim") != null) {
            ranking.setDataFim(rs.getDate("dt_fim").toLocalDate());
        }
        ranking.setStatus(StatusRankingEnum.valueOf(rs.getString("ds_status")));
        return ranking;
    }
}
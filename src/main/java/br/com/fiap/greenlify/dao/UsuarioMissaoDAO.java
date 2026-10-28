package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.models.Missao;
import br.com.fiap.greenlify.models.Usuario;
import br.com.fiap.greenlify.models.UsuarioMissao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO da entidade UsuarioMissao, que registra a relação entre um usuário e uma missão
 * (quantidade de tentativas e progresso atual) na tabela Usuario_Missao.
 *
 * Ordem das colunas assumida na tabela, usada na leitura por índice:
 * 1 id_usuario_missao, 2 nr_tentativas, 3 nr_progresso_atual, 4 id_usuario, 5 id_missao.
 *
 * Missões contínuas contam o progresso por ciclo mensal: o progresso do ciclo atual é o
 * número de uploads APROVADOS do usuário na missão dentro do mês corrente (ver
 * {@link UploadDAO#contarAprovadosNoCiclo(int, int, LocalDate)}). Nas leituras, o
 * progresso das contínuas é sempre o do ciclo atual, então volta a 0 a cada mês.
 */
public class UsuarioMissaoDAO {

    /** Resultado de {@link #avancarProgressoMensal}: novo progresso e se este passo concluiu a missão. */
    public record Avanco(int progresso, boolean concluiu) {
    }

    /** Meta e período de vigência de uma missão mensal, lidos dentro da transação. */
    private record Vigencia(int meta, LocalDate inicio, LocalDate fim) {
    }

    /**
     * Cadastra o registro de um usuário em uma missão, em uma transação. O id_usuario_missao
     * é gerado pelo banco (auto increment), então fica de fora do INSERT e, ao final, é
     * preenchido no próprio objeto.
     *
     * <p>Trava o usuário e a missão (FOR UPDATE) e só então confere: a missão existe, uma
     * missão mensal ainda não terminou e o usuário ainda não a possui. Assim duas chamadas
     * simultâneas não geram registro duplicado e o cadastro não corre contra uma alteração
     * da missão feita pelo administrador.</p>
     *
     * @param usuarioMissao dados do registro a ser cadastrado
     * @throws IllegalArgumentException se o usuário ou a missão não existem, a missão mensal
     *                                  já terminou ou o usuário já possui a missão
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public void cadastrar(UsuarioMissao usuarioMissao) {
        int idUsuario = usuarioMissao.getUsuario().getIdUsuario();
        int idMissao = usuarioMissao.getMissao().getIdMissao();

        try (Connection con = ConnectionFactory.obterConexao()) {
            boolean confirmado = false;
            try {
                con.setAutoCommit(false);

                if (!travarUsuario(con, idUsuario)) {
                    throw new IllegalArgumentException("O usuário informado não existe.");
                }
                if (!travarMissao(con, idMissao)) {
                    throw new IllegalArgumentException("A missão informada não existe.");
                }
                Vigencia vigencia = buscarVigenciaMensal(con, idMissao);
                if (vigencia != null && vigencia.fim() != null && LocalDate.now().isAfter(vigencia.fim())) {
                    throw new IllegalArgumentException("A missão já terminou.");
                }
                if (buscarPorUsuarioEMissao(con, idUsuario, idMissao) != null) {
                    throw new IllegalArgumentException("O usuário já possui essa missão.");
                }

                cadastrar(con, usuarioMissao);

                con.commit();
                confirmado = true;
            } finally {
                if (!confirmado) {
                    con.rollback();
                }
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível cadastrar a missão do usuário.", e);
        }
    }

    /**
     * Exclui um registro de usuário/missão pelo id.
     *
     * @param idUsuarioMissao id do registro a ser removido
     * @throws IllegalStateException se o banco falhar
     */
    public void excluir(int idUsuarioMissao) {
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Usuario_Missao WHERE id_usuario_missao = ?")) {
            ps.setInt(1, idUsuarioMissao);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir o registro de usuário e missão.", e);
        }
    }

    /**
     * Busca um registro de usuário/missão pelo id.
     *
     * @param idUsuarioMissao id do registro procurado
     * @return o registro encontrado, ou {@code null} caso não exista
     */
    public UsuarioMissao buscarPorId(int idUsuarioMissao) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        UsuarioMissao usuarioMissao = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Usuario_Missao WHERE id_usuario_missao = ?");
            ps.setInt(1, idUsuarioMissao);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                usuarioMissao = new UsuarioMissao();
                usuarioMissao.setIdUsuarioMissao(rs.getInt(1));
                usuarioMissao.setTentativas(rs.getInt(2));
                usuarioMissao.setProgressoAtual(rs.getInt(3));

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt(4));
                usuarioMissao.setUsuario(usuario);

                Missao missao = new Missao();
                missao.setIdMissao(rs.getInt(5));
                usuarioMissao.setMissao(missao);
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
        if (usuarioMissao != null) {
            aplicarProgressoDoCiclo(List.of(usuarioMissao), usuarioMissao.getUsuario().getIdUsuario());
        }
        return usuarioMissao;
    }

    /**
     * Lista todos os registros de usuário/missão cadastrados.
     *
     * @return lista de registros (pode vir vazia, nunca nula)
     */
    public List<UsuarioMissao> listar() {
        Connection conexao = ConnectionFactory.obterConexao();
        List<UsuarioMissao> usuarioMissoes = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Usuario_Missao");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UsuarioMissao usuarioMissao = new UsuarioMissao();

                usuarioMissao.setIdUsuarioMissao(rs.getInt(1));
                usuarioMissao.setTentativas(rs.getInt(2));
                usuarioMissao.setProgressoAtual(rs.getInt(3));

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt(4));
                usuarioMissao.setUsuario(usuario);

                Missao missao = new Missao();
                missao.setIdMissao(rs.getInt(5));
                usuarioMissao.setMissao(missao);

                usuarioMissoes.add(usuarioMissao);
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
        aplicarProgressoDoCiclo(usuarioMissoes, null);
        return usuarioMissoes;
    }

    /**
     * Lista todas as missões associadas a um usuário.
     *
     * @param idUsuario id do usuário dono dos registros
     * @return lista de registros do usuário (pode vir vazia, nunca nula)
     */
    public List<UsuarioMissao> listarPorUsuario(int idUsuario) {
        Connection conexao = ConnectionFactory.obterConexao();
        List<UsuarioMissao> usuarioMissoes = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Usuario_Missao WHERE id_usuario = ?");
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UsuarioMissao usuarioMissao = new UsuarioMissao();

                usuarioMissao.setIdUsuarioMissao(rs.getInt(1));
                usuarioMissao.setTentativas(rs.getInt(2));
                usuarioMissao.setProgressoAtual(rs.getInt(3));

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt(4));
                usuarioMissao.setUsuario(usuario);

                Missao missao = new Missao();
                missao.setIdMissao(rs.getInt(5));
                usuarioMissao.setMissao(missao);

                usuarioMissoes.add(usuarioMissao);
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
        aplicarProgressoDoCiclo(usuarioMissoes, idUsuario);
        return usuarioMissoes;
    }

    /**
     * Troca o progresso das missões contínuas pelo do ciclo mensal atual (uploads
     * aprovados no mês corrente). Missões mensais não são alteradas. Uma única consulta.
     *
     * @param lista     registros já lidos
     * @param idUsuario usuário dos registros, ou {@code null} para todos
     */
    private void aplicarProgressoDoCiclo(List<UsuarioMissao> lista, Integer idUsuario) {
        if (lista.isEmpty()) {
            return;
        }
        LocalDate inicio = UploadDAO.inicioDoCiclo(LocalDate.now());
        String sql = "SELECT um.id_usuario_missao, COUNT(up.id_upload) AS total "
                + "FROM Usuario_Missao um "
                + "JOIN Missao_Continua mc ON mc.id_missao = um.id_missao "
                + "LEFT JOIN Upload up ON up.id_usuario = um.id_usuario AND up.id_missao = um.id_missao "
                + "AND up.ds_status = ? AND up.dt_envio >= ? AND up.dt_envio < ? "
                + (idUsuario != null ? "WHERE um.id_usuario = ? " : "")
                + "GROUP BY um.id_usuario_missao";

        Map<Integer, Integer> doCiclo = new HashMap<>();
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, StatusUploadEnum.APROVADO.name());
            ps.setDate(2, Date.valueOf(inicio));
            ps.setDate(3, Date.valueOf(inicio.plusMonths(1)));
            if (idUsuario != null) {
                ps.setInt(4, idUsuario);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    doCiclo.put(rs.getInt(1), rs.getInt(2));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        for (UsuarioMissao um : lista) {
            Integer total = doCiclo.get(um.getIdUsuarioMissao());
            if (total != null) {
                um.setProgressoAtual(total);
            }
        }
    }

    // ------------------------------------------------------------------
    // Operações dentro de uma transação (usam a conexão de quem chamou:
    // não fazem commit nem fecham a conexão; erros de SQL sobem para o rollback)
    // ------------------------------------------------------------------

    /**
     * Trava a linha do usuário (FOR UPDATE) até o fim da transação: operações de
     * pontuação do mesmo usuário ficam em fila, mesmo em outra instância da aplicação.
     *
     * @return {@code false} se o usuário não existe
     */
    public boolean travarUsuario(Connection con, int idUsuario) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_usuario FROM Usuario WHERE id_usuario = ? FOR UPDATE")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Trava a linha da missão (FOR UPDATE) até o fim da transação: cadastro de usuário na missão
     * e alteração/exclusão da missão pelo administrador ficam em fila.
     *
     * @return {@code false} se a missão não existe
     */
    public boolean travarMissao(Connection con, int idMissao) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_missao FROM Missao WHERE id_missao = ? FOR UPDATE")) {
            ps.setInt(1, idMissao);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Meta e período da missão mensal, ou {@code null} se a missão não é mensal. */
    private Vigencia buscarVigenciaMensal(Connection con, int idMissao) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT vl_meta, dt_inicio, dt_fim FROM Missao_Mensal WHERE id_missao = ?")) {
            ps.setInt(1, idMissao);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Date inicio = rs.getDate("dt_inicio");
                Date fim = rs.getDate("dt_fim");
                return new Vigencia(rs.getInt("vl_meta"),
                        inicio != null ? inicio.toLocalDate() : null,
                        fim != null ? fim.toLocalDate() : null);
            }
        }
    }

    /**
     * Busca o registro do usuário na missão (há no máximo um por par usuário + missão).
     *
     * @return o registro, ou {@code null} se o usuário ainda não começou a missão
     */
    public UsuarioMissao buscarPorUsuarioEMissao(Connection con, int idUsuario, int idMissao) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM Usuario_Missao WHERE id_usuario = ? AND id_missao = ?")) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idMissao);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                UsuarioMissao um = new UsuarioMissao();
                um.setIdUsuarioMissao(rs.getInt(1));
                um.setTentativas(rs.getInt(2));
                um.setProgressoAtual(rs.getInt(3));

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt(4));
                um.setUsuario(usuario);

                Missao missao = new Missao();
                missao.setIdMissao(rs.getInt(5));
                um.setMissao(missao);
                return um;
            }
        }
    }

    /** Cadastra o registro e preenche o id gerado no próprio objeto. */
    public void cadastrar(Connection con, UsuarioMissao usuarioMissao) throws SQLException {
        String sql = "INSERT INTO Usuario_Missao (nr_tentativas, nr_progresso_atual, id_usuario, id_missao) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID_USUARIO_MISSAO"})) {
            ps.setInt(1, usuarioMissao.getTentativas());
            ps.setInt(2, usuarioMissao.getProgressoAtual());
            ps.setInt(3, usuarioMissao.getUsuario().getIdUsuario());
            ps.setInt(4, usuarioMissao.getMissao().getIdMissao());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    usuarioMissao.setIdUsuarioMissao(rs.getInt(1));
                }
            }
        }
    }

    /** Atualiza só tentativas e progresso do registro. */
    public void atualizarProgresso(Connection con, int idUsuarioMissao, int tentativas, int progresso)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Usuario_Missao SET nr_tentativas = ?, nr_progresso_atual = ? WHERE id_usuario_missao = ?")) {
            ps.setInt(1, tentativas);
            ps.setInt(2, progresso);
            ps.setInt(3, idUsuarioMissao);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Missões concluídas (usadas pelos rankings)
    // ------------------------------------------------------------------

    /**
     * Conclusões de missões contínuas: parte de Usuario_Missao e conta cada mês em que o
     * usuário tem pelo menos um upload APROVADO daquela missão (o limite de realizações não é
     * exigido aqui). É agrupado por mês porque o progresso reinicia todo mês e assim as
     * conclusões de meses anteriores continuam valendo nos rankings.
     *
     * <p>Missões mensais não entram aqui: a conclusão mensal vale para o streak e para os
     * registros de conclusão (dt_conclusao), não para a pontuação do ranking.</p>
     */
    private static final String SQL_CONCLUIDAS_CONTINUAS =
            "SELECT um.id_usuario AS id_usuario "
                    + "FROM Usuario_Missao um "
                    + "JOIN Missao_Continua mc ON mc.id_missao = um.id_missao "
                    + "JOIN Upload up ON up.id_usuario = um.id_usuario AND up.id_missao = um.id_missao "
                    + "WHERE up.ds_status = 'APROVADO' ";

    private static final String SQL_CONCLUIDAS_CONTINUAS_AGRUPAR =
            "GROUP BY um.id_usuario_missao, um.id_usuario, TRUNC(up.dt_envio, 'MM') ";

    /** Monta a consulta de conclusões por usuário, com ou sem filtro de período. */
    private static String sqlConcluidas(boolean comPeriodo) {
        return "SELECT id_usuario, COUNT(*) AS total FROM ("
                + SQL_CONCLUIDAS_CONTINUAS
                + (comPeriodo ? "AND up.dt_envio >= ? AND up.dt_envio < ? " : "")
                + SQL_CONCLUIDAS_CONTINUAS_AGRUPAR
                + ") GROUP BY id_usuario";
    }

    /**
     * Conta, por usuário, as missões realizadas: quantos registros de Usuario_Missao (de missão
     * contínua) têm pelo menos um upload APROVADO da mesma missão. Cada missão conta uma única vez,
     * não importa quantos uploads aprovados ou em quantos meses. É o número mostrado no perfil e
     * no resumo do usuário.
     *
     * @return mapa idUsuario -> total de missões concluídas (só usuários com pelo menos uma)
     */
    public Map<Integer, Integer> contarConcluidasPorUsuario() {
        String sql = "SELECT um.id_usuario AS id_usuario, COUNT(DISTINCT um.id_usuario_missao) AS total "
                + "FROM Usuario_Missao um "
                + "JOIN Missao_Continua mc ON mc.id_missao = um.id_missao "
                + "WHERE EXISTS (SELECT 1 FROM Upload up "
                + "WHERE up.id_usuario = um.id_usuario AND up.id_missao = um.id_missao "
                + "AND up.ds_status = 'APROVADO') "
                + "GROUP BY um.id_usuario";
        return consultarConcluidas(sql, null, null);
    }

    /**
     * Conta, por usuário, as missões contínuas concluídas dentro de um período (mês dos
     * uploads aprovados dentro do período).
     *
     * @param inicio primeiro dia do período (inclusive)
     * @param fim    último dia do período (inclusive)
     * @return mapa idUsuario -> total de missões concluídas no período
     */
    public Map<Integer, Integer> contarConcluidasPorUsuario(LocalDate inicio, LocalDate fim) {
        return consultarConcluidas(sqlConcluidas(true), inicio, fim);
    }

    /**
     * Pontos elegíveis por usuário dentro de um período (ranking mensal). Mesma regra do all time
     * ({@link #somarPontosElegiveisPorUsuario()}): para cada Usuario_Missao de missão contínua,
     * cada mês com pelo menos um upload APROVADO daquela missão ({@code dt_envio} dentro do
     * período) vale uma vez os pontos da pontuação da missão.
     *
     * @param inicio primeiro dia do período (inclusive)
     * @param fim    último dia do período (inclusive)
     * @return mapa idUsuario -> pontos no período (só usuários com pontos)
     * @throws IllegalStateException se o banco falhar
     */
    public Map<Integer, Integer> somarPontosElegiveisPorUsuario(LocalDate inicio, LocalDate fim) {
        String sql = "SELECT id_usuario, SUM(pontos) AS total FROM ("
                + "SELECT um.id_usuario AS id_usuario, p.nr_pontuacao AS pontos "
                + "FROM Usuario_Missao um "
                + "JOIN Missao_Continua mc ON mc.id_missao = um.id_missao "
                + "JOIN Pontuacao p ON p.id_pontuacao = mc.id_pontuacao "
                + "JOIN Upload up ON up.id_usuario = um.id_usuario AND up.id_missao = um.id_missao "
                + "WHERE up.ds_status = 'APROVADO' "
                + "AND up.dt_envio >= ? AND up.dt_envio < ? "
                + "GROUP BY um.id_usuario_missao, um.id_usuario, p.nr_pontuacao, "
                + "TRUNC(up.dt_envio, 'MM')) "
                + "GROUP BY id_usuario";

        Map<Integer, Integer> totais = new HashMap<>();
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(inicio));
            ps.setDate(2, Date.valueOf(fim.plusDays(1)));   // dia seguinte, exclusivo: pega o dia final inteiro

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totais.put(rs.getInt("id_usuario"), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível calcular os pontos do período.", e);
        }
        return totais;
    }

    /**
     * Pontos elegíveis por usuário, a métrica do ranking all time no Redis: para cada Usuario_Missao
     * de missão contínua, cada mês com pelo menos um upload APROVADO daquela missão vale uma vez os
     * pontos da pontuação da missão. Missões mensais, uploads que não estão APROVADOS e usuários sem
     * registro em Usuario_Missao na missão não pontuam. É a fonte da verdade com que a reconciliação corrige o Redis.
     *
     * <p>Ao contrário das consultas de contagem, uma falha do banco não devolve mapa vazio: o mapa
     * vazio significaria "ninguém pontuou" e a reconciliação apagaria o Redis.</p>
     *
     * @return mapa idUsuario -> pontos elegíveis (só usuários com pontos)
     * @throws IllegalStateException se o banco falhar
     */
    public Map<Integer, Integer> somarPontosElegiveisPorUsuario() {
        String sql = "SELECT id_usuario, SUM(pontos) AS total FROM ("
                + "SELECT um.id_usuario AS id_usuario, p.nr_pontuacao AS pontos "
                + "FROM Usuario_Missao um "
                + "JOIN Missao_Continua mc ON mc.id_missao = um.id_missao "
                + "JOIN Pontuacao p ON p.id_pontuacao = mc.id_pontuacao "
                + "JOIN Upload up ON up.id_usuario = um.id_usuario AND up.id_missao = um.id_missao "
                + "WHERE up.ds_status = 'APROVADO' "
                + "GROUP BY um.id_usuario_missao, um.id_usuario, p.nr_pontuacao, "
                + "TRUNC(up.dt_envio, 'MM')) "
                + "GROUP BY id_usuario";

        Map<Integer, Integer> totais = new HashMap<>();
        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                totais.put(rs.getInt("id_usuario"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível calcular os pontos elegíveis.", e);
        }
        return totais;
    }

    /**
     * Grava a data em que o usuário concluiu a missão contínua, na conexão (transação) de quem
     * chamou. Dentro do mesmo mês a primeira conclusão é a que vale; em um novo mês (novo
     * ciclo das missões contínuas) a data é atualizada para a conclusão mais recente, e não
     * fica presa à primeira de todas.
     *
     * @param idUsuarioMissao id do registro em Usuario_Missao
     * @param data            dia da conclusão
     */
    public void registrarConclusao(Connection con, int idUsuarioMissao, LocalDate data) throws SQLException {
        String sql = "UPDATE Usuario_Missao SET dt_conclusao = ? "
                + "WHERE id_usuario_missao = ? AND (dt_conclusao IS NULL OR dt_conclusao < ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(data));
            ps.setInt(2, idUsuarioMissao);
            ps.setDate(3, Date.valueOf(UploadDAO.inicioDoCiclo(data)));   // conclusões de meses anteriores
            ps.executeUpdate();
        }
    }

    /**
     * Avança em um passo o progresso de uma missão mensal do usuário. Só avança: o
     * encerramento da missão é uma etapa separada ({@link #encerrarMissaoMensal}), executada
     * na mesma transação apenas quando o passo atinge a meta.
     *
     * <p>Tudo em uma transação com o usuário travado (FOR UPDATE). A meta e o período são lidos
     * aqui dentro, não vêm do chamador: duas chamadas simultâneas não passam da meta nem
     * concluem a missão duas vezes, e nenhum passo é aceito fora da vigência. Se qualquer etapa
     * falhar (inclusive o streak), nada é gravado.</p>
     *
     * @return o novo progresso e se a missão acabou de ser concluída
     * @throws IllegalArgumentException se o usuário não começou a missão, ela não é mensal, está
     *                                  fora do período de vigência ou já foi concluída
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public Avanco avancarProgressoMensal(int idUsuario, int idMissao, LocalDate dia) {
        try (Connection con = ConnectionFactory.obterConexao()) {
            boolean confirmado = false;
            try {
                con.setAutoCommit(false);

                if (!travarUsuario(con, idUsuario)) {
                    throw new IllegalArgumentException("O usuário informado não existe.");
                }
                UsuarioMissao um = buscarPorUsuarioEMissao(con, idUsuario, idMissao);
                if (um == null) {
                    throw new IllegalArgumentException("Registro de usuário e missão não encontrado");
                }
                Vigencia vigencia = buscarVigenciaMensal(con, idMissao);
                if (vigencia == null || vigencia.meta() <= 0) {
                    throw new IllegalArgumentException("A missão informada não é uma missão mensal.");
                }
                if (vigencia.inicio() == null || vigencia.fim() == null
                        || dia.isBefore(vigencia.inicio()) || dia.isAfter(vigencia.fim())) {
                    throw new IllegalArgumentException("A missão está fora do período de vigência.");
                }
                if (um.concluida(vigencia.meta())) {
                    throw new IllegalArgumentException("A missão já foi concluída.");
                }

                // 1) avanço: só o progresso e a tentativa
                if (!um.avancarProgresso(vigencia.meta())) {
                    throw new IllegalArgumentException("A missão já foi concluída.");
                }
                atualizarProgresso(con, um.getIdUsuarioMissao(), um.getTentativas(), um.getProgressoAtual());

                // 2) encerramento: só se este passo atingiu a meta
                boolean concluiu = um.concluida(vigencia.meta());
                if (concluiu) {
                    encerrarMissaoMensal(con, idUsuario, um.getIdUsuarioMissao(), dia);
                }

                con.commit();
                confirmado = true;
                return new Avanco(um.getProgressoAtual(), concluiu);
            } finally {
                if (!confirmado) {
                    con.rollback();
                }
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível registrar o progresso.", e);
        }
    }

    /**
     * Encerra a missão mensal do usuário, na conexão (transação) de quem chamou: grava a data de
     * conclusão (uma única vez, por isso exige dt_conclusao ainda vazia) e atualiza o streak
     * do usuário. Não mexe em ranking: a missão mensal não pontua nele.
     *
     * @throws IllegalArgumentException se a missão já tinha sido encerrada
     */
    public void encerrarMissaoMensal(Connection con, int idUsuario, int idUsuarioMissao, LocalDate dia)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Usuario_Missao SET dt_conclusao = ? "
                        + "WHERE id_usuario_missao = ? AND dt_conclusao IS NULL")) {
            ps.setDate(1, Date.valueOf(dia));
            ps.setInt(2, idUsuarioMissao);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("A missão já foi concluída.");
            }
        }
        new StreakDAO().registrarAtividadeMensal(con, idUsuario, dia);   // única regra do streak
    }

    /** Executa a contagem com uma conexão local (não usa o campo compartilhado). */
    private Map<Integer, Integer> consultarConcluidas(String sql, LocalDate inicio, LocalDate fim) {
        Map<Integer, Integer> totais = new HashMap<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (inicio != null && fim != null) {
                ps.setDate(1, Date.valueOf(inicio));
                ps.setDate(2, Date.valueOf(fim.plusDays(1)));   // dia seguinte, exclusivo: pega o dia final inteiro
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totais.put(rs.getInt("id_usuario"), rs.getInt("total"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totais;
    }
}
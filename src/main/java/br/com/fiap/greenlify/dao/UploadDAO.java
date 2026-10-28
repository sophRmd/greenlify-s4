package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.enums.CategoriaEnum;
import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.enums.TipoMetricaEnum;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;
import br.com.fiap.greenlify.models.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO responsável pelos uploads de comprovantes enviados pelos usuários
 * para validação de uma missão contínua.
 */
public class UploadDAO {

    /**
     * Upload + Usuário (u_) + Missão contínua (m_, mc_, p_) em uma única consulta.
     * A missão só vem preenchida se for uma missão contínua (mesmo critério do
     * MissaoContinuaDAO.buscarPorId, que usa INNER JOIN com Missao_Continua).
     */
    private static final String SELECT_COMPLETO =
            "SELECT up.id_upload, up.ds_status, up.dt_envio, up.dt_analise, up.ds_arquivo, "
                    + "       up.ds_analise, up.nr_confianca_duplicidade, "
                    + "       u.id_usuario AS u_id, u.nm_usuario AS u_nome, u.ds_cpf AS u_cpf, "
                    + "       u.dt_nascimento AS u_nascimento, u.ds_email AS u_email, u.ds_senha AS u_senha, "
                    + "       u.nr_nivel_acesso AS u_nivel, u.ds_status_conta AS u_status, u.id_grupo AS u_grupo, "
                    + "       mc.id_missao AS mc_id, m.nm_missao AS m_nome, m.ds_missao AS m_descricao, "
                    + "       m.ds_requisitos AS m_requisitos, m.ds_comprovacao AS m_comprovacao, "
                    + "       m.ds_categoria AS m_categoria, m.ds_tipo_missao AS m_tipo, "
                    + "       mc.nr_limite_realizacoes AS mc_limite, mc.ds_tipo_metrica AS mc_metrica, "
                    + "       mc.vl_metrica AS mc_valor, p.id_pontuacao AS p_id, p.nr_pontuacao AS p_valor "
                    + "FROM Upload up "
                    + "LEFT JOIN Usuario u ON u.id_usuario = up.id_usuario "
                    + "LEFT JOIN Missao_Continua mc ON mc.id_missao = up.id_missao "
                    + "LEFT JOIN Missao m ON m.id_missao = mc.id_missao "
                    + "LEFT JOIN Pontuacao p ON p.id_pontuacao = mc.id_pontuacao ";

    /**
     * Busca um upload pelo id, carregando também o usuário e a missão
     * relacionados, quando existirem, em uma única consulta.
     *
     * @param idUpload id do upload procurado
     * @return o upload encontrado, ou {@code null} se não existir
     */
    public Upload buscarPorId(int idUpload) {
        Upload upload = null;

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "WHERE up.id_upload = ?")) {

            ps.setInt(1, idUpload);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    upload = mapear(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return upload;
    }

    /**
     * Lista todos os uploads cadastrados, cada um já com o usuário e a
     * missão correspondentes carregados (quando existirem), em uma única consulta.
     *
     * @return lista de uploads (pode vir vazia, nunca nula)
     */
    public List<Upload> listar() {
        List<Upload> uploads = new ArrayList<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(SELECT_COMPLETO + "ORDER BY up.id_upload");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                uploads.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return uploads;
    }

    private Upload mapear(ResultSet rs) throws SQLException {
        Upload upload = new Upload();
        upload.setIdUpload(rs.getInt("id_upload"));
        upload.setStatus(StatusUploadEnum.valueOf(rs.getString("ds_status")));
        upload.setDataEnvio(rs.getDate("dt_envio").toLocalDate());

        if (rs.getDate("dt_analise") != null) {
            upload.setDataAnalise(rs.getDate("dt_analise").toLocalDate());
        }

        upload.setArquivo(rs.getString("ds_arquivo"));
        upload.setAnalise(rs.getString("ds_analise"));
        upload.setConfiancaDuplicidade(rs.getInt("nr_confianca_duplicidade"));

        upload.setUsuario(mapearUsuario(rs));
        upload.setMissao(mapearMissao(rs));
        return upload;
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

    /** Monta a missão contínua a partir das colunas m_*, mc_* e p_*; null se não existir. */
    private MissaoContinua mapearMissao(ResultSet rs) throws SQLException {
        int idMissao = rs.getInt("mc_id");
        if (rs.wasNull()) {
            return null;
        }
        MissaoContinua continua = new MissaoContinua();
        continua.setIdMissao(idMissao);
        continua.setNome(rs.getString("m_nome"));
        continua.setDescricao(rs.getString("m_descricao"));
        continua.setRequisitos(rs.getString("m_requisitos"));
        continua.setComoComprovar(rs.getString("m_comprovacao"));
        continua.setCategoria(CategoriaEnum.valueOf(rs.getString("m_categoria")));
        continua.setTipoMissao(TipoMissaoEnum.valueOf(rs.getString("m_tipo")));
        continua.setLimiteRealizacoes(rs.getInt("mc_limite"));
        continua.setTipoMetrica(TipoMetricaEnum.valueOf(rs.getString("mc_metrica")));
        continua.setValorMetrica(rs.getInt("mc_valor"));

        int idPontuacao = rs.getInt("p_id");
        if (!rs.wasNull()) {
            Pontuacao pontuacao = new Pontuacao();
            pontuacao.setIdPontuacao(idPontuacao);
            pontuacao.setValorBase(rs.getInt("p_valor"));
            continua.setPontuacao(pontuacao);
        }
        return continua;
    }

    /**
     * Conta quantas missões cada usuário realizou, isto é, quantos uploads
     * APROVADOS ele tem, agrupados por usuário, em uma única consulta.
     *
     * <p>Os limites são opcionais: {@code inicio} e {@code fim} (inclusive) filtram pela
     * data de envio do upload; com ambos nulos, conta tudo (ranking all time).</p>
     *
     * @param inicio primeiro dia do período, ou {@code null} para não limitar
     * @param fim    último dia do período (inclusive), ou {@code null} para não limitar
     * @return mapa idUsuario -> total de missões realizadas (usuários sem nenhuma não aparecem)
     */
    public Map<Integer, Integer> contarAprovadosPorUsuario(LocalDate inicio, LocalDate fim) {
        StringBuilder sql = new StringBuilder(
                "SELECT id_usuario, COUNT(*) AS total FROM Upload "
                        + "WHERE ds_status = ? AND id_usuario IS NOT NULL");
        if (inicio != null) {
            sql.append(" AND dt_envio >= ?");
        }
        if (fim != null) {
            sql.append(" AND dt_envio < ?");   // dia seguinte, exclusivo: pega o dia final inteiro
        }
        sql.append(" GROUP BY id_usuario");

        Map<Integer, Integer> totais = new HashMap<>();

        try (Connection con = ConnectionFactory.obterConexao();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int i = 1;
            ps.setString(i++, StatusUploadEnum.APROVADO.name());
            if (inicio != null) {
                ps.setDate(i++, Date.valueOf(inicio));
            }
            if (fim != null) {
                ps.setDate(i++, Date.valueOf(fim.plusDays(1)));
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

    // ------------------------------------------------------------------
    // Ciclo mensal das missões contínuas e gravação transacional do envio
    // ------------------------------------------------------------------

    /** Primeiro dia do ciclo (mês) a que a data pertence: o progresso das contínuas reinicia nele. */
    public static LocalDate inicioDoCiclo(LocalDate data) {
        return data.withDayOfMonth(1);
    }

    /**
     * Progresso do usuário na missão no ciclo (mês) da data informada: quantos uploads
     * APROVADOS ele tem na missão entre o primeiro dia do mês e o primeiro dia do mês seguinte.
     */
    public int contarAprovadosNoCiclo(int idUsuario, int idMissao, LocalDate dia) {
        try (Connection con = ConnectionFactory.obterConexao()) {
            return contarAprovadosNoCiclo(con, idUsuario, idMissao, dia);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /** Mesma contagem de {@link #contarAprovadosNoCiclo(int, int, LocalDate)}, na conexão (transação) recebida. */
    public int contarAprovadosNoCiclo(Connection con, int idUsuario, int idMissao, LocalDate dia)
            throws SQLException {
        LocalDate inicio = inicioDoCiclo(dia);
        String sql = "SELECT COUNT(*) FROM Upload WHERE id_usuario = ? AND id_missao = ? "
                + "AND ds_status = ? AND dt_envio >= ? AND dt_envio < ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idMissao);
            ps.setString(3, StatusUploadEnum.APROVADO.name());
            ps.setDate(4, Date.valueOf(inicio));
            ps.setDate(5, Date.valueOf(inicio.plusMonths(1)));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Resultado de {@link #registrarEnvio}: se o envio concluiu a missão no mês e quantos pontos foram
     * creditados na carteira (os mesmos usados no ranking all time, lidos da missão já travada).
     */
    public record ResultadoEnvio(boolean concluiu, int pontos) {
    }

    /**
     * Grava o envio inteiro em UMA transação, com uma única conexão: upload, fingerprint,
     * frames, progresso do usuário na missão e, se o envio conclui a missão, o crédito dos
     * pontos na carteira e a data de conclusão. Se qualquer etapa falhar, nada é gravado.
     * <p>
     * Primeiro trava a linha do usuário e a da missão (FOR UPDATE) e só então confere o limite
     * do mês: dois envios simultâneos do mesmo usuário ficam em fila, e o segundo enxerga o
     * progresso do primeiro, então os pontos nunca são creditados em duplicidade. O limite e a
     * pontuação são relidos do banco já com a missão travada (não vêm do objeto carregado antes),
     * então uma alteração da missão pelo administrador não é perdida nem aplicada pela metade.
     * O saldo é somado pelo próprio SQL ({@link CarteiraDAO#creditarPontos}).
     * <p>
     * O progresso é do ciclo mensal e só conta uploads APROVADOS no mês do envio (incluindo este);
     * envios reprovados ou parcialmente aprovados não avançam o progresso nem pontuam.
     *
     * @param upload      upload já analisado (status, datas e dono preenchidos)
     * @param fingerprint fingerprint do vídeo, ou {@code null} se o arquivo foi recusado
     * @param frames      frames da fingerprint (vazio se não houver fingerprint)
     * @param missao      missão contínua do envio (só o id é usado; as regras vêm do banco)
     * @return se este envio concluiu a missão no mês e os pontos creditados
     * @throws IllegalArgumentException se o usuário não existe, já atingiu o limite do mês, o upload não
     *                                  tem status ou o vídeo (hash) já foi enviado antes
     * @throws IllegalStateException    se o banco falhar (nada foi gravado)
     */
    public ResultadoEnvio registrarEnvio(Upload upload, Fingerprint fingerprint, List<FingerprintFrame> frames,
                                         MissaoContinua missao) {
        // Só um upload com resultado de análise é gravado; o status decide se conta como realização.
        if (upload.getStatus() == null) {
            throw new IllegalArgumentException("O upload não tem status de análise.");
        }
        int idUsuario = upload.getUsuario().getIdUsuario();
        int idMissao = missao.getIdMissao();
        LocalDate dia = upload.getDataEnvio();
        boolean aprovado = MissaoContinua.realizacaoValida(upload.getStatus());
        UsuarioMissaoDAO usuarioMissaoDAO = new UsuarioMissaoDAO();

        try (Connection con = ConnectionFactory.obterConexao()) {
            boolean confirmado = false;
            try {
                con.setAutoCommit(false);

                if (!usuarioMissaoDAO.travarUsuario(con, idUsuario)) {
                    throw new IllegalArgumentException("O usuário informado não existe.");
                }
                if (!usuarioMissaoDAO.travarMissao(con, idMissao)) {
                    throw new IllegalArgumentException("A missão informada não existe.");
                }
                RegrasContinua regras = lerRegrasContinua(con, idMissao);
                if (regras == null) {
                    throw new IllegalArgumentException("A missão informada não é uma missão contínua.");
                }

                int aprovadosNoCiclo = contarAprovadosNoCiclo(con, idUsuario, idMissao, dia);
                if (aprovadosNoCiclo >= regras.limite()) {
                    throw new IllegalArgumentException("O usuário já concluiu essa missão neste mês.");
                }

                // Mesmo vídeo nunca vale duas vezes: na transação (e não só na análise prévia), para
                // que um envio concorrente com o mesmo hash não seja aprovado em duplicidade.
                if (aprovado && fingerprint != null
                        && new FingerprintDAO().buscarIdUploadPorHash(con, fingerprint.getHashVideo()) > 0) {
                    throw new IllegalArgumentException("Este vídeo já foi enviado antes.");
                }

                inserir(con, upload);

                if (fingerprint != null) {
                    fingerprint.setUpload(upload);
                    new FingerprintDAO().cadastrar(con, fingerprint);
                    FingerprintFrameDAO frameDAO = new FingerprintFrameDAO();
                    for (FingerprintFrame frame : frames) {
                        frame.setFingerprint(fingerprint);
                        frameDAO.cadastrar(con, frame);
                    }
                }

                int progresso = aprovadosNoCiclo + (aprovado ? 1 : 0);
                UsuarioMissao andamento = usuarioMissaoDAO.buscarPorUsuarioEMissao(con, idUsuario, idMissao);
                if (andamento == null) {
                    andamento = new UsuarioMissao();
                    Usuario dono = new Usuario();
                    dono.setIdUsuario(idUsuario);
                    andamento.setUsuario(dono);
                    Missao referencia = new Missao();
                    referencia.setIdMissao(idMissao);
                    andamento.setMissao(referencia);
                    andamento.setTentativas(1);
                    andamento.setProgressoAtual(progresso);
                    usuarioMissaoDAO.cadastrar(con, andamento);
                } else {
                    usuarioMissaoDAO.atualizarProgresso(con, andamento.getIdUsuarioMissao(),
                            andamento.getTentativas() + 1, progresso);
                }

                boolean concluiu = aprovado && progresso >= regras.limite();
                int pontos = 0;
                if (concluiu) {
                    // pontos pela regra única (Pontuacao.pontosElegiveis), não por conta própria
                    pontos = regras.pontosElegiveis(upload.getStatus(), progresso);
                    new CarteiraDAO().creditarPontos(con, idUsuario, pontos);
                    usuarioMissaoDAO.registrarConclusao(con, andamento.getIdUsuarioMissao(), dia);
                }

                con.commit();
                confirmado = true;
                return new ResultadoEnvio(concluiu, pontos);
            } finally {
                // Desfaz antes de devolver a conexão: no Oracle, ligar o autocommit com a
                // transação aberta confirmaria tudo.
                if (!confirmado) {
                    con.rollback();
                }
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o upload.", e);
        }
    }

    /** Limite de realizações por mês e pontos da missão contínua, lidos dentro da transação. */
    private record RegrasContinua(int limite, int pontos) {

        /** Pontos devidos por este envio, pela mesma regra de {@link Pontuacao#pontosElegiveis}. */
        int pontosElegiveis(StatusUploadEnum status, int realizacoesValidas) {
            Pontuacao pontuacao = new Pontuacao();
            pontuacao.setValorBase(pontos);
            return pontuacao.pontosElegiveis(TipoMissaoEnum.MISSAO_CONTINUA, status, realizacoesValidas >= limite);
        }
    }

    /** Lê as regras atuais da missão contínua; {@code null} se a missão não é contínua. */
    private RegrasContinua lerRegrasContinua(Connection con, int idMissao) throws SQLException {
        String sql = "SELECT mc.nr_limite_realizacoes, p.nr_pontuacao FROM Missao_Continua mc "
                + "LEFT JOIN Pontuacao p ON p.id_pontuacao = mc.id_pontuacao WHERE mc.id_missao = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMissao);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new RegrasContinua(rs.getInt(1), rs.getInt(2));   // sem pontuação: getInt devolve 0
            }
        }
    }

    /** Insere o upload na conexão da transação e preenche o id gerado no objeto. */
    private void inserir(Connection con, Upload upload) throws SQLException {
        String sql = "INSERT INTO Upload (ds_status, dt_envio, dt_analise, ds_arquivo, ds_analise, "
                + "nr_confianca_duplicidade, id_usuario, id_missao) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID_UPLOAD"})) {
            ps.setString(1, upload.getStatus().toString());
            ps.setDate(2, Date.valueOf(upload.getDataEnvio()));
            ps.setDate(3, upload.getDataAnalise() != null ? Date.valueOf(upload.getDataAnalise()) : null);
            ps.setString(4, upload.getArquivo());
            ps.setString(5, upload.getAnalise());
            ps.setInt(6, upload.getConfiancaDuplicidade());
            ps.setInt(7, upload.getUsuario().getIdUsuario());
            ps.setInt(8, upload.getMissao().getIdMissao());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("O banco não devolveu o id do upload.");
                }
                upload.setIdUpload(rs.getInt(1));
            }
        }
    }
}
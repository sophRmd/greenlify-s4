package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.models.Fingerprint;
import br.com.fiap.greenlify.models.Upload;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de acesso a dados (DAO) responsável pelas operações de
 * persistência de {@link Fingerprint} na tabela {@code Fingerprint}.
 *
 * <p>Falhas de escrita e de consulta de duplicidade lançam {@link IllegalStateException}: nunca se
 * trata uma falha de banco como "não há duplicidade".</p>
 */
public class FingerprintDAO {

    /**
     * Cadastra uma nova fingerprint, vinculada ao upload informado.
     *
     * @param fingerprint fingerprint a ser cadastrada
     */
    public void cadastrar(Fingerprint fingerprint) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement(
                    "INSERT INTO Fingerprint (ds_hash_video, dt_geracao, id_upload) VALUES (?, ?, ?)"
            );
            ps.setString(1, fingerprint.getHashVideo());
            ps.setDate(2, Date.valueOf(fingerprint.getDataGeracao()));
            ps.setInt(3, fingerprint.getUpload().getIdUpload());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar a fingerprint.", e);
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
     * Atualiza os dados de uma fingerprint já cadastrada.
     *
     * @param fingerprint fingerprint com os dados atualizados
     */
    public void alterar(Fingerprint fingerprint) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("UPDATE Fingerprint SET ds_hash_video = ?, dt_geracao = ?, id_upload = ? WHERE id_fingerprint = ?");
            ps.setString(1, fingerprint.getHashVideo());
            ps.setDate(2, Date.valueOf(fingerprint.getDataGeracao()));
            ps.setInt(3, fingerprint.getUpload().getIdUpload());
            ps.setInt(4, fingerprint.getIdFingerprint());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível alterar a fingerprint.", e);
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
     * Remove uma fingerprint do banco de dados.
     *
     * @param idFingerprint identificador da fingerprint a ser removida
     */
    public void excluir(Integer idFingerprint) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Fingerprint WHERE id_fingerprint = ?");
            ps.setInt(1, idFingerprint);
            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir a fingerprint.", e);
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
     * Busca uma fingerprint pelo seu identificador.
     *
     * @param idFingerprint identificador da fingerprint
     * @return a fingerprint encontrada, ou {@code null} caso não exista
     */
    public Fingerprint buscarPorId(Integer idFingerprint) {
        Connection conexao = ConnectionFactory.obterConexao();
        Fingerprint fingerprint = null;
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Fingerprint WHERE id_fingerprint = ?");
            ps.setInt(1, idFingerprint);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                fingerprint = new Fingerprint();
                fingerprint.setIdFingerprint(rs.getInt(1));
                fingerprint.setHashVideo(rs.getString(2));
                fingerprint.setDataGeracao(rs.getDate(3).toLocalDate());

                int idUpload = rs.getInt(4);
                if (!rs.wasNull()) {
                    Upload upload = new Upload();
                    upload.setIdUpload(idUpload);
                    fingerprint.setUpload(upload);
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
        return fingerprint;
    }

    /**
     * Lista todas as fingerprints cadastradas no banco de dados.
     *
     * @return lista de fingerprints cadastradas
     */
    public List<Fingerprint> listar() {
        Connection conexao = ConnectionFactory.obterConexao();
        List<Fingerprint> lista = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Fingerprint");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Fingerprint fingerprint = new Fingerprint();
                fingerprint.setIdFingerprint(rs.getInt(1));
                fingerprint.setHashVideo(rs.getString(2));
                fingerprint.setDataGeracao(rs.getDate(3).toLocalDate());

                int idUpload = rs.getInt(4);
                if (!rs.wasNull()) {
                    Upload upload = new Upload();
                    upload.setIdUpload(idUpload);
                    fingerprint.setUpload(upload);
                }

                lista.add(fingerprint);
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
        return lista;
    }

    /**
     * Cadastra uma fingerprint usando uma conexão já aberta (para participar da
     * transação de quem chamou) e preenche o id gerado no próprio objeto. Não faz
     * commit nem fecha a conexão; erros de SQL sobem para o chamador decidir o rollback.
     */
    public void cadastrar(Connection conexao, Fingerprint fingerprint) throws SQLException {
        String sql = "INSERT INTO Fingerprint (ds_hash_video, dt_geracao, id_upload) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql, new String[]{"ID_FINGERPRINT"})) {
            ps.setString(1, fingerprint.getHashVideo());
            ps.setDate(2, Date.valueOf(fingerprint.getDataGeracao()));
            ps.setInt(3, fingerprint.getUpload().getIdUpload());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    fingerprint.setIdFingerprint(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Procura uma fingerprint com exatamente o mesmo hash de vídeo (sem diferenciar maiúsculas).
     *
     * @return o id do upload dono da fingerprint mais antiga com esse hash, ou 0 se não houver
     * @throws IllegalStateException se o banco falhar
     */
    public int buscarIdUploadPorHash(String hashVideo) {
        try (Connection conexao = ConnectionFactory.obterConexao()) {
            return buscarIdUploadPorHash(conexao, hashVideo);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível verificar a duplicidade do vídeo.", e);
        }
    }

    /** Mesma busca de {@link #buscarIdUploadPorHash(String)}, na conexão (transação) recebida. */
    public int buscarIdUploadPorHash(Connection conexao, String hashVideo) throws SQLException {
        String sql = "SELECT id_upload FROM (SELECT id_upload FROM Fingerprint "
                + "WHERE LOWER(ds_hash_video) = LOWER(?) ORDER BY id_fingerprint) WHERE ROWNUM = 1";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, hashVideo == null ? "" : hashVideo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Informa se o upload já tem fingerprint (um vídeo tem uma só).
     *
     * @throws IllegalStateException se o banco falhar
     */
    public boolean existePorUpload(int idUpload) {
        String sql = "SELECT COUNT(*) FROM Fingerprint WHERE id_upload = ?";
        try (Connection conexao = ConnectionFactory.obterConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idUpload);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível verificar a fingerprint do upload.", e);
        }
    }
}
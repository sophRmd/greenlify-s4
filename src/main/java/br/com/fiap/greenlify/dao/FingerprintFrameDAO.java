package br.com.fiap.greenlify.dao;

import br.com.fiap.greenlify.models.Fingerprint;
import br.com.fiap.greenlify.models.FingerprintFrame;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da entidade FingerprintFrame (tabela Fingerprint_Frame). Cada frame
 * pertence a uma Fingerprint, referenciada pela coluna id_fingerprint.
 * Falhas de escrita lançam {@link IllegalStateException}.
 */
public class FingerprintFrameDAO {

    /**
     * Cadastra um novo frame de fingerprint.
     * Ao final, o id gerado pelo banco é preenchido no próprio objeto.
     *
     * @param frame dados do frame a ser cadastrado
     */
    public void cadastrar(FingerprintFrame frame) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "INSERT INTO Fingerprint_Frame (nr_frame, nr_tempo_segundo, " +
                    "ds_phash, ds_embedding, id_fingerprint) " +
                    "VALUES (?, ?, ?, ?, ?)";
            ps = conexao.prepareStatement(sql, new String[]{"ID_FINGERPRINT_FRAME"});
            ps.setInt(1, frame.getNumeroFrame());
            ps.setInt(2, frame.getTempoSegundo());
            ps.setString(3, frame.getpHash());
            ps.setString(4, frame.getEmbedding());
            ps.setInt(5, frame.getFingerprint().getIdFingerprint());

            ps.executeUpdate();

            // Recupera o id_fingerprint_frame gerado automaticamente (IDENTITY) e preenche no objeto
            ResultSet rsGerado = ps.getGeneratedKeys();
            if (rsGerado.next()) {
                frame.setIdFingerprintFrame(rsGerado.getInt(1));
            }
            rsGerado.close();

            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível gravar o frame.", e);
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
     * Atualiza os dados de um frame já existente.
     *
     * @param frame dados atualizados do frame (o id é usado como referência)
     */
    public void alterar(FingerprintFrame frame) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            String sql = "UPDATE Fingerprint_Frame SET nr_frame = ?, nr_tempo_segundo = ?, " +
                    "ds_phash = ?, ds_embedding = ?, id_fingerprint = ? " +
                    "WHERE id_fingerprint_frame = ?";
            ps = conexao.prepareStatement(sql);
            ps.setInt(1, frame.getNumeroFrame());
            ps.setInt(2, frame.getTempoSegundo());
            ps.setString(3, frame.getpHash());
            ps.setString(4, frame.getEmbedding());
            ps.setInt(5, frame.getFingerprint().getIdFingerprint());
            ps.setInt(6, frame.getIdFingerprintFrame());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível alterar o frame.", e);
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
     * Exclui um frame pelo id.
     *
     * @param idFingerprintFrame id do frame a ser removido
     */
    public void excluir(int idFingerprintFrame) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("DELETE FROM Fingerprint_Frame WHERE id_fingerprint_frame = ?");
            ps.setInt(1, idFingerprintFrame);
            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Não foi possível excluir o frame.", e);
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
     * Busca um frame pelo id.
     *
     * @param idFingerprintFrame id do frame procurado
     * @return o frame encontrado, ou {@code null} caso não exista
     */
    public FingerprintFrame buscarPorId(int idFingerprintFrame) {
        Connection conexao = ConnectionFactory.obterConexao();
        PreparedStatement ps = null;
        FingerprintFrame frame = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Fingerprint_Frame WHERE id_fingerprint_frame = ?");
            ps.setInt(1, idFingerprintFrame);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                frame = new FingerprintFrame();
                frame.setIdFingerprintFrame(rs.getInt(1));
                frame.setNumeroFrame(rs.getInt(2));
                frame.setTempoSegundo(rs.getInt(3));
                frame.setpHash(rs.getString(4));
                frame.setEmbedding(rs.getString(5));

                Fingerprint fingerprint = new Fingerprint();
                fingerprint.setIdFingerprint(rs.getInt(6));
                frame.setFingerprint(fingerprint);
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
        return frame;
    }

    /**
     * Lista todos os frames cadastrados.
     *
     * @return lista de frames (pode vir vazia, nunca nula)
     */
    public List<FingerprintFrame> listar() {
        Connection conexao = ConnectionFactory.obterConexao();
        List<FingerprintFrame> frames = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Fingerprint_Frame");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                FingerprintFrame frame = new FingerprintFrame();

                frame.setIdFingerprintFrame(rs.getInt(1));
                frame.setNumeroFrame(rs.getInt(2));
                frame.setTempoSegundo(rs.getInt(3));
                frame.setpHash(rs.getString(4));
                frame.setEmbedding(rs.getString(5));

                Fingerprint fingerprint = new Fingerprint();
                fingerprint.setIdFingerprint(rs.getInt(6));
                frame.setFingerprint(fingerprint);

                frames.add(frame);
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
        return frames;
    }

    /**
     * Lista os frames de uma determinada fingerprint, ordenados pelo número do frame.
     *
     * @param idFingerprint id da fingerprint dona dos frames
     * @return lista de frames (pode vir vazia, nunca nula)
     */
    public List<FingerprintFrame> listarPorFingerprint(int idFingerprint) {
        Connection conexao = ConnectionFactory.obterConexao();
        List<FingerprintFrame> frames = new ArrayList<>();
        PreparedStatement ps = null;

        try {
            ps = conexao.prepareStatement("SELECT * FROM Fingerprint_Frame WHERE id_fingerprint = ? ORDER BY nr_frame");
            ps.setInt(1, idFingerprint);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                FingerprintFrame frame = new FingerprintFrame();

                frame.setIdFingerprintFrame(rs.getInt(1));
                frame.setNumeroFrame(rs.getInt(2));
                frame.setTempoSegundo(rs.getInt(3));
                frame.setpHash(rs.getString(4));
                frame.setEmbedding(rs.getString(5));

                Fingerprint fingerprint = new Fingerprint();
                fingerprint.setIdFingerprint(rs.getInt(6));
                frame.setFingerprint(fingerprint);

                frames.add(frame);
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
        return frames;
    }

    /**
     * Cadastra um frame usando uma conexão já aberta (para participar da transação
     * de quem chamou) e preenche o id gerado no próprio objeto. Não faz commit nem
     * fecha a conexão; erros de SQL sobem para o chamador decidir o rollback.
     */
    public void cadastrar(Connection conexao, FingerprintFrame frame) throws SQLException {
        String sql = "INSERT INTO Fingerprint_Frame (nr_frame, nr_tempo_segundo, "
                + "ds_phash, ds_embedding, id_fingerprint) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexao.prepareStatement(sql, new String[]{"ID_FINGERPRINT_FRAME"})) {
            ps.setInt(1, frame.getNumeroFrame());
            ps.setInt(2, frame.getTempoSegundo());
            ps.setString(3, frame.getpHash());
            ps.setString(4, frame.getEmbedding());
            ps.setInt(5, frame.getFingerprint().getIdFingerprint());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    frame.setIdFingerprintFrame(rs.getInt(1));
                }
            }
        }
    }
}
package br.com.fiap.greenlify.models;

public class FingerprintFrame {
    private int idFingerprintFrame;
    private int numeroFrame;
    private int tempoSegundo;
    private String pHash;
    private String embedding;
    private Fingerprint fingerprint;


    public int getIdFingerprintFrame() {
        return idFingerprintFrame;
    }

    public void setIdFingerprintFrame(int idFingerprintFrame) {
        this.idFingerprintFrame = idFingerprintFrame;
    }

    public int getNumeroFrame() {
        return numeroFrame;
    }

    public void setNumeroFrame(int numeroFrame) {
        this.numeroFrame = numeroFrame;
    }

    public int getTempoSegundo() {
        return tempoSegundo;
    }

    public void setTempoSegundo(int tempoSegundo) {
        this.tempoSegundo = tempoSegundo;
    }

    public String getpHash() {
        return pHash;
    }

    public void setpHash(String pHash) {
        this.pHash = pHash;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(Fingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }
}

package br.com.fiap.greenlify.models;

import java.time.LocalDate;
import java.util.List;

/**
 * Representa a impressão digital (hash) de um arquivo enviado em um
 * {@link Upload}, utilizada para identificar e evitar o envio de arquivos
 * duplicados na plataforma.
 */
public class Fingerprint {

    private int idFingerprint;
    private String hashVideo;
    private LocalDate dataGeracao;
    private Upload upload;

    /**
     * Verifica se já existe, na lista informada, uma fingerprint com o mesmo
     * hash da fingerprint recém-gerada, indicando duplicidade de arquivo.
     *
     * @param fingerprints lista de fingerprints já cadastradas
     * @param fpNovo       fingerprint recém-gerada a ser validada
     * @return {@code true} se já existir uma fingerprint com o mesmo hash;
     *         {@code false} caso contrário ou se a fingerprint informada for inválida
     */
    public boolean validarDuplicidade(List<Fingerprint> fingerprints, Fingerprint fpNovo) {
        if (fpNovo == null || fpNovo.getHashVideo() == null) {
            return false;
        }

        for (Fingerprint fp : fingerprints) {
            if (fp.getHashVideo() != null && fp.getHashVideo().equals(fpNovo.getHashVideo())) {
                return true;
            }
        }

        return false;
    }

    public int getIdFingerprint() {
        return idFingerprint;
    }

    public void setIdFingerprint(int idFingerprint) {
        this.idFingerprint = idFingerprint;
    }

    public String getHashVideo() {
        return hashVideo;
    }

    public void setHashVideo(String hashVideo) {
        this.hashVideo = hashVideo;
    }

    public LocalDate getDataGeracao() {
        return dataGeracao;
    }

    public void setDataGeracao(LocalDate dataGeracao) {
        this.dataGeracao = dataGeracao;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }
}
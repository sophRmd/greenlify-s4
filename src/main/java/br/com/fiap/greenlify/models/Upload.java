package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.dao.MissaoContinuaDAO;
import br.com.fiap.greenlify.enums.StatusUploadEnum;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/**
 * Representa o envio de um arquivo (foto ou vídeo) como comprovação da
 * realização de uma {@link Missao}, incluindo a validação do arquivo e a
 * geração da respectiva {@link Fingerprint}.
 */
public class Upload {

    private int idUpload;
    private StatusUploadEnum status;
    private LocalDate dataEnvio;
    private LocalDate dataAnalise;
    private String arquivo;
    private String analise;
    private int confiancaDuplicidade;
    private Missao missao;
    private UsuarioComum usuario;

    /**
     * Valida o arquivo enviado pelo usuário para comprovar a realização de
     * uma missão, verificando nome, tamanho e extensão. Caso todas as
     * regras sejam atendidas, o upload é aprovado; caso contrário, é
     * recusado com o respectivo motivo registrado em {@link #getAnalise()}.
     *
     * @param nomeArquivo   nome do arquivo enviado
     * @param tamanhoMB     tamanho do arquivo em megabytes
     * @param idMissao      identificador da missão relacionada ao upload
     * @param usuarioLogado usuário responsável pelo envio
     * @return o próprio upload, caso aprovado; {@code null} caso o arquivo
     *         seja recusado por alguma das regras de validação
     */
    public Upload verificaArquivo(String nomeArquivo, double tamanhoMB, int idMissao, UsuarioComum usuarioLogado) {
        if (nomeArquivo != null) {
            this.arquivo = nomeArquivo;
        }

        this.usuario = usuarioLogado;
        this.dataEnvio = LocalDate.now();
        this.dataAnalise = LocalDate.now();

        MissaoContinuaDAO missaoDAO = new MissaoContinuaDAO();
        this.missao = missaoDAO.buscarPorId(idMissao);

        if (this.missao == null) {
            MissaoContinua missaoTemp = new MissaoContinua();
            missaoTemp.setIdMissao(idMissao);
            this.missao = missaoTemp;
        }

        // Validação das regras de negócio
        if (nomeArquivo == null || nomeArquivo.trim().isEmpty()) {
            this.status = StatusUploadEnum.REPROVADO;
            this.analise = "Arquivo recusado: Nome do arquivo inválido ou não informado.";
            return null;
        }

        if (tamanhoMB > 100.0) {
            this.status = StatusUploadEnum.REPROVADO;
            this.analise = String.format("Arquivo recusado: Tamanho do arquivo (%.2f MB) excede o limite máximo permitido de 100 MB.", tamanhoMB);
            return null;
        }

        String nome = nomeArquivo.toLowerCase();
        boolean extensaoValida = nome.endsWith(".mp4") || nome.endsWith(".mov");

        if (!extensaoValida) {
            this.status = StatusUploadEnum.REPROVADO;
            this.analise = "Arquivo recusado: Extensão não permitida. Apenas vídeos MP4 e MOV são aceitos.";
            return null;
        }

        this.status = StatusUploadEnum.APROVADO;
        this.analise = String.format("Arquivo aprovado: Formato válido e tamanho permitido (%.2f MB).", tamanhoMB);

        return this;
    }

    /**
     * Gera a fingerprint (hash SHA-256) do conteúdo do arquivo enviado,
     * utilizada posteriormente para identificar uploads duplicados.
     *
     * @param upload upload a partir do qual a fingerprint será gerada
     * @return a fingerprint gerada, ou {@code null} caso o upload seja
     *         inválido ou o algoritmo de hash não esteja disponível
     */
    public Fingerprint gerarFingerprint(Upload upload) {
        if (upload == null || upload.getArquivo() == null) {
            return null;
        }

        try {
            Fingerprint fingerprint = new Fingerprint();
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Gera a hash SHA-256 a partir da String do arquivo/conteúdo
            byte[] bytesArquivo = upload.getArquivo().getBytes();
            byte[] hash = md.digest(bytesArquivo);

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            fingerprint.setHashVideo(sb.toString());
            fingerprint.setDataGeracao(LocalDate.now());

            if (upload.getIdUpload() > 0) {
                fingerprint.setUpload(upload);
            }

            return fingerprint;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getIdUpload() {
        return idUpload;
    }

    public void setIdUpload(int idUpload) {
        this.idUpload = idUpload;
    }

    public StatusUploadEnum getStatus() {
        return status;
    }

    public void setStatus(StatusUploadEnum status) {
        this.status = status;
    }

    public LocalDate getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDate dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public LocalDate getDataAnalise() {
        return dataAnalise;
    }

    public void setDataAnalise(LocalDate dataAnalise) {
        this.dataAnalise = dataAnalise;
    }

    public String getArquivo() {
        return arquivo;
    }

    public void setArquivo(String arquivo) {
        this.arquivo = arquivo;
    }

    public String getAnalise() {
        return analise;
    }

    public void setAnalise(String analise) {
        this.analise = analise;
    }

    public Missao getMissao() {
        return missao;
    }

    public void setMissao(Missao missao) {
        this.missao = missao;
    }

    public UsuarioComum getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioComum usuario) {
        this.usuario = usuario;
    }

    public int getConfiancaDuplicidade() {
        return confiancaDuplicidade;
    }

    public void setConfiancaDuplicidade(int confiancaDuplicidade) {
        this.confiancaDuplicidade = confiancaDuplicidade;
    }
}
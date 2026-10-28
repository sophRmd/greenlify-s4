package br.com.fiap.greenlify.util;

import java.math.BigInteger;

/**
 * Frame de vídeo já preparado para comparação de duplicidade.
 *
 * <p>O pHash e o embedding são gerados por um serviço externo (IA, em Python).
 * Esta classe apenas compara os valores recebidos:</p>
 * <ul>
 *   <li><b>pHash</b>: distância de Hamming entre os hashes (quantos bits diferem);</li>
 *   <li><b>embedding</b>: similaridade de cosseno entre os vetores.</li>
 * </ul>
 * Dois frames são considerados semelhantes se QUALQUER um dos critérios bater.
 */
public final class FrameComparavel {

    /** Máximo de bits diferentes entre dois pHashes para considerar semelhantes. */
    public static final int LIMIAR_HAMMING = 10;

    /** Similaridade de cosseno mínima entre embeddings para considerar semelhantes. */
    public static final double LIMIAR_COSSENO = 0.95;

    private final int idUpload;
    private final BigInteger pHash;
    private final int tamanhoPHash;
    private final double[] embedding;
    private final double norma;

    private FrameComparavel(int idUpload, BigInteger pHash, int tamanhoPHash, double[] embedding, double norma) {
        this.idUpload = idUpload;
        this.pHash = pHash;
        this.tamanhoPHash = tamanhoPHash;
        this.embedding = embedding;
        this.norma = norma;
    }

    /**
     * @param idUpload  upload dono do frame (0 para frames ainda não gravados)
     * @param pHash     pHash em hexadecimal (pode ser nulo)
     * @param embedding embedding como números separados por vírgula (pode ser nulo)
     */
    public static FrameComparavel de(int idUpload, String pHash, String embedding) {
        BigInteger hash = null;
        int tamanho = 0;
        if (pHash != null && !pHash.isBlank()) {
            try {
                hash = new BigInteger(pHash.trim(), 16);
                tamanho = pHash.trim().length();
            } catch (NumberFormatException e) {
                hash = null;
            }
        }

        double[] vetor = lerEmbedding(embedding);
        double norma = 0;
        if (vetor != null) {
            for (double v : vetor) {
                norma += v * v;
            }
            norma = Math.sqrt(norma);
            if (norma == 0) {
                vetor = null;
            }
        }
        return new FrameComparavel(idUpload, hash, tamanho, vetor, norma);
    }

    public int getIdUpload() {
        return idUpload;
    }

    /** @return {@code true} se os frames forem semelhantes por pHash ou por embedding */
    public boolean semelhanteA(FrameComparavel outro) {
        return pHashProximo(outro) || embeddingProximo(outro);
    }

    private boolean pHashProximo(FrameComparavel outro) {
        if (pHash == null || outro.pHash == null || tamanhoPHash != outro.tamanhoPHash) {
            return false;
        }
        return pHash.xor(outro.pHash).bitCount() <= LIMIAR_HAMMING;
    }

    private boolean embeddingProximo(FrameComparavel outro) {
        if (embedding == null || outro.embedding == null || embedding.length != outro.embedding.length) {
            return false;
        }
        double produto = 0;
        for (int i = 0; i < embedding.length; i++) {
            produto += embedding[i] * outro.embedding[i];
        }
        return produto / (norma * outro.norma) >= LIMIAR_COSSENO;
    }

    private static double[] lerEmbedding(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.replace("[", "").replace("]", "").trim();
        if (limpo.isEmpty()) {
            return null;
        }
        String[] partes = limpo.split("[,\\s]+");
        double[] vetor = new double[partes.length];
        try {
            for (int i = 0; i < partes.length; i++) {
                vetor[i] = Double.parseDouble(partes[i]);
                if (Double.isNaN(vetor[i]) || Double.isInfinite(vetor[i])) {
                    return null;
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return vetor;
    }
}

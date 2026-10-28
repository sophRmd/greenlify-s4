package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.enums.StatusUploadEnum;

/**
 * Andamento de um usuário em uma missão. Três coisas diferentes, que não se misturam:
 * <ul>
 *   <li><b>progresso</b>: quanto já foi realizado ({@link #getProgressoAtual()});</li>
 *   <li><b>conclusão efetiva</b>: o progresso atingiu a meta ({@link #concluida(int)});</li>
 *   <li><b>pontuação elegível</b>: só existe quando há conclusão válida (veja
 *       {@link MissaoContinua#pontosElegiveis}).</li>
 * </ul>
 */
public class UsuarioMissao {
    private int idUsuarioMissao;
    private int tentativas;
    private int progressoAtual;
    private Usuario usuario;
    private Missao missao;

    /**
     * Avança um passo no progresso (e conta uma tentativa), sem passar da meta.
     * Quem decide o avanço é o servidor, nunca o cliente. Avançar não encerra a missão:
     * quem encerra decide com {@link #concluida(int)} depois do avanço.
     *
     * @return {@code true} se o progresso avançou; {@code false} se a meta é inválida ou já
     *         foi atingida (nada muda)
     */
    public boolean avancarProgresso(int meta) {
        if (meta <= 0 || progressoAtual >= meta) {
            return false;
        }
        progressoAtual++;
        tentativas++;
        return true;
    }

    /** Conclusão efetiva: o progresso atingiu a meta (que precisa ser maior que zero). */
    public boolean concluida(int meta) {
        return meta > 0 && progressoAtual >= meta;
    }

    /**
     * Estados válidos de um upload e as mudanças permitidas entre eles:
     * <pre>
     * EM_ANALISE            -> APROVADO | PARCIALMENTE_APROVADO | REPROVADO
     * PARCIALMENTE_APROVADO -> APROVADO | REPROVADO   (após revisão)
     * APROVADO, REPROVADO   -> finais (não mudam mais)
     * </pre>
     * Só APROVADO vale como realização ({@link MissaoContinua#realizacaoValida}).
     */
    public static boolean transicaoUploadValida(StatusUploadEnum atual, StatusUploadEnum novo) {
        if (atual == null || novo == null || atual == novo) {
            return false;
        }
        return switch (atual) {
            case EM_ANALISE -> true;
            case PARCIALMENTE_APROVADO -> novo == StatusUploadEnum.APROVADO
                    || novo == StatusUploadEnum.REPROVADO;
            case APROVADO, REPROVADO -> false;
        };
    }

    public int getIdUsuarioMissao() {
        return idUsuarioMissao;
    }

    public void setIdUsuarioMissao(int idUsuarioMissao) {
        this.idUsuarioMissao = idUsuarioMissao;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void setTentativas(int tentativas) {
        this.tentativas = tentativas;
    }

    public int getProgressoAtual() {
        return progressoAtual;
    }

    public void setProgressoAtual(int progressoAtual) {
        this.progressoAtual = progressoAtual;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Missao getMissao() {
        return missao;
    }

    public void setMissao(Missao missao) {
        this.missao = missao;
    }
}
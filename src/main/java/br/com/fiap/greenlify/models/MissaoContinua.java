package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.dao.PontuacaoDAO;
import br.com.fiap.greenlify.enums.CategoriaEnum;
import br.com.fiap.greenlify.enums.TipoMetricaEnum;
import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;

/**
 * Representa uma missão do tipo contínua, ou seja, uma missão que pode ser
 * realizada repetidamente pelo usuário até atingir um limite de realizações,
 * gerando progresso mensurável (ex.: economia de água, energia ou carbono).
 */
public class MissaoContinua extends Missao {

    private int limiteRealizacoes;
    private TipoMetricaEnum tipoMetrica;
    private int valorMetrica;
    private Pontuacao pontuacao;

    /**
     * Cria uma missão contínua vazia, já definindo o tipo da missão como
     * {@link TipoMissaoEnum#MISSAO_CONTINUA}.
     */
    public MissaoContinua() {
        super();
        setTipoMissao(TipoMissaoEnum.MISSAO_CONTINUA);
    }

    /**
     * Cria uma missão contínua com todos os dados informados.
     *
     * @param idMissao         identificador da missão
     * @param nome             nome da missão
     * @param descricao        descrição da missão
     * @param comoComprovar    instruções de como comprovar a realização da missão
     * @param requisitos       requisitos necessários para realizar a missão
     * @param categoria        categoria da missão
     * @param limiteRealizacoes número máximo de vezes que a missão pode ser realizada
     * @param pontuacao      identificador da pontuação associada à missão
     * @param tipoMetrica      tipo de métrica utilizada para medir o progresso
     * @param valorMetrica     valor de referência da métrica
     */
    public MissaoContinua(int idMissao, String nome, String descricao, String comoComprovar, String requisitos,
                          CategoriaEnum categoria, int limiteRealizacoes, Pontuacao pontuacao, double valorProgresso,
                          TipoMetricaEnum tipoMetrica, int valorMetrica) {

        setIdMissao(idMissao);
        setNome(nome);
        setDescricao(descricao);
        setComoComprovar(comoComprovar);
        setRequisitos(requisitos);
        setCategoria(categoria);
        setTipoMissao(TipoMissaoEnum.MISSAO_CONTINUA);

        this.limiteRealizacoes = limiteRealizacoes;
        this.pontuacao = pontuacao;
        this.tipoMetrica = tipoMetrica;
        this.valorMetrica = valorMetrica;
    }

    /**
     * Regra única de validação do envio: só um upload APROVADO vale como realização
     * (progresso, conclusão e pontos). Em análise, parcialmente aprovado e reprovado não valem.
     */
    public static boolean realizacaoValida(StatusUploadEnum statusUpload) {
        return statusUpload == StatusUploadEnum.APROVADO;
    }

    /**
     * Conclusão efetiva: o usuário concluiu a missão quando as realizações válidas do ciclo
     * (uploads aprovados no mês) atingem o limite de realizações.
     */
    public boolean concluida(int realizacoesValidas) {
        return realizacoesValidas >= limiteRealizacoes;
    }

    /** Pontos configurados para a missão (valor da pontuação cadastrada), ou 0 se não houver. */
    public int pontosConfigurados() {
        return pontuacao != null ? pontuacao.getValorBase() : 0;
    }

    /**
     * Pontuação elegível: os pontos configurados só são devidos quando o envio atual é válido
     * e é ele que conclui a missão; qualquer outro caso vale 0 (progresso sem conclusão não pontua).
     *
     * @param statusUpload       status do upload que está sendo registrado
     * @param realizacoesValidas realizações válidas do ciclo, já contando este envio
     */
    public int pontosElegiveis(StatusUploadEnum statusUpload, int realizacoesValidas) {
        if (pontuacao == null) {
            return 0;
        }
        // a regra fica só em Pontuacao.pontosElegiveis
        return pontuacao.pontosElegiveis(TipoMissaoEnum.MISSAO_CONTINUA, statusUpload, concluida(realizacoesValidas));
    }

    @Override
    public String toString() {
        PontuacaoDAO pontuacaoDAO = new PontuacaoDAO();
        Pontuacao pont = pontuacao != null ? pontuacaoDAO.buscarPorId(pontuacao.getIdPontuacao()) : null;

        if (pont != null) {
            return "\n\nID: " + getIdMissao() +
                    "\nNome: " + getNome() +
                    "\nDescrição: " + getDescricao() +
                    "\nComo comprovar: " + getComoComprovar() +
                    "\nRequisitos: " + getRequisitos() +
                    "\nCategoria : " + getCategoria() +
                    "\nLimite de Realizações: " + getLimiteRealizacoes() +
                    "\nTipo de métrica: " + getTipoMetrica() +
                    "\nValor métrica: " + getValorMetrica() +
                    "\nDificuldade: " + pont.getDificuldade() +
                    "\nImpacto: " + pont.getImpacto() +
                    "\nPontos: " + pont.getValorBase();
        } else {
            return "\n\nID: " + getIdMissao() +
                    "\nNome: " + getNome() +
                    "\nDescrição: " + getDescricao() +
                    "\nComo comprovar: " + getComoComprovar() +
                    "\nRequisitos: " + getRequisitos() +
                    "\nCategoria : " + getCategoria() +
                    "\nLimite de Realizações: " + getLimiteRealizacoes() +
                    "\nTipo de métrica: " + getTipoMetrica() +
                    "\nValor métrica: " + getValorMetrica() +
                    "\nPontuação: não definida";
        }
    }


    public int getLimiteRealizacoes() {
        return limiteRealizacoes;
    }

    public void setLimiteRealizacoes(int limiteRealizacoes) {
        this.limiteRealizacoes = limiteRealizacoes;
    }

    public TipoMetricaEnum getTipoMetrica() {
        return tipoMetrica;
    }

    public void setTipoMetrica(TipoMetricaEnum tipoMetrica) {
        this.tipoMetrica = tipoMetrica;
    }

    public int getValorMetrica() {
        return valorMetrica;
    }

    public void setValorMetrica(int valorMetrica) {
        this.valorMetrica = valorMetrica;
    }

    public Pontuacao getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(Pontuacao pontuacao) {
        this.pontuacao = pontuacao;
    }
}
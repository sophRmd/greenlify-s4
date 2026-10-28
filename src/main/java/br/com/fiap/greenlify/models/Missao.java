package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.enums.CategoriaEnum;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;

/**
 * Classe base que representa uma missão sustentável cadastrada na
 * plataforma GreenLify. Serve como superclasse para os tipos específicos de
 * missão, como {@link MissaoContinua} e {@link MissaoMensal}.
 */
public class Missao {

    private int idMissao;
    private String nome;
    private String descricao;
    private String comoComprovar;
    private String requisitos;
    private CategoriaEnum categoria;
    private TipoMissaoEnum tipoMissao;

    /**
     * Regra do tipo (fonte única, vale também para referências por id): uma conclusão válida
     * de missão contínua ou mensal atualiza o streak do usuário.
     */
    public boolean atualizaStreak() {
        return tipoMissao != null;
    }

    /**
     * Regra do tipo (fonte única, vale também para referências por id): só a missão mensal
     * contribui para o ranking; a contínua não.
     */
    public boolean pontuaNoRanking() {
        return tipoMissao == TipoMissaoEnum.MISSAO_MENSAL;
    }

    @Override
    public String toString() {
        return "\n\nID Missão: " + idMissao +
                "\nNome: " + nome +
                "\nDescricao:" + descricao +
                "\nComoComprovar: " + comoComprovar +
                "\nRequisitos: " + requisitos +
                "\nCategoria: " + categoria +
                "\nTipoMissao: " + tipoMissao;
    }

    public int getIdMissao() {
        return idMissao;
    }

    public void setIdMissao(int idMissao) {
        this.idMissao = idMissao;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getComoComprovar() {
        return comoComprovar;
    }

    public void setComoComprovar(String comoComprovar) {
        this.comoComprovar = comoComprovar;
    }

    public String getRequisitos() {
        return requisitos;
    }

    public void setRequisitos(String requisitos) {
        this.requisitos = requisitos;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public CategoriaEnum getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaEnum categoria) {
        this.categoria = categoria;
    }

    public TipoMissaoEnum getTipoMissao() {
        return tipoMissao;
    }

    public void setTipoMissao(TipoMissaoEnum tipoMissao) {
        this.tipoMissao = tipoMissao;
    }
}
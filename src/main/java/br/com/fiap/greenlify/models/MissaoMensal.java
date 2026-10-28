package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.enums.TipoMissaoEnum;

import java.time.LocalDate;

/**
 * Representa uma missão do tipo mensal, ou seja, uma missão com período de
 * vigência definido (data de início e fim) e uma meta de progresso a ser
 * atingida dentro desse período.
 */
public class MissaoMensal extends Missao {
    private int meta;
    private LocalDate dataInicio;
    private LocalDate dataFim;

    /** Cria uma missão mensal vazia, já definindo o tipo como {@link TipoMissaoEnum#MISSAO_MENSAL}. */
    public MissaoMensal() {
        super();
        setTipoMissao(TipoMissaoEnum.MISSAO_MENSAL);
    }

    /**
     * Vigência: a missão só aceita progresso se o período é válido e o dia está dentro
     * dele (início e fim inclusive).
     */
    public boolean vigenteEm(LocalDate dia) {
        return dia != null && verificaValidade()
                && !dia.isBefore(dataInicio) && !dia.isAfter(dataFim);
    }

    /** Conclusão efetiva: o progresso atingiu a meta. */
    public boolean metaAtingida(int progresso) {
        return progresso >= meta;
    }

    @Override
    public String toString() {
        return  "\nNome: " + getNome() +
                "\nDescrição: " + getDescricao() +
                "\nComo comprovar: " + getComoComprovar() +
                "\nRequisitos: " + getRequisitos() +
                "\nCategoria: " + getCategoria() +
                "\nData de início: " + getDataInicio() +
                "\nData de término: " + getDataFim();
    }

    /**
     * Verifica se o período de vigência da missão é válido, ou seja, se a
     * data de início é anterior ou igual à data de término.
     *
     * @return {@code true} se o período for válido; {@code false} caso contrário
     */
    public boolean verificaValidade() {
        if (dataInicio == null || dataFim == null) {
            return false;
        }
        return !dataInicio.isAfter(dataFim);
    }


    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public int getMeta() {
        return meta;
    }

    public void setMeta(int meta) {
        this.meta = meta;
    }
}
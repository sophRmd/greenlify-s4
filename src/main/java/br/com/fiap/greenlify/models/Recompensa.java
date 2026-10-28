package br.com.fiap.greenlify.models;

/**
 * Representa uma recompensa que pode ser concedida a um usuário, geralmente
 * associada a uma colocação no {@link Ranking}, concedendo um bônus
 * percentual de pontos sobre o saldo e/ou uma quantidade fixa de pontos.
 *
 * <p>As recompensas de id 1 a 6 são as faixas de premiação do ranking individual (ver
 * {@link PosicaoRanking#idRecompensaPorPosicao(int)}) e precisam existir.</p>
 */
public class Recompensa {

    private int idRecompensa;
    private int bonusPercentual;
    private int bonusPontos;
    private String descricao;

    /**
     * Bônus em pontos para o saldo informado, pela regra cadastrada: percentual do saldo (parte
     * inteira) mais os pontos fixos. Única definição dessa conta.
     *
     * @param saldo saldo de pontos da carteira antes do bônus
     * @return os pontos a somar ao saldo (nunca negativo)
     */
    public long calcularBonus(long saldo) {
        long percentual = (Math.max(saldo, 0) * Math.max(bonusPercentual, 0)) / 100;
        return percentual + Math.max(bonusPontos, 0);
    }

    public int getIdRecompensa() {
        return idRecompensa;
    }

    public void setIdRecompensa(int idRecompensa) {
        this.idRecompensa = idRecompensa;
    }

    public int getBonusPercentual() {
        return bonusPercentual;
    }

    public void setBonusPercentual(int bonusPercentual) {
        this.bonusPercentual = bonusPercentual;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public int getBonusPontos() {
        return bonusPontos;
    }

    public void setBonusPontos(int bonusPontos) {
        this.bonusPontos = bonusPontos;
    }
}
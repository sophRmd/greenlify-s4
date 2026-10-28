package br.com.fiap.greenlify.models;

/**
 * Representa um nível de contribuição sustentável, utilizado para
 * classificar o {@link PerfilSustentavel} de um usuário de acordo com a
 * quantidade de XP acumulada.
 */
public class NivelContribuicao {

    private int idNivel;
    private int nivel;
    private String nome;
    private int xpMinimo;
    private int xpMaximo;

    @Override
    public String toString() {
        return "\nNível: " + nivel + " - " + nome +
                "\nXP mínimo: " + xpMinimo +
                "\nXP máximo: " + xpMaximo;
    }

    public int getIdNivel() {
        return idNivel;
    }

    public void setIdNivel(int idNivel) {
        this.idNivel = idNivel;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getXpMinimo() {
        return xpMinimo;
    }

    public void setXpMinimo(int xpMinimo) {
        this.xpMinimo = xpMinimo;
    }

    public int getXpMaximo() {
        return xpMaximo;
    }

    public void setXpMaximo(int xpMaximo) {
        this.xpMaximo = xpMaximo;
    }
}
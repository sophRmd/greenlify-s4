package br.com.fiap.greenlify.models;

/**
 * Representa um selo de conquista que pode ser obtido por um usuário ao
 * atingir uma quantidade mínima de pontos, concedendo benefícios como vale
 * energia e crédito de carbono.
 */
public class Selo {

    private int idSelo;
    private String nome;
    private int valeEnergia;
    private int creditoCarbono;
    private int pontosNecessarios;

    @Override
    public String toString() {
        return "\n\nID: " + idSelo +
                "\nNome: " + nome +
                "\nVale Energia: " + valeEnergia +
                "\nCredito de Carbono: " + creditoCarbono +
                "\nPontos necessários: " + pontosNecessarios;
    }

    public int getIdSelo() {
        return idSelo;
    }

    public void setIdSelo(int idSelo) {
        this.idSelo = idSelo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getValeEnergia() {
        return valeEnergia;
    }

    public void setValeEnergia(int valeEnergia) {
        this.valeEnergia = valeEnergia;
    }

    public int getCreditoCarbono() {
        return creditoCarbono;
    }

    public void setCreditoCarbono(int creditoCarbono) {
        this.creditoCarbono = creditoCarbono;
    }

    public int getPontosNecessarios() {
        return pontosNecessarios;
    }

    public void setPontosNecessarios(int pontosNecessarios) {
        this.pontosNecessarios = pontosNecessarios;
    }
}
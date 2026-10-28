package br.com.fiap.greenlify.models;

/**
 * Representa o perfil sustentável de um {@link UsuarioComum}, reunindo suas
 * estatísticas de contribuição ambiental, como XP acumulado, missões
 * concluídas e totais economizados de água, energia e carbono.
 */
public class PerfilSustentavel extends UsuarioComum {

    private int idPerfilSustentavel;
    private int xpSustentavel;
    private int totalMissoesConcluidas;
    private double totalAguaEconomizada;
    private double totalEnergiaEconomizada;
    private double totalCarbonoEconomizado;
    private UsuarioComum usuario;
    private NivelContribuicao nivel;

    @Override
    public String toString() {
        return "\nXP: " + xpSustentavel +
                "\nMissões concluídas: " + totalMissoesConcluidas +
                "\nÁgua economizada: " + totalAguaEconomizada +
                "\nEnergia economizada: " + totalEnergiaEconomizada +
                "\nCarbono economizado: " + totalCarbonoEconomizado +
                nivel;
    }

    public int getIdPerfilSustentavel() {
        return idPerfilSustentavel;
    }

    public void setIdPerfilSustentavel(int idPerfilSustentavel) {
        this.idPerfilSustentavel = idPerfilSustentavel;
    }

    public int getTotalMissoesConcluidas() {
        return totalMissoesConcluidas;
    }

    public void setTotalMissoesConcluidas(int totalMissoesConcluidas) {
        this.totalMissoesConcluidas = totalMissoesConcluidas;
    }

    public int getXpSustentavel() {
        return xpSustentavel;
    }

    public void setXpSustentavel(int xpSustentavel) {
        this.xpSustentavel = xpSustentavel;
    }

    public double getTotalCarbonoEconomizado() {
        return totalCarbonoEconomizado;
    }

    public void setTotalCarbonoEconomizado(double totalCarbonoEconomizado) {
        this.totalCarbonoEconomizado = totalCarbonoEconomizado;
    }

    public double getTotalEnergiaEconomizada() {
        return totalEnergiaEconomizada;
    }

    public void setTotalEnergiaEconomizada(double totalEnergiaEconomizada) {
        this.totalEnergiaEconomizada = totalEnergiaEconomizada;
    }

    public double getTotalAguaEconomizada() {
        return totalAguaEconomizada;
    }

    public void setTotalAguaEconomizada(double totalAguaEconomizada) {
        this.totalAguaEconomizada = totalAguaEconomizada;
    }

    public UsuarioComum getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioComum usuario) {
        this.usuario = usuario;
    }

    public NivelContribuicao getNivel() {
        return nivel;
    }

    public void setNivel(NivelContribuicao nivel) {
        this.nivel = nivel;
    }
}
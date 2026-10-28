package br.com.fiap.greenlify.models;

import java.time.LocalDate;

/**
 * Representa a associação entre um {@link Usuario} e um {@link Selo}
 * conquistado, registrando a data em que o selo foi coletado.
 */
public class UsuarioSelo {

    private int idUsuarioSelo;
    private LocalDate dataColeta;
    private Usuario usuario;
    private Selo selo;

    /** Regra de coleta: o saldo de pontos da carteira precisa cobrir os pontos necessários do selo. */
    public static boolean saldoSuficiente(int saldoPontos, Selo selo) {
        return selo != null && saldoPontos >= selo.getPontosNecessarios();
    }

    /** Regra de coleta: a data é obrigatória e não pode ser futura. */
    public static boolean dataColetaValida(LocalDate dataColeta, LocalDate hoje) {
        return dataColeta != null && hoje != null && !dataColeta.isAfter(hoje);
    }

    /** Se este registro é a coleta do selo informado pelo usuário informado (um selo por usuário). */
    public boolean ehColetaDe(int idUsuario, int idSelo) {
        return usuario != null && selo != null
                && usuario.getIdUsuario() == idUsuario && selo.getIdSelo() == idSelo;
    }

    public int getIdUsuarioSelo() {
        return idUsuarioSelo;
    }

    public void setIdUsuarioSelo(int idUsuarioSelo) {
        this.idUsuarioSelo = idUsuarioSelo;
    }

    public LocalDate getDataColeta() {
        return dataColeta;
    }

    public void setDataColeta(LocalDate dataColeta) {
        this.dataColeta = dataColeta;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Selo getSelo() {
        return selo;
    }

    public void setSelo(Selo selo) {
        this.selo = selo;
    }
}
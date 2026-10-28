package br.com.fiap.greenlify.models;

import java.time.LocalDate;

/**
 * Representa o resgate de uma {@link Recompensa} por um {@link Usuario},
 * registrando a data em que a concessão foi realizada.
 *
 * <p>É o prêmio do ranking individual (uma vez por usuário e ranking). Não é o resgate de um Selo
 * Verde ({@link UsuarioSelo}): só os selos contam na pontuação do ranking de grupos, e este
 * registro não conta.</p>
 */
public class Resgate {

    private int idResgate;
    private LocalDate dataConcessao;
    private Usuario usuario;
    private Recompensa recompensa;

    /**
     * O prêmio de um ranking é gravado com a data final do período: usuário + data identifica
     * usuário + ranking. Esta é a única definição dessa data.
     */
    public static LocalDate dataDoRanking(Ranking ranking) {
        return ranking != null ? ranking.getDataFim() : null;
    }

    /** Regra de concessão: a data é obrigatória e não pode ser futura (o prêmio sai com o ranking já encerrado). */
    public static boolean dataConcessaoValida(LocalDate dataConcessao, LocalDate hoje) {
        return dataConcessao != null && hoje != null && !dataConcessao.isAfter(hoje);
    }

    /** Se este resgate é o prêmio do ranking informado. */
    public boolean pertenceAoRanking(Ranking ranking) {
        return dataConcessao != null && dataConcessao.equals(dataDoRanking(ranking));
    }

    public int getIdResgate() {
        return idResgate;
    }

    public void setIdResgate(int idResgate) {
        this.idResgate = idResgate;
    }

    public LocalDate getDataConcessao() {
        return dataConcessao;
    }

    public void setDataConcessao(LocalDate dataConcessao) {
        this.dataConcessao = dataConcessao;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Recompensa getRecompensa() {
        return recompensa;
    }

    public void setRecompensa(Recompensa recompensa) {
        this.recompensa = recompensa;
    }
}
package br.com.fiap.greenlify.models;

import java.time.LocalDate;

/**
 * Representa a sequência de atividades (streak) de um usuário na
 * plataforma, controlando o período de participação e a maior sequência de
 * meses consecutivos com atividade registrada.
 *
 * <p>{@code sequenciaAtual} é o número de meses consecutivos com atividade
 * até agora (equivale à coluna {@code nr_meses_ativos} no banco) — se o
 * usuário deixa um mês sem atividade, ela volta a 1 na próxima atividade.</p>
 */
public class Streak {

    private int idStreak;
    private int maiorSequencia;
    private int sequenciaAtual;
    private LocalDate ultimaAtividade;
    private LocalDate primeiraAtividade;
    private Usuario usuario;

    @Override
    public String toString() {
        return "\n\n=== STREAK ===" +
                "\nSequência atual: " + sequenciaAtual + " mês(es)" +
                "\nMaior sequência já alcançada: " + maiorSequencia + " mês(es)" +
                "\nPrimeira atividade: " + (primeiraAtividade != null ? primeiraAtividade : "ainda não iniciado") +
                "\nÚltima atividade: " + (ultimaAtividade != null ? ultimaAtividade : "ainda não iniciado");
    }

    /**
     * Registra que o usuário concluiu pelo menos uma missão mensal na data
     * informada, atualizando a sequência de meses ativos.
     *
     * <p>Regras:</p>
     * <ul>
     *     <li>Se já existir uma atividade registrada no mesmo mês, nada é
     *     alterado (o streak só conta uma vez por mês);</li>
     *     <li>Uma data de mês anterior à última atividade é ignorada (a sequência nunca
     *     anda para trás, então repetir ou atrasar o registro não a quebra);</li>
     *     <li>Se a atividade anterior foi no mês imediatamente anterior,
     *     a sequência atual é incrementada;</li>
     *     <li>Caso contrário (primeira atividade ou houve um mês sem
     *     nenhuma missão concluída), a sequência atual reinicia em 1.</li>
     * </ul>
     *
     * <p>É o único evento que altera o streak: a conclusão de uma missão mensal. Progresso sem
     * conclusão, uploads e missões contínuas não passam por aqui.</p>
     *
     * @param dataConclusao data em que a missão mensal foi concluída
     * @return {@code true} se a sequência foi alterada; {@code false} se a chamada não teve efeito
     */
    public boolean registrarAtividadeMensal(LocalDate dataConclusao) {
        if (dataConclusao == null) {
            return false;
        }
        if (ultimaAtividade != null
                && (mesmoMes(ultimaAtividade, dataConclusao) || dataConclusao.isBefore(ultimaAtividade))) {
            return false;
        }

        if (primeiraAtividade == null) {
            primeiraAtividade = dataConclusao;
        }

        if (ultimaAtividade != null && eMesSeguinte(ultimaAtividade, dataConclusao)) {
            sequenciaAtual = sequenciaAtual + 1;
        } else {
            sequenciaAtual = 1;
        }

        if (sequenciaAtual > maiorSequencia) {
            maiorSequencia = sequenciaAtual;
        }

        ultimaAtividade = dataConclusao;
        return true;
    }

    /**
     * Zera (apenas em memória) a sequência atual quando já passou um mês inteiro sem
     * atividade, para que uma sequência quebrada não continue sendo exibida como ativa.
     * O banco só é atualizado na próxima atividade, que já reinicia a sequência em 1.
     */
    public void encerrarSeInterrompida(LocalDate hoje) {
        if (ultimaAtividade == null) {
            return;
        }
        int mesesSemAtividade = ((hoje.getYear() * 12) + hoje.getMonthValue())
                - ((ultimaAtividade.getYear() * 12) + ultimaAtividade.getMonthValue());
        if (mesesSemAtividade > 1) {
            sequenciaAtual = 0;
        }
    }

    public boolean mesmoMes(LocalDate data1, LocalDate data2) {
        return data1.getYear() == data2.getYear() && data1.getMonthValue() == data2.getMonthValue();
    }

    public boolean eMesSeguinte(LocalDate dataAnterior, LocalDate dataAtual) {
        int totalMesesAnterior = (dataAnterior.getYear() * 12) + dataAnterior.getMonthValue();
        int totalMesesAtual = (dataAtual.getYear() * 12) + dataAtual.getMonthValue();
        return (totalMesesAtual - totalMesesAnterior) == 1;
    }

    public int getSequenciaAtual() {
        return sequenciaAtual;
    }

    public void setSequenciaAtual(int sequenciaAtual) {
        this.sequenciaAtual = sequenciaAtual;
    }

    public int getIdStreak() {
        return idStreak;
    }

    public void setIdStreak(int idStreak) {
        this.idStreak = idStreak;
    }

    public LocalDate getPrimeiraAtividade() {
        return primeiraAtividade;
    }

    public void setPrimeiraAtividade(LocalDate primeiraAtividade) {
        this.primeiraAtividade = primeiraAtividade;
    }

    public LocalDate getUltimaAtividade() {
        return ultimaAtividade;
    }

    public void setUltimaAtividade(LocalDate ultimaAtividade) {
        this.ultimaAtividade = ultimaAtividade;
    }

    public int getMaiorSequencia() {
        return maiorSequencia;
    }

    public void setMaiorSequencia(int maiorSequencia) {
        this.maiorSequencia = maiorSequencia;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
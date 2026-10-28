package br.com.fiap.greenlify.models;

import java.time.LocalDate;

/**
 * Representa um grupo de usuários dentro da plataforma GreenLify, utilizado
 * para agrupar {@link UsuarioComum} e compor o ranking por grupos.
 *
 * <p>{@code qtdUsuarios} é o número de integrantes (tamanho do grupo). A pontuação do grupo no
 * ranking é outra métrica: o total de Selos Verdes resgatados individualmente pelos integrantes
 * (ver {@code Ranking.calcularRankingGrupos}). Um grupo maior não tem, por isso, mais pontos.</p>
 */
public class Grupo {

    private int idGrupo;
    private String nome;
    private LocalDate dataCriacao;
    /** Integrantes do grupo (calculado a partir de Usuario.id_grupo; não é pontuação). */
    private int qtdUsuarios;
    private int statusGrupo;

    @Override
    public String toString() {
        return "\n\nID: " + idGrupo +
                "\nNome: " + nome +
                "\nData de Criacao :" + dataCriacao +
                "\nQuantidade de usuarios: " + qtdUsuarios +
                "\nStatus do grupo: " + statusGrupo;
    }

    public int getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(int idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDate getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDate dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public int getQtdUsuarios() {
        return qtdUsuarios;
    }

    public void setQtdUsuarios(int qtdUsuarios) {
        this.qtdUsuarios = qtdUsuarios;
    }

    public int getStatusGrupo() {
        return statusGrupo;
    }

    public void setStatusGrupo(int statusGrupo) {
        this.statusGrupo = statusGrupo;
    }
}
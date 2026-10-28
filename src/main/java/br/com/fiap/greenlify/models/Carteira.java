package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.dao.CarteiraDAO;

/**
 * Representa a carteira de pontos de um {@link UsuarioComum}.
 * <p>
 * Armazena a quantidade de pontos acumulados pelo usuário e o respectivo
 * valor convertido em reais, utilizado como base para pagamentos e resgates
 * dentro da plataforma.
 */
public class Carteira {

    private int idCarteira;
    private int qtdPontos;
    private double valorReal;
    private UsuarioComum usuario;

    @Override
    public String toString() {
        return "\nPontos: " + qtdPontos +
                "\nValor convertido: R$" + String.format("%.2f", valorReal);
    }

    /**
     * Converte a quantidade de pontos da carteira em valor monetário (reais),
     * buscando os dados mais atuais da carteira no banco de dados antes de
     * realizar o cálculo.
     *
     * @param idCarteira identificador da carteira a ser convertida
     * @return a própria carteira, já atualizada com o valor convertido
     */
    public Carteira converterPontos(int idCarteira) {
        CarteiraDAO carteiraDAO = new CarteiraDAO();
        Carteira carteiraBanco = carteiraDAO.buscarPorId(idCarteira);

        if (carteiraBanco != null) {
            this.idCarteira = carteiraBanco.getIdCarteira();
            this.qtdPontos = carteiraBanco.getQtdPontos();
            this.usuario = carteiraBanco.getUsuario();

            this.valorReal = this.qtdPontos * 0.009052;
            carteiraDAO.alterar(this);
        }

        return this;
    }

    /**
     * Recalcula o valor em reais com base na quantidade atual de pontos.
     * Deve ser chamado sempre que {@code qtdPontos} for alterado, para
     * manter {@code valorReal} consistente antes de persistir a carteira.
     */
    public void recalcularValorReal() {
        this.valorReal = this.qtdPontos * 0.009052;
    }

    public int getIdCarteira() {
        return idCarteira;
    }

    public void setIdCarteira(int idCarteira) {
        this.idCarteira = idCarteira;
    }

    public double getValorReal() {
        return valorReal;
    }

    public void setValorReal(double valorReal) {
        this.valorReal = valorReal;
    }

    public int getQtdPontos() {
        return qtdPontos;
    }

    public void setQtdPontos(int qtdPontos) {
        this.qtdPontos = qtdPontos;
    }

    public UsuarioComum getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioComum usuario) {
        this.usuario = usuario;
    }
}
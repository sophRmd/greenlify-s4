package br.com.fiap.greenlify.models;

import java.time.LocalDate;

public class PagamentoConta {
    private int idPagamento;
    private LocalDate dataPagamento;
    private double valorPago;
    private Usuario usuario;
    private ContaLuz contaLuz;

    public int getIdPagamento() {
        return idPagamento;
    }

    public void setIdPagamento(int idPagamento) {
        this.idPagamento = idPagamento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public double getValorPago() {
        return valorPago;
    }

    public void setValorPago(double valorPago) {
        this.valorPago = valorPago;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public ContaLuz getContaLuz() {
        return contaLuz;
    }

    public void setContaLuz(ContaLuz contaLuz) {
        this.contaLuz = contaLuz;
    }
}

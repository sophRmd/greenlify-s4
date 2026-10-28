package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.dao.UploadDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;

import java.time.LocalDate;

/**
 * Representa um usuário comum da plataforma GreenLify, ou seja, um usuário
 * que participa das missões sustentáveis, envia comprovações e acumula
 * pontos.
 */
public class UsuarioComum extends Usuario {
    private String cpf;
    private LocalDate dataNascimento;
    private Grupo grupo;

    @Override
    public String toString() {
        if (getGrupo() != null) {
            return "\n\nID: " + getIdUsuario() +
                    "\nNome: " + getNome() +
                    "\nCPF: " + getCpf() +
                    "\nData de nascimento: " + getDataNascimento() +
                    "\nEmail: " + getEmail() +
                    "\nGrupo: " + getGrupo().getNome();
        } else {
            return "\n\nID: " + getIdUsuario() +
                    "\nNome: " + getNome() +
                    "\nCPF: " + getCpf() +
                    "\nData de nascimento: " + getDataNascimento() +
                    "\nEmail: " + getEmail();
        }
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}
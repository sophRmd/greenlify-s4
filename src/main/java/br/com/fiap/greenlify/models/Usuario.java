package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.dao.AdminDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.enums.StatusContaEnum;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/**
 * Classe base que representa um usuário da plataforma GreenLify, contendo
 * os dados cadastrais e de acesso comuns a {@link UsuarioComum} e
 * {@link Admin}.
 */
public class Usuario {

    private int idUsuario;
    private String nome;
    private String email;
    private String senha;
    private int nivelAcesso;
    private StatusContaEnum statusConta;


    /**
     * Autentica um usuário a partir do e-mail e senha informados, buscando
     * primeiro entre os usuários comuns e, caso não encontre, entre os
     * administradores. Se a autenticação for bem-sucedida, os dados do
     * usuário encontrado são copiados para esta instância.
     *
     * @param email e-mail informado para login
     * @param senha senha informada para login
     * @return {@code true} se as credenciais forem válidas; {@code false} caso contrário
     */
    public boolean autenticar(String email, String senha) {
        UsuarioComumDAO daoComum = new UsuarioComumDAO();
        UsuarioComum usuarioEncontrado = daoComum.buscarPorEmailESenha(email, senha);

        if (usuarioEncontrado != null) {
            this.setIdUsuario(usuarioEncontrado.getIdUsuario());
            this.setNome(usuarioEncontrado.getNome());
            this.setEmail(usuarioEncontrado.getEmail());
            this.setSenha(usuarioEncontrado.getSenha());
            this.setNivelAcesso(usuarioEncontrado.getNivelAcesso());
            this.setStatusConta(usuarioEncontrado.getStatusConta());
            return true;
        }

        AdminDAO adminDAO = new AdminDAO();
        Admin adminEncontrado = adminDAO.buscarPorEmailESenha(email, senha);

        if (adminEncontrado != null) {
            this.setIdUsuario(adminEncontrado.getIdUsuario());
            this.setNome(adminEncontrado.getNome());
            this.setEmail(adminEncontrado.getEmail());
            this.setSenha(adminEncontrado.getSenha());
            this.setNivelAcesso(adminEncontrado.getNivelAcesso());
            this.setStatusConta(adminEncontrado.getStatusConta());
            return true;
        }

        return false;
    }

    /**
     * Gera o hash SHA-256 da senha informada, utilizado para armazenar a
     * senha do usuário de forma segura.
     *
     * @param senha senha em texto puro a ser criptografada
     * @return a senha criptografada, representada em formato hexadecimal
     * @throws RuntimeException caso o algoritmo SHA-256 não esteja disponível
     */
    public String gerarHashSenha(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(senha.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao criptografar senha");
        }
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public int getNivelAcesso() {
        return nivelAcesso;
    }

    public void setNivelAcesso(int nivelAcesso) {
        this.nivelAcesso = nivelAcesso;
    }

    public StatusContaEnum getStatusConta() {
        return statusConta;
    }

    public void setStatusConta(StatusContaEnum statusConta) {
        this.statusConta = statusConta;
    }
}
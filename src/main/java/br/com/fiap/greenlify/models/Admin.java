package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.dao.MissaoContinuaDAO;
import br.com.fiap.greenlify.dao.PontuacaoDAO;

/**
 * Representa um usuário administrador do sistema GreenLify.
 * <p>
 * Um administrador possui privilégios para gerenciar as missões contínuas
 * cadastradas na plataforma, como criação e exclusão, diferenciando-se de um
 * {@link UsuarioComum}, que apenas participa das missões.
 */
public class Admin extends Usuario {

    /**
     * Cadastra uma nova missão contínua, associando-a a uma pontuação de
     * acordo com o impacto e a dificuldade informados.
     *
     * @param missao     dados básicos da missão a ser cadastrada
     * @param continua   dados específicos da missão contínua
     * @param dificuldade nível de dificuldade da missão (ex.: FACIL, MEDIO, DIFICIL)
     * @param impacto    nível de impacto ambiental da missão (ex.: BAIXO, MEDIO, ALTO)
     */
    public void cadastrarMissaoContinua(Missao missao, MissaoContinua continua, String dificuldade, String impacto) {
        PontuacaoDAO pontuacaoDao = new PontuacaoDAO();
        Pontuacao pontuacao = pontuacaoDao.buscarPorImpactoEDificuldade(impacto, dificuldade);

        MissaoContinuaDAO missaoDao = new MissaoContinuaDAO();
        missaoDao.cadastrarMissao(missao, continua, pontuacao);
    }

    /**
     * Remove uma missão contínua previamente cadastrada.
     *
     * @param continua missão contínua a ser excluída
     */
    public void excluirMissaoContinua(MissaoContinua continua) {
        MissaoContinuaDAO missaoContinuaDAO = new MissaoContinuaDAO();
        missaoContinuaDAO.excluir(continua);
    }


}
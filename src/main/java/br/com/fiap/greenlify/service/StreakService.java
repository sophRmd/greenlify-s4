package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.AdminDAO;
import br.com.fiap.greenlify.dao.StreakDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.dto.StreakResponseDTO;
import br.com.fiap.greenlify.mapper.StreakMapper;
import br.com.fiap.greenlify.models.Streak;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class StreakService {
    private final StreakDAO streakDAO;
    private final UsuarioComumDAO usuarioDAO;
    private final AdminDAO adminDAO;

    public StreakService() {
        this.streakDAO = new StreakDAO();
        this.usuarioDAO = new UsuarioComumDAO();
        this.adminDAO = new AdminDAO();
    }

    public StreakResponseDTO buscarPorIdUsuario(int idUsuario) {
        Streak streak = streakDAO.buscarPorIdUsuario(idUsuario);
        if (streak == null) {
            throw new IllegalArgumentException("Sequência não encontrada");
        }
        streak.encerrarSeInterrompida(LocalDate.now());
        return StreakMapper.toDto(streak);
    }

    /**
     * Retorna a sequência do usuário, criando uma zerada caso ainda não exista.
     */
    public StreakResponseDTO buscarOuCriar(int idUsuario) {
        verificarUsuario(idUsuario);
        Streak streak = streakDAO.buscarOuCriar(idUsuario);
        streak.encerrarSeInterrompida(LocalDate.now());
        return StreakMapper.toDto(streak);
    }

    private void verificarUsuario(int idUsuario) {
        if (usuarioDAO.buscarPorId(idUsuario) == null && adminDAO.buscarPorId(idUsuario) == null) {
            System.out.println("ERRO: Usuário informado não existe.");
            throw new IllegalArgumentException("O usuário informado não existe.");
        }
    }

}
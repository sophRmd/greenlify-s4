package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.CarteiraDAO;
import br.com.fiap.greenlify.dao.SeloDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.dao.UsuarioSeloDAO;
import br.com.fiap.greenlify.dto.UsuarioSeloRequestDTO;
import br.com.fiap.greenlify.dto.UsuarioSeloResponseDTO;
import br.com.fiap.greenlify.mapper.UsuarioSeloMapper;
import br.com.fiap.greenlify.models.Carteira;
import br.com.fiap.greenlify.models.UsuarioSelo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioSeloService {
    private final UsuarioSeloDAO usuarioSeloDAO;
    private final UsuarioComumDAO usuarioDAO;
    private final SeloDAO seloDAO;
    private final CarteiraDAO carteiraDAO;

    public UsuarioSeloService() {
        this.usuarioSeloDAO = new UsuarioSeloDAO();
        this.usuarioDAO = new UsuarioComumDAO();
        this.seloDAO = new SeloDAO();
        this.carteiraDAO = new CarteiraDAO();
    }

    public UsuarioSeloResponseDTO buscarPorId(int id) {
        UsuarioSelo usuarioSelo = usuarioSeloDAO.buscarPorId(id);
        if (usuarioSelo == null) {
            throw new IllegalArgumentException("Coleta de selo não encontrada");
        }
        return UsuarioSeloMapper.toDto(usuarioSelo);
    }

    public List<UsuarioSeloResponseDTO> listar() {
        return usuarioSeloDAO.listar()
                .stream()
                .map(UsuarioSeloMapper::toDto)
                .toList();
    }

    public List<UsuarioSeloResponseDTO> listarPorUsuario(int idUsuario) {
        return usuarioSeloDAO.buscarPorIdUsuario(idUsuario)
                .stream()
                .map(UsuarioSeloMapper::toDto)
                .toList();
    }

    public void cadastrar(UsuarioSeloRequestDTO dto) {
        validar(dto);

        // Um selo por usuário: sem esta regra o mesmo resgate poderia ser contado de novo no ranking do grupo.
        if (usuarioSeloDAO.existeColeta(dto.idUsuario(), dto.idSelo(), 0)) {
            System.out.println("ERRO: O usuário já coletou esse selo.");
            throw new IllegalArgumentException("O usuário já coletou esse selo.");
        }
        Carteira carteira = carteiraDAO.buscarPorIdUsuario(dto.idUsuario());
        int saldo = carteira != null ? carteira.getQtdPontos() : 0;
        if (saldo < seloDAO.buscarPorId(dto.idSelo()).getPontosNecessarios()) {
            System.out.println("ERRO: Pontos insuficientes para coletar o selo.");
            throw new IllegalArgumentException("Pontos insuficientes para coletar esse selo.");
        }

        usuarioSeloDAO.cadastrar(UsuarioSeloMapper.dtoToEntity(dto));
    }

    public void excluir(int id) {
        UsuarioSelo usuarioSelo = usuarioSeloDAO.buscarPorId(id);
        if (usuarioSelo == null) {
            throw new IllegalArgumentException("Coleta de selo não encontrada");
        }
        usuarioSeloDAO.excluir(id);
    }

    public void alterar(UsuarioSeloRequestDTO dto, int id) {
        UsuarioSelo existente = usuarioSeloDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Coleta de selo não encontrada");
        }
        validar(dto);
        // Alterar o usuário ou o selo não pode criar uma segunda coleta do mesmo selo.
        if (usuarioSeloDAO.existeColeta(dto.idUsuario(), dto.idSelo(), id)) {
            throw new IllegalArgumentException("O usuário já coletou esse selo.");
        }

        UsuarioSelo usuarioSelo = UsuarioSeloMapper.dtoToEntity(dto);
        usuarioSelo.setIdUsuarioSelo(id);
        usuarioSeloDAO.alterar(usuarioSelo);
    }

    private void validar(UsuarioSeloRequestDTO dto) {
        if (dto.dataColeta() == null) {
            System.out.println("ERRO: A data de coleta é obrigatória.");
            throw new IllegalArgumentException("A data de coleta é obrigatória.");
        }
        if (dto.dataColeta().isAfter(LocalDate.now())) {
            System.out.println("ERRO: A data de coleta não pode ser futura.");
            throw new IllegalArgumentException("A data de coleta não pode ser futura.");
        }
        if (dto.idUsuario() <= 0) {
            System.out.println("ERRO: O usuário é obrigatório.");
            throw new IllegalArgumentException("O usuário é obrigatório.");
        }
        if (dto.idSelo() <= 0) {
            System.out.println("ERRO: O selo é obrigatório.");
            throw new IllegalArgumentException("O selo é obrigatório.");
        }
        if (usuarioDAO.buscarPorId(dto.idUsuario()) == null) {
            System.out.println("ERRO: Usuário informado não existe.");
            throw new IllegalArgumentException("O usuário informado não existe.");
        }
        if (seloDAO.buscarPorId(dto.idSelo()) == null) {
            System.out.println("ERRO: Selo informado não existe.");
            throw new IllegalArgumentException("O selo informado não existe.");
        }
    }
}
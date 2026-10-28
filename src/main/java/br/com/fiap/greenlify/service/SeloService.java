package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.SeloDAO;
import br.com.fiap.greenlify.dto.SeloRequestDTO;
import br.com.fiap.greenlify.dto.SeloResponseDTO;
import br.com.fiap.greenlify.mapper.SeloMapper;
import br.com.fiap.greenlify.models.Selo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeloService {
    private final SeloDAO seloDAO;

    public SeloService() {
        this.seloDAO = new SeloDAO();
    }

    public SeloResponseDTO buscarPorId(int id) {
        Selo selo = seloDAO.buscarPorId(id);
        if (selo == null) {
            throw new IllegalArgumentException("Selo não encontrado");
        }
        return SeloMapper.toDto(selo);
    }

    public List<SeloResponseDTO> listar() {
        return seloDAO.listar()
                .stream()
                .map(SeloMapper::toDto)
                .toList();
    }

    public void cadastrar(SeloRequestDTO dto) {
        validar(dto);
        if (dto.idSelo() <= 0) {
            System.out.println("ERRO: O id do selo deve ser maior que zero.");
            throw new IllegalArgumentException("O id do selo deve ser maior que zero.");
        }
        if (seloDAO.buscarPorId(dto.idSelo()) != null) {
            System.out.println("ERRO: Já existe um selo com esse id.");
            throw new IllegalArgumentException("Já existe um selo com esse id.");
        }
        seloDAO.cadastrar(SeloMapper.dtoToEntity(dto));
    }

    public void excluir(int id) {
        Selo selo = seloDAO.buscarPorId(id);
        if (selo == null) {
            throw new IllegalArgumentException("Selo não encontrado");
        }
        // Cada resgate de selo conta na pontuação do grupo: apagar o selo apagaria esses pontos.
        if (seloDAO.contarResgates(id) > 0) {
            System.out.println("ERRO: O selo já foi resgatado por usuários.");
            throw new IllegalArgumentException(
                    "O selo já foi resgatado por usuários e não pode ser excluído.");
        }
        seloDAO.excluir(id);
    }

    public void alterar(SeloRequestDTO dto, int id) {
        Selo existente = seloDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Selo não encontrado");
        }
        if (dto.idSelo() != id) {
            System.out.println("ERRO: O id informado está incorreto.");
            throw new IllegalArgumentException("O id informado está incorreto.");
        }
        validar(dto);
        seloDAO.alterar(SeloMapper.dtoToEntity(dto));
    }

    private void validar(SeloRequestDTO dto) {
        if (dto.nome() == null || dto.nome().isBlank()) {
            System.out.println("ERRO: Nome é obrigatório");
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (dto.valeEnergia() < 0) {
            System.out.println("ERRO: O vale-energia não pode ser negativo.");
            throw new IllegalArgumentException("O vale-energia não pode ser negativo.");
        }
        if (dto.creditoCarbono() < 0) {
            System.out.println("ERRO: O crédito de carbono não pode ser negativo.");
            throw new IllegalArgumentException("O crédito de carbono não pode ser negativo.");
        }
        if (dto.pontosNecessarios() <= 0) {
            System.out.println("ERRO: Os pontos necessários devem ser maiores que zero.");
            throw new IllegalArgumentException("Os pontos necessários devem ser maiores que zero.");
        }
    }
}
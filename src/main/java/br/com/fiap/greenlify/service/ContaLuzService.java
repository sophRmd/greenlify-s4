package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.ContaLuzDAO;
import br.com.fiap.greenlify.dto.ContaLuzRequestDTO;
import br.com.fiap.greenlify.dto.ContalLuzResponseDTO;
import br.com.fiap.greenlify.mapper.ContaLuzMapper;
import br.com.fiap.greenlify.models.ContaLuz;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContaLuzService {

    private final ContaLuzDAO contaLuzDAO;

    public ContaLuzService() {
        this.contaLuzDAO = new ContaLuzDAO();
    }

    public List<ContalLuzResponseDTO> listar() {
        return contaLuzDAO.listar()
                .stream()
                .map(ContaLuzMapper::toDto)
                .toList();
    }

    public ContalLuzResponseDTO buscarPorId(int id) {
        var conta = contaLuzDAO.buscarPorId(id);
        if (conta == null)
            throw new IllegalArgumentException("Conta de luz não encontrada");
        return ContaLuzMapper.toDto(conta);
    }

    public void cadastrar(ContaLuzRequestDTO dto) {
        validar(dto);

        ContaLuz conta = ContaLuzMapper.dtoToEntity(dto);
        // O DAO insere o id explicitamente e o DTO não o carrega,
        // então o próximo id é calculado a partir das contas existentes.
        conta.setIdContaLuz(proximoId());
        contaLuzDAO.cadastrar(conta);
    }

    public void atualizar(int id, ContaLuzRequestDTO dto) {
        if (contaLuzDAO.buscarPorId(id) == null)
            throw new IllegalArgumentException("Conta de luz não encontrada");

        validar(dto);

        // O DTO não carrega o id, então ele vem da URL.
        ContaLuz conta = ContaLuzMapper.dtoToEntity(dto);
        conta.setIdContaLuz(id);
        contaLuzDAO.alterar(conta);
    }

    public void excluir(int id) {
        if (contaLuzDAO.buscarPorId(id) == null)
            throw new IllegalArgumentException("Conta de luz não encontrada");

        contaLuzDAO.excluir(id);
    }

    private void validar(ContaLuzRequestDTO dto) {
        if (dto.codigoBarras() == null || dto.codigoBarras().isBlank()) {
            System.out.println("ERRO: O código de barras é obrigatório");
            throw new IllegalArgumentException("O código de barras é obrigatório.");
        }
        if (dto.valorConta() <= 0) {
            System.out.println("ERRO: O valor da conta deve ser maior que zero.");
            throw new IllegalArgumentException("O valor da conta deve ser maior que zero.");
        }

    }

    private int proximoId() {
        return contaLuzDAO.listar()
                .stream()
                .mapToInt(ContaLuz::getIdContaLuz)
                .max()
                .orElse(0) + 1;
    }
}
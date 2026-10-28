package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.CarteiraDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.dto.CarteiraRequestDTO;
import br.com.fiap.greenlify.dto.CarteiraResponseDTO;
import br.com.fiap.greenlify.mapper.CarteiraMapper;
import br.com.fiap.greenlify.models.Carteira;
import br.com.fiap.greenlify.models.UsuarioComum;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarteiraService {

    private final CarteiraDAO carteiraDAO;
    private final UsuarioComumDAO usuarioDAO;

    public CarteiraService() {
        this.carteiraDAO = new CarteiraDAO();
        this.usuarioDAO = new UsuarioComumDAO();
    }

    public List<CarteiraResponseDTO> listar() {
        return carteiraDAO.listar()
                .stream()
                .map(CarteiraMapper::toDto)
                .toList();
    }

    public CarteiraResponseDTO buscarPorId(int id) {
        var carteira = carteiraDAO.buscarPorId(id);
        if (carteira == null)
            throw new IllegalArgumentException("Carteira não encontrada");
        return CarteiraMapper.toDto(carteira);
    }

    public CarteiraResponseDTO buscarPorIdUsuario(int idUsuario) {
        var carteira = carteiraDAO.buscarPorIdUsuario(idUsuario);
        if (carteira == null) {
            // a carteira só é gravada no primeiro crédito de pontos; antes disso o usuário tem 0 pts
            UsuarioComum usuario = usuarioDAO.buscarPorId(idUsuario);
            if (usuario == null)
                throw new IllegalArgumentException("Carteira não encontrada");
            carteira = new Carteira();
            carteira.setUsuario(usuario);
            carteira.setIdCarteira(idUsuario);
        }
        return CarteiraMapper.toDto(carteira);
    }

    public void cadastrar(int idUsuario, CarteiraRequestDTO dto) {
        UsuarioComum usuario = usuarioDAO.buscarPorId(idUsuario);
        if (usuario == null) {
            System.out.println("ERRO: Usuário não encontrado");
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        if (carteiraDAO.buscarPorIdUsuario(idUsuario) != null) {
            System.out.println("ERRO: Usuário já possui carteira");
            throw new IllegalArgumentException("Usuário já possui carteira.");
        }

        Carteira carteira = CarteiraMapper.dtoToEntity(dto);
        carteira.setUsuario(usuario);
        carteiraDAO.cadastrar(carteira);
    }

    /**
     * Pontos e valor em reais pertencem ao servidor e não podem ser alterados pelo cliente, então
     * não há nada a gravar: o saldo existente é preservado. Só confirma que a carteira existe.
     */
    public void atualizar(int id, CarteiraRequestDTO dto) {
        if (carteiraDAO.buscarPorId(id) == null)
            throw new IllegalArgumentException("Carteira não encontrada");
    }

    public void excluir(int id) {
        if (carteiraDAO.buscarPorId(id) == null)
            throw new IllegalArgumentException("Carteira não encontrada");

        carteiraDAO.excluir(id);
    }
}
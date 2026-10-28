package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.CarteiraDAO;
import br.com.fiap.greenlify.dao.ContaLuzDAO;
import br.com.fiap.greenlify.dao.PagamentoContaDAO;
import br.com.fiap.greenlify.dto.PagamentoContaRequestDTO;
import br.com.fiap.greenlify.dto.PagamentoContaResponseDTO;
import br.com.fiap.greenlify.mapper.PagamentoContaMapper;
import br.com.fiap.greenlify.models.Carteira;
import br.com.fiap.greenlify.models.PagamentoConta;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PagamentoContaService {
    // Mesma taxa de conversão usada em Carteira (R$ por ponto).
    private static final double REAIS_POR_PONTO = 0.009052;

    private final PagamentoContaDAO pagamentoDAO;
    private final CarteiraDAO carteiraDAO;
    private final ContaLuzDAO contaLuzDAO;

    public PagamentoContaService() {
        this.pagamentoDAO = new PagamentoContaDAO();
        this.carteiraDAO = new CarteiraDAO();
        this.contaLuzDAO = new ContaLuzDAO();
    }

    public PagamentoContaResponseDTO buscarPorId(int id) {
        PagamentoConta pagamento = pagamentoDAO.buscarPorId(id);
        if (pagamento == null) {
            throw new IllegalArgumentException("Pagamento não encontrado");
        }
        return PagamentoContaMapper.toDto(pagamento);
    }

    public List<PagamentoContaResponseDTO> listar() {
        return pagamentoDAO.listar()
                .stream()
                .map(PagamentoContaMapper::toDto)
                .toList();
    }

    public List<PagamentoContaResponseDTO> listarPorUsuario(int idUsuario) {
        return pagamentoDAO.listarPorUsuario(idUsuario)
                .stream()
                .map(PagamentoContaMapper::toDto)
                .toList();
    }

    public void cadastrar(PagamentoContaRequestDTO dto) {
        validar(dto);
        if (contaLuzDAO.buscarPorId(dto.idContaLuz()) == null) {
            System.out.println("ERRO: Conta de luz informada não existe.");
            throw new IllegalArgumentException("A conta de luz informada não existe.");
        }
        Carteira carteira = carteiraDAO.buscarPorIdUsuario(dto.idUsuario());
        if (carteira == null || carteira.getValorReal() < dto.valorPago()) {
            System.out.println("ERRO: Saldo insuficiente na carteira.");
            throw new IllegalArgumentException("Saldo insuficiente na carteira para pagar a conta.");
        }

        PagamentoConta pagamento = PagamentoContaMapper.dtoToEntity(dto);
        pagamentoDAO.cadastrar(pagamento);
        if (pagamento.getIdPagamento() <= 0) {
            throw new IllegalStateException("Não foi possível gravar o pagamento.");
        }

        // Pagamento gravado: debita da carteira os pontos equivalentes ao valor pago.
        int pontosUsados = (int) Math.ceil(dto.valorPago() / REAIS_POR_PONTO);
        carteira.setQtdPontos(Math.max(0, carteira.getQtdPontos() - pontosUsados));
        carteira.recalcularValorReal();
        carteiraDAO.atualizarSaldoPontos(carteira);
    }

    public void excluir(int id) {
        PagamentoConta pagamento = pagamentoDAO.buscarPorId(id);
        if (pagamento == null) {
            throw new IllegalArgumentException("Pagamento não encontrado");
        }
        pagamentoDAO.excluir(id);
    }

    public void alterar(PagamentoContaRequestDTO dto, int id) {
        PagamentoConta existente = pagamentoDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Pagamento não encontrado");
        }
        validar(dto);

        PagamentoConta pagamento = PagamentoContaMapper.dtoToEntity(dto);
        pagamento.setIdPagamento(id);
        pagamentoDAO.alterar(pagamento);
    }

    private void validar(PagamentoContaRequestDTO dto) {
        if (dto.dataPagamento() == null) {
            System.out.println("ERRO: A data do pagamento é obrigatória.");
            throw new IllegalArgumentException("A data do pagamento é obrigatória.");
        }
        if (dto.valorPago() <= 0) {
            System.out.println("ERRO: O valor pago deve ser maior que zero.");
            throw new IllegalArgumentException("O valor pago deve ser maior que zero.");
        }
        if (dto.idUsuario() <= 0) {
            System.out.println("ERRO: O usuário é obrigatório.");
            throw new IllegalArgumentException("O usuário é obrigatório.");
        }
        if (dto.idContaLuz() <= 0) {
            System.out.println("ERRO: A conta de luz é obrigatória.");
            throw new IllegalArgumentException("A conta de luz é obrigatória.");
        }
    }
}
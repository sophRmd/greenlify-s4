package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.PagamentoContaRequestDTO;
import br.com.fiap.greenlify.dto.PagamentoContaResponseDTO;
import br.com.fiap.greenlify.models.PagamentoConta;

public class PagamentoContaMapper {
    public static PagamentoConta dtoToEntity (PagamentoContaRequestDTO dto){
        PagamentoConta pagamento = new PagamentoConta();
        pagamento.setDataPagamento(dto.dataPagamento());
        pagamento.setValorPago(dto.valorPago());
        pagamento.setUsuario(ReferenciaMapper.usuario(dto.idUsuario()));
        pagamento.setContaLuz(ReferenciaMapper.contaLuz(dto.idContaLuz()));
        return pagamento;
    }

    public static PagamentoContaResponseDTO toDto (PagamentoConta pagamento){
        return new PagamentoContaResponseDTO(
                pagamento.getIdPagamento(),
                pagamento.getDataPagamento(),
                pagamento.getValorPago(),
                UsuarioResumoMapper.toDto(pagamento.getUsuario()),
                pagamento.getContaLuz()
        );
    }
}

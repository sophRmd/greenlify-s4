package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.ContaLuzRequestDTO;
import br.com.fiap.greenlify.dto.ContalLuzResponseDTO;
import br.com.fiap.greenlify.models.ContaLuz;

public class ContaLuzMapper {
    public static ContaLuz dtoToEntity(ContaLuzRequestDTO dto){
        ContaLuz contaLuz = new ContaLuz();
        contaLuz.setCodigoBarras(dto.codigoBarras());
        contaLuz.setValorConta(dto.valorConta());
        return contaLuz;
    }

    public static ContalLuzResponseDTO toDto (ContaLuz contaLuz){
        return new ContalLuzResponseDTO(
                contaLuz.getIdContaLuz(),
                contaLuz.getCodigoBarras(),
                contaLuz.getValorConta()
        );
    }
}

package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.CarteiraRequestDTO;
import br.com.fiap.greenlify.dto.CarteiraResponseDTO;
import br.com.fiap.greenlify.models.Carteira;

public class CarteiraMapper {
    /** Carteira nova: o saldo sempre começa zerado, o cliente não define pontos nem valor. */
    public static Carteira dtoToEntity(CarteiraRequestDTO dto){
        Carteira carteira = new Carteira();
        carteira.setQtdPontos(0);
        carteira.recalcularValorReal();
        return carteira;
    }

    public static CarteiraResponseDTO toDto (Carteira carteira){
        return new CarteiraResponseDTO(
                carteira.getIdCarteira(),
                carteira.getQtdPontos(),
                carteira.getValorReal()
        );
    }
}
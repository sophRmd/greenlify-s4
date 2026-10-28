package br.com.fiap.greenlify.mapper;

import br.com.fiap.greenlify.dto.PontuacaoRequestDTO;
import br.com.fiap.greenlify.dto.PontuacaoResponseDTO;
import br.com.fiap.greenlify.models.Pontuacao;

public class PontuacaoMapper {
    public static Pontuacao dtoToEntity (PontuacaoRequestDTO dto){
        Pontuacao pontuacao = new Pontuacao();
        pontuacao.setIdPontuacao(dto.idPontuacao());
        pontuacao.setImpacto(dto.impacto());
        pontuacao.setDificuldade(dto.dificuldade());
        // O valor nunca vem do cliente: é sempre o da tabela para impacto e dificuldade.
        pontuacao.setValorBase(Pontuacao.valorPadrao(dto.impacto(), dto.dificuldade()));
        return pontuacao;
    }

    public static PontuacaoResponseDTO toDto (Pontuacao pontuacao){
        PontuacaoResponseDTO dto = new PontuacaoResponseDTO(
                pontuacao.getIdPontuacao(),
                pontuacao.getImpacto(),
                pontuacao.getDificuldade(),
                pontuacao.getValorBase()
        );
        return dto;
    }
}
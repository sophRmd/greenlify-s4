package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.PontuacaoDAO;
import br.com.fiap.greenlify.dto.PontuacaoRequestDTO;
import br.com.fiap.greenlify.dto.PontuacaoResponseDTO;
import br.com.fiap.greenlify.mapper.PontuacaoMapper;
import br.com.fiap.greenlify.models.Pontuacao;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PontuacaoService {
    private final PontuacaoDAO pontuacaoDAO;

    public PontuacaoService() {
        this.pontuacaoDAO = new PontuacaoDAO();
    }

    public PontuacaoResponseDTO buscarPorId(int id) {
        Pontuacao pontuacao = pontuacaoDAO.buscarPorId(id);
        if (pontuacao == null) {
            throw new IllegalArgumentException("Pontuação não encontrada");
        }
        return PontuacaoMapper.toDto(pontuacao);
    }

    public PontuacaoResponseDTO buscarPorImpactoEDificuldade(String impacto, String dificuldade) {
        if (impacto == null || impacto.isBlank()) {
            throw new IllegalArgumentException("Impacto é obrigatório.");
        }
        if (dificuldade == null || dificuldade.isBlank()) {
            throw new IllegalArgumentException("Dificuldade é obrigatória.");
        }
        String impactoValido = impacto.trim().toUpperCase();
        String dificuldadeValida = dificuldade.trim().toUpperCase();
        Pontuacao pontuacao = pontuacaoDAO.buscarPorImpactoEDificuldade(impactoValido, dificuldadeValida);
        if (pontuacao == null) {
            throw new IllegalArgumentException("Pontuação não encontrada");
        }
        return PontuacaoMapper.toDto(pontuacao);
    }

    public List<PontuacaoResponseDTO> listar() {
        return pontuacaoDAO.listar()
                .stream()
                .map(PontuacaoMapper::toDto)
                .toList();
    }

    /**
     * Cadastra a pontuação. O valor em pontos nunca vem do cliente: é o da tabela para o impacto e a
     * dificuldade, e o DAO ainda confere a correspondência antes de gravar.
     *
     * @throws IllegalArgumentException se o DTO é inválido ou a combinação/id já existe
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public void cadastrar(PontuacaoRequestDTO dto) {
        validar(dto);

        if (pontuacaoDAO.buscarPorId(dto.idPontuacao()) != null) {
            System.out.println("ERRO: Já existe uma pontuação com esse id.");
            throw new IllegalArgumentException("Já existe uma pontuação com esse id.");
        }
        if (existeCombinacao(dto, null)) {
            System.out.println("ERRO: Já existe uma pontuação para essa combinação de impacto e dificuldade.");
            throw new IllegalArgumentException(
                    "Já existe uma pontuação para essa combinação de impacto e dificuldade.");
        }
        pontuacaoDAO.cadastrar(entidadeConsistente(dto));
    }

    /**
     * Exclui a pontuação, desde que nenhuma missão contínua a use (conferido pelo DAO).
     *
     * @throws IllegalArgumentException se a pontuação não existe ou está em uso
     * @throws IllegalStateException    se o banco falhar
     */
    public void excluir(int id) {
        Pontuacao pontuacao = pontuacaoDAO.buscarPorId(id);
        if (pontuacao == null) {
            throw new IllegalArgumentException("Pontuação não encontrada");
        }
        pontuacaoDAO.excluir(id);
    }

    /**
     * Altera impacto e dificuldade; o valor em pontos é recalculado pela tabela. Missões que usam
     * a pontuação passam a creditar o novo valor nas próximas conclusões; o que já foi creditado
     * não muda.
     *
     * @throws IllegalArgumentException se a pontuação não existe, o DTO é inválido ou a combinação já existe
     * @throws IllegalStateException    se o banco falhar
     */
    public void alterar(PontuacaoRequestDTO dto, int id) {
        Pontuacao existente = pontuacaoDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Pontuação não encontrada");
        }
        if (dto.idPontuacao() != id) {
            System.out.println("ERRO: O id informado está incorreto.");
            throw new IllegalArgumentException("O id informado está incorreto.");
        }
        validar(dto);

        if (existeCombinacao(dto, id)) {
            System.out.println("ERRO: Já existe uma pontuação para essa combinação de impacto e dificuldade.");
            throw new IllegalArgumentException(
                    "Já existe uma pontuação para essa combinação de impacto e dificuldade.");
        }
        pontuacaoDAO.alterar(entidadeConsistente(dto));
    }

    /** Monta a pontuação e confere se o valor corresponde a impacto e dificuldade (senão, recusa). */
    private Pontuacao entidadeConsistente(PontuacaoRequestDTO dto) {
        Pontuacao pontuacao = PontuacaoMapper.dtoToEntity(dto);
        if (!pontuacao.correspondeAoCalculo()) {
            throw new IllegalArgumentException(
                    "O valor da pontuação não corresponde ao impacto e à dificuldade informados.");
        }
        return pontuacao;
    }

    private boolean existeCombinacao(PontuacaoRequestDTO dto, Integer idIgnorado) {
        Pontuacao outra = pontuacaoDAO.buscarPorImpactoEDificuldade(
                dto.impacto().toString(), dto.dificuldade().toString());
        return outra != null && (idIgnorado == null || outra.getIdPontuacao() != idIgnorado);
    }

    private void validar(PontuacaoRequestDTO dto) {
        if (dto.idPontuacao() <= 0) {
            System.out.println("ERRO: O id da pontuação deve ser maior que zero.");
            throw new IllegalArgumentException("O id da pontuação deve ser maior que zero.");
        }
        if (dto.impacto() == null) {
            System.out.println("ERRO: O impacto é obrigatório.");
            throw new IllegalArgumentException("O impacto é obrigatório.");
        }
        if (dto.dificuldade() == null) {
            System.out.println("ERRO: A dificuldade é obrigatória.");
            throw new IllegalArgumentException("A dificuldade é obrigatória.");
        }
    }
}
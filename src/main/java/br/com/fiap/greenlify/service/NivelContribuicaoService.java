package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.NivelContribuicaoDAO;
import br.com.fiap.greenlify.dto.NivelContribuicaoRequestDTO;
import br.com.fiap.greenlify.dto.NivelContribuicaoResponseDTO;
import br.com.fiap.greenlify.mapper.NivelContribuicaoMapper;
import br.com.fiap.greenlify.models.NivelContribuicao;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NivelContribuicaoService {
    private final NivelContribuicaoDAO nivelDAO;

    public NivelContribuicaoService() {
        this.nivelDAO = new NivelContribuicaoDAO();
    }

    public NivelContribuicaoResponseDTO buscarPorId(int id) {
        NivelContribuicao nivel = nivelDAO.buscarPorId(id);
        if (nivel == null) {
            throw new IllegalArgumentException("Nível de contribuição não encontrado");
        }
        return NivelContribuicaoMapper.toDto(nivel);
    }

    public List<NivelContribuicaoResponseDTO> listar() {
        return nivelDAO.listar()
                .stream()
                .map(NivelContribuicaoMapper::toDto)
                .toList();
    }

    public void cadastrar(NivelContribuicaoRequestDTO dto) {
        validar(dto);
        nivelDAO.cadastrar(NivelContribuicaoMapper.dtoToEntity(dto));
    }

    public void excluir(int id) {
        NivelContribuicao nivel = nivelDAO.buscarPorId(id);
        if (nivel == null) {
            throw new IllegalArgumentException("Nível de contribuição não encontrado");
        }
        nivelDAO.excluir(id);
    }

    public void alterar(NivelContribuicaoRequestDTO dto, int id) {
        NivelContribuicao existente = nivelDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Nível de contribuição não encontrado");
        }
        validar(dto);

        NivelContribuicao nivel = NivelContribuicaoMapper.dtoToEntity(dto);
        nivel.setIdNivel(id);
        nivelDAO.alterar(nivel);
    }

    private void validar(NivelContribuicaoRequestDTO dto) {
        if (dto.nivel() <= 0) {
            System.out.println("ERRO: O número do nível deve ser maior que zero.");
            throw new IllegalArgumentException("O número do nível deve ser maior que zero.");
        }
        if (dto.nome() == null || dto.nome().isBlank()) {
            System.out.println("ERRO: Nome é obrigatório");
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (dto.xpMinimo() < 0) {
            System.out.println("ERRO: O XP mínimo não pode ser negativo.");
            throw new IllegalArgumentException("O XP mínimo não pode ser negativo.");
        }
        if (dto.xpMaximo() <= dto.xpMinimo()) {
            System.out.println("ERRO: O XP máximo deve ser maior que o XP mínimo.");
            throw new IllegalArgumentException("O XP máximo deve ser maior que o XP mínimo.");
        }
    }
}
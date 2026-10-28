package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.GrupoDAO;
import br.com.fiap.greenlify.dto.GrupoRequestDTO;
import br.com.fiap.greenlify.dto.GrupoResponseDTO;
import br.com.fiap.greenlify.mapper.GrupoMapper;
import br.com.fiap.greenlify.models.Grupo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GrupoService {

    // Status atribuído a todo grupo novo, já que o DTO de cadastro não o informa.
    // Ajuste para o valor que representa "ativo" no seu banco.
    private static final int STATUS_INICIAL = 1;

    private final GrupoDAO grupoDAO;

    public GrupoService() {
        this.grupoDAO = new GrupoDAO();
    }

    /**
     * Nº de integrantes por grupo, contado a partir de Usuario.id_grupo (a coluna nr_usuarios não é
     * atualizada ao entrar/sair). Só mede o tamanho do grupo: a pontuação no ranking vem dos selos
     * resgatados pelos integrantes, não dessa contagem.
     */
    private Map<Integer, Integer> contarIntegrantes() {
        return grupoDAO.contarIntegrantes();
    }

    public List<GrupoResponseDTO> listar() {
        Map<Integer, Integer> integrantes = contarIntegrantes();
        return grupoDAO.listar()
                .stream()
                .peek(g -> g.setQtdUsuarios(integrantes.getOrDefault(g.getIdGrupo(), 0)))
                .map(GrupoMapper::toDto)
                .toList();
    }

    public GrupoResponseDTO buscarPorId(int id) {
        var grupo = grupoDAO.buscarPorId(id);
        if (grupo == null)
            throw new IllegalArgumentException("Grupo não encontrado");
        grupo.setQtdUsuarios(contarIntegrantes().getOrDefault(grupo.getIdGrupo(), 0));
        return GrupoMapper.toDto(grupo);
    }

    public void cadastrar(GrupoRequestDTO dto) {
        validar(dto);

        Grupo grupo = GrupoMapper.dtoToEntity(dto);
        grupo.setStatusGrupo(STATUS_INICIAL);
        grupoDAO.cadastrar(grupo);
    }

    public void atualizar(int id, GrupoRequestDTO dto) {
        Grupo grupoExiste = grupoDAO.buscarPorId(id);
        if (grupoExiste == null)
            throw new IllegalArgumentException("Grupo não encontrado");

        validar(dto);

        // O DTO não carrega o id nem o status: o id vem da URL
        // e o status atual é preservado para o UPDATE não zerá-lo.
        // A quantidade informada pelo cliente é ignorada: o tamanho real do grupo é o nº de integrantes.
        Grupo grupo = GrupoMapper.dtoToEntity(dto);
        grupo.setIdGrupo(id);
        grupo.setStatusGrupo(grupoExiste.getStatusGrupo());
        grupo.setQtdUsuarios(contarIntegrantes().getOrDefault(id, 0));
        grupoDAO.alterar(grupo);
    }

    public void excluir(int id) {
        if (grupoDAO.buscarPorId(id) == null)
            throw new IllegalArgumentException("Grupo não encontrado");

        grupoDAO.excluir(id);
    }

    private void validar(GrupoRequestDTO dto) {
        if (dto.nome() == null || dto.nome().isBlank()) {
            System.out.println("ERRO: O nome do grupo é obrigatório");
            throw new IllegalArgumentException("O nome do grupo é obrigatório.");
        }
        if (dto.dataCriacao() == null) {
            System.out.println("ERRO: A data de criação é obrigatória");
            throw new IllegalArgumentException("A data de criação é obrigatória.");
        }
        if (dto.qtdUsuarios() < 0) {
            System.out.println("ERRO: A quantidade de usuários não pode ser negativa.");
            throw new IllegalArgumentException("A quantidade de usuários não pode ser negativa.");
        }
    }
}
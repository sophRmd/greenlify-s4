package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.PerfilSustentavelDAO;
import br.com.fiap.greenlify.dao.UsuarioMissaoDAO;
import br.com.fiap.greenlify.dto.PerfilSustentavelRequestDTO;
import br.com.fiap.greenlify.dto.PerfilSustentavelResponseDTO;
import br.com.fiap.greenlify.mapper.PerfilSustentavelMapper;
import br.com.fiap.greenlify.models.PerfilSustentavel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PerfilSustentavelService {
    private final PerfilSustentavelDAO perfilDAO;
    private final UsuarioMissaoDAO usuarioMissaoDAO;

    public PerfilSustentavelService() {
        this.perfilDAO = new PerfilSustentavelDAO();
        this.usuarioMissaoDAO = new UsuarioMissaoDAO();
    }

    public PerfilSustentavelResponseDTO buscarPorId(int id) {
        PerfilSustentavel perfil = perfilDAO.buscarPorId(id);
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil sustentável não encontrado");
        }
        return PerfilSustentavelMapper.toDto(comTotalReal(perfil, usuarioMissaoDAO.contarConcluidasPorUsuario()));
    }

    public List<PerfilSustentavelResponseDTO> listar() {
        Map<Integer, Integer> totais = usuarioMissaoDAO.contarConcluidasPorUsuario();
        return perfilDAO.listar()
                .stream()
                .map(p -> PerfilSustentavelMapper.toDto(comTotalReal(p, totais)))
                .toList();
    }

    public void cadastrar(PerfilSustentavelRequestDTO dto) {
        validar(dto);
        perfilDAO.cadastrar(PerfilSustentavelMapper.dtoToEntity(dto));
    }

    public void excluir(int id) {
        PerfilSustentavel perfil = perfilDAO.buscarPorId(id);
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil sustentável não encontrado");
        }
        perfilDAO.excluir(id);
    }

    public void alterar(PerfilSustentavelRequestDTO dto, int id) {
        PerfilSustentavel existente = perfilDAO.buscarPorId(id);
        if (existente == null) {
            throw new IllegalArgumentException("Perfil sustentável não encontrado");
        }
        if (dto.idPerfilSustentavel() != id) {
            System.out.println("ERRO: O id informado está incorreto.");
            throw new IllegalArgumentException("O id informado está incorreto.");
        }
        validar(dto);
        // o total de missões concluídas não é informado pelo cliente: é o mesmo do ranking
        perfilDAO.alterar(comTotalReal(PerfilSustentavelMapper.dtoToEntity(dto), usuarioMissaoDAO.contarConcluidasPorUsuario()));
    }

    /**
     * Troca o total de missões concluídas pelo mesmo número usado no ranking (conclusões de missões
     * contínuas). É diferente da pontuação e dos indicadores ambientais (água, energia, carbono),
     * que continuam sendo os do perfil.
     */
    private PerfilSustentavel comTotalReal(PerfilSustentavel perfil, Map<Integer, Integer> totais) {
        int idUsuario = perfil.getUsuario() != null
                ? perfil.getUsuario().getIdUsuario()
                : perfil.getIdPerfilSustentavel();
        perfil.setTotalMissoesConcluidas(totais.getOrDefault(idUsuario, 0));
        return perfil;
    }

    private void validar(PerfilSustentavelRequestDTO dto) {
        if (dto.idUsuario() <= 0) {
            System.out.println("ERRO: O usuário é obrigatório.");
            throw new IllegalArgumentException("O usuário é obrigatório.");
        }
        if (dto.idNivel() <= 0) {
            System.out.println("ERRO: O nível de contribuição é obrigatório.");
            throw new IllegalArgumentException("O nível de contribuição é obrigatório.");
        }
        if (dto.xpSustentavel() < 0) {
            System.out.println("ERRO: O XP não pode ser negativo.");
            throw new IllegalArgumentException("O XP não pode ser negativo.");
        }
        if (dto.totalMissoesConcluidas() < 0) {
            System.out.println("ERRO: O total de missões concluídas não pode ser negativo.");
            throw new IllegalArgumentException("O total de missões concluídas não pode ser negativo.");
        }
        if (dto.totalAguaEconomizada() < 0
                || dto.totalEnergiaEconomizada() < 0
                || dto.totalCarbonoEconomizado() < 0) {
            System.out.println("ERRO: Os totais economizados não podem ser negativos.");
            throw new IllegalArgumentException("Os totais economizados não podem ser negativos.");
        }
    }
}
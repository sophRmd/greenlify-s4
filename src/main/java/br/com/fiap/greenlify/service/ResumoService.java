package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.AdminDAO;
import br.com.fiap.greenlify.dao.CarteiraDAO;
import br.com.fiap.greenlify.dao.GrupoDAO;
import br.com.fiap.greenlify.dao.MissaoContinuaDAO;
import br.com.fiap.greenlify.dao.MissaoMensalDAO;
import br.com.fiap.greenlify.dao.PosicaoRankingDAO;
import br.com.fiap.greenlify.dao.SeloDAO;
import br.com.fiap.greenlify.dao.StreakDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.dao.UsuarioMissaoDAO;
import br.com.fiap.greenlify.dao.UsuarioSeloDAO;
import br.com.fiap.greenlify.dto.ResumoGrupoResponseDTO;
import br.com.fiap.greenlify.dto.ResumoImpactoResponseDTO;
import br.com.fiap.greenlify.dto.ResumoMissoesResponseDTO;
import br.com.fiap.greenlify.dto.ResumoMissoesResponseDTO.GrupoMissoes;
import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;
import br.com.fiap.greenlify.models.Carteira;
import br.com.fiap.greenlify.models.Grupo;
import br.com.fiap.greenlify.models.MissaoContinua;
import br.com.fiap.greenlify.models.MissaoMensal;
import br.com.fiap.greenlify.models.PosicaoRanking;
import br.com.fiap.greenlify.models.Selo;
import br.com.fiap.greenlify.models.Streak;
import br.com.fiap.greenlify.models.Usuario;
import br.com.fiap.greenlify.models.UsuarioComum;
import br.com.fiap.greenlify.models.UsuarioMissao;
import br.com.fiap.greenlify.models.UsuarioSelo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resumos calculados a partir dos dados que já existem no banco (missões,
 * progresso do usuário, selos, ranking e streak). Somente leitura.
 */
@Service
public class ResumoService {
    private final UsuarioComumDAO usuarioDAO;
    private final AdminDAO adminDAO;
    private final MissaoContinuaDAO continuaDAO;
    private final MissaoMensalDAO mensalDAO;
    private final UsuarioMissaoDAO usuarioMissaoDAO;
    private final GrupoDAO grupoDAO;
    private final SeloDAO seloDAO;
    private final UsuarioSeloDAO usuarioSeloDAO;
    private final CarteiraDAO carteiraDAO;
    private final PosicaoRankingDAO posicaoDAO;
    private final StreakDAO streakDAO;

    public ResumoService() {
        this.usuarioDAO = new UsuarioComumDAO();
        this.adminDAO = new AdminDAO();
        this.continuaDAO = new MissaoContinuaDAO();
        this.mensalDAO = new MissaoMensalDAO();
        this.usuarioMissaoDAO = new UsuarioMissaoDAO();
        this.grupoDAO = new GrupoDAO();
        this.seloDAO = new SeloDAO();
        this.usuarioSeloDAO = new UsuarioSeloDAO();
        this.carteiraDAO = new CarteiraDAO();
        this.posicaoDAO = new PosicaoRankingDAO();
        this.streakDAO = new StreakDAO();
    }

    // ---------- Missões do usuário ----------

    public ResumoMissoesResponseDTO resumirMissoes(int idUsuario) {
        verificarUsuario(idUsuario);
        Map<Integer, Integer> progresso = progressoPorMissao(idUsuario);
        LocalDate hoje = LocalDate.now();

        int total = 0;
        int emAndamento = 0;
        int concluidas = 0;
        int pontos = 0;
        for (MissaoContinua m : continuaDAO.listar()) {
            total++;
            if (m.getPontuacao() != null) {
                // mesma regra que credita pontos: o que a missão vale ao ser concluída
                pontos += m.getPontuacao().pontosElegiveis(
                        TipoMissaoEnum.MISSAO_CONTINUA, StatusUploadEnum.APROVADO, true);
            }
            Integer feito = progresso.get(m.getIdMissao());
            if (feito != null && concluida(feito, m.getLimiteRealizacoes())) {
                concluidas++;
            } else if (feito != null) {
                emAndamento++;
            }
        }
        GrupoMissoes continuas = new GrupoMissoes(total, emAndamento, concluidas,
                percentual(concluidas, total), pontos, -1);

        int totalMensais = 0;
        int andamentoMensais = 0;
        int concluidasMensais = 0;
        int diasParaExpirar = -1;
        for (MissaoMensal m : mensalDAO.listar()) {
            if (m.getDataFim() == null || m.getDataFim().isBefore(hoje)) {
                continue; // só missões mensais vigentes
            }
            totalMensais++;
            Integer feito = progresso.get(m.getIdMissao());
            if (feito != null && concluida(feito, m.getMeta())) {
                concluidasMensais++;
            } else {
                if (feito != null) {
                    andamentoMensais++;
                }
                int dias = (int) ChronoUnit.DAYS.between(hoje, m.getDataFim());
                if (diasParaExpirar < 0 || dias < diasParaExpirar) {
                    diasParaExpirar = dias;
                }
            }
        }
        GrupoMissoes mensais = new GrupoMissoes(totalMensais, andamentoMensais, concluidasMensais,
                percentual(concluidasMensais, totalMensais), 0, diasParaExpirar);

        return new ResumoMissoesResponseDTO(idUsuario, continuas, mensais);
    }

    // ---------- Grupo ----------

    public ResumoGrupoResponseDTO resumirGrupo(int idGrupo) {
        Grupo grupo = grupoDAO.buscarPorId(idGrupo);
        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não encontrado");
        }

        Map<Integer, Integer> quantidadePorSelo = new HashMap<>();
        List<ResumoGrupoResponseDTO.Integrante> integrantes = new ArrayList<>();
        int totalSelos = 0;

        for (UsuarioComum u : usuarioDAO.listar()) {
            if (u.getGrupo() == null || u.getGrupo().getIdGrupo() != idGrupo) {
                continue;
            }
            int selosDoUsuario = 0;
            for (UsuarioSelo us : usuarioSeloDAO.buscarPorIdUsuario(u.getIdUsuario())) {
                if (us.getSelo() != null) {
                    selosDoUsuario++;
                    quantidadePorSelo.merge(us.getSelo().getIdSelo(), 1, Integer::sum);
                }
            }
            totalSelos += selosDoUsuario;
            integrantes.add(new ResumoGrupoResponseDTO.Integrante(0, u.getIdUsuario(), u.getNome(), selosDoUsuario));
        }

        integrantes.sort((a, b) -> Integer.compare(b.selos(), a.selos()));
        List<ResumoGrupoResponseDTO.Integrante> ordenados = new ArrayList<>();
        for (int i = 0; i < integrantes.size(); i++) {
            ResumoGrupoResponseDTO.Integrante in = integrantes.get(i);
            ordenados.add(new ResumoGrupoResponseDTO.Integrante(i + 1, in.idUsuario(), in.nome(), in.selos()));
        }

        List<Selo> todosSelos = seloDAO.listar();
        List<ResumoGrupoResponseDTO.SeloQuantidade> selos = new ArrayList<>();
        Set<Integer> tipos = new HashSet<>();
        for (Selo s : todosSelos) {
            int qtd = quantidadePorSelo.getOrDefault(s.getIdSelo(), 0);
            if (qtd > 0) {
                tipos.add(s.getIdSelo());
            }
            selos.add(new ResumoGrupoResponseDTO.SeloQuantidade(s.getIdSelo(), s.getNome(), qtd));
        }

        return new ResumoGrupoResponseDTO(grupo.getIdGrupo(), grupo.getNome(), totalSelos,
                tipos.size(), todosSelos.size(), selos, ordenados);
    }

    // ---------- Meu Impacto ----------

    public ResumoImpactoResponseDTO resumirImpacto(int idUsuario) {
        Usuario usuario = verificarUsuario(idUsuario);

        Carteira carteira = carteiraDAO.buscarPorIdUsuario(idUsuario);
        int pontos = carteira != null ? carteira.getQtdPontos() : 0;

        int posicao = 0;
        int idRanking = posicaoDAO.obterUltimoIdRankingUsuario();
        if (idRanking > 0) {
            for (PosicaoRanking p : posicaoDAO.listarPorRanking(idRanking)) {
                if (p.getUsuario() != null && p.getUsuario().getIdUsuario() == idUsuario) {
                    posicao = p.getPosicao();
                    break;
                }
            }
        }

        Streak streak = streakDAO.buscarPorIdUsuario(idUsuario);
        if (streak != null) {
            streak.encerrarSeInterrompida(LocalDate.now());
        }
        int meses = streak != null ? streak.getSequenciaAtual() : 0;

        // mesmo total usado pelo ranking (conclusões de missões contínuas); não é a pontuação
        int concluidas = usuarioMissaoDAO.contarConcluidasPorUsuario().getOrDefault(idUsuario, 0);

        return new ResumoImpactoResponseDTO(idUsuario, usuario.getNome(), pontos, posicao, meses, concluidas);
    }

    // ---------- auxiliares ----------

    private Usuario verificarUsuario(int idUsuario) {
        Usuario usuario = usuarioDAO.buscarPorId(idUsuario);
        if (usuario == null) {
            usuario = adminDAO.buscarPorId(idUsuario);
        }
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        return usuario;
    }

    private Map<Integer, Integer> progressoPorMissao(int idUsuario) {
        Map<Integer, Integer> mapa = new HashMap<>();
        for (UsuarioMissao um : usuarioMissaoDAO.listarPorUsuario(idUsuario)) {
            if (um.getMissao() != null) {
                mapa.put(um.getMissao().getIdMissao(), um.getProgressoAtual());
            }
        }
        return mapa;
    }

    private boolean concluida(int progresso, int meta) {
        return meta > 0 && progresso >= meta;
    }

    private int percentual(int parte, int total) {
        return total == 0 ? 0 : Math.round(100f * parte / total);
    }
}
package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.dao.*;
import br.com.fiap.greenlify.enums.StatusRankingEnum;
import br.com.fiap.greenlify.enums.TipoRankingEnum;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa um período de ranking mensal dentro da plataforma GreenLify, responsável por calcular
 * e listar a classificação de usuários e de grupos.
 *
 * <p>Individual: pontos das missões contínuas concluídas dentro do período (empate: mais missões
 * concluídas e, depois, cadastro mais antigo).
 * Grupal: total de Selos Verdes resgatados individualmente pelos integrantes atuais do grupo
 * (ver {@code UsuarioSeloDAO.contarSelosPorGrupo}; o tamanho do grupo não entra na conta).
 * Esses registros (Ranking/Posicao_Ranking) são do ciclo
 * mensal, controlado pelo administrador, e não se misturam com o ranking all time, que não tem
 * período, soma a pontuação elegível e vive no Redis (ver {@code RankingAllTimeService}).</p>
 */
public class Ranking {

    private int idRanking;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private TipoRankingEnum tipo;
    private StatusRankingEnum status;

    /** Calcula as posições conforme o tipo do ranking (individual: usuários; grupal: grupos). */
    public List<PosicaoRanking> calcularPosicoes() {
        if (tipo == null) {
            throw new IllegalStateException("O ranking não tem tipo definido.");
        }
        return switch (tipo) {
            case INDIVIDUAL -> calcularRankingUsuarios();
            case GRUPAL -> calcularRankingGrupos();
        };
    }

    /**
     * Estados válidos: {@code ABERTO -> FECHADO}. Só ranking aberto aceita atualização;
     * um ranking fechado não volta a ser aberto.
     */
    public boolean aceitaAtualizacao() {
        return status == StatusRankingEnum.ABERTO;
    }

    /** Se o período é válido (início e fim informados, início não depois do fim). */
    public boolean periodoValido() {
        return dataInicio != null && dataFim != null && !dataInicio.isAfter(dataFim);
    }

    /** Se o período já terminou em {@code hoje} (o ranking não é mais vigente). Ranking vigente não pode ser fechado. */
    public boolean terminou(LocalDate hoje) {
        return dataFim != null && hoje != null && dataFim.isBefore(hoje);
    }

    /** Se este ranking aberto já passou do fim do período em {@code hoje} e pode ser fechado pelo administrador. */
    public boolean deveFechar(LocalDate hoje) {
        return aceitaAtualizacao() && terminou(hoje);
    }

    /**
     * Fecha o ranking (única mudança de estado permitida).
     *
     * @return {@code true} se estava aberto e foi fechado; {@code false} se já estava fechado
     */
    public boolean fechar() {
        if (!aceitaAtualizacao()) {
            return false;
        }
        status = StatusRankingEnum.FECHADO;
        return true;
    }

    /**
     * Calcula o ranking de usuários do período (de {@code dataInicio} a
     * {@code dataFim}): ordena do maior para o menor total de pontos das
     * missões concluídas dentro do período e, em caso de empate, por mais
     * missões concluídas e pelo cadastro mais antigo (menor id de usuário);
     * a carteira não entra na classificação.
     *
     * @return lista de posições de ranking dos usuários, já ordenada e
     *         numerada, ou lista vazia caso não existam usuários cadastrados
     */
    public List<PosicaoRanking> calcularRankingUsuarios() {
        UsuarioComumDAO usuarioDAO = new UsuarioComumDAO();

        List<UsuarioComum> todosUsuarios = usuarioDAO.listar();
        List<PosicaoRanking> posicoes = new ArrayList<>();

        if (todosUsuarios == null || todosUsuarios.isEmpty()) {
            return posicoes;
        }

        // uma única consulta agrupada: missões contínuas concluídas por usuário dentro do período
        // (só o ciclo mensal; o total geral fica no ranking all time, que vive no Redis)
        Map<Integer, Integer> missoesNoPeriodo = new HashMap<>();
        Map<Integer, Integer> pontosNoPeriodo = new HashMap<>();
        if (periodoValido()) {
            UsuarioMissaoDAO usuarioMissaoDAO = new UsuarioMissaoDAO();
            missoesNoPeriodo = usuarioMissaoDAO.contarConcluidasPorUsuario(dataInicio, dataFim);
            pontosNoPeriodo = usuarioMissaoDAO.somarPontosElegiveisPorUsuario(dataInicio, dataFim);
        }

        for (UsuarioComum usuario : todosUsuarios) {
            PosicaoRanking pr = new PosicaoRanking();
            pr.setUsuario(usuario);
            int missoes = missoesNoPeriodo.getOrDefault(usuario.getIdUsuario(), 0);
            pr.setPontuacao(pontosNoPeriodo.getOrDefault(usuario.getIdUsuario(), 0));
            pr.setMissoesRealizadas(missoes);
            pr.setRanking(this);

            posicoes.add(pr);
        }

        posicoes.sort(new Comparator<PosicaoRanking>() {
            @Override
            public int compare(PosicaoRanking p1, PosicaoRanking p2) {
                int porPontos = Integer.compare(p2.getPontuacao(), p1.getPontuacao());
                if (porPontos != 0) {
                    return porPontos;
                }
                int porMissoes = Integer.compare(p2.getMissoesRealizadas(), p1.getMissoesRealizadas());
                if (porMissoes != 0) {
                    return porMissoes;
                }
                return Integer.compare(p1.getUsuario().getIdUsuario(), p2.getUsuario().getIdUsuario());
            }
        });

        for (int i = 0; i < posicoes.size(); i++) {
            posicoes.get(i).setPosicao(i + 1);
        }

        return posicoes;
    }

    /**
     * Exibe no console o ranking de usuários, considerando apenas as
     * posições que possuem um usuário associado.
     *
     * @param posicoes lista de posições de ranking a ser exibida
     */
    public void listarRankingUsuarios(List<PosicaoRanking> posicoes) {
        List<PosicaoRanking> posicoesUsuarios = new ArrayList<>();

        if (posicoes != null) {
            for (PosicaoRanking pr : posicoes) {
                if (pr.getUsuario() != null) {
                    posicoesUsuarios.add(pr);
                }
            }
        }

        if (!posicoesUsuarios.isEmpty()) {
            System.out.println("=== RANKING DE USUÁRIOS ===");
            for (PosicaoRanking pr : posicoesUsuarios) {
                System.out.println("\n" + pr.getPosicao() + "° lugar");
                System.out.println("Nome: " + pr.getUsuario().getNome());
                System.out.println("Pontos: " + pr.getPontuacao());
            }
        } else {
            System.out.println("Nenhum usuário encontrado no ranking.");
        }
    }

    /**
     * Calcula o ranking de grupos com base nos Selos Verdes resgatados
     * individualmente pelos integrantes de cada grupo (um selo conta como
     * uma unidade, independentemente da pontuação necessária para
     * obtê-lo), ordenando do maior para o menor total e atribuindo a
     * posição correspondente. Só contam resgates válidos, sem duplicar
     * (regras em {@code UsuarioSeloDAO.contarSelosPorGrupo}); o número de
     * integrantes e a pontuação de missões não entram na conta.
     *
     * @return lista de posições de ranking dos grupos, já ordenada e
     *         numerada, ou lista vazia caso não existam grupos cadastrados
     */
    public List<PosicaoRanking> calcularRankingGrupos() {
        GrupoDAO grupoDAO = new GrupoDAO();

        List<Grupo> todosGrupos = grupoDAO.listar();
        List<PosicaoRanking> posicoes = new ArrayList<>();

        if (todosGrupos == null || todosGrupos.isEmpty()) {
            return posicoes;
        }

        // uma única consulta agrupada: selos válidos resgatados pelos integrantes atuais de cada grupo
        Map<Integer, Integer> selosPorGrupo = new UsuarioSeloDAO().contarSelosPorGrupo();

        for (Grupo grupo : todosGrupos) {
            PosicaoRanking pr = new PosicaoRanking();
            pr.setGrupo(grupo);
            pr.setPontuacao(selosPorGrupo.getOrDefault(grupo.getIdGrupo(), 0));
            pr.setRanking(this);

            posicoes.add(pr);
        }

        // empate: grupo cadastrado primeiro (menor id), para a ordem ser sempre a mesma
        posicoes.sort(Comparator.comparingInt(PosicaoRanking::getPontuacao).reversed()
                .thenComparingInt(p -> p.getGrupo().getIdGrupo()));
        for (int i = 0; i < posicoes.size(); i++) {
            posicoes.get(i).setPosicao(i + 1);
        }

        return posicoes;
    }

    /**
     * Exibe no console o ranking de grupos referente ao período de ranking
     * mais recente, considerando apenas as posições que possuem um grupo
     * associado.
     *
     * @param listaRanking lista de posições de ranking a ser filtrada e exibida
     */
    public void listarRankingGrupos(List<PosicaoRanking> listaRanking) {
        PosicaoRankingDAO posicaoDAO = new PosicaoRankingDAO();
        int ultimoIdRankingGrupo = posicaoDAO.obterUltimoIdRankingGrupo();

        List<PosicaoRanking> posicoesGrupos = new ArrayList<>();

        if (listaRanking != null) {
            for (PosicaoRanking pr : listaRanking) {
                if (pr.getGrupo() != null) {
                    if (pr.getRanking() == null || pr.getRanking().getIdRanking() == 0
                            || pr.getRanking().getIdRanking() == ultimoIdRankingGrupo) {
                        posicoesGrupos.add(pr);
                    }
                }
            }
        }

        System.out.println("\n=== RANKING DE GRUPOS  ===");

        if (posicoesGrupos.isEmpty()) {
            System.out.println("Nenhum grupo encontrado para exibir no ranking.");
            return;
        }

        for (PosicaoRanking pr : posicoesGrupos) {
            System.out.println("\n" + pr.getPosicao() + "º Lugar - Grupo: "
                    + pr.getGrupo().getNome() + " | Selos Totais: " + pr.getPontuacao());
        }
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public int getIdRanking() {
        return idRanking;
    }

    public void setIdRanking(int idRanking) {
        this.idRanking = idRanking;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public TipoRankingEnum getTipo() {
        return tipo;
    }

    public void setTipo(TipoRankingEnum tipo) {
        this.tipo = tipo;
    }

    public StatusRankingEnum getStatus() {
        return status;
    }

    public void setStatus(StatusRankingEnum status) {
        this.status = status;
    }
}
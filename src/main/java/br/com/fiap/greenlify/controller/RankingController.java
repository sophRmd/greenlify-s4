package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dao.RankingRedisDAO;
import br.com.fiap.greenlify.dto.RankingRequestDTO;
import br.com.fiap.greenlify.dto.RankingResponseDTO;
import br.com.fiap.greenlify.service.RankingAllTimeService;
import br.com.fiap.greenlify.service.RankingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rankings mensais. O ciclo tem três etapas separadas, feitas pelo administrador (os endpoints não
 * exigem login): criar ({@code POST /rankings}, {@code POST /rankings/mes-atual}), atualizar as
 * posições ({@code POST /rankings/atualizar}) e fechar em definitivo com as recompensas
 * ({@code POST /rankings/{id}/fechar}). Uma etapa não dispara a outra.
 *
 * <p>Respostas de erro: 404 para ranking inexistente, 400 para validação e 500 para falha interna
 * (mensagem própria do serviço em {@link IllegalStateException}; qualquer outra falha devolve só uma
 * mensagem genérica, sem detalhes internos).</p>
 *
 * <p>CORS: a origem do frontend vem de {@code app.cors.allowed-origins} (variável
 * {@code APP_CORS_ALLOWED_ORIGINS}, uma única origem); sem ela, o padrão continua {@code *}.</p>
 */
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RestController
@RequestMapping("/rankings")
public class RankingController {
    private final RankingService service;
    private final RankingAllTimeService allTimeService;

    public RankingController(RankingService service, RankingAllTimeService allTimeService) {
        this.service = service;
        this.allTimeService = allTimeService;
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody RankingRequestDTO ranking) {
        try {
            service.cadastrar(ranking);
            return ResponseEntity.status(HttpStatus.CREATED).body("Ranking cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar o ranking");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao cadastrar o ranking");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao cadastrar o ranking");
        }
    }

    /**
     * Cria os rankings do mês atual (individual e de grupos) que ainda não existirem. Só cria:
     * não calcula posições nem fecha nada. Pode ser repetido sem duplicar.
     */
    @PostMapping("/mes-atual")
    public ResponseEntity<String> criarDoMes() {
        try {
            int criados = service.criarRankingsDoMes();
            return ResponseEntity.ok("Rankings do mês criados agora: " + criados + ".");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao criar os rankings do mês");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao criar os rankings do mês");
        }
    }

    /**
     * Recalcula as posições dos rankings ABERTOS (não cria nem fecha rankings) e corrige o ranking
     * all time (Redis) a partir do Oracle.
     */
    @PostMapping("/atualizar")
    public ResponseEntity<String> atualizar() {
        try {
            service.atualizarRankings();
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao atualizar os rankings");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao atualizar os rankings");
        }

        // O all time é projeção do Oracle: se a reconciliação não rodar, os rankings do mês já foram atualizados.
        String allTime;
        try {
            RankingRedisDAO.Reconciliacao r = allTimeService.reconciliar();
            allTime = r == null
                    ? "ranking all time não reconciliado (Redis indisponível)."
                    : "ranking all time reconciliado (corrigidos: " + r.corrigidos()
                    + ", removidos: " + r.removidos() + ").";
        } catch (RuntimeException e) {
            allTime = "ranking all time não reconciliado: " + e.getMessage();
        }
        return ResponseEntity.ok("Rankings atualizados com sucesso! " + allTime);
    }

    /**
     * Fecha em definitivo um ranking já encerrado (nunca um vigente) e, no individual, concede as
     * recompensas. Se algo falhar o ranking continua aberto; repetir não duplica prêmios, e em
     * ranking já fechado só reaplica os prêmios pendentes, sem recalcular as posições.
     */
    @PostMapping("/{id}/fechar")
    public ResponseEntity<String> fechar(
            @PathVariable int id) {
        try {
            RankingService.ResultadoFechamento r = service.fecharRanking(id);
            String situacao = r.fechadoAgora() ? "Ranking fechado!" : "Ranking já estava fechado; prêmios reprocessados.";
            return ResponseEntity.ok(situacao + " Recompensas concedidas: " + r.concedidas()
                    + ", já concedidas antes: " + r.jaConcedidas()
                    + ", não concedidas: " + r.naoConcedidas() + ".");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao fechar o ranking");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao fechar o ranking");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao fechar o ranking");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar o ranking");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao buscar o ranking");
        }
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<RankingResponseDTO> rankings = service.listar();
            return ResponseEntity.ok(rankings);
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao listar os rankings");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao listar os rankings");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> alterar(
            @PathVariable int id,
            @RequestBody RankingRequestDTO objeto) {
        try {
            service.alterar(objeto, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao alterar o ranking");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao alterar o ranking");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao alterar o ranking");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(
            @PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir o ranking");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao excluir o ranking");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao excluir o ranking");
        }
    }

    /** Recurso inexistente vira 404; qualquer outra falha de validação vira 400. */
    private ResponseEntity<String> erro(IllegalArgumentException e, String contexto) {
        String mensagem = e.getMessage();
        if (mensagem != null && mensagem.contains("não encontrad")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mensagem);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(contexto + ": " + mensagem);
    }

    /** Falha do banco ou de regra interna: a mensagem do serviço já é própria para o usuário. */
    private ResponseEntity<String> falha(IllegalStateException e, String contexto) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(contexto + ": " + e.getMessage());
    }

    /** Falha inesperada: detalhe só no log do servidor, resposta genérica. */
    private ResponseEntity<String> falhaInesperada(RuntimeException e, String contexto) {
        System.err.println(contexto + ": " + e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(contexto + ".");
    }
}
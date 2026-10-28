package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.PosicaoRankingGrupoResponseDTO;
import br.com.fiap.greenlify.dto.PosicaoRankingIndividualResponseDTO;
import br.com.fiap.greenlify.service.PosicaoRankingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta das posições dos rankings (só leitura). Respostas de erro: 404 para posição inexistente,
 * 400 para validação e 500 para falha interna. CORS: origem de {@code app.cors.allowed-origins}
 * (padrão {@code *}).
 */
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RestController
@RequestMapping("/posicoes-ranking")
public class PosicaoRankingController {
    private final PosicaoRankingService service;

    public PosicaoRankingController(PosicaoRankingService service) {
        this.service = service;
    }

    // ---------- Grupo ----------

    @GetMapping("/grupo")
    public ResponseEntity<?> listarGrupos() {
        try {
            List<PosicaoRankingGrupoResponseDTO> posicoes = service.listarGrupos();
            return ResponseEntity.ok(posicoes);
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao listar as posições dos grupos");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao listar as posições dos grupos");
        }
    }

    @GetMapping("/grupo/{id}")
    public ResponseEntity<?> buscarGrupoPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarGrupoPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar a posição do grupo");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao buscar a posição do grupo");
        }
    }

    // ---------- Individual ----------

    @GetMapping("/individual")
    public ResponseEntity<?> listarIndividuais() {
        try {
            List<PosicaoRankingIndividualResponseDTO> posicoes = service.listarIndividuais();
            return ResponseEntity.ok(posicoes);
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao listar as posições individuais");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao listar as posições individuais");
        }
    }

    @GetMapping("/individual/{id}")
    public ResponseEntity<?> buscarIndividualPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarIndividualPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar a posição individual");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao buscar a posição individual");
        }
    }

    /** Posição inexistente vira 404; qualquer outra falha de validação vira 400. */
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
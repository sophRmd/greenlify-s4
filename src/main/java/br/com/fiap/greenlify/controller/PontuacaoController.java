package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.PontuacaoRequestDTO;
import br.com.fiap.greenlify.dto.PontuacaoResponseDTO;
import br.com.fiap.greenlify.service.PontuacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tabela de pontuação das missões (impacto x dificuldade). Respostas de erro: 404 para pontuação
 * inexistente, 400 para validação e 500 para falha interna. CORS: origem de
 * {@code app.cors.allowed-origins} (padrão {@code *}).
 */
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RestController
@RequestMapping("/pontuacoes")
public class PontuacaoController {
    private final PontuacaoService service;

    public PontuacaoController(PontuacaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody PontuacaoRequestDTO pontuacao) {
        try {
            service.cadastrar(pontuacao);
            return ResponseEntity.status(HttpStatus.CREATED).body("Pontuação cadastrada com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar a pontuação");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao cadastrar a pontuação");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao cadastrar a pontuação");
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorImpactoEDificuldade(
            @RequestParam String impacto,
            @RequestParam String dificuldade) {
        try {
            return ResponseEntity.ok(service.buscarPorImpactoEDificuldade(impacto, dificuldade));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar a pontuação");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao buscar a pontuação");
        }
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<PontuacaoResponseDTO> pontuacoes = service.listar();
            return ResponseEntity.ok(pontuacoes);
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao listar as pontuações");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao listar as pontuações");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> alterar(@PathVariable int id,
                                          @RequestBody PontuacaoRequestDTO objeto) {
        try {
            service.alterar(objeto, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao alterar a pontuação");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao alterar a pontuação");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao alterar a pontuação");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir a pontuação");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao excluir a pontuação");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao excluir a pontuação");
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
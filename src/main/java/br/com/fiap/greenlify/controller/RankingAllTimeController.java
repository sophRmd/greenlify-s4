package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dao.RankingRedisDAO;
import br.com.fiap.greenlify.dto.RankingAllTimeResponseDTO;
import br.com.fiap.greenlify.service.RankingAllTimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ranking all time. Respostas de erro: 503 se o Redis estiver fora do ar na reconciliação, 500 para
 * falha interna. CORS: origem de {@code app.cors.allowed-origins} (padrão {@code *}).
 */
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RestController
@RequestMapping("/rankings/all-time")
public class RankingAllTimeController {
    private final RankingAllTimeService service;

    public RankingAllTimeController(RankingAllTimeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<RankingAllTimeResponseDTO> ranking = service.listar();
            return ResponseEntity.ok(ranking);
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao listar o ranking all time");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao listar o ranking all time");
        }
    }

    /**
     * Recalcula os pontos elegíveis no Oracle e corrige o Redis. Pode ser repetido sem efeito colateral.
     */
    @PostMapping("/reconciliar")
    public ResponseEntity<String> reconciliar() {
        try {
            RankingRedisDAO.Reconciliacao r = service.reconciliar();
            if (r == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Redis indisponível: o ranking all time continua sendo respondido pelo Oracle.");
            }
            return ResponseEntity.ok("Ranking all time reconciliado. Corrigidos: " + r.corrigidos()
                    + ", removidos: " + r.removidos() + ".");
        } catch (IllegalStateException e) {
            return falha(e, "Erro ao reconciliar o ranking all time");
        } catch (RuntimeException e) {
            return falhaInesperada(e, "Erro ao reconciliar o ranking all time");
        }
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
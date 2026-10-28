package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.PagamentoContaRequestDTO;
import br.com.fiap.greenlify.dto.PagamentoContaResponseDTO;
import br.com.fiap.greenlify.service.PagamentoContaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/pagamentos")
public class PagamentoContaController {
    private final PagamentoContaService service;

    public PagamentoContaController() {
        this.service = new PagamentoContaService();
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody PagamentoContaRequestDTO pagamento) {
        try {
            service.cadastrar(pagamento);
            return ResponseEntity.status(HttpStatus.CREATED).body("Pagamento cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar o pagamento");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar o pagamento: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar o pagamento");
        }
    }

    @GetMapping
    public ResponseEntity<List<PagamentoContaResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<PagamentoContaResponseDTO>> listarPorUsuario(@PathVariable int idUsuario) {
        return ResponseEntity.ok(service.listarPorUsuario(idUsuario));
    }

    /** Recurso inexistente vira 404; qualquer outra falha de validação vira 400. */
    private ResponseEntity<String> erro(IllegalArgumentException e, String contexto) {
        String mensagem = e.getMessage();
        if (mensagem != null && mensagem.contains("não encontrad")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mensagem);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(contexto + ": " + mensagem);
    }
}
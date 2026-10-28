package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.FingerprintRequestDTO;
import br.com.fiap.greenlify.dto.FingerprintResponseDTO;
import br.com.fiap.greenlify.service.FingerprintService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Fingerprints dos vídeos enviados. Os endpoints não exigem login. */
@CrossOrigin("*")
@RestController
@RequestMapping("/fingerprints")
public class FingerprintController {
    private final FingerprintService service;

    public FingerprintController() {
        this.service = new FingerprintService();
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody FingerprintRequestDTO fingerprint) {
        try {
            service.cadastrar(fingerprint);
            return ResponseEntity.status(HttpStatus.CREATED).body("Fingerprint cadastrada com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar a fingerprint");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar a fingerprint: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar a fingerprint");
        }
    }

    @GetMapping
    public ResponseEntity<List<FingerprintResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> alterar(@PathVariable int id,
                                          @RequestBody FingerprintRequestDTO objeto) {
        try {
            service.alterar(objeto, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao alterar a fingerprint");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao alterar a fingerprint: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir a fingerprint");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir a fingerprint: " + e.getMessage());
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
}
package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.FingerprintFrameRequestDTO;
import br.com.fiap.greenlify.dto.FingerprintFrameResponseDTO;
import br.com.fiap.greenlify.service.FingerprintFrameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Frames das fingerprints. Os endpoints não exigem login. */
@CrossOrigin("*")
@RestController
@RequestMapping("/fingerprint-frames")
public class FingerprintFrameController {
    private final FingerprintFrameService service;

    public FingerprintFrameController() {
        this.service = new FingerprintFrameService();
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody FingerprintFrameRequestDTO frame) {
        try {
            service.cadastrar(frame);
            return ResponseEntity.status(HttpStatus.CREATED).body("Frame cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar o frame");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar o frame: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar o frame");
        }
    }

    @GetMapping
    public ResponseEntity<List<FingerprintFrameResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/fingerprint/{idFingerprint}")
    public ResponseEntity<List<FingerprintFrameResponseDTO>> listarPorFingerprint(@PathVariable int idFingerprint) {
        return ResponseEntity.ok(service.listarPorFingerprint(idFingerprint));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> alterar(@PathVariable int id,
                                          @RequestBody FingerprintFrameRequestDTO objeto) {
        try {
            service.alterar(objeto, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao alterar o frame");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao alterar o frame: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir o frame");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir o frame: " + e.getMessage());
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
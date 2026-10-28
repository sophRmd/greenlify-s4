package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.ResgateRequestDTO;
import br.com.fiap.greenlify.dto.ResgateResponseDTO;
import br.com.fiap.greenlify.service.ResgateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/resgates")
public class ResgateController {
    private final ResgateService service;

    public ResgateController() {
        this.service = new ResgateService();
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody ResgateRequestDTO resgate) {
        try {
            service.cadastrar(resgate);
            return ResponseEntity.status(HttpStatus.CREATED).body("Resgate cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar o resgate");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar o resgate: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar o resgate");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> alterar(@PathVariable int id,
                                          @RequestBody ResgateRequestDTO objeto) {
        try {
            service.alterar(objeto, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao alterar o resgate");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao alterar o resgate: " + e.getMessage());
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
package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.UsuarioSeloRequestDTO;
import br.com.fiap.greenlify.dto.UsuarioSeloResponseDTO;
import br.com.fiap.greenlify.service.UsuarioSeloService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/usuarios-selos")
public class UsuarioSeloController {
    private final UsuarioSeloService service;

    public UsuarioSeloController() {
        this.service = new UsuarioSeloService();
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody UsuarioSeloRequestDTO usuarioSelo) {
        try {
            service.cadastrar(usuarioSelo);
            return ResponseEntity.status(HttpStatus.CREATED).body("Coleta de selo registrada com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao registrar a coleta de selo");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao registrar a coleta de selo: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar a coleta de selo");
        }
    }

    @GetMapping
    public ResponseEntity<List<UsuarioSeloResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<UsuarioSeloResponseDTO>> listarPorUsuario(@PathVariable int idUsuario) {
        return ResponseEntity.ok(service.listarPorUsuario(idUsuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> alterar(@PathVariable int id,
                                          @RequestBody UsuarioSeloRequestDTO objeto) {
        try {
            service.alterar(objeto, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao alterar a coleta de selo");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao alterar a coleta de selo: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir a coleta de selo");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir a coleta de selo: " + e.getMessage());
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
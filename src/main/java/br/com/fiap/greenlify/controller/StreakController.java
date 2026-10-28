package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.StreakResponseDTO;
import br.com.fiap.greenlify.service.StreakService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/streaks")
public class StreakController {
    private final StreakService service;

    public StreakController() {
        this.service = new StreakService();
    }

    @PostMapping("/usuario/{idUsuario}/iniciar")
    public ResponseEntity<?> buscarOuCriar(@PathVariable int idUsuario) {
        try {
            return ResponseEntity.ok(service.buscarOuCriar(idUsuario));
        } catch (IllegalArgumentException e) {
            // aqui toda recusa significa que o usuário/grupo da URL não existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao iniciar a sequência: " + e.getMessage());
        }
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<?> buscarPorIdUsuario(@PathVariable int idUsuario) {
        try {
            return ResponseEntity.ok(service.buscarPorIdUsuario(idUsuario));
        } catch (IllegalArgumentException e) {
            // aqui toda recusa significa que o usuário/grupo da URL não existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao buscar a sequência: " + e.getMessage());
        }
    }

}
package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.ResumoGrupoResponseDTO;
import br.com.fiap.greenlify.dto.ResumoImpactoResponseDTO;
import br.com.fiap.greenlify.dto.ResumoMissoesResponseDTO;
import br.com.fiap.greenlify.service.ResumoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/resumos")
public class ResumoController {
    private final ResumoService service;

    public ResumoController() {
        this.service = new ResumoService();
    }

    @GetMapping("/missoes/{idUsuario}")
    public ResponseEntity<?> resumirMissoes(@PathVariable int idUsuario) {
        try {
            return ResponseEntity.ok(service.resumirMissoes(idUsuario));
        } catch (IllegalArgumentException e) {
            // aqui toda recusa significa que o usuário/grupo da URL não existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao montar o resumo de missões: " + e.getMessage());
        }
    }

    @GetMapping("/grupos/{idGrupo}")
    public ResponseEntity<?> resumirGrupo(@PathVariable int idGrupo) {
        try {
            return ResponseEntity.ok(service.resumirGrupo(idGrupo));
        } catch (IllegalArgumentException e) {
            // aqui toda recusa significa que o usuário/grupo da URL não existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao montar o resumo do grupo: " + e.getMessage());
        }
    }

    @GetMapping("/impacto/{idUsuario}")
    public ResponseEntity<?> resumirImpacto(@PathVariable int idUsuario) {
        try {
            return ResponseEntity.ok(service.resumirImpacto(idUsuario));
        } catch (IllegalArgumentException e) {
            // aqui toda recusa significa que o usuário/grupo da URL não existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao montar o resumo de impacto: " + e.getMessage());
        }
    }
}
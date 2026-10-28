package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.PerfilSustentavelRequestDTO;
import br.com.fiap.greenlify.dto.PerfilSustentavelResponseDTO;
import br.com.fiap.greenlify.service.PerfilSustentavelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/perfis-sustentaveis")
public class PerfilSustentavelController {
    private final PerfilSustentavelService service;

    public PerfilSustentavelController() {
        this.service = new PerfilSustentavelService();
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody PerfilSustentavelRequestDTO perfil) {
        try {
            service.cadastrar(perfil);
            return ResponseEntity.status(HttpStatus.CREATED).body("Perfil sustentável cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar o perfil sustentável");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar o perfil sustentável: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar o perfil sustentável");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> alterar(@PathVariable int id,
                                          @RequestBody PerfilSustentavelRequestDTO objeto) {
        try {
            service.alterar(objeto, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao alterar o perfil sustentável");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao alterar o perfil sustentável: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir o perfil sustentável");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir o perfil sustentável: " + e.getMessage());
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
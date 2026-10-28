package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.CarteiraRequestDTO;
import br.com.fiap.greenlify.service.CarteiraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/carteira")
public class CarteiraController {

    private CarteiraService service = new CarteiraService();

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<?> buscarPorIdUsuario(@PathVariable int idUsuario) {
        try {
            return ResponseEntity.ok(service.buscarPorIdUsuario(idUsuario));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar a carteira");
        }
    }

    @PostMapping("/usuario/{idUsuario}")
    public ResponseEntity<String> cadastrar(
            @PathVariable int idUsuario,
            @RequestBody CarteiraRequestDTO dto) {
        try {
            service.cadastrar(idUsuario, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("Carteira cadastrada com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar a carteira");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar a carteira: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> atualizar(
            @PathVariable int id,
            @RequestBody CarteiraRequestDTO dto) {
        try {
            service.atualizar(id, dto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao atualizar a carteira");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao atualizar a carteira: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir a carteira");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir a carteira: " + e.getMessage());
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
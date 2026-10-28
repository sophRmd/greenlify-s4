package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.NivelContribuicaoRequestDTO;
import br.com.fiap.greenlify.dto.NivelContribuicaoResponseDTO;
import br.com.fiap.greenlify.service.NivelContribuicaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/niveis-contribuicao")
public class NivelContribuicaoController {
    private final NivelContribuicaoService service;

    public NivelContribuicaoController() {
        this.service = new NivelContribuicaoService();
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@RequestBody NivelContribuicaoRequestDTO nivel) {
        try {
            service.cadastrar(nivel);
            return ResponseEntity.status(HttpStatus.CREATED).body("Nível de contribuição cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao cadastrar o nível de contribuição");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar o nível de contribuição: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao buscar o nível de contribuição");
        }
    }

    @GetMapping
    public ResponseEntity<List<NivelContribuicaoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> alterar(@PathVariable int id,
                                          @RequestBody NivelContribuicaoRequestDTO objeto) {
        try {
            service.alterar(objeto, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao alterar o nível de contribuição");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao alterar o nível de contribuição: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir o nível de contribuição");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir o nível de contribuição: " + e.getMessage());
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
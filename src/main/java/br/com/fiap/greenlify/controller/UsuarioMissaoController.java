package br.com.fiap.greenlify.controller;

import br.com.fiap.greenlify.dto.UsuarioMissaoRequestDTO;
import br.com.fiap.greenlify.dto.UsuarioMissaoResponseDTO;
import br.com.fiap.greenlify.service.UsuarioMissaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/usuarios-missoes")
public class UsuarioMissaoController {
    private final UsuarioMissaoService service;

    public UsuarioMissaoController() {
        this.service = new UsuarioMissaoService();
    }

    /**
     * Inicia a missão para o usuário. Sem login, o usuário vem no parâmetro de consulta
     * {@code idUsuario} (ex.: {@code POST /usuarios-missoes?idUsuario=1}); a missão vem no corpo.
     */
    @PostMapping
    public ResponseEntity<String> cadastrar(
            @RequestBody UsuarioMissaoRequestDTO usuarioMissao,
            @RequestParam int idUsuario) {
        try {
            service.cadastrar(usuarioMissao, idUsuario);
            return ResponseEntity.status(HttpStatus.CREATED).body("Missão atribuída ao usuário com sucesso!");
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao atribuir a missão ao usuário");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao atribuir a missão ao usuário: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<UsuarioMissaoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<UsuarioMissaoResponseDTO>> listarPorUsuario(@PathVariable int idUsuario) {
        return ResponseEntity.ok(service.listarPorUsuario(idUsuario));
    }

    /**
     * Registra um passo de progresso na missão mensal. O cliente não informa o progresso: o
     * servidor avança um passo e decide quando a missão foi concluída. O dono do registro é
     * lido do próprio registro (não há login).
     */
    @PostMapping("/{id}/progresso")
    public ResponseEntity<?> registrarProgresso(@PathVariable int id) {
        try {
            int idUsuario = service.buscarPorId(id).usuario().idUsuario();
            return ResponseEntity.ok(service.registrarProgresso(id, idUsuario));
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao registrar o progresso");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao registrar o progresso: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable int id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return erro(e, "Erro ao excluir o registro");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir o registro: " + e.getMessage());
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
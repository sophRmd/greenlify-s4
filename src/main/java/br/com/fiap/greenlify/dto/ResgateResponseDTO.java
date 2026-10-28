package br.com.fiap.greenlify.dto;

import br.com.fiap.greenlify.models.Recompensa;
import java.time.LocalDate;

public record ResgateResponseDTO(
        int idResgate,
        LocalDate dataConcessao,
        UsuarioResumoDTO usuario,
        Recompensa recompensa
) {
}

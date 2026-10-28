package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.MissaoContinuaDAO;
import br.com.fiap.greenlify.dao.PontuacaoDAO;
import br.com.fiap.greenlify.dto.MissaoContinuaCompletaRequestDTO;
import br.com.fiap.greenlify.dto.MissaoContinuaCompletaResponseDTO;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;
import br.com.fiap.greenlify.mapper.MissaoContinuaCompletaMapper;
import br.com.fiap.greenlify.models.MissaoContinua;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class MissaoContinuaService {

    private final MissaoContinuaDAO missaoContinuaDAO;

    public MissaoContinuaService() {
        this.missaoContinuaDAO = new MissaoContinuaDAO();
    }

    public List<MissaoContinuaCompletaResponseDTO> listar() {
        return missaoContinuaDAO.listar()
                .stream()
                .map(MissaoContinuaCompletaMapper::toDto)
                .toList();
    }

    public MissaoContinuaCompletaResponseDTO buscarPorId(int id) {
        return MissaoContinuaCompletaMapper.toDto(buscarExistente(id));
    }

    public void cadastrar(MissaoContinuaCompletaRequestDTO dto) {
        validar(dto);

        MissaoContinua missao = MissaoContinuaCompletaMapper.dtoToEntity(dto);
        // O mapper não preenche o tipo, e o DAO grava missao.getTipoMissao().toString().
        missao.setTipoMissao(TipoMissaoEnum.MISSAO_CONTINUA);

        // MissaoContinua é usada como Missao e como MissaoContinua; a pontuação vem do DTO.
        // Se o banco falhar, o DAO desfaz tudo e lança IllegalStateException (não é mais engolido).
        missaoContinuaDAO.cadastrarMissao(missao, missao, missao.getPontuacao());
    }

    /**
     * Atualiza a missão. Os dados descritivos e a pontuação podem mudar a qualquer momento (a
     * pontuação só vale para conclusões futuras); o limite de realizações não pode mudar depois
     * que há usuários ou uploads na missão, para não alterar conclusões já registradas. Essa
     * regra é conferida pelo DAO, dentro da transação.
     *
     * @throws IllegalArgumentException se a missão não existe, o DTO é inválido ou o limite não pode mudar
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public void atualizar(int id, MissaoContinuaCompletaRequestDTO dto) {
        buscarExistente(id);
        validar(dto);

        // O DTO não carrega o id, então ele vem da URL.
        MissaoContinua missao = MissaoContinuaCompletaMapper.dtoToEntity(dto);
        missao.setIdMissao(id);
        missao.setTipoMissao(TipoMissaoEnum.MISSAO_CONTINUA);

        missaoContinuaDAO.alterar(missao, missao);
    }

    /**
     * Exclui a missão, desde que ninguém tenha começado ou enviado upload nela (conferido pelo DAO,
     * dentro da transação), para não apagar nem deixar órfão o histórico dos usuários.
     *
     * @throws IllegalArgumentException se a missão não existe ou já tem participações
     * @throws IllegalStateException    se o banco falhar (nada é removido)
     */
    public void excluir(int id) {
        MissaoContinua existente = buscarExistente(id);
        missaoContinuaDAO.excluir(existente);
    }

    // O DAO nunca devolve null: se o id não existe, retorna um objeto com os valores padrão.
    // Também confere o tipo, para não alterar/excluir outros tipos de missão por este endpoint.
    private MissaoContinua buscarExistente(int id) {
        MissaoContinua missao = missaoContinuaDAO.buscarPorId(id);
        if (missao == null
                || !Objects.equals(missao.getIdMissao(), id)
                || missao.getTipoMissao() != TipoMissaoEnum.MISSAO_CONTINUA)
            throw new IllegalArgumentException("Missão contínua não encontrada");
        return missao;
    }

    private void validar(MissaoContinuaCompletaRequestDTO dto) {
        if (dto.nome() == null || dto.nome().isBlank()) {
            System.out.println("ERRO: O nome da missão é obrigatório");
            throw new IllegalArgumentException("O nome da missão é obrigatório.");
        }
        if (dto.categoria() == null) {
            System.out.println("ERRO: A categoria é obrigatória");
            throw new IllegalArgumentException("A categoria é obrigatória.");
        }
        if (dto.tipoMetrica() == null) {
            System.out.println("ERRO: O tipo de métrica é obrigatório");
            throw new IllegalArgumentException("O tipo de métrica é obrigatório.");
        }
        if (dto.limiteRealizacoes() <= 0) {
            System.out.println("ERRO: O limite de realizações deve ser maior que zero.");
            throw new IllegalArgumentException("O limite de realizações deve ser maior que zero.");
        }
        if (dto.valorMetrica() <= 0) {
            System.out.println("ERRO: O valor da métrica deve ser maior que zero.");
            throw new IllegalArgumentException("O valor da métrica deve ser maior que zero.");
        }
        if (dto.idPontuacao() <= 0) {
            System.out.println("ERRO: A pontuação é obrigatória");
            throw new IllegalArgumentException("A pontuação é obrigatória.");
        }
        if (new PontuacaoDAO().buscarPorId(dto.idPontuacao()) == null) {
            System.out.println("ERRO: A pontuação informada não existe");
            throw new IllegalArgumentException("A pontuação informada não existe.");
        }
    }
}
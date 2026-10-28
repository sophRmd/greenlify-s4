package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dao.MissaoMensalDAO;
import br.com.fiap.greenlify.dto.MissaoMensalCompletaRequestDTO;
import br.com.fiap.greenlify.dto.MissaoMensalCompletaResponseDTO;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;
import br.com.fiap.greenlify.mapper.MissaoMensalCompletaMapper;
import br.com.fiap.greenlify.models.MissaoMensal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class MissaoMensalService {

    private final MissaoMensalDAO missaoMensalDAO;

    public MissaoMensalService() {
        this.missaoMensalDAO = new MissaoMensalDAO();
    }

    public List<MissaoMensalCompletaResponseDTO> listar() {
        return missaoMensalDAO.listar()
                .stream()
                .map(MissaoMensalCompletaMapper::toDto)
                .toList();
    }

    public MissaoMensalCompletaResponseDTO buscarPorId(int id) {
        return MissaoMensalCompletaMapper.toDto(buscarExistente(id));
    }

    public void cadastrar(MissaoMensalCompletaRequestDTO dto) {
        validar(dto);

        MissaoMensal missao = MissaoMensalCompletaMapper.dtoToEntity(dto);
        // O mapper não preenche o tipo, e o DAO grava missao.getTipoMissao().toString().
        missao.setTipoMissao(TipoMissaoEnum.MISSAO_MENSAL);

        // MissaoMensal é usada como Missao e como MissaoMensal.
        // Se o banco falhar, o DAO desfaz tudo e lança IllegalStateException (não é mais engolido).
        missaoMensalDAO.cadastrarMissao(missao, missao);
    }

    /**
     * Atualiza a missão. Os dados descritivos podem mudar a qualquer momento; a meta e a data de
     * início não podem mudar (e o fim só pode ser prorrogado) depois que usuários começaram a
     * missão, para não alterar progresso e conclusões já registrados. A regra é conferida pelo
     * DAO, dentro da transação.
     *
     * @throws IllegalArgumentException se a missão não existe, o DTO é inválido ou a regra não pode mudar
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public void atualizar(int id, MissaoMensalCompletaRequestDTO dto) {
        buscarExistente(id);
        validar(dto);

        // O DTO não carrega o id, então ele vem da URL.
        MissaoMensal missao = MissaoMensalCompletaMapper.dtoToEntity(dto);
        missao.setIdMissao(id);
        missao.setTipoMissao(TipoMissaoEnum.MISSAO_MENSAL);

        missaoMensalDAO.alterar(missao, missao);
    }

    /**
     * Exclui a missão, desde que nenhum usuário a tenha começado (conferido pelo DAO, dentro da
     * transação), para não apagar nem deixar órfão o histórico dos usuários.
     *
     * @throws IllegalArgumentException se a missão não existe ou já tem usuários
     * @throws IllegalStateException    se o banco falhar (nada é removido)
     */
    public void excluir(int id) {
        MissaoMensal existente = buscarExistente(id);
        missaoMensalDAO.excluir(existente);
    }

    // O DAO nunca devolve null: se o id não existe, retorna um objeto com os valores padrão.
    // Também confere o tipo, para não alterar/excluir outros tipos de missão por este endpoint.
    private MissaoMensal buscarExistente(int id) {
        MissaoMensal missao = missaoMensalDAO.buscarPorId(id);
        if (missao == null
                || !Objects.equals(missao.getIdMissao(), id)
                || missao.getTipoMissao() != TipoMissaoEnum.MISSAO_MENSAL)
            throw new IllegalArgumentException("Missão mensal não encontrada");
        return missao;
    }

    private void validar(MissaoMensalCompletaRequestDTO dto) {
        if (dto.nome() == null || dto.nome().isBlank()) {
            System.out.println("ERRO: O nome da missão é obrigatório");
            throw new IllegalArgumentException("O nome da missão é obrigatório.");
        }
        if (dto.categoria() == null) {
            System.out.println("ERRO: A categoria é obrigatória");
            throw new IllegalArgumentException("A categoria é obrigatória.");
        }
        if (dto.meta() <= 0) {
            System.out.println("ERRO: A meta deve ser maior que zero.");
            throw new IllegalArgumentException("A meta deve ser maior que zero.");
        }
        // O DAO também recusa o cadastro/alteração sem as datas.
        if (dto.dataInicio() == null || dto.dataFim() == null) {
            System.out.println("ERRO: As datas de início e fim são obrigatórias");
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias.");
        }
        if (dto.dataFim().isBefore(dto.dataInicio())) {
            System.out.println("ERRO: A data de fim não pode ser anterior à data de início");
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início.");
        }
    }
}
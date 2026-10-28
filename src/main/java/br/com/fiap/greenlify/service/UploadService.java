package br.com.fiap.greenlify.service;

import br.com.fiap.greenlify.dto.FingerprintFrameRequestDTO;
import br.com.fiap.greenlify.dao.FingerprintDAO;
import br.com.fiap.greenlify.dao.FingerprintFrameDAO;
import br.com.fiap.greenlify.dao.MissaoContinuaDAO;
import br.com.fiap.greenlify.dao.UploadDAO;
import br.com.fiap.greenlify.dao.UsuarioComumDAO;
import br.com.fiap.greenlify.dto.UploadRequestDTO;
import br.com.fiap.greenlify.dto.UploadResponseDTO;
import br.com.fiap.greenlify.enums.StatusContaEnum;
import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;
import br.com.fiap.greenlify.mapper.UploadMapper;
import br.com.fiap.greenlify.models.Fingerprint;
import br.com.fiap.greenlify.models.FingerprintFrame;
import br.com.fiap.greenlify.models.MissaoContinua;
import br.com.fiap.greenlify.models.Upload;
import br.com.fiap.greenlify.models.UsuarioComum;
import br.com.fiap.greenlify.util.FrameComparavel;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UploadService {
    private final UploadDAO uploadDAO;
    private final UsuarioComumDAO usuarioDAO;
    private final MissaoContinuaDAO missaoDAO;
    private final FingerprintDAO fingerprintDAO;
    private final FingerprintFrameDAO frameDAO;
    private final RankingAllTimeService rankingAllTimeService;
    private final RankingService rankingService;
    private final AiServiceClient aiClient;

    /** Confiança (%) a partir da qual o upload é reprovado por duplicidade. */
    private static final int CONFIANCA_REPROVAR = 80;
    /** Confiança (%) a partir da qual o upload fica parcialmente aprovado (suspeita). */
    private static final int CONFIANCA_PARCIAL = 50;
    private static final int MAXIMO_FRAMES = 120;
    private static final Pattern PHASH = Pattern.compile("^[0-9a-fA-F]{8,64}$");

    /**
     * Travas por usuário: envios do mesmo usuário rodam um de cada vez (o mesmo usuário cai
     * sempre na mesma trava; usuários diferentes só dividem trava por coincidência do resto
     * da divisão, o que apenas os coloca em fila). A trava no banco (FOR UPDATE, em
     * {@link UploadDAO#registrarEnvio}) cobre também outras instâncias da aplicação.
     */
    private static final Object[] TRAVAS_POR_USUARIO = criarTravas(64);

    private static Object[] criarTravas(int quantidade) {
        Object[] travas = new Object[quantidade];
        for (int i = 0; i < quantidade; i++) {
            travas[i] = new Object();
        }
        return travas;
    }

    public UploadService() {
        this.uploadDAO = new UploadDAO();
        this.usuarioDAO = new UsuarioComumDAO();
        this.missaoDAO = new MissaoContinuaDAO();
        this.fingerprintDAO = new FingerprintDAO();
        this.frameDAO = new FingerprintFrameDAO();
        this.rankingAllTimeService = new RankingAllTimeService();
        this.rankingService = new RankingService();
        this.aiClient = new AiServiceClient();
    }

    public UploadResponseDTO buscarPorId(int id) {
        Upload upload = uploadDAO.buscarPorId(id);
        if (upload == null) {
            throw new IllegalArgumentException("Upload não encontrado");
        }
        return UploadMapper.toDto(upload);
    }

    public List<UploadResponseDTO> listar() {
        return uploadDAO.listar()
                .stream()
                .map(UploadMapper::toDto)
                .toList();
    }

    /**
     * Registra o envio de uma comprovação: valida o arquivo (regras do model
     * {@link Upload}), compara os frames (pHash e embedding, gerados pela IA)
     * com os de todos os uploads já gravados, define status e confiança de
     * duplicidade e grava tudo em uma única transação: Upload, Fingerprint, Frames,
     * progresso na missão e, quando a conclusão é válida, o crédito dos pontos.
     *
     * <p>O progresso das missões contínuas é mensal: conta os uploads aprovados no mês
     * corrente, então o limite de realizações vale por mês e o usuário volta a pontuar
     * no mês seguinte. Missão concluída soma a pontuação elegível no ranking all time (Redis); os
     * rankings mensais só mudam por ação do administrador.</p>
     *
     * <p>Fluxo antigo (hash e frames calculados no navegador). O fluxo com a IA real é
     * {@link #cadastrarComVideo}.</p>
     *
     * @return o upload gravado, com o resultado da análise
     * @throws IllegalArgumentException se o envio for inválido, a missão não existir ou não for contínua,
     *                                  o vídeo (hash) já foi enviado antes ou o limite do mês já foi atingido
     * @throws IllegalStateException    se o banco falhar (nada é gravado)
     */
    public UploadResponseDTO cadastrar(UploadRequestDTO dto) {
        if (dto.idUsuario() <= 0) {
            System.out.println("ERRO: O usuário é obrigatório.");
            throw new IllegalArgumentException("O usuário é obrigatório.");
        }
        synchronized (TRAVAS_POR_USUARIO[dto.idUsuario() % TRAVAS_POR_USUARIO.length]) {
            return cadastrarSerializado(dto);
        }
    }

    private UploadResponseDTO cadastrarSerializado(UploadRequestDTO dto) {
        MissaoContinua missao = validar(dto);

        UsuarioComum usuario = usuarioDAO.buscarPorId(dto.idUsuario());
        Upload upload = new Upload();
        boolean arquivoValido = upload.verificaArquivo(
                dto.arquivo(), dto.tamanhoMB(), dto.idMissao(), usuario) != null;

        Fingerprint fingerprint = null;
        List<FingerprintFrame> frames = new ArrayList<>();
        if (arquivoValido) {
            analisarDuplicidade(upload, dto.frames());

            fingerprint = new Fingerprint();
            fingerprint.setHashVideo(dto.hashVideo().trim());
            fingerprint.setDataGeracao(LocalDate.now());
            for (FingerprintFrameRequestDTO f : dto.frames()) {
                FingerprintFrame frame = new FingerprintFrame();
                frame.setNumeroFrame(f.numeroFrame());
                frame.setTempoSegundo(f.tempoSegundo());
                frame.setpHash(f.pHash());
                frame.setEmbedding(f.embedding());
                frames.add(frame);
            }
        }

        // Tudo (upload, fingerprint, frames, progresso e crédito) na mesma transação.
        UploadDAO.ResultadoEnvio resultado = uploadDAO.registrarEnvio(upload, fingerprint, frames, missao);

        if (resultado.concluiu()) {
            // Conclusão já gravada no Oracle: soma no ranking all time os mesmos pontos creditados na
            // carteira (lidos pelo DAO da missão já travada na transação, regra única da pontuação).
            // Se o Redis falhar, a conclusão não é desfeita; a reconciliação corrige.
            LocalDate dia = upload.getDataEnvio() != null ? upload.getDataEnvio() : LocalDate.now();
            rankingAllTimeService.registrarMissaoConcluida(
                    usuario.getIdUsuario(), missao.getIdMissao(), dia, resultado.pontos());
        }

        atualizarPosicoesDoMes(upload);
        return UploadMapper.toDto(upload);
    }

    /**
     * Upload aprovado muda os pontos do mês: recalcula as posições dos rankings mensais abertos
     * (não cria nem fecha rankings). O upload já está gravado; se o recálculo falhar, só é
     * registrado e o próximo POST /rankings/atualizar corrige.
     */
    private void atualizarPosicoesDoMes(Upload upload) {
        if (upload.getStatus() != StatusUploadEnum.APROVADO) {
            return;
        }
        try {
            rankingService.atualizarRankings();
        } catch (RuntimeException e) {
            System.err.println("Não foi possível atualizar os rankings do mês: " + e.getMessage());
        }
    }

    /**
     * Fluxo com o vídeo real: o Python calcula o pHash e o Gemini confere se a missão foi cumprida.
     *
     * <ol>
     *   <li>valida usuário, missão, limite do mês e o arquivo;</li>
     *   <li>pede o pHash ao Python e compara com os já gravados (duplicidade);</li>
     *   <li>só se for inédito, pede o parecer ao Gemini;</li>
     *   <li>grava tudo na mesma transação de sempre ({@link UploadDAO#registrarEnvio}).</li>
     * </ol>
     *
     * <p>As chamadas lentas (Python/Gemini) ficam fora da trava por usuário, para que a espera
     * de um envio não prenda outros usuários. Dentro da trava só há consulta e gravação no banco,
     * e a duplicidade é conferida de novo ali, caso outro envio tenha entrado nesse meio-tempo.</p>
     *
     * @param nomeArquivo nome original do arquivo (usado só para validar a extensão)
     * @param video       caminho do arquivo temporário já salvo em disco
     */
    public UploadResponseDTO cadastrarComVideo(int idUsuario, int idMissao, String nomeArquivo,
                                               double tamanhoMB, Path video) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException("O usuário é obrigatório.");
        }
        MissaoContinua missao = validarContexto(idUsuario, idMissao, nomeArquivo, tamanhoMB);
        UsuarioComum usuario = usuarioDAO.buscarPorId(idUsuario);

        Upload upload = new Upload();
        boolean arquivoValido = upload.verificaArquivo(nomeArquivo, tamanhoMB, idMissao, usuario) != null;

        Fingerprint fingerprint = null;
        List<FingerprintFrame> frames = new ArrayList<>();
        FrameComparavel frameNovo = null;

        if (arquivoValido) {
            AiServiceClient.ResultadoPhash phash = aiClient.calcularPhash(video, nomeArquivo);
            frameNovo = FrameComparavel.de(0, phash.phash(), null);

            analisarDuplicidadeDeFrames(upload, List.of(frameNovo));
            if (upload.getStatus() == StatusUploadEnum.APROVADO) {
                aplicarParecerDaIA(upload, aiClient.analisarVideo(video, nomeArquivo, missao.getNome()));
            }

            String hashVideo = sha256(video);
            if (fingerprintDAO.buscarIdUploadPorHash(hashVideo) > 0) {
                throw new IllegalArgumentException("Este vídeo já foi enviado antes.");
            }

            fingerprint = new Fingerprint();
            fingerprint.setHashVideo(hashVideo);
            fingerprint.setDataGeracao(LocalDate.now());

            FingerprintFrame frame = new FingerprintFrame();
            frame.setNumeroFrame(phash.numeroFrame());
            frame.setTempoSegundo(phash.tempoSegundo());
            frame.setpHash(phash.phash());
            frame.setEmbedding(null);   // embedding ainda não implementado (coluna reservada)
            frames.add(frame);
        }

        UploadDAO.ResultadoEnvio resultado;
        synchronized (TRAVAS_POR_USUARIO[idUsuario % TRAVAS_POR_USUARIO.length]) {
            if (frameNovo != null && upload.getStatus() == StatusUploadEnum.APROVADO) {
                Upload reconferencia = new Upload();
                analisarDuplicidadeDeFrames(reconferencia, List.of(frameNovo));
                if (reconferencia.getStatus() == StatusUploadEnum.REPROVADO) {
                    upload.setStatus(reconferencia.getStatus());
                    upload.setAnalise(reconferencia.getAnalise());
                    upload.setConfiancaDuplicidade(reconferencia.getConfiancaDuplicidade());
                }
            }
            resultado = uploadDAO.registrarEnvio(upload, fingerprint, frames, missao);
        }

        if (resultado.concluiu()) {
            LocalDate dia = upload.getDataEnvio() != null ? upload.getDataEnvio() : LocalDate.now();
            rankingAllTimeService.registrarMissaoConcluida(
                    usuario.getIdUsuario(), missao.getIdMissao(), dia, resultado.pontos());
        }
        atualizarPosicoesDoMes(upload);
        return UploadMapper.toDto(upload);
    }

    /** Traduz o parecer do Gemini para status e texto do upload. */
    private void aplicarParecerDaIA(Upload upload, AiServiceClient.AnaliseIA analise) {
        String justificativa = analise.justificativa() == null ? "" : analise.justificativa().trim();
        if (analise.valido()) {
            upload.setStatus(StatusUploadEnum.APROVADO);
            upload.setAnalise(limitar("Aprovado pela IA: " + justificativa));
        } else {
            upload.setStatus(StatusUploadEnum.REPROVADO);
            upload.setAnalise(limitar("Reprovado pela IA: " + justificativa));
        }
    }

    /** Evita estourar a coluna ds_analise (ajuste se ela tiver tamanho menor que 255). */
    private static String limitar(String texto) {
        return texto.length() <= 250 ? texto : texto.substring(0, 247) + "...";
    }

    /** SHA-256 real do arquivo, lido em blocos (não carrega o vídeo inteiro na memória). */
    private static String sha256(Path arquivo) {
        try (InputStream in = Files.newInputStream(arquivo)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = in.read(buffer)) != -1) {
                md.update(buffer, 0, lidos);
            }
            return java.util.HexFormat.of().formatHex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Não foi possível calcular o hash do vídeo.", e);
        }
    }

    /**
     * Compara os frames novos com os de cada upload já gravado. A confiança de
     * duplicidade é o maior percentual de frames novos que têm um frame
     * semelhante (pHash ou embedding) em um mesmo upload anterior.
     */
    private void analisarDuplicidade(Upload upload, List<FingerprintFrameRequestDTO> framesNovos) {
        List<FrameComparavel> novos = new ArrayList<>();
        for (FingerprintFrameRequestDTO f : framesNovos) {
            novos.add(FrameComparavel.de(0, f.pHash(), f.embedding()));
        }
        analisarDuplicidadeDeFrames(upload, novos);
    }

    /** Mesma análise, a partir de frames já convertidos (usada também pelo fluxo com o vídeo real). */
    private void analisarDuplicidadeDeFrames(Upload upload, List<FrameComparavel> novos) {
        Map<Integer, List<FrameComparavel>> existentes = new HashMap<>();
        for (Fingerprint fp : fingerprintDAO.listar()) {
            if (fp.getUpload() == null) {
                continue;
            }
            int idUpload = fp.getUpload().getIdUpload();
            for (FingerprintFrame f : frameDAO.listarPorFingerprint(fp.getIdFingerprint())) {
                existentes.computeIfAbsent(idUpload, k -> new ArrayList<>())
                        .add(FrameComparavel.de(idUpload, f.getpHash(), f.getEmbedding()));
            }
        }

        int melhorConfianca = 0;
        int uploadMaisSimilar = 0;
        for (Map.Entry<Integer, List<FrameComparavel>> entrada : existentes.entrySet()) {
            int semelhantes = 0;
            for (FrameComparavel novo : novos) {
                for (FrameComparavel antigo : entrada.getValue()) {
                    if (novo.semelhanteA(antigo)) {
                        semelhantes++;
                        break;
                    }
                }
            }
            int confianca = (int) Math.round(100.0 * semelhantes / novos.size());
            if (confianca > melhorConfianca) {
                melhorConfianca = confianca;
                uploadMaisSimilar = entrada.getKey();
            }
        }

        upload.setConfiancaDuplicidade(melhorConfianca);
        upload.setDataAnalise(LocalDate.now());
        if (melhorConfianca >= CONFIANCA_REPROVAR) {
            upload.setStatus(StatusUploadEnum.REPROVADO);
            upload.setAnalise(String.format(
                    "Reprovado: vídeo %d%% semelhante ao upload #%d (possível duplicidade).",
                    melhorConfianca, uploadMaisSimilar));
        } else if (melhorConfianca >= CONFIANCA_PARCIAL) {
            upload.setStatus(StatusUploadEnum.PARCIALMENTE_APROVADO);
            upload.setAnalise(String.format(
                    "Parcialmente aprovado: vídeo %d%% semelhante ao upload #%d. Requer revisão.",
                    melhorConfianca, uploadMaisSimilar));
        } else {
            upload.setStatus(StatusUploadEnum.APROVADO);
            upload.setAnalise(String.format(
                    "Aprovado: nenhuma duplicidade relevante encontrada (maior semelhança: %d%%).",
                    melhorConfianca));
        }
    }

    /** Valida o envio e devolve a missão contínua (já carregada) para quem for gravá-lo. */
    private MissaoContinua validar(UploadRequestDTO dto) {
        MissaoContinua missao = validarContexto(dto.idUsuario(), dto.idMissao(), dto.arquivo(), dto.tamanhoMB());
        validarFingerprint(dto);
        return missao;
    }

    /** Valida usuário, missão, limite do mês e nome do arquivo (comum aos dois fluxos de envio). */
    private MissaoContinua validarContexto(int idUsuario, int idMissao, String arquivo, double tamanhoMB) {
        if (arquivo == null || arquivo.isBlank()) {
            System.out.println("ERRO: O arquivo é obrigatório.");
            throw new IllegalArgumentException("O arquivo é obrigatório.");
        }
        if (tamanhoMB <= 0) {
            throw new IllegalArgumentException("O tamanho do arquivo deve ser maior que 0.");
        }
        if (idUsuario <= 0) {
            System.out.println("ERRO: O usuário é obrigatório.");
            throw new IllegalArgumentException("O usuário é obrigatório.");
        }
        if (idMissao <= 0) {
            System.out.println("ERRO: A missão é obrigatória.");
            throw new IllegalArgumentException("A missão é obrigatória.");
        }
        UsuarioComum solicitante = usuarioDAO.buscarPorId(idUsuario);
        if (solicitante == null) {
            System.out.println("ERRO: Usuário informado não existe.");
            throw new IllegalArgumentException("O usuário informado não existe.");
        }
        if (solicitante.getStatusConta() != StatusContaEnum.ATIVA) {
            System.out.println("ERRO: A conta do usuário não está ativa.");
            throw new IllegalArgumentException("A conta do usuário não está ativa.");
        }
        // O DAO devolve null se o id não existe ou a missão não é contínua.
        MissaoContinua missao = missaoDAO.buscarPorId(idMissao);
        if (missao == null || missao.getTipoMissao() != TipoMissaoEnum.MISSAO_CONTINUA) {
            System.out.println("ERRO: Missão informada não existe.");
            throw new IllegalArgumentException("A missão informada não existe.");
        }
        // O limite de realizações vale por mês: conta os uploads aprovados do mês corrente.
        // (A conferência definitiva, sem corrida entre envios simultâneos, é refeita dentro da transação.)
        if (uploadDAO.contarAprovadosNoCiclo(idUsuario, idMissao, LocalDate.now())
                >= missao.getLimiteRealizacoes()) {
            System.out.println("ERRO: Missão já concluída pelo usuário neste mês.");
            throw new IllegalArgumentException("O usuário já concluiu essa missão neste mês.");
        }

        return missao;
    }

    /** Valida hash e frames da requisição (fluxo antigo, com fingerprint calculada no navegador). */
    private void validarFingerprint(UploadRequestDTO dto) {
        if (dto.hashVideo() == null || dto.hashVideo().isBlank()) {
            throw new IllegalArgumentException("O hash do vídeo é obrigatório.");
        }
        // Mesma regra do FingerprintService: o mesmo hash não pode pertencer a outro upload.
        if (fingerprintDAO.buscarIdUploadPorHash(dto.hashVideo()) > 0) {
            System.out.println("ERRO: Hash já registrado em outro upload.");
            throw new IllegalArgumentException("Este vídeo já foi enviado antes.");
        }

        if (dto.frames() == null || dto.frames().isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um frame analisado.");
        }
        if (dto.frames().size() > MAXIMO_FRAMES) {
            throw new IllegalArgumentException("Limite de " + MAXIMO_FRAMES + " frames por upload.");
        }
        Set<Integer> numeros = new HashSet<>();
        for (FingerprintFrameRequestDTO frame : dto.frames()) {
            if (frame == null) {
                throw new IllegalArgumentException("Frame inválido.");
            }
            if (frame.numeroFrame() < 0) {
                throw new IllegalArgumentException("O número do frame não pode ser negativo.");
            }
            if (frame.tempoSegundo() < 0) {
                throw new IllegalArgumentException("O tempo em segundos não pode ser negativo.");
            }
            if (!numeros.add(frame.numeroFrame())) {
                throw new IllegalArgumentException("Número de frame repetido: " + frame.numeroFrame() + ".");
            }
            if (frame.pHash() == null || frame.pHash().isBlank()) {
                throw new IllegalArgumentException("O pHash é obrigatório.");
            }
            if (!PHASH.matcher(frame.pHash().trim()).matches()) {
                throw new IllegalArgumentException("O pHash deve ser hexadecimal (8 a 64 caracteres).");
            }
            if (frame.embedding() == null || frame.embedding().isBlank()) {
                throw new IllegalArgumentException("O embedding é obrigatório.");
            }
        }
    }
}
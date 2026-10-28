package br.com.fiap.greenlify.service;

import tools.jackson.databind.JsonNode;          // Spring Boot 4 usa Jackson 3. Se não compilar,
import tools.jackson.databind.json.JsonMapper;   // troque por com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente do microsserviço Python (pHash + Gemini), hospedado à parte.
 *
 * <p>Segue o mesmo estilo do projeto (sem injeção): lê a configuração das variáveis de
 * ambiente {@code AI_SERVICE_URL} e {@code AI_SERVICE_KEY}, como o ConnectionFactory faz com o banco.
 * O vídeo é enviado direto do disco (streaming), sem carregar até 100 MB na memória.</p>
 */
public class AiServiceClient {

    /** pHash do quadro analisado, com a posição dele no vídeo. */
    public record ResultadoPhash(String phash, int numeroFrame, int tempoSegundo) {
    }

    /** Parecer do Gemini sobre o vídeo. */
    public record AnaliseIA(boolean valido, String justificativa) {
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Cobre o cold start do Render (30–60 s) + o cálculo do pHash. */
    private static final Duration TIMEOUT_PHASH = Duration.ofSeconds(120);
    /** Cobre upload do vídeo ao Gemini + processamento + resposta. */
    private static final Duration TIMEOUT_ANALISE = Duration.ofSeconds(240);

    private final String baseUrl;
    private final String chave;
    private final HttpClient http;

    public AiServiceClient() {
        String url = System.getenv().getOrDefault("AI_SERVICE_URL", "http://localhost:8000");
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.chave = System.getenv().getOrDefault("AI_SERVICE_KEY", "");
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    /** Pede ao Python o pHash do quadro central do vídeo. */
    public ResultadoPhash calcularPhash(Path video, String nomeArquivo) {
        String corpo = postarVideo("/phash", video, nomeArquivo, Map.of(), TIMEOUT_PHASH);
        JsonNode json = JSON.readTree(corpo);
        String phash = json.path("phash").asText("");
        if (phash.isBlank()) {
            throw new IllegalStateException("O serviço de IA não devolveu o pHash do vídeo.");
        }
        return new ResultadoPhash(phash, json.path("frame").asInt(0), json.path("tempo_segundo").asInt(0));
    }

    /** Pede ao Python (Gemini) que confira se o vídeo cumpre a missão. */
    public AnaliseIA analisarVideo(Path video, String nomeArquivo, String tituloMissao) {
        String corpo = postarVideo("/analisar", video, nomeArquivo, Map.of("missao", tituloMissao), TIMEOUT_ANALISE);
        JsonNode json = JSON.readTree(corpo);
        if (!json.has("valido")) {
            throw new IllegalStateException("Resposta inesperada do serviço de IA.");
        }
        return new AnaliseIA(json.path("valido").asBoolean(false), json.path("justificativa").asText(""));
    }

    // ------------------------------------------------------------------

    private String postarVideo(String rota, Path video, String nomeArquivo,
                               Map<String, String> campos, Duration timeout) {
        String boundary = "----greenlify" + UUID.randomUUID();
        String nomeSeguro = nomeArquivo.replaceAll("[^A-Za-z0-9._-]", "_");

        List<BodyPublisher> partes = new ArrayList<>();
        for (Map.Entry<String, String> campo : new LinkedHashMap<>(campos).entrySet()) {
            partes.add(texto("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + campo.getKey() + "\"\r\n\r\n"
                    + campo.getValue() + "\r\n"));
        }
        partes.add(texto("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"video\"; filename=\"" + nomeSeguro + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n"));
        try {
            partes.add(BodyPublishers.ofFile(video));   // streaming direto do disco
        } catch (java.io.FileNotFoundException e) {
            throw new IllegalStateException("Arquivo temporário do vídeo não encontrado.", e);
        }
        partes.add(texto("\r\n--" + boundary + "--\r\n"));

        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(baseUrl + rota))
                .timeout(timeout)
                .header("X-API-KEY", chave)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.concat(partes.toArray(new BodyPublisher[0])))
                .build();

        try {
            HttpResponse<String> resposta = http.send(requisicao,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = resposta.statusCode();
            if (status == 401) {
                throw new IllegalStateException("Chave do serviço de IA (AI_SERVICE_KEY) inválida.");
            }
            if (status != 200) {
                throw new IllegalStateException("Serviço de IA respondeu HTTP " + status + ": " + resposta.body());
            }
            return resposta.body();
        } catch (IOException e) {
            throw new IllegalStateException("Serviço de IA indisponível ou demorou demais para responder.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chamada ao serviço de IA interrompida.", e);
        }
    }

    private static BodyPublisher texto(String conteudo) {
        return BodyPublishers.ofString(conteudo, StandardCharsets.UTF_8);
    }
}

package br.com.fiap.greenlify.dao;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sorted Set {@value #CHAVE} do ranking all time: membro = id do usuário, score = pontos elegíveis
 * (missões contínuas concluídas, pela regra de {@code Pontuacao.pontosElegiveis}). É uma projeção
 * do Oracle, que continua sendo a fonte da verdade: nenhum método lança exceção quando o Redis
 * está fora do ar ({@code listar} e {@code reconciliar} devolvem {@code null}).
 *
 * <p>Usa REDIS_HOST/REDIS_PORT (mesmas do application.properties) e, se o Redis for hospedado,
 * REDIS_USERNAME, REDIS_PASSWORD e REDIS_SSL (true para conexão com TLS).</p>
 */
public class RankingRedisDAO {

    public static final String CHAVE = "ranking:all-time";

    /** Eventos já somados (um Set por mês, com validade): impede somar duas vezes o mesmo evento. */
    public static final String CHAVE_EVENTOS = "ranking:all-time:eventos";

    private static final long VALIDADE_EVENTOS_SEGUNDOS = 62L * 24 * 3600;

    /**
     * Soma os pontos só se o evento ainda não foi registrado. As duas operações (SADD do evento e
     * ZINCRBY) rodam juntas no Redis, então uma tentativa repetida nunca soma duas vezes.
     */
    private static final DefaultRedisScript<Long> SCRIPT_INCREMENTAR = new DefaultRedisScript<>(
            "if redis.call('SADD', KEYS[2], ARGV[1]) == 1 then "
                    + "redis.call('EXPIRE', KEYS[2], ARGV[3]) "
                    + "redis.call('ZINCRBY', KEYS[1], ARGV[2], ARGV[4]) "
                    + "return 1 end "
                    + "return 0", Long.class);

    private static volatile StringRedisTemplate template;

    /** Resultado de {@link #incrementar}. */
    public enum Resultado {
        APLICADO, JA_PROCESSADO, SEM_PONTOS, REDIS_INDISPONIVEL
    }

    /** Resumo de {@link #reconciliar}: quantos membros foram corrigidos e quantos removidos. */
    public record Reconciliacao(int corrigidos, int removidos) {
    }

    /**
     * Soma os pontos elegíveis ao usuário (ZINCRBY), uma única vez por evento.
     *
     * @param idEvento identifica a conclusão (ex.: usuário + missão + mês); repetir o mesmo id não soma de novo
     * @param dia      dia da conclusão (define o mês do conjunto de eventos)
     */
    public Resultado incrementar(int idUsuario, String idEvento, LocalDate dia, int pontos) {
        if (pontos <= 0) {
            return Resultado.SEM_PONTOS;
        }
        try {
            YearMonth mes = dia != null ? YearMonth.from(dia) : YearMonth.now();
            Long aplicado = obterTemplate().execute(SCRIPT_INCREMENTAR,
                    List.of(CHAVE, CHAVE_EVENTOS + ":" + mes),
                    idEvento, String.valueOf(pontos), String.valueOf(VALIDADE_EVENTOS_SEGUNDOS),
                    String.valueOf(idUsuario));
            return aplicado != null && aplicado == 1L ? Resultado.APLICADO : Resultado.JA_PROCESSADO;
        } catch (RuntimeException e) {
            System.err.println("Redis indisponível (incrementar): " + causa(e));
            return Resultado.REDIS_INDISPONIVEL;
        }
    }

    /** ZREVRANGE 0 -1 WITHSCORES: idUsuario -> pontos, ou {@code null} se o Redis falhar. */
    public Map<Integer, Integer> listar() {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuplas =
                    obterTemplate().opsForZSet().reverseRangeWithScores(CHAVE, 0, -1);
            Map<Integer, Integer> mapa = new HashMap<>();
            if (tuplas != null) {
                for (ZSetOperations.TypedTuple<String> t : tuplas) {
                    if (t.getValue() != null && t.getScore() != null) {
                        mapa.put(Integer.parseInt(t.getValue()), (int) Math.round(t.getScore()));
                    }
                }
            }
            return mapa;
        } catch (RuntimeException e) {
            System.err.println("Redis indisponível (listar): " + causa(e));
            return null;
        }
    }

    /**
     * Corrige o Sorted Set para ficar igual aos totais do Oracle, membro a membro: grava o score
     * dos que diferem ou faltam e remove os que não têm pontos no Oracle. Não apaga a chave nem
     * a repovoa, então quem consulta durante a reconciliação nunca vê o ranking vazio.
     *
     * @param oracle idUsuario -> pontos elegíveis (só usuários com pontos), vindos do Oracle
     * @return o resumo, ou {@code null} se o Redis estiver fora do ar
     */
    public Reconciliacao reconciliar(Map<Integer, Integer> oracle) {
        try {
            Map<Integer, Integer> atual = listar();
            if (atual == null) {
                return null;
            }
            ZSetOperations<String, String> zset = obterTemplate().opsForZSet();
            int corrigidos = 0;
            int removidos = 0;

            for (Map.Entry<Integer, Integer> e : oracle.entrySet()) {
                if (e.getValue() <= 0) {
                    continue;
                }
                if (!e.getValue().equals(atual.get(e.getKey()))) {
                    zset.add(CHAVE, String.valueOf(e.getKey()), e.getValue());
                    corrigidos++;
                }
            }
            for (Integer idUsuario : atual.keySet()) {
                Integer esperado = oracle.get(idUsuario);
                if (esperado == null || esperado <= 0) {
                    zset.remove(CHAVE, String.valueOf(idUsuario));
                    removidos++;
                }
            }
            return new Reconciliacao(corrigidos, removidos);
        } catch (RuntimeException e) {
            System.err.println("Redis indisponível (reconciliar): " + causa(e));
            return null;
        }
    }

    private static StringRedisTemplate obterTemplate() {
        StringRedisTemplate t = template;
        if (t == null) {
            synchronized (RankingRedisDAO.class) {
                t = template;
                if (t == null) {
                    RedisStandaloneConfiguration servidor = new RedisStandaloneConfiguration(
                            System.getenv().getOrDefault("REDIS_HOST", "localhost"),
                            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379").trim()));
                    // Redis hospedado (ex.: Redis Cloud) exige usuário/senha; local e Docker não precisam.
                    String usuario = System.getenv().getOrDefault("REDIS_USERNAME", "").trim();
                    String senha = System.getenv().getOrDefault("REDIS_PASSWORD", "").trim();
                    if (!usuario.isEmpty()) {
                        servidor.setUsername(usuario);
                    }
                    if (!senha.isEmpty()) {
                        servidor.setPassword(senha);
                    }
                    boolean ssl = "true".equalsIgnoreCase(System.getenv().getOrDefault("REDIS_SSL", "false").trim());
                    LettuceClientConfiguration cliente = ssl
                            ? LettuceClientConfiguration.builder()
                            .commandTimeout(Duration.ofSeconds(2)).useSsl().and().build()
                            : LettuceClientConfiguration.builder()
                            .commandTimeout(Duration.ofSeconds(2)).build();
                    System.out.println("Redis: conectando em " + servidor.getHostName() + ":" + servidor.getPort()
                            + " (usuário informado: " + !usuario.isEmpty() + ", senha informada: " + !senha.isEmpty()
                            + ", ssl: " + ssl + ")");
                    LettuceConnectionFactory fabrica = new LettuceConnectionFactory(servidor, cliente);
                    fabrica.afterPropertiesSet();
                    t = new StringRedisTemplate(fabrica);
                    t.afterPropertiesSet();
                    template = t;
                }
            }
        }
        return t;
    }

    /** Causa real da falha (a mensagem do Spring é genérica: "Unable to connect to Redis"). */
    private static String causa(Throwable e) {
        Throwable raiz = e;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        return e.getMessage() + " | causa: " + raiz.getClass().getSimpleName() + ": " + raiz.getMessage();
    }
}
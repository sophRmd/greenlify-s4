package br.com.fiap.greenlify.models;

import br.com.fiap.greenlify.enums.DificuldadeEnum;
import br.com.fiap.greenlify.enums.ImpactoEnum;
import br.com.fiap.greenlify.enums.StatusUploadEnum;
import br.com.fiap.greenlify.enums.TipoMissaoEnum;

/**
 * Representa a pontuação atribuída a uma missão, calculada a partir da
 * combinação entre o impacto ambiental e a dificuldade de realização.
 *
 * <p>{@code valorBase} é a configuração gravada (coluna {@code nr_pontuacao}) e é o único valor
 * usado para creditar pontos: a missão aponta para uma pontuação e quem credita lê o valor dela.
 * A tabela de {@link #valorPadrao} gera o valor no cadastro da pontuação, e
 * {@link #correspondeAoCalculo()} confere se o valor gravado ainda bate com impacto e dificuldade.</p>
 *
 * <p>Este é o lugar único da regra de pontuação elegível ({@link #pontosElegiveis}): quem credita
 * pontos ou atualiza o ranking deve perguntar aqui, e não repetir a regra.</p>
 */
public class Pontuacao {

    private int idPontuacao;
    private ImpactoEnum impacto;
    private DificuldadeEnum dificuldade;
    private int valorBase;

    /**
     * Cria uma pontuação sem dados definidos.
     */
    public Pontuacao() {
    }

    /**
     * Tabela de pontos por combinação entre impacto e dificuldade da missão.
     *
     * @return o valor base da combinação, ou {@code 0} caso algum dos parâmetros seja nulo
     */
    public static int valorPadrao(ImpactoEnum impacto, DificuldadeEnum dificuldade) {
        if (impacto == null || dificuldade == null) {
            return 0;
        }

        return switch (impacto) {
            case BAIXO -> switch (dificuldade) {
                case FACIL -> 20;
                case MEDIO -> 40;
                case DIFICIL -> 60;
            };
            case MEDIO -> switch (dificuldade) {
                case FACIL -> 40;
                case MEDIO -> 60;
                case DIFICIL -> 80;
            };
            case ALTO -> switch (dificuldade) {
                case FACIL -> 60;
                case MEDIO -> 80;
                case DIFICIL -> 100;
            };
        };
    }

    /**
     * Calcula o valor base de pontos com base na combinação entre impacto e
     * dificuldade da missão (mesma tabela de {@link #valorPadrao}).
     *
     * @param impacto     nível de impacto ambiental da missão
     * @param dificuldade nível de dificuldade da missão
     * @return o valor base de pontos correspondente à combinação informada,
     *         ou {@code 0} caso algum dos parâmetros seja nulo
     */
    public int calculaPontuacao(ImpactoEnum impacto, DificuldadeEnum dificuldade) {
        return valorPadrao(impacto, dificuldade);
    }

    /**
     * Confere se o valor gravado ({@link #getValorBase()}) corresponde ao impacto e à dificuldade
     * desta pontuação. É falso quando falta impacto ou dificuldade, ou quando o valor foi mexido
     * no banco sem passar pela tabela.
     */
    public boolean correspondeAoCalculo() {
        return impacto != null && dificuldade != null && valorBase == valorPadrao(impacto, dificuldade);
    }

    /**
     * Regra única da pontuação elegível: os pontos configurados só são devidos quando a missão é
     * contínua, o envio que está sendo registrado é válido (upload APROVADO) e é ele que conclui
     * a missão. Missão mensal nunca soma pontos (ela vale para streak e conclusão), e progresso
     * sem conclusão, upload reprovado ou em análise também valem 0.
     *
     * @param tipoMissao    tipo da missão concluída
     * @param statusUpload  status do upload que está sendo registrado (null se não há upload)
     * @param missaoConcluida se este evento concluiu a missão no ciclo
     * @return os pontos devidos pelo evento
     */
    public int pontosElegiveis(TipoMissaoEnum tipoMissao, StatusUploadEnum statusUpload, boolean missaoConcluida) {
        if (tipoMissao == TipoMissaoEnum.MISSAO_CONTINUA
                && MissaoContinua.realizacaoValida(statusUpload)
                && missaoConcluida) {
            return valorBase;
        }
        return 0;
    }

    public int getIdPontuacao() {
        return idPontuacao;
    }

    public void setIdPontuacao(int idPontuacao) {
        this.idPontuacao = idPontuacao;
    }

    public ImpactoEnum getImpacto() {
        return impacto;
    }

    public void setImpacto(ImpactoEnum impacto) {
        this.impacto = impacto;
        if (this.dificuldade != null) {
            this.valorBase = valorPadrao(this.impacto, this.dificuldade);
        }
    }

    public DificuldadeEnum getDificuldade() {
        return dificuldade;
    }

    public void setDificuldade(DificuldadeEnum dificuldade) {
        this.dificuldade = dificuldade;
        if (this.impacto != null) {
            this.valorBase = valorPadrao(this.impacto, this.dificuldade);
        }
    }

    public int getValorBase() {
        return valorBase;
    }

    public void setValorBase(int valorBase) {
        this.valorBase = Math.max(0, valorBase);   // pontuação nunca é negativa
    }

    @Override
    public String toString() {
        return "ID Pontuação: " + idPontuacao +
                "\nImpacto: " + impacto +
                "\nDificuldade: " + dificuldade +
                "\nPontos: " + valorBase;
    }
}
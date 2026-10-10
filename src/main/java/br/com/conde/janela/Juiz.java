package br.com.conde.janela;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

import br.com.conde.janela.Minimos.Agregador;

/**
 * O JUIZ, que sao DOIS juizes de naturezas diferentes.
 *
 * <p><b>1. A VARREDURA INGENUA, para o minimo.</b> Olhar a janela inteira a cada passo
 * custa n vezes w e nao tem como estar errado: nao guarda estado entre passos, nao
 * descarta nada e nao depende de o fluxo ter forma alguma. No minimo nao ha
 * arredondamento, entao a comparacao e de igualdade bit por bit.
 *
 * <p><b>2. A SOMA EXATA EM BIGDECIMAL, para a soma.</b> Aqui a varredura ingenua NAO
 * serve de juiz, e essa e a parte interessante: ela tambem e ponto flutuante e tambem
 * arredonda, so que menos.
 *
 * <p>Todo double e exatamente representavel em BigDecimal, entao somar os w valores em
 * BigDecimal da a soma EXATA daqueles bits, sem arredondamento nenhum. E o unico juiz
 * desta serie que nao e uma implementacao concorrente: e a resposta.
 */
public final class Juiz {

    private Juiz() {
    }

    /** O MINIMO EXATO, pela varredura, sem contar operacao nenhuma. */
    public static double[] minimoExato(double[] fluxo, int largura) {
        return Minimos.varredura(fluxo, largura, new Contador());
    }

    /**
     * A SOMA EXATA de cada janela, em BigDecimal.
     *
     * <p>Custa caro, e so por isso as tabelas da soma usam fluxos pequenos. O que ela da
     * em troca e um numero contra o qual nao ha recurso: ela nao arredonda.
     */
    public static BigDecimal[] somaExata(double[] fluxo, int largura) {
        BigDecimal[] saida = new BigDecimal[fluxo.length - largura + 1];

        for (int i = 0; i < saida.length; i++) {
            BigDecimal soma = BigDecimal.ZERO;

            for (int j = i; j < i + largura; j++) soma = soma.add(new BigDecimal(fluxo[j]));

            saida[i] = soma;
        }

        return saida;
    }

    /** O maior desvio ABSOLUTO entre o que o agregador deu e a resposta exata. */
    public static double maiorDesvio(double[] obtido, BigDecimal[] exato) {
        if (obtido.length != exato.length) return Double.POSITIVE_INFINITY;

        double maior = 0;

        for (int i = 0; i < obtido.length; i++)
            maior = Math.max(maior, new BigDecimal(obtido[i]).subtract(exato[i]).abs().doubleValue());

        return maior;
    }

    /** O maior desvio RELATIVO, que e o que vale quando os valores sao grandes. */
    public static double maiorDesvioRelativo(double[] obtido, BigDecimal[] exato) {
        if (obtido.length != exato.length) return Double.POSITIVE_INFINITY;

        double maior = 0;

        for (int i = 0; i < obtido.length; i++) {
            if (exato[i].signum() == 0) continue;

            double relativo = new BigDecimal(obtido[i]).subtract(exato[i]).abs()
                    .divide(exato[i].abs(), MathContext.DECIMAL64).doubleValue();

            maior = Math.max(maior, relativo);
        }

        return maior;
    }

    /** O desvio absoluto na ULTIMA janela, para medir se o erro ACUMULA ou nao. */
    public static double desvioNoFim(double[] obtido, BigDecimal[] exato) {
        if (obtido.length == 0 || obtido.length != exato.length) return Double.POSITIVE_INFINITY;

        int ultima = obtido.length - 1;

        return new BigDecimal(obtido[ultima]).subtract(exato[ultima]).abs().doubleValue();
    }

    /** Dois vetores de minimo sao iguais bit por bit? No minimo nao ha arredondamento. */
    public static boolean iguais(double[] um, double[] outro) {
        return Arrays.equals(um, outro);
    }

    /** Em quantas posicoes os dois discordam, para a tabela poder dizer quantas. */
    public static int quantasDiferencas(double[] um, double[] outro) {
        if (um.length != outro.length) return Math.max(um.length, outro.length);

        int quantas = 0;

        for (int i = 0; i < um.length; i++) if (um[i] != outro[i]) quantas++;

        return quantas;
    }

    /** Roda o agregador e devolve o resultado, para a tabela nao repetir isto toda vez. */
    public static double[] rodar(Agregador agregador, double[] fluxo, int largura, Contador contador) {
        return agregador.sobre(fluxo, largura, contador);
    }
}

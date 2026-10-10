package br.com.conde.janela;

import java.util.LinkedHashMap;
import java.util.Map;

import br.com.conde.janela.Minimos.Agregador;

/**
 * A SOMA DE CADA JANELA, que parece o caso facil e e o unico em que alguem erra.
 *
 * <p>No minimo nao da para fazer o truque obvio: nao existe "tirar" um elemento de um
 * minimo. Na soma existe, e e exatamente por isso que a soma e perigosa: a solucao
 * obvia funciona, e funciona ERRADO de um jeito que nao aparece em teste pequeno.
 *
 * <p>Somar o que entra e subtrair o que sai e O(1) de verdade, sem amortizacao nenhuma.
 * E em ponto flutuante ela DERIVA: o que se perde ao somar um numero grande com um
 * pequeno nao volta ao subtrair o grande de novo.
 */
public final class Somas {

    private Somas() {
    }

    /** A VARREDURA INGENUA: soma a janela inteira a cada passo. Custa n vezes w. */
    public static double[] varredura(double[] fluxo, int largura, Contador contador) {
        double[] saida = new double[fluxo.length - largura + 1];

        for (int i = 0; i < saida.length; i++) {
            double soma = 0;

            for (int j = i; j < i + largura; j++) {
                soma += fluxo[j];
                contador.operou();
            }

            saida[i] = soma;
            contador.fechouOPasso();
        }

        return saida;
    }

    /**
     * A SOMA INCREMENTAL: soma o que entra, subtrai o que sai.
     *
     * <p>Duas operacoes por passo, sem amortizacao e sem estrutura nenhuma. E a resposta
     * certa para a pergunta do custo.
     *
     * <p>E ela DERIVA. Quando a janela tem um valor de um bilhao e os outros valem menos
     * de um, somar um dos pequenos ao total perde os digitos de baixo dele, porque o
     * resultado so tem cinquenta e tres bits de mantissa. Subtrair o bilhao depois nao
     * devolve esses digitos: eles nao estao em lugar nenhum.
     *
     * <p>O erro nao se cancela com o tempo. Ele se ACUMULA, e a tabela da deriva mede
     * quanto.
     */
    public static double[] incremental(double[] fluxo, int largura, Contador contador) {
        double[] saida = new double[fluxo.length - largura + 1];
        double soma = 0;

        for (int i = 0; i < largura; i++) {
            soma += fluxo[i];
            contador.operou();
        }

        saida[0] = soma;
        contador.fechouOPasso();

        for (int i = largura; i < fluxo.length; i++) {
            soma += fluxo[i];
            soma -= fluxo[i - largura];
            contador.operou(2);

            saida[i - largura + 1] = soma;
            contador.fechouOPasso();
        }

        return saida;
    }

    /**
     * A SOMA INCREMENTAL com RECALCULO de tempos em tempos, que e o remendo usual.
     *
     * <p>Ela limita a deriva em vez de resolve-la: o erro so pode crescer por
     * {@code aCada} passos antes de ser zerado. Custa n vezes w dividido por
     * {@code aCada} a mais.
     *
     * <p>E um remendo honesto e tem um preco escondido: o passo do recalculo custa w, e o
     * algoritmo volta a ter um pior passo grande justamente para corrigir o erro de nao
     * ter um.
     */
    public static double[] incrementalComRecalculo(double[] fluxo, int largura, int aCada, Contador contador) {
        double[] saida = new double[fluxo.length - largura + 1];
        double soma = 0;

        for (int i = 0; i < largura; i++) {
            soma += fluxo[i];
            contador.operou();
        }

        saida[0] = soma;
        contador.fechouOPasso();

        for (int i = largura; i < fluxo.length; i++) {
            int posicao = i - largura + 1;

            if (posicao % aCada == 0) {
                soma = 0;

                for (int j = posicao; j < posicao + largura; j++) {
                    soma += fluxo[j];
                    contador.operou();
                }
            } else {
                soma += fluxo[i];
                soma -= fluxo[i - largura];
                contador.operou(2);
            }

            saida[posicao] = soma;
            contador.fechouOPasso();
        }

        return saida;
    }

    /**
     * DUAS PILHAS, que e a mesma estrutura do minimo com a soma no lugar.
     *
     * <p>Ela nunca SUBTRAI nada: cada agregado e sempre uma soma de uma sequencia
     * contigua, montada de uma ponta so. E por isso ela nao deriva do jeito que a
     * incremental deriva, e por isso ela existe.
     *
     * <p>O preco e ser O(1) amortizado em vez de O(1), e ter um passo que custa w.
     */
    public static double[] duasPilhas(double[] fluxo, int largura, Contador contador) {
        return Minimos.duasPilhas(fluxo, largura, contador, Double::sum);
    }

    /** Quantas vezes a versao com recalculo recomeca a conta. */
    public static final int RECALCULO_A_CADA = 1_000;

    /** Os quatro, na ordem em que as tabelas os mostram. */
    public static Map<String, Agregador> todos() {
        Map<String, Agregador> mapa = new LinkedHashMap<>();

        mapa.put("varredura ingenua", Somas::varredura);
        mapa.put("incremental", Somas::incremental);
        mapa.put("incremental com recalculo", (fluxo, largura, contador) ->
                incrementalComRecalculo(fluxo, largura, RECALCULO_A_CADA, contador));
        mapa.put("duas pilhas", Somas::duasPilhas);

        return mapa;
    }
}

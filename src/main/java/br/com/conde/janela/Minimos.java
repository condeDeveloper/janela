package br.com.conde.janela;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * O MINIMO DE CADA JANELA de largura w, por cinco caminhos diferentes.
 *
 * <p>Todos devolvem o mesmo vetor de n menos w mais um numeros. O que muda e o custo, e
 * o custo e o assunto inteiro.
 *
 * <ul>
 *   <li>{@link #varredura} e o JUIZ: olha a janela inteira a cada passo, custa n vezes w
 *       e nao tem como estar errado.
 *   <li>{@link #filaMonotonica} custa O(1) amortizado e tem um passo que custa w.
 *   <li>{@link #duasPilhas} custa O(1) amortizado e serve para QUALQUER operacao
 *       associativa, nao so para minimo.
 *   <li>{@link #monteComDescarteTardio} custa O(log n) por passo e guarda lixo: a
 *       memoria dele pode chegar ao fluxo inteiro.
 *   <li>{@link #arvoreDeSegmentos} custa O(log n) por passo e nao guarda lixo nenhum.
 * </ul>
 */
public final class Minimos {

    private Minimos() {
    }

    /** O contrato, para o juiz poder medir qualquer um deles. */
    public interface Agregador {
        double[] sobre(double[] fluxo, int largura, Contador contador);
    }

    /**
     * A VARREDURA INGENUA, que e o juiz.
     *
     * <p>Para cada posicao, olha os w valores da janela e pega o menor. Custa n vezes w e
     * nao tem truque nenhum para estar errado: ela nao guarda estado entre os passos, nao
     * descarta nada e nao depende de o fluxo ter forma alguma.
     *
     * <p>E e por isso que ela e o juiz. Um algoritmo esperto que concorde com ela em
     * todas as formas de fluxo esta certo; um que discorde esta errado, e nao ha
     * discussao sobre qual dos dois e qual.
     */
    public static double[] varredura(double[] fluxo, int largura, Contador contador) {
        conferir(fluxo, largura);

        double[] saida = new double[fluxo.length - largura + 1];

        for (int i = 0; i < saida.length; i++) {
            double menor = fluxo[i];

            for (int j = i + 1; j < i + largura; j++) {
                contador.operou();

                if (fluxo[j] < menor) menor = fluxo[j];
            }

            saida[i] = menor;
            contador.primeiraResposta();
            contador.fechouOPasso();
        }

        return saida;
    }

    /**
     * A FILA MONOTONICA, que e a resposta classica.
     *
     * <p>Guarda os indices dos candidatos a minimo em ordem CRESCENTE de valor. Quando um
     * valor novo chega, todo mundo maior ou igual a ele no fim da fila ja nao pode ser
     * minimo de janela nenhuma dali em diante, e sai.
     *
     * <p>Cada elemento entra uma vez e sai uma vez, entao o custo total e 2n e o custo
     * por elemento e O(1) AMORTIZADO. A palavra amortizado esta fazendo trabalho pesado
     * ali, e a tabela do pior passo mede quanto.
     *
     * <p>Ela so serve para operacoes que tem ORDEM, como minimo e maximo. Para soma ou
     * produto ela nao existe.
     */
    public static double[] filaMonotonica(double[] fluxo, int largura, Contador contador) {
        conferir(fluxo, largura);

        double[] saida = new double[fluxo.length - largura + 1];
        Deque<Integer> candidatos = new ArrayDeque<>();

        for (int i = 0; i < fluxo.length; i++) {
            // sai da frente quem ja nao cabe na janela
            while (!candidatos.isEmpty() && candidatos.peekFirst() <= i - largura) {
                contador.operou();
                candidatos.pollFirst();
            }

            // sai do fim quem nunca mais vai ser minimo
            while (!candidatos.isEmpty() && fluxo[candidatos.peekLast()] >= fluxo[i]) {
                contador.operou();
                candidatos.pollLast();
            }

            candidatos.addLast(i);
            contador.operou();
            contador.guardando(candidatos.size());

            if (i >= largura - 1) {
                saida[i - largura + 1] = fluxo[candidatos.peekFirst()];
                contador.primeiraResposta();
            }

            contador.fechouOPasso();
        }

        return saida;
    }

    /**
     * DUAS PILHAS, que e a resposta que quase ninguem conhece e que serve para mais
     * coisas.
     *
     * <p>Uma fila feita de duas pilhas, cada uma guardando o agregado acumulado do seu
     * proprio fundo ate o topo. O minimo da janela e o menor entre o topo das duas. Quando
     * a pilha de saida esvazia, a de entrada e virada inteira para dentro dela.
     *
     * <p>Cada elemento e virado no maximo uma vez, entao tambem e O(1) amortizado. A
     * diferenca para a fila monotonica e que esta aqui nao precisa que a operacao tenha
     * ORDEM: ela serve para soma, produto, maximo, ou-exclusivo, qualquer coisa
     * ASSOCIATIVA. E e por isso que ela resolve a soma sem subtrair nada.
     */
    public static double[] duasPilhas(double[] fluxo, int largura, Contador contador) {
        return duasPilhas(fluxo, largura, contador, Math::min);
    }

    /** O mesmo, com a operacao que se quiser, desde que seja associativa. */
    public static double[] duasPilhas(double[] fluxo, int largura, Contador contador,
                                      java.util.function.DoubleBinaryOperator operacao) {

        conferir(fluxo, largura);

        double[] saida = new double[fluxo.length - largura + 1];

        // cada pilha guarda o valor e o agregado do fundo ate ali
        List<double[]> entrando = new ArrayList<>();
        List<double[]> saindo = new ArrayList<>();

        for (int i = 0; i < fluxo.length; i++) {
            double agregadoDeEntrada = entrando.isEmpty()
                    ? fluxo[i] : operacao.applyAsDouble(entrando.get(entrando.size() - 1)[1], fluxo[i]);

            entrando.add(new double[] {fluxo[i], agregadoDeEntrada});
            contador.operou();

            if (i >= largura - 1) {
                if (saindo.isEmpty()) {
                    // vira a pilha de entrada inteira para dentro da de saida
                    while (!entrando.isEmpty()) {
                        double valor = entrando.remove(entrando.size() - 1)[0];
                        double agregado = saindo.isEmpty()
                                ? valor : operacao.applyAsDouble(saindo.get(saindo.size() - 1)[1], valor);

                        saindo.add(new double[] {valor, agregado});
                        contador.operou();
                    }
                }

                double doLadoQueSai = saindo.get(saindo.size() - 1)[1];

                saida[i - largura + 1] = entrando.isEmpty() ? doLadoQueSai
                        : operacao.applyAsDouble(doLadoQueSai, entrando.get(entrando.size() - 1)[1]);

                contador.operou();
                contador.primeiraResposta();
                saindo.remove(saindo.size() - 1);
            }

            contador.guardando(entrando.size() + saindo.size());
            contador.fechouOPasso();
        }

        return saida;
    }

    /**
     * O MONTE COM DESCARTE TARDIO, que e a resposta que parece a mais simples.
     *
     * <p>Joga tudo num monte de minimo e, na hora de ler, descarta do topo o que ja saiu
     * da janela. Sao dez linhas e funciona.
     *
     * <p>O que ele nao faz e TIRAR o que saiu da janela: tirar do meio de um monte custa
     * caro, entao ele deixa o lixo la dentro e so remove quando o lixo chega ao topo.
     *
     * <p>Num fluxo DECRESCENTE o lixo nunca chega ao topo, porque o topo e sempre o
     * elemento mais novo. A memoria dele vira o fluxo INTEIRO, e a tabela da memoria mede
     * isso.
     */
    public static double[] monteComDescarteTardio(double[] fluxo, int largura, Contador contador) {
        conferir(fluxo, largura);

        double[] saida = new double[fluxo.length - largura + 1];
        PriorityQueue<int[]> monte = new PriorityQueue<>(
                (um, outro) -> Double.compare(fluxo[um[0]], fluxo[outro[0]]));

        for (int i = 0; i < fluxo.length; i++) {
            monte.add(new int[] {i});
            contador.operou(1 + log2(monte.size()));

            if (i >= largura - 1) {
                while (monte.peek()[0] <= i - largura) {
                    contador.operou(1 + log2(monte.size()));
                    monte.poll();
                }

                saida[i - largura + 1] = fluxo[monte.peek()[0]];
                contador.operou();
                contador.primeiraResposta();
            }

            contador.guardando(monte.size());
            contador.fechouOPasso();
        }

        return saida;
    }

    /**
     * A ARVORE DE SEGMENTOS, que custa O(log n) por passo e nao guarda lixo.
     *
     * <p>Ela e a unica da lista que responde qualquer intervalo, e nao so o intervalo que
     * esta deslizando. Esta aqui para medir o preco dessa generalidade.
     *
     * <p>A construcao e de baixo para cima, sem recursao nenhuma, e a consulta tambem. A
     * profundidade seria log n e caberia na pilha, mas a serie nao usa recursao onde um
     * laco resolve.
     */
    public static double[] arvoreDeSegmentos(double[] fluxo, int largura, Contador contador) {
        conferir(fluxo, largura);

        int n = fluxo.length;
        double[] arvore = new double[2 * n];

        System.arraycopy(fluxo, 0, arvore, n, n);

        for (int i = n - 1; i > 0; i--) {
            arvore[i] = Math.min(arvore[2 * i], arvore[2 * i + 1]);
            contador.operou();
        }

        // a construcao e UM passo, e um passo que custa n. Nao e artefato da medida: e o
        // que o algoritmo faz, e e por isso que ele nao serve para um fluxo que nao acabou
        contador.fechouOPasso();

        double[] saida = new double[n - largura + 1];

        for (int i = 0; i < saida.length; i++) {
            double menor = Double.POSITIVE_INFINITY;

            for (int esquerda = i + n, direita = i + largura + n; esquerda < direita;
                 esquerda /= 2, direita /= 2) {

                contador.operou();

                if ((esquerda & 1) != 0) menor = Math.min(menor, arvore[esquerda++]);
                if ((direita & 1) != 0) menor = Math.min(menor, arvore[--direita]);
            }

            saida[i] = menor;
            contador.primeiraResposta();
            contador.fechouOPasso();
        }

        contador.guardando(2L * n);

        return saida;
    }

    /** Os cinco, na ordem em que as tabelas os mostram. */
    public static Map<String, Agregador> todos() {
        Map<String, Agregador> mapa = new LinkedHashMap<>();

        mapa.put("varredura ingenua", Minimos::varredura);
        mapa.put("fila monotonica", Minimos::filaMonotonica);
        mapa.put("duas pilhas", Minimos::duasPilhas);
        mapa.put("monte tardio", Minimos::monteComDescarteTardio);
        mapa.put("arvore de segmentos", Minimos::arvoreDeSegmentos);

        return mapa;
    }

    /** Os quatro espertos, que e o que as tabelas de custo comparam. */
    public static Map<String, Agregador> osEspertos() {
        Map<String, Agregador> mapa = todos();

        mapa.remove("varredura ingenua");

        return mapa;
    }

    private static void conferir(double[] fluxo, int largura) {
        if (largura <= 0) throw new IllegalArgumentException("a largura tem de ser positiva, e veio " + largura);

        if (largura > fluxo.length)
            throw new IllegalArgumentException("a janela de " + largura
                    + " nao cabe num fluxo de " + fluxo.length);
    }

    /** O logaritmo de base dois, por inteiro, para contar o custo do monte. */
    static int log2(int quantos) {
        return quantos <= 1 ? 0 : 63 - Long.numberOfLeadingZeros(quantos);
    }
}

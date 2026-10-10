package br.com.conde.janela;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import br.com.conde.janela.Minimos.Agregador;

/**
 * As MEDIDAS, em operacoes e em desvio, nunca em segundos.
 *
 * <p>Segundo nao e medida: muda com a maquina, com o que mais esta rodando e com o humor
 * do coletor de lixo, e a integracao continua roda em tres sistemas.
 *
 * <p>Duas colunas se repetem em quase todas as tabelas, e elas sao o assunto: OPERACOES
 * POR ELEMENTO, que e o custo amortizado, e PIOR PASSO, que e o custo do passo mais caro.
 * O segundo e o que o primeiro esconde.
 */
public final class Medidas {

    private Medidas() {
    }

    /** O tamanho do fluxo nas tabelas de minimo. */
    public static final int FLUXO = 50_000;

    /** O tamanho do fluxo nas tabelas de soma, que sao julgadas em BigDecimal e custam caro. */
    public static final int FLUXO_DA_SOMA = 20_000;

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String fluxo, int largura, String agregador,
                                boolean iguaisAoJuiz, int diferencas) {
    }

    /**
     * OS QUATRO ESPERTOS contra a VARREDURA INGENUA, em todas as formas de fluxo.
     *
     * <p>No minimo nao ha arredondamento: ou os vetores sao iguais bit por bit ou ha um
     * erro. A coluna das diferencas existe para dizer QUANTAS posicoes discordam quando
     * alguma discorda, e nao so que alguma discorda.
     */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> linhas = new ArrayList<>();

        Fluxos.todas(5_000).forEach((nome, fluxo) -> {
            for (int largura : new int[] {1, 2, 17, 256, 5_000}) {
                double[] doJuiz = Juiz.minimoExato(fluxo, largura);

                Minimos.osEspertos().forEach((quem, agregador) -> {
                    double[] obtido = agregador.sobre(fluxo, largura, new Contador());

                    linhas.add(new LinhaCorrecao(nome, largura, quem,
                            Juiz.iguais(obtido, doJuiz), Juiz.quantasDiferencas(obtido, doJuiz)));
                });
            }
        });

        return linhas;
    }

    // ───────────────────── 2. o custo ─────────────────────

    public record LinhaCusto(int largura, String agregador, long operacoes,
                             double porElemento, long piorPasso) {
    }

    /**
     * O CUSTO POR ELEMENTO conforme a janela cresce, num fluxo sorteado.
     *
     * <p>A varredura ingenua custa w por elemento e sai da tabela rapido. Os quatro
     * espertos nao crescem com w, e ai a comparacao entre eles vira outra pergunta: nao
     * "qual cresce menos", e sim "qual cobra o que, por passo".
     */
    public static List<LinhaCusto> oCusto() {
        List<LinhaCusto> linhas = new ArrayList<>();
        double[] fluxo = Fluxos.sorteado(FLUXO, 3);

        for (int largura : new int[] {8, 64, 512, 4_096})
            Minimos.todos().forEach((nome, agregador) -> {
                Contador contador = new Contador();

                agregador.sobre(fluxo, largura, contador);

                linhas.add(new LinhaCusto(largura, nome, contador.operacoes(),
                        (double) contador.operacoes() / FLUXO, contador.piorPasso()));
            });

        return linhas;
    }

    // ───────────────────── 3. o pior passo ─────────────────────

    public record LinhaPasso(String fluxo, String agregador, double porElemento,
                             long piorPasso, int largura, double piorSobreALargura,
                             long antesDaPrimeiraResposta) {
    }

    /** A largura da janela nas tabelas do pior passo e do custo fixo. */
    public static final int LARGURA = 512;

    /**
     * O PIOR PASSO, que e o que o "O(1) amortizado" esconde.
     *
     * <p>A fila monotonica custa duas operacoes por elemento em media. Num fluxo DENTE DE
     * SERRA, o passo em que o dente despenca descarta a fila inteira de uma vez, e esse
     * passo custa a largura da janela.
     *
     * <p>Em media por elemento, isso some: o passo caro e raro e o total continua O(n).
     * Para um lote, media e o que importa. Para uma interface que tem prazo por EVENTO, e
     * o pior passo que decide, e ele e w e nao 1.
     *
     * <p>A ultima coluna e outra pergunta e mudou a leitura da tabela: quantas operacoes
     * o algoritmo gasta ANTES de conseguir dar a primeira resposta. Os tres de fluxo
     * respondem depois de ver w elementos. A arvore de segmentos precisa dos n, e num
     * fluxo que nao acabou "precisa de n" quer dizer "nao serve".
     */
    public static List<LinhaPasso> oPiorPasso() {
        List<LinhaPasso> linhas = new ArrayList<>();

        java.util.Map<String, double[]> casos = new java.util.LinkedHashMap<>(Fluxos.todas(FLUXO));

        // um dente do tamanho da janela: e o fluxo feito para a fila monotonica errar
        casos.put("dente de " + LARGURA, Fluxos.denteDeSerra(FLUXO, LARGURA));

        casos.forEach((nome, fluxo) ->
                Minimos.osEspertos().forEach((quem, agregador) -> {
                    Contador contador = new Contador();

                    agregador.sobre(fluxo, LARGURA, contador);

                    linhas.add(new LinhaPasso(nome, quem, (double) contador.operacoes() / FLUXO,
                            contador.piorPasso(), LARGURA,
                            (double) contador.piorPasso() / LARGURA,
                            contador.antesDaPrimeiraResposta()));
                }));

        return linhas;
    }

    // ───────────────────── 4. a memoria ─────────────────────

    public record LinhaMemoria(String fluxo, String agregador, int largura,
                               long maiorGuardado, double sobreALargura) {
    }

    /**
     * A MEMORIA: quantos elementos cada um chega a guardar ao mesmo tempo.
     *
     * <p>A fila monotonica e as duas pilhas nunca passam de w, por construcao. O monte com
     * descarte tardio nao tira o que saiu da janela: ele espera o lixo chegar ao topo.
     *
     * <p>Num fluxo DECRESCENTE o lixo nunca chega ao topo, porque o topo e sempre o
     * elemento mais novo, e a memoria dele vira o FLUXO INTEIRO.
     *
     * <p>A coluna da direita e o numero que importa: quantas vezes a largura da janela.
     */
    public static List<LinhaMemoria> aMemoria() {
        List<LinhaMemoria> linhas = new ArrayList<>();
        int largura = 64;

        Fluxos.todas(FLUXO).forEach((nome, fluxo) ->
                Minimos.osEspertos().forEach((quem, agregador) -> {
                    Contador contador = new Contador();

                    agregador.sobre(fluxo, largura, contador);

                    linhas.add(new LinhaMemoria(nome, quem, largura, contador.maiorGuardado(),
                            (double) contador.maiorGuardado() / largura));
                }));

        return linhas;
    }

    // ───────────────────── 5. a deriva da soma ─────────────────────

    public record LinhaDeriva(String fluxo, String agregador, int largura,
                              double maiorDesvio, double desvioNoFim, double desvioRelativo,
                              double porElemento, long piorPasso) {
    }

    /**
     * A DERIVA DA SOMA INCREMENTAL, julgada pela soma EXATA em BigDecimal.
     *
     * <p>Somar o que entra e subtrair o que sai e O(1) de verdade, sem amortizacao. E em
     * ponto flutuante o que se perde ao somar um numero pequeno a um grande nao volta ao
     * subtrair o grande.
     *
     * <p>A coluna do DESVIO NO FIM e a que separa erro de arredondamento de DERIVA: se o
     * erro so arredondasse, ele seria do mesmo tamanho na primeira e na ultima janela. Se
     * ele acumula, a ultima e pior.
     *
     * <p>As duas pilhas nunca subtraem nada, e por isso nao derivam.
     */
    public static List<LinhaDeriva> aDeriva() {
        List<LinhaDeriva> linhas = new ArrayList<>();
        int largura = 100;

        record Caso(String nome, double[] fluxo) {
        }

        List<Caso> casos = List.of(
                new Caso("sorteado", Fluxos.sorteado(FLUXO_DA_SOMA, 5)),
                new Caso("um grande a cada 50", Fluxos.grandeEPequeno(FLUXO_DA_SOMA, 50, 5)),
                new Caso("um grande a cada 1000", Fluxos.grandeEPequeno(FLUXO_DA_SOMA, 1_000, 5)));

        for (Caso caso : casos) {
            BigDecimal[] exato = Juiz.somaExata(caso.fluxo(), largura);

            Somas.todos().forEach((nome, agregador) -> {
                Contador contador = new Contador();
                double[] obtido = agregador.sobre(caso.fluxo(), largura, contador);

                linhas.add(new LinhaDeriva(caso.nome(), nome, largura,
                        Juiz.maiorDesvio(obtido, exato),
                        Juiz.desvioNoFim(obtido, exato),
                        Juiz.maiorDesvioRelativo(obtido, exato),
                        (double) contador.operacoes() / caso.fluxo().length,
                        contador.piorPasso()));
            });
        }

        return linhas;
    }

    public record LinhaAcumulo(int janela, double desvioDaIncremental, double desvioDasDuasPilhas) {
    }

    /**
     * O ERRO ACUMULA, e esta tabela mostra ele acumulando.
     *
     * <p>Se fosse so arredondamento, o desvio da janela mil seria do mesmo tamanho do da
     * janela dez. A tabela anda pelo fluxo e mostra o desvio crescendo.
     */
    public static List<LinhaAcumulo> oAcumulo() {
        List<LinhaAcumulo> linhas = new ArrayList<>();
        int largura = 100;
        double[] fluxo = Fluxos.grandeEPequeno(FLUXO_DA_SOMA, 50, 5);

        BigDecimal[] exato = Juiz.somaExata(fluxo, largura);
        double[] incremental = Somas.incremental(fluxo, largura, new Contador());
        double[] duasPilhas = Somas.duasPilhas(fluxo, largura, new Contador());

        for (int janela : new int[] {0, 10, 100, 1_000, 5_000, 10_000, 19_900}) {
            if (janela >= exato.length) continue;

            linhas.add(new LinhaAcumulo(janela,
                    new BigDecimal(incremental[janela]).subtract(exato[janela]).abs().doubleValue(),
                    new BigDecimal(duasPilhas[janela]).subtract(exato[janela]).abs().doubleValue()));
        }

        return linhas;
    }

    /** O agregador de soma que a tabela da deriva chama de certo, para os testes citarem. */
    public static Agregador aSomaQueNaoDeriva() {
        return Somas::duasPilhas;
    }
}

package br.com.conde.janela;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Os testes da SOMA, julgada pela soma EXATA em BigDecimal.
 *
 * <p>Aqui a varredura ingenua NAO e o juiz, e essa e a parte interessante do arquivo:
 * ela tambem e ponto flutuante e tambem arredonda, so que menos. O juiz e o BigDecimal,
 * que nao arredonda.
 */
class SomasTest {

    private final Contador contador = new Contador();

    @Test
    @DisplayName("com inteiros pequenos todas as versoes dao a resposta escrita a mao")
    void comInteirosPequenosTodasAcertam() {
        double[] fluxo = {3, 1, 4, 1, 5, 9, 2, 6};

        Somas.todos().forEach((nome, agregador) ->
                assertThat(agregador.sobre(fluxo, 3, new Contador()))
                        .as(nome).containsExactly(8, 6, 10, 15, 16, 17));
    }

    @Test
    @DisplayName("num fluxo sorteado de grandeza unica as quatro versoes dao a soma exata")
    void semGrandezasMisturadasNinguemErra() {
        double[] fluxo = Fluxos.sorteado(5_000, 7);
        BigDecimal[] exato = Juiz.somaExata(fluxo, 100);

        Somas.todos().forEach((nome, agregador) ->
                assertThat(Juiz.maiorDesvio(agregador.sobre(fluxo, 100, new Contador()), exato))
                        .as(nome).isZero());
    }

    /**
     * O ACHADO: com grandezas misturadas a soma incremental DERIVA, e a derivada
     * ACUMULA.
     *
     * <p>Se fosse so arredondamento, o desvio da ultima janela seria do mesmo tamanho do
     * da primeira. Ele e dezenas de vezes maior.
     */
    @Test
    @DisplayName("com grandezas misturadas o erro da incremental cresce do comeco para o fim")
    void oErroDaIncrementalAcumula() {
        double[] fluxo = Fluxos.grandeEPequeno(20_000, 50, 5);
        BigDecimal[] exato = Juiz.somaExata(fluxo, 100);

        double[] incremental = Somas.incremental(fluxo, 100, contador);
        double[] duasPilhas = Somas.duasPilhas(fluxo, 100, contador);

        double noComeco = desvio(incremental, exato, 0);
        double noFim = desvio(incremental, exato, exato.length - 1);

        assertThat(noFim).as("o erro da incremental no fim").isGreaterThan(10 * noComeco);

        // e o das duas pilhas nao cresce: ele fica na mesma ordem de grandeza
        assertThat(desvio(duasPilhas, exato, exato.length - 1))
                .as("o erro das duas pilhas no fim").isLessThan(2 * desvio(duasPilhas, exato, 0));

        assertThat(Juiz.maiorDesvio(incremental, exato))
                .isGreaterThan(5 * Juiz.maiorDesvio(duasPilhas, exato));
    }

    /**
     * E O NUMERO QUE DOI: quando a janela nao tem nenhum valor grande, a soma vale umas
     * dezenas, e o erro que sobrou dos valores grandes anteriores vale mais de seis por
     * cento dela.
     */
    @Test
    @DisplayName("a soma incremental chega a errar seis por cento do valor da janela")
    void aIncrementalErraPorCento() {
        double[] fluxo = Fluxos.grandeEPequeno(20_000, 1_000, 5);
        BigDecimal[] exato = Juiz.somaExata(fluxo, 100);

        double daIncremental = Juiz.maiorDesvioRelativo(Somas.incremental(fluxo, 100, contador), exato);
        double dasDuasPilhas = Juiz.maiorDesvioRelativo(Somas.duasPilhas(fluxo, 100, contador), exato);

        assertThat(daIncremental).isGreaterThan(0.05);
        assertThat(dasDuasPilhas).isLessThan(1e-14);

        // treze ordens de grandeza entre as duas
        assertThat(daIncremental / dasDuasPilhas).isGreaterThan(1e12);
    }

    /**
     * A VARREDURA INGENUA tambem erra, e e por isso que ela nao serve de juiz aqui.
     *
     * <p>Ela erra menos do que a incremental porque recomeca a conta a cada janela e o
     * erro nao atravessa de uma para a outra. Mas ela soma w numeros de grandezas
     * diferentes em sequencia, e isso arredonda.
     */
    @Test
    @DisplayName("a varredura ingenua tambem erra, so que menos, e por isso nao e o juiz da soma")
    void aVarreduraTambemErra() {
        double[] fluxo = Fluxos.grandeEPequeno(20_000, 50, 5);
        BigDecimal[] exato = Juiz.somaExata(fluxo, 100);

        double daVarredura = Juiz.maiorDesvio(Somas.varredura(fluxo, 100, contador), exato);

        assertThat(daVarredura).as("ela erra").isPositive();
        assertThat(daVarredura).as("e erra menos do que a incremental")
                .isLessThan(Juiz.maiorDesvio(Somas.incremental(fluxo, 100, contador), exato));
    }

    /**
     * O RECALCULO e um remendo honesto: ele limita a deriva em vez de resolve-la, e cobra
     * por isso um passo que custa w.
     */
    @Test
    @DisplayName("o recalculo periodico corta a deriva pela metade e devolve o passo caro")
    void oRecalculoLimitaADeriva() {
        double[] fluxo = Fluxos.grandeEPequeno(20_000, 50, 5);
        BigDecimal[] exato = Juiz.somaExata(fluxo, 100);

        Contador daIncremental = new Contador();
        Contador doRecalculo = new Contador();

        double[] incremental = Somas.incremental(fluxo, 100, daIncremental);
        double[] comRecalculo = Somas.incrementalComRecalculo(fluxo, 100, Somas.RECALCULO_A_CADA, doRecalculo);

        assertThat(Juiz.maiorDesvio(comRecalculo, exato))
                .isLessThan(Juiz.maiorDesvio(incremental, exato));

        // e nao resolve: ainda erra mais do que as duas pilhas
        assertThat(Juiz.maiorDesvio(comRecalculo, exato))
                .isGreaterThan(Juiz.maiorDesvio(Somas.duasPilhas(fluxo, 100, contador), exato));

        // os dois tem o mesmo pior passo, e por motivos diferentes
        assertThat(daIncremental.piorPasso()).isEqualTo(100);
        assertThat(doRecalculo.piorPasso()).isEqualTo(100);
        assertThat(doRecalculo.operacoes()).isGreaterThan(daIncremental.operacoes());
    }

    @Test
    @DisplayName("a incremental custa duas operacoes por elemento e a varredura custa a largura")
    void oCustoDeCadaUma() {
        double[] fluxo = Fluxos.sorteado(10_000, 1);

        Contador daVarredura = new Contador();
        Contador daIncremental = new Contador();
        Contador dasDuasPilhas = new Contador();

        Somas.varredura(fluxo, 100, daVarredura);
        Somas.incremental(fluxo, 100, daIncremental);
        Somas.duasPilhas(fluxo, 100, dasDuasPilhas);

        assertThat((double) daVarredura.operacoes() / 10_000).isCloseTo(100, within(1.0));
        assertThat((double) daIncremental.operacoes() / 10_000).isCloseTo(2.0, within(0.05));
        assertThat((double) dasDuasPilhas.operacoes() / 10_000).isCloseTo(3.0, within(0.05));
    }

    @Test
    @DisplayName("com largura um a soma de cada janela e o proprio valor")
    void comLarguraUm() {
        double[] fluxo = Fluxos.sorteado(200, 2);

        Somas.todos().forEach((nome, agregador) ->
                assertThat(agregador.sobre(fluxo, 1, new Contador())).as(nome).isEqualTo(fluxo));
    }

    private static double desvio(double[] obtido, BigDecimal[] exato, int posicao) {
        return new BigDecimal(obtido[posicao]).subtract(exato[posicao]).abs().doubleValue();
    }
}

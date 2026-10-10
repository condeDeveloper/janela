package br.com.conde.janela;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Os testes do JUIZ e dos FLUXOS.
 *
 * <p>Um juiz que ninguem confere nao e juiz. E os fluxos precisam de mais cuidado do que
 * o juiz: a forma do fluxo decide tudo neste repositorio, e um gerador que entregasse
 * outra forma faria as tabelas medirem outra coisa sem avisar.
 */
class JuizTest {

    /**
     * A SOMA EXATA e exata: todo double e representavel em BigDecimal, entao somar w
     * doubles em BigDecimal nao arredonda em lugar nenhum.
     */
    @Test
    @DisplayName("a soma exata de valores que cabem em double da o numero escrito a mao")
    void aSomaExataEExata() {
        BigDecimal[] soma = Juiz.somaExata(new double[] {0.5, 0.25, 0.125, 1}, 2);

        assertThat(soma).hasSize(3);
        assertThat(soma[0]).isEqualByComparingTo(new BigDecimal("0.75"));
        assertThat(soma[1]).isEqualByComparingTo(new BigDecimal("0.375"));
        assertThat(soma[2]).isEqualByComparingTo(new BigDecimal("1.125"));
    }

    /**
     * E ELA NAO ARREDONDA onde o double arredonda. Somar 0,1 e 0,2 em double da
     * 0,30000000000000004, e a soma exata dos BITS daqueles dois doubles e outro numero,
     * que nao e nem 0,3 nem 0,30000000000000004.
     */
    @Test
    @DisplayName("a soma exata enxerga o que o double perde em 0,1 mais 0,2")
    void aSomaExataEnxergaOQueODoublePerde() {
        double[] fluxo = {0.1, 0.2};

        BigDecimal exata = Juiz.somaExata(fluxo, 2)[0];
        double emDouble = 0.1 + 0.2;

        assertThat(new BigDecimal(emDouble)).isNotEqualByComparingTo(exata);
        assertThat(exata.doubleValue()).isEqualTo(emDouble);

        // e o desvio existe e e pequeno: e exatamente o arredondamento da soma
        assertThat(Juiz.maiorDesvio(new double[] {emDouble}, new BigDecimal[] {exata}))
                .isPositive().isLessThan(1e-16);
    }

    @Test
    @DisplayName("o desvio de vetores de tamanhos diferentes e infinito e nao uma comparacao torta")
    void osTamanhosDiferentes() {
        BigDecimal[] exato = Juiz.somaExata(new double[] {1, 2, 3}, 2);

        assertThat(Juiz.maiorDesvio(new double[] {3}, exato)).isInfinite();
        assertThat(Juiz.maiorDesvioRelativo(new double[] {3}, exato)).isInfinite();
        assertThat(Juiz.desvioNoFim(new double[] {3}, exato)).isInfinite();
        assertThat(Juiz.desvioNoFim(new double[0], new BigDecimal[0])).isInfinite();
    }

    @Test
    @DisplayName("o desvio relativo pula as janelas de soma zero em vez de dividir por zero")
    void oDesvioRelativoPulaOZero() {
        double[] fluxo = {1, -1, 1, -1};

        assertThat(Juiz.maiorDesvioRelativo(Somas.varredura(fluxo, 2, new Contador()),
                Juiz.somaExata(fluxo, 2))).isZero();
    }

    @Test
    @DisplayName("quantas diferencas conta as posicoes, e a igualdade e bit por bit")
    void aContagemDeDiferencas() {
        assertThat(Juiz.quantasDiferencas(new double[] {1, 2, 3}, new double[] {1, 9, 3})).isEqualTo(1);
        assertThat(Juiz.quantasDiferencas(new double[] {1, 2}, new double[] {1, 2, 3})).isEqualTo(3);
        assertThat(Juiz.iguais(new double[] {1, 2}, new double[] {1, 2})).isTrue();
        assertThat(Juiz.iguais(new double[] {1, 2}, new double[] {1, 2.0000001})).isFalse();
    }

    // ───────────────────────────── os fluxos ─────────────────────────────

    @Test
    @DisplayName("as formas sao o que prometem")
    void asFormasSaoOQuePrometem() {
        assertThat(Fluxos.crescente(5)).containsExactly(0, 1, 2, 3, 4);
        assertThat(Fluxos.decrescente(5)).containsExactly(5, 4, 3, 2, 1);
        assertThat(Fluxos.constante(4, 7)).containsExactly(7, 7, 7, 7);
        assertThat(Fluxos.denteDeSerra(7, 3)).containsExactly(0, 1, 2, 0, 1, 2, 0);
        assertThat(Fluxos.todas(10)).hasSize(5);
    }

    @Test
    @DisplayName("o fluxo sorteado fica entre zero e um e cobre o intervalo")
    void oFluxoSorteado() {
        double[] fluxo = Fluxos.sorteado(20_000, 3);
        int[] caixas = new int[10];

        for (double valor : fluxo) {
            assertThat(valor).isBetween(0.0, 1.0);
            caixas[(int) (valor * 10)]++;
        }

        for (int i = 0; i < 10; i++) assertThat(caixas[i]).as("faixa %d", i).isBetween(1_700, 2_300);
    }

    @Test
    @DisplayName("a mesma semente da o mesmo fluxo e sementes diferentes dao fluxos diferentes")
    void oSorteioERepetivel() {
        assertThat(Fluxos.sorteado(1_000, 5)).isEqualTo(Fluxos.sorteado(1_000, 5));
        assertThat(Fluxos.sorteado(1_000, 5)).isNotEqualTo(Fluxos.sorteado(1_000, 6));
    }

    /**
     * O DESLOCAMENTO DE 32 BITS importa mais aqui do que no costume.
     *
     * <p>Num congruente linear sem descarte, os valores sobem e descem em xadrez. Um
     * fluxo xadrez e um caso FACIL para a fila monotonica, que descartaria um por passo,
     * sempre, e o "fluxo sorteado" das tabelas viraria mais uma forma fixa sem ninguem
     * reparar.
     */
    @Test
    @DisplayName("o sorteio nao alterna sobe e desce, que seria um caso facil disfarcado de sorteado")
    void oSorteioNaoAlterna() {
        double[] fluxo = Fluxos.sorteado(10_000, 11);
        int alternou = 0;

        for (int i = 2; i < fluxo.length; i++) {
            boolean subiuAntes = fluxo[i - 1] > fluxo[i - 2];
            boolean subiuAgora = fluxo[i] > fluxo[i - 1];

            if (subiuAntes != subiuAgora) alternou++;
        }

        // num fluxo sorteado de verdade a direcao muda em dois tercos dos passos
        assertThat((double) alternou / fluxo.length).isBetween(0.6, 0.72);
    }

    @Test
    @DisplayName("a semente zero nao trava o sorteio")
    void aSementeZeroNaoTrava() {
        Fluxos.Sorteio sorteio = new Fluxos.Sorteio(0);
        Set<Integer> vistos = new HashSet<>();

        for (int i = 0; i < 100; i++) vistos.add(sorteio.proximo());

        assertThat(vistos).hasSizeGreaterThan(90);
    }

    /**
     * O FLUXO DE GRANDEZAS MISTURADAS e o que quebra a soma incremental, e ele precisa
     * ser o que promete: um valor enorme a cada {@code deQuantos}, e o resto pequeno.
     */
    @Test
    @DisplayName("o fluxo de grandezas misturadas tem um valor enorme na posicao certa e o resto pequeno")
    void oFluxoDeGrandezasMisturadas() {
        double[] fluxo = Fluxos.grandeEPequeno(1_000, 50, 1);

        for (int i = 0; i < fluxo.length; i++)
            if (i % 50 == 0) assertThat(fluxo[i]).as("posicao %d", i).isGreaterThan(1e15);
            else assertThat(fluxo[i]).as("posicao %d", i).isBetween(0.0, 1.0);

        assertThat(java.util.Arrays.stream(fluxo).filter(valor -> valor > 1e15).count()).isEqualTo(20);
    }

    /**
     * A GRANDEZA ESCOLHIDA PRECISA SER GRANDE O BASTANTE: com um bilhao, somar um valor
     * pequeno ainda guarda alguns digitos dele; com um quatrilhao, nao guarda nenhum.
     */
    @Test
    @DisplayName("somar um valor pequeno a um quatrilhao perde o valor pequeno inteiro")
    void aGrandezaEscolhidaApagaOPequeno() {
        double grande = 1e15;
        double pequeno = 0.03;

        // com 1e15, somar 0,03 nao muda nada: o resultado volta igual
        assertThat(grande + pequeno - grande).isNotEqualTo(pequeno);
        assertThat(grande + pequeno - grande).isCloseTo(0.0, within(0.06));

        // com 1e9, ainda sobra quase tudo
        assertThat(1e9 + pequeno - 1e9).isCloseTo(pequeno, within(1e-7));
    }

    @Test
    @DisplayName("o minimo exato e a varredura, sem contar operacao nenhuma")
    void oMinimoExato() {
        double[] fluxo = {3, 1, 4, 1, 5};

        assertThat(Juiz.minimoExato(fluxo, 2)).containsExactly(1, 1, 1, 1);
        assertThat(Juiz.rodar(Minimos::filaMonotonica, fluxo, 2, new Contador()))
                .isEqualTo(Juiz.minimoExato(fluxo, 2));
    }
}

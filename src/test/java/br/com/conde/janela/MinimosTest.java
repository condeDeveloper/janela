package br.com.conde.janela;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Os testes dos CINCO caminhos para o minimo de cada janela.
 *
 * <p>No minimo nao ha arredondamento: a resposta e um dos valores do fluxo, escolhido, e
 * nao uma conta sobre eles. Entao a comparacao com o juiz e de IGUALDADE BIT POR BIT, e
 * nao de proximidade.
 */
class MinimosTest {

    private final Contador contador = new Contador();

    @Test
    @DisplayName("os quatro espertos batem com a varredura ingenua em toda forma e toda largura")
    void todosBatemComOJuiz() {
        for (int n : new int[] {1, 2, 7, 64, 1_000})
            Fluxos.todas(n).forEach((nome, fluxo) -> {
                for (int largura = 1; largura <= n; largura = largura < 4 ? largura + 1 : largura * 3) {
                    double[] doJuiz = Juiz.minimoExato(fluxo, largura);
                    int finalLargura = largura;

                    Minimos.osEspertos().forEach((quem, agregador) ->
                            assertThat(agregador.sobre(fluxo, finalLargura, contador))
                                    .as("%s em %s com n=%d largura=%d", quem, nome, n, finalLargura)
                                    .isEqualTo(doJuiz));
                }
            });
    }

    /**
     * O caso em que uma implementacao desatenta erra: valores IGUAIS dentro da janela.
     *
     * <p>Na fila monotonica, descartar com "maior que" em vez de "maior ou igual a"
     * deixa duplicatas na fila e a faz crescer alem de w. A resposta continua certa e a
     * memoria nao: e um erro que so a tabela da memoria pega.
     */
    @Test
    @DisplayName("com valores repetidos a resposta continua certa e a fila nao passa da largura")
    void osValoresRepetidos() {
        double[] fluxo = {5, 5, 5, 1, 5, 5, 5, 5, 1, 5};

        for (int largura : new int[] {1, 2, 3, 4, 10}) {
            double[] doJuiz = Juiz.minimoExato(fluxo, largura);

            Minimos.osEspertos().forEach((quem, agregador) ->
                    assertThat(agregador.sobre(fluxo, largura, new Contador()))
                            .as("%s com largura %d", quem, largura).isEqualTo(doJuiz));
        }

        Contador daFila = new Contador();

        Minimos.filaMonotonica(Fluxos.constante(10_000, 7), 64, daFila);

        assertThat(daFila.maiorGuardado()).as("a fila nao guarda duplicata").isEqualTo(1);
    }

    @Test
    @DisplayName("com largura um a resposta e o proprio fluxo")
    void comLarguraUm() {
        double[] fluxo = Fluxos.sorteado(500, 9);

        Minimos.todos().forEach((quem, agregador) ->
                assertThat(agregador.sobre(fluxo, 1, new Contador())).as(quem).isEqualTo(fluxo));
    }

    @Test
    @DisplayName("com largura igual ao fluxo a resposta e um numero so, o menor de todos")
    void comLarguraCheia() {
        double[] fluxo = Fluxos.sorteado(500, 9);
        double menor = java.util.Arrays.stream(fluxo).min().orElseThrow();

        Minimos.todos().forEach((quem, agregador) -> {
            double[] saida = agregador.sobre(fluxo, fluxo.length, new Contador());

            assertThat(saida).as(quem).hasSize(1);
            assertThat(saida[0]).as(quem).isEqualTo(menor);
        });
    }

    @Test
    @DisplayName("uma largura que nao cabe ou nao e positiva reclama")
    void asLargurasInvalidas() {
        double[] fluxo = {1, 2, 3};

        Minimos.todos().forEach((quem, agregador) -> {
            assertThatThrownBy(() -> agregador.sobre(fluxo, 0, new Contador()))
                    .as(quem).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> agregador.sobre(fluxo, 4, new Contador()))
                    .as(quem).isInstanceOf(IllegalArgumentException.class);
        });
    }

    /**
     * AS DUAS PILHAS servem para QUALQUER operacao associativa, e e isso que as separa
     * da fila monotonica.
     *
     * <p>A fila monotonica precisa de ORDEM para saber quem descartar. A soma nao tem
     * ordem: nao existe "este valor nunca mais vai ser a soma". Para soma, produto ou
     * ou-exclusivo, a fila monotonica simplesmente nao existe.
     */
    @Test
    @DisplayName("as duas pilhas servem para soma, maximo e produto, nao so para minimo")
    void asDuasPilhasServemParaQualquerOperacaoAssociativa() {
        double[] fluxo = {3, 1, 4, 1, 5, 9, 2, 6};

        assertThat(Minimos.duasPilhas(fluxo, 3, contador, Double::sum))
                .containsExactly(8, 6, 10, 15, 16, 17);

        assertThat(Minimos.duasPilhas(fluxo, 3, contador, Math::max))
                .containsExactly(4, 4, 5, 9, 9, 9);

        assertThat(Minimos.duasPilhas(fluxo, 3, contador, (um, outro) -> um * outro))
                .containsExactly(12, 4, 20, 45, 90, 108);

        assertThat(Minimos.duasPilhas(fluxo, 3, contador, Math::min))
                .containsExactly(1, 1, 1, 1, 2, 2);
    }

    /**
     * O PIOR PASSO da fila monotonica so chega a w com um fluxo FEITO para isso.
     *
     * <p>Num dente de serra do tamanho da janela, a fila enche com o dente inteiro e o
     * primeiro valor do dente seguinte descarta tudo de uma vez. Com dentes menores que a
     * janela, o pior passo e o tamanho do dente, e nao w.
     */
    @Test
    @DisplayName("o pior passo da fila monotonica e o tamanho do dente, e chega a w so com dente de w")
    void oPiorPassoDaFilaDependeDoFluxo() {
        int largura = 512;

        Contador comDenteGrande = new Contador();
        Contador comDentePequeno = new Contador();
        Contador crescente = new Contador();

        Minimos.filaMonotonica(Fluxos.denteDeSerra(20_000, largura), largura, comDenteGrande);
        Minimos.filaMonotonica(Fluxos.denteDeSerra(20_000, 64), largura, comDentePequeno);
        Minimos.filaMonotonica(Fluxos.crescente(20_000), largura, crescente);

        assertThat(comDenteGrande.piorPasso()).isEqualTo(largura + 1);
        assertThat(comDentePequeno.piorPasso()).isEqualTo(65);
        assertThat(crescente.piorPasso()).isEqualTo(2);
    }

    /**
     * E O PIOR PASSO DAS DUAS PILHAS e w SEMPRE, em qualquer fluxo: a viragem da pilha
     * nao depende da forma do fluxo, so da janela.
     */
    @Test
    @DisplayName("o pior passo das duas pilhas e a largura em todo fluxo, sem excecao")
    void oPiorPassoDasDuasPilhasNaoDependeDoFluxo() {
        int largura = 256;

        Fluxos.todas(10_000).forEach((nome, fluxo) -> {
            Contador contadorDoFluxo = new Contador();

            Minimos.duasPilhas(fluxo, largura, contadorDoFluxo);

            assertThat(contadorDoFluxo.piorPasso()).as(nome).isEqualTo(largura + 2);
        });
    }

    /**
     * A MEMORIA DO MONTE COM DESCARTE TARDIO vira o fluxo INTEIRO num fluxo
     * decrescente: o lixo nunca chega ao topo, porque o topo e sempre o mais novo.
     */
    @Test
    @DisplayName("num fluxo decrescente o monte tardio guarda o fluxo inteiro e a fila guarda um")
    void aMemoriaDoMonteNoFluxoDecrescente() {
        int n = 20_000;
        double[] fluxo = Fluxos.decrescente(n);

        Contador doMonte = new Contador();
        Contador daFila = new Contador();

        Minimos.monteComDescarteTardio(fluxo, 64, doMonte);
        Minimos.filaMonotonica(fluxo, 64, daFila);

        assertThat(doMonte.maiorGuardado()).isEqualTo(n);
        assertThat(daFila.maiorGuardado()).isEqualTo(1);
    }

    /**
     * A ARVORE DE SEGMENTOS precisa do fluxo INTEIRO antes de responder qualquer coisa,
     * e os outros tres respondem depois de ver w elementos.
     *
     * <p>Num fluxo que nao acabou, "precisa de n" quer dizer "nao serve".
     */
    @Test
    @DisplayName("a arvore de segmentos gasta o fluxo inteiro antes da primeira resposta")
    void aArvorePrecisaDoFluxoInteiro() {
        int n = 20_000;
        int largura = 128;
        double[] fluxo = Fluxos.sorteado(n, 4);

        Contador daArvore = new Contador();
        Contador daFila = new Contador();

        Minimos.arvoreDeSegmentos(fluxo, largura, daArvore);
        Minimos.filaMonotonica(fluxo, largura, daFila);

        assertThat(daArvore.antesDaPrimeiraResposta()).isGreaterThan(n);
        assertThat(daFila.antesDaPrimeiraResposta()).isLessThan(3L * largura);
    }

    @Test
    @DisplayName("o logaritmo de base dois por inteiro")
    void oLogaritmo() {
        assertThat(Minimos.log2(0)).isZero();
        assertThat(Minimos.log2(1)).isZero();
        assertThat(Minimos.log2(2)).isEqualTo(1);
        assertThat(Minimos.log2(3)).isEqualTo(1);
        assertThat(Minimos.log2(1024)).isEqualTo(10);
        assertThat(Minimos.log2(1023)).isEqualTo(9);
    }

    @Test
    @DisplayName("o contador soma, fecha passo, marca a agua e zera")
    void oContador() {
        Contador proprio = new Contador();

        proprio.operou(5);
        proprio.fechouOPasso();
        proprio.operou(2);
        proprio.fechouOPasso();
        proprio.guardando(9);
        proprio.guardando(3);

        assertThat(proprio.operacoes()).isEqualTo(7);
        assertThat(proprio.piorPasso()).isEqualTo(5);
        assertThat(proprio.maiorGuardado()).isEqualTo(9);
        assertThat(proprio.toString()).contains("7 operacoes").contains("pior passo 5");

        // sem primeira resposta marcada, ele devolve o total
        assertThat(proprio.antesDaPrimeiraResposta()).isEqualTo(7);

        proprio.primeiraResposta();
        proprio.operou(100);

        assertThat(proprio.antesDaPrimeiraResposta()).as("a primeira marcacao vale").isEqualTo(7);

        proprio.zerar();

        assertThat(proprio.operacoes()).isZero();
        assertThat(proprio.piorPasso()).isZero();
        assertThat(proprio.maiorGuardado()).isZero();
    }
}

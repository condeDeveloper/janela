package br.com.conde.janela;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Os testes das TABELAS que o medidor imprime.
 *
 * <p>Toda frase do README que e um numero esta aqui como afirmacao. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
class MedidasTest {

    /*
     * As tabelas sao calculadas UMA VEZ e compartilhadas. Elas sao deterministas, de
     * semente fixa, e a da deriva julga vinte mil janelas em BigDecimal.
     */
    private static final List<Medidas.LinhaCorrecao> CORRECAO = Medidas.aCorrecao();
    private static final List<Medidas.LinhaCusto> CUSTO = Medidas.oCusto();
    private static final List<Medidas.LinhaPasso> PASSO = Medidas.oPiorPasso();
    private static final List<Medidas.LinhaMemoria> MEMORIA = Medidas.aMemoria();
    private static final List<Medidas.LinhaDeriva> DERIVA = Medidas.aDeriva();
    private static final List<Medidas.LinhaAcumulo> ACUMULO = Medidas.oAcumulo();

    // ───────────────────── a correcao ─────────────────────

    @Test
    @DisplayName("as cem comparacoes contra a varredura ingenua batem todas, bit por bit")
    void aTabelaDaCorrecao() {
        assertThat(CORRECAO).hasSize(5 * 5 * 4);
        assertThat(CORRECAO).allSatisfy(l -> {
            assertThat(l.iguaisAoJuiz())
                    .as("%s em %s com largura %d", l.agregador(), l.fluxo(), l.largura()).isTrue();
            assertThat(l.diferencas()).isZero();
        });

        // as cinco formas e as cinco larguras estao todas la
        assertThat(CORRECAO).extracting(Medidas.LinhaCorrecao::fluxo).containsAll(
                List.of("sorteado", "crescente", "decrescente", "constante", "dente de serra"));

        assertThat(CORRECAO).extracting(Medidas.LinhaCorrecao::largura).containsAll(
                List.of(1, 2, 17, 256, 5_000));
    }

    // ───────────────────── o custo ─────────────────────

    /**
     * O CUSTO DA VARREDURA cresce com a janela e o dos quatro espertos nao.
     *
     * <p>A fila monotonica custa 2,00 operacoes por elemento com janela de 8 e com janela
     * de 4.096. O numero nao se mexe.
     */
    @Test
    @DisplayName("a fila monotonica custa duas operacoes por elemento em toda largura, e a varredura custa a largura")
    void aTabelaDoCusto() {
        assertThat(CUSTO).hasSize(4 * 5);

        for (int largura : new int[] {8, 64, 512, 4_096}) {
            assertThat(acharCusto(largura, "fila monotonica").porElemento())
                    .as("a fila com largura %d", largura).isCloseTo(2.0, within(0.02));

            assertThat(acharCusto(largura, "duas pilhas").porElemento())
                    .as("as duas pilhas com largura %d", largura).isCloseTo(3.0, within(0.15));

            // a varredura olha w menos um vizinhos por janela, e ha n menos w mais um
            // janelas: o custo por elemento do fluxo inteiro e o produto dos dois sobre n
            double esperado = (largura - 1.0) * (Medidas.FLUXO - largura + 1) / Medidas.FLUXO;

            assertThat(acharCusto(largura, "varredura ingenua").porElemento())
                    .as("a varredura com largura %d", largura).isCloseTo(esperado, within(1.0));
        }
    }

    /**
     * E A ARVORE DE SEGMENTOS custa o LOGARITMO DA JANELA por elemento, e nao uma
     * constante: a consulta anda log2(w) niveis.
     */
    @Test
    @DisplayName("a arvore de segmentos custa o logaritmo da janela por elemento")
    void aArvoreCustaOLogaritmoDaJanela() {
        for (int largura : new int[] {8, 64, 512, 4_096})
            assertThat(acharCusto(largura, "arvore de segmentos").porElemento())
                    .as("a arvore com largura %d", largura)
                    .isCloseTo(Minimos.log2(largura) + 1.0, within(1.2));
    }

    /**
     * O MONTE COM DESCARTE TARDIO tem um comportamento que eu nao esperava: ele piora
     * quando a janela DIMINUI.
     *
     * <p>Com janela pequena, mais elementos expiram por passo e o lixo se junta dentro do
     * monte sem chegar ao topo. Quando chega, um unico passo descarta a pilha inteira de
     * uma vez, e esse passo custa 20.819 operacoes com janela de OITO.
     */
    @Test
    @DisplayName("o pior passo do monte tardio piora quando a janela diminui, e chega a 20 mil com janela de 8")
    void oMonteTardioPioraComAJanelaPequena() {
        long comOito = acharCusto(8, "monte tardio").piorPasso();
        long comQuatroMil = acharCusto(4_096, "monte tardio").piorPasso();

        assertThat(comOito).isGreaterThan(10_000);
        assertThat(comQuatroMil).isLessThan(1_000);
        assertThat(comOito).isGreaterThan(50 * comQuatroMil);
    }

    // ───────────────────── o pior passo ─────────────────────

    /**
     * O ACHADO PRINCIPAL: o "O(1) amortizado" esconde um passo que custa a largura
     * inteira da janela.
     *
     * <p>Num dente de serra do tamanho da janela, a fila monotonica custa 1,99 operacoes
     * por elemento em media e 513 no pior passo. Para um lote, a media e o que importa;
     * para uma interface com prazo por evento, e o pior passo.
     */
    @Test
    @DisplayName("a fila monotonica custa 2 por elemento e 513 no pior passo, no fluxo feito para isso")
    void aTabelaDoPiorPasso() {
        assertThat(PASSO).hasSize(6 * 4);

        Medidas.LinhaPasso noDente = acharPasso("dente de " + Medidas.LARGURA, "fila monotonica");

        assertThat(noDente.porElemento()).isCloseTo(2.0, within(0.05));
        assertThat(noDente.piorPasso()).isEqualTo(Medidas.LARGURA + 1);
        assertThat(noDente.piorSobreALargura()).isCloseTo(1.0, within(0.01));

        // e num fluxo crescente o mesmo algoritmo tem pior passo DOIS
        assertThat(acharPasso("crescente", "fila monotonica").piorPasso()).isEqualTo(2);
    }

    /**
     * E AS DUAS PILHAS tem o pior passo igual a largura em TODO fluxo, sem excecao: a
     * viragem da pilha nao depende da forma do fluxo.
     */
    @Test
    @DisplayName("o pior passo das duas pilhas e a largura nos seis fluxos")
    void asDuasPilhasTemPiorPassoFixo() {
        for (Medidas.LinhaPasso l : PASSO)
            if (l.agregador().equals("duas pilhas")) {
                assertThat(l.piorPasso()).as("em %s", l.fluxo()).isEqualTo(Medidas.LARGURA + 2);
                assertThat(l.porElemento()).as("em %s", l.fluxo()).isCloseTo(3.0, within(0.05));
            }
    }

    /**
     * A COLUNA QUE MUDOU A LEITURA DA TABELA: quantas operacoes antes da PRIMEIRA
     * resposta.
     *
     * <p>Os tres de fluxo respondem depois de ver a janela. A arvore de segmentos precisa
     * do vetor inteiro, e num fluxo que nao acabou isso quer dizer que ela nao serve.
     */
    @Test
    @DisplayName("a arvore precisa do fluxo inteiro antes de responder, e os outros tres precisam da janela")
    void aArvoreNaoEDeFluxo() {
        for (Medidas.LinhaPasso l : PASSO) {
            if (l.agregador().equals("arvore de segmentos"))
                assertThat(l.antesDaPrimeiraResposta()).as("em %s", l.fluxo())
                        .isGreaterThan(Medidas.FLUXO);
            else
                assertThat(l.antesDaPrimeiraResposta()).as("%s em %s", l.agregador(), l.fluxo())
                        .isLessThan(10L * Medidas.LARGURA);
        }
    }

    // ───────────────────── a memoria ─────────────────────

    /**
     * O ACHADO DA MEMORIA: o monte com descarte tardio guarda o FLUXO INTEIRO num fluxo
     * decrescente, porque o lixo nunca chega ao topo.
     *
     * <p>Sao 50.000 elementos guardados para responder uma janela de 64: 781 vezes a
     * largura.
     */
    @Test
    @DisplayName("o monte tardio guarda o fluxo inteiro no decrescente, e a fila guarda um elemento")
    void aTabelaDaMemoria() {
        assertThat(MEMORIA).hasSize(5 * 4);

        assertThat(acharMemoria("decrescente", "monte tardio").maiorGuardado())
                .isEqualTo(Medidas.FLUXO);
        assertThat(acharMemoria("decrescente", "monte tardio").sobreALargura())
                .isGreaterThan(700);

        assertThat(acharMemoria("decrescente", "fila monotonica").maiorGuardado()).isEqualTo(1);

        // e no sorteado ele ja guarda quase o fluxo inteiro tambem
        assertThat(acharMemoria("sorteado", "monte tardio").sobreALargura()).isGreaterThan(500);
    }

    @Test
    @DisplayName("a fila monotonica e as duas pilhas nunca passam da largura, em fluxo nenhum")
    void osDoisQueNaoPassamDaLargura() {
        for (Medidas.LinhaMemoria l : MEMORIA)
            if (l.agregador().equals("fila monotonica") || l.agregador().equals("duas pilhas"))
                assertThat(l.maiorGuardado()).as("%s em %s", l.agregador(), l.fluxo())
                        .isLessThanOrEqualTo(l.largura());
    }

    // ───────────────────── a deriva ─────────────────────

    /**
     * O ACHADO QUE ME CORRIGIU SOBRE QUEM E O JUIZ: na soma, a varredura ingenua NAO
     * serve de juiz, porque ela tambem erra.
     *
     * <p>Num fluxo sem grandezas misturadas, as quatro versoes dao a soma exata e a
     * tabela nao separa nada. Com grandezas misturadas, as quatro erram, cada uma por um
     * tanto, e o unico que nao erra e o BigDecimal.
     */
    @Test
    @DisplayName("num fluxo sorteado as quatro somas sao exatas, e com grandezas misturadas as quatro erram")
    void aVarreduraTambemErra() {
        for (Medidas.LinhaDeriva l : DERIVA)
            if (l.fluxo().equals("sorteado"))
                assertThat(l.maiorDesvio()).as(l.agregador()).isZero();

        assertThat(acharDeriva("um grande a cada 50", "varredura ingenua").maiorDesvio()).isPositive();
        assertThat(acharDeriva("um grande a cada 50", "incremental").maiorDesvio()).isPositive();
        assertThat(acharDeriva("um grande a cada 50", "duas pilhas").maiorDesvio()).isPositive();
    }

    /**
     * E O ACHADO PRINCIPAL DA SOMA: a incremental erra SEIS POR CENTO do valor da
     * janela, e as duas pilhas erram na decima quinta casa.
     */
    @Test
    @DisplayName("a soma incremental erra seis por cento e as duas pilhas erram na decima quinta casa")
    void aIncrementalErraPorCento() {
        Medidas.LinhaDeriva incremental = acharDeriva("um grande a cada 1000", "incremental");
        Medidas.LinhaDeriva duasPilhas = acharDeriva("um grande a cada 1000", "duas pilhas");

        assertThat(incremental.desvioRelativo()).isGreaterThan(0.05);
        assertThat(duasPilhas.desvioRelativo()).isLessThan(1e-14);
        assertThat(incremental.desvioRelativo() / duasPilhas.desvioRelativo()).isGreaterThan(1e12);

        // e a incremental custa duas operacoes por elemento, que e o que a torna tentadora
        assertThat(incremental.porElemento()).isCloseTo(2.0, within(0.05));
        assertThat(duasPilhas.porElemento()).isCloseTo(3.0, within(0.05));
    }

    /**
     * O ERRO ACUMULA, e a tabela mostra ele acumulando.
     *
     * <p>Se fosse so arredondamento, o desvio da janela 19.900 seria do mesmo tamanho do
     * da janela zero. Ele e trinta vezes maior.
     */
    @Test
    @DisplayName("o desvio da incremental cresce da janela zero ate a ultima, e o das duas pilhas nao")
    void aTabelaDoAcumulo() {
        assertThat(ACUMULO).hasSize(7);

        Medidas.LinhaAcumulo primeira = ACUMULO.get(0);
        Medidas.LinhaAcumulo ultima = ACUMULO.get(ACUMULO.size() - 1);

        assertThat(ultima.desvioDaIncremental()).isGreaterThan(10 * primeira.desvioDaIncremental());
        assertThat(ultima.desvioDasDuasPilhas()).isLessThan(2 * primeira.desvioDasDuasPilhas());

        // e na ultima janela a incremental erra uma ordem de grandeza mais
        assertThat(ultima.desvioDaIncremental()).isGreaterThan(10 * ultima.desvioDasDuasPilhas());

        // a primeira janela e a mesma conta nos dois: ai eles empatam
        assertThat(primeira.desvioDaIncremental() + primeira.desvioDasDuasPilhas())
                .isCloseTo(1.0, within(0.01));
    }

    // ───────────────────── achadores ─────────────────────

    private static Medidas.LinhaCusto acharCusto(int largura, String agregador) {
        return CUSTO.stream()
                .filter(l -> l.largura() == largura && l.agregador().equals(agregador))
                .findFirst()
                .orElseThrow();
    }

    private static Medidas.LinhaPasso acharPasso(String fluxo, String agregador) {
        return PASSO.stream()
                .filter(l -> l.fluxo().equals(fluxo) && l.agregador().equals(agregador))
                .findFirst()
                .orElseThrow();
    }

    private static Medidas.LinhaMemoria acharMemoria(String fluxo, String agregador) {
        return MEMORIA.stream()
                .filter(l -> l.fluxo().equals(fluxo) && l.agregador().equals(agregador))
                .findFirst()
                .orElseThrow();
    }

    private static Medidas.LinhaDeriva acharDeriva(String fluxo, String agregador) {
        return DERIVA.stream()
                .filter(l -> l.fluxo().equals(fluxo) && l.agregador().equals(agregador))
                .findFirst()
                .orElseThrow();
    }
}

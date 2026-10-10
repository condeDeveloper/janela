package br.com.conde.janela;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao
 * quebra quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("custo") || comando.equals("tudo")) custo();
        if (comando.equals("passo") || comando.equals("tudo")) passo();
        if (comando.equals("memoria") || comando.equals("tudo")) memoria();
        if (comando.equals("deriva") || comando.equals("tudo")) deriva();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("OS QUATRO ESPERTOS contra a VARREDURA INGENUA, em cinco formas de fluxo.");
        System.out.println();

        var linhas = Medidas.aCorrecao();
        long discordam = linhas.stream().filter(l -> !l.iguaisAoJuiz()).count();

        System.out.printf("  %-16s %10s %-22s %18s %14s%n",
                "fluxo", "largura", "agregador", "igual ao juiz", "diferencas");

        for (var l : linhas)
            if (!l.iguaisAoJuiz() || l.largura() == 256)
                System.out.printf("  %-16s %10d %-22s %18s %14d%n",
                        l.fluxo(), l.largura(), l.agregador(),
                        l.iguaisAoJuiz() ? "sim" : "NAO", l.diferencas());

        System.out.println();
        System.out.printf("  %d comparacoes, %d discordancias.%n", linhas.size(), discordam);
        System.out.println();
    }

    private static void custo() {
        System.out.println();
        System.out.println("O CUSTO POR ELEMENTO conforme a janela cresce, num fluxo sorteado de 50.000.");
        System.out.println();
        System.out.printf("  %10s %-22s %16s %18s %14s%n",
                "largura", "agregador", "operacoes", "por elemento", "pior passo");

        for (var l : Medidas.oCusto())
            System.out.printf(Locale.ROOT, "  %10d %-22s %16d %18.2f %14d%n",
                    l.largura(), l.agregador(), l.operacoes(), l.porElemento(), l.piorPasso());

        System.out.println();
    }

    private static void passo() {
        System.out.println();
        System.out.println("O PIOR PASSO, com janela de " + Medidas.LARGURA
                + ", que e o que o amortizado esconde.");
        System.out.println();
        System.out.printf("  %-16s %-22s %18s %14s %22s %26s%n",
                "fluxo", "agregador", "por elemento", "pior passo", "pior sobre a largura",
                "antes da primeira resposta");

        for (var l : Medidas.oPiorPasso())
            System.out.printf(Locale.ROOT, "  %-16s %-22s %18.2f %14d %22.2f %26d%n",
                    l.fluxo(), l.agregador(), l.porElemento(), l.piorPasso(),
                    l.piorSobreALargura(), l.antesDaPrimeiraResposta());

        System.out.println();
    }

    private static void memoria() {
        System.out.println();
        System.out.println("A MEMORIA: quantos elementos cada um guarda ao mesmo tempo, com janela de 64.");
        System.out.println();
        System.out.printf("  %-16s %-22s %18s %22s%n",
                "fluxo", "agregador", "maior guardado", "sobre a largura");

        for (var l : Medidas.aMemoria())
            System.out.printf(Locale.ROOT, "  %-16s %-22s %18d %22.1f%n",
                    l.fluxo(), l.agregador(), l.maiorGuardado(), l.sobreALargura());

        System.out.println();
    }

    private static void deriva() {
        System.out.println();
        System.out.println("A DERIVA DA SOMA, julgada pela soma EXATA em BigDecimal, com janela de 100.");
        System.out.println();
        System.out.printf("  %-22s %-26s %16s %16s %18s %14s %12s%n",
                "fluxo", "agregador", "maior desvio", "desvio no fim", "desvio relativo",
                "por elemento", "pior passo");

        for (var l : Medidas.aDeriva())
            System.out.printf(Locale.ROOT, "  %-22s %-26s %16.6e %16.6e %18.3e %14.2f %12d%n",
                    l.fluxo(), l.agregador(), l.maiorDesvio(), l.desvioNoFim(), l.desvioRelativo(),
                    l.porElemento(), l.piorPasso());

        System.out.println();
        System.out.println("O ERRO ACUMULA: o desvio da incremental, janela por janela.");
        System.out.println();
        System.out.printf("  %12s %26s %26s%n", "janela", "desvio da incremental", "desvio das duas pilhas");

        for (var l : Medidas.oAcumulo())
            System.out.printf(Locale.ROOT, "  %12d %26.6e %26.6e%n",
                    l.janela(), l.desvioDaIncremental(), l.desvioDasDuasPilhas());

        System.out.println();
    }
}

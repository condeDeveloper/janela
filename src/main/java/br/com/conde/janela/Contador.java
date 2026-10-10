package br.com.conde.janela;

/**
 * O CONTADOR de OPERACOES, e tambem do PIOR PASSO.
 *
 * <p>Operacao, nunca segundo. Segundo muda com a maquina, com o que mais esta rodando e
 * com o humor do coletor de lixo, e a integracao continua roda em tres sistemas.
 *
 * <p>Este contador tem uma coisa a mais do que os outros da serie: ele conta tambem o
 * MAIOR numero de operacoes gastas num unico passo. Essa e a medida que o "amortizado
 * O(1)" esconde, e e ela que decide se o algoritmo serve para uma interface que tem
 * prazo por evento em vez de prazo por lote.
 */
public final class Contador {

    private long operacoes;
    private long noPassoAtual;
    private long piorPasso;
    private long maiorGuardado;
    private long antesDaPrimeira = -1;

    public void operou() {
        operacoes++;
        noPassoAtual++;
    }

    public void operou(long quantas) {
        operacoes += quantas;
        noPassoAtual += quantas;
    }

    /** Fecha o passo atual e comeca outro. Todo agregador chama isto por elemento. */
    public void fechouOPasso() {
        piorPasso = Math.max(piorPasso, noPassoAtual);
        noPassoAtual = 0;
    }

    /**
     * O algoritmo acabou de produzir a PRIMEIRA resposta.
     *
     * <p>E a medida que separa um algoritmo de FLUXO de um que precisa do vetor inteiro.
     * A fila monotonica responde a primeira janela depois de ver w elementos; a arvore de
     * segmentos precisa dos n antes de responder qualquer coisa.
     *
     * <p>Num fluxo que nao acabou, "precisa de n" quer dizer "nao serve".
     */
    public void primeiraResposta() {
        if (antesDaPrimeira < 0) antesDaPrimeira = operacoes;
    }

    public long antesDaPrimeiraResposta() {
        return antesDaPrimeira < 0 ? operacoes : antesDaPrimeira;
    }

    /** Quantos elementos o algoritmo esta guardando agora, para a marca de agua. */
    public void guardando(long quantos) {
        maiorGuardado = Math.max(maiorGuardado, quantos);
    }

    public long operacoes() {
        return operacoes;
    }

    public long piorPasso() {
        return piorPasso;
    }

    public long maiorGuardado() {
        return maiorGuardado;
    }

    public void zerar() {
        operacoes = 0;
        noPassoAtual = 0;
        piorPasso = 0;
        maiorGuardado = 0;
        antesDaPrimeira = -1;
    }

    @Override
    public String toString() {
        return operacoes + " operacoes, pior passo " + piorPasso + ", maior guardado " + maiorGuardado;
    }
}

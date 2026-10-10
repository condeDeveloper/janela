package br.com.conde.janela;

/**
 * OS FLUXOS que as tabelas medem.
 *
 * <p>A forma do fluxo decide tudo neste repositorio, e e esse o assunto. Numa fila
 * monotonica, um fluxo CRESCENTE nunca descarta nada e um fluxo DECRESCENTE descarta
 * tudo a cada passo. Os dois tem o mesmo tamanho e o mesmo algoritmo, e custam coisas
 * diferentes.
 *
 * <p>Todos sao gerados por semente fixa ou por formula. A integracao continua roda em
 * tres sistemas, e "o pior passo custou 500" com fluxos diferentes em cada sistema nao e
 * uma medida.
 */
public final class Fluxos {

    private Fluxos() {
    }

    /**
     * O CONGRUENTE LINEAR do repositorio, com o descarte dos bits de baixo.
     *
     * <p>O deslocamento de 32 bits tira os bits que alternam par e impar a cada passo.
     * Sem ele, "valores sorteados" subiriam e desceriam em xadrez, e um fluxo xadrez e
     * justamente um caso facil para a fila monotonica: ela descartaria um por passo,
     * sempre. O fluxo sorteado deixaria de ser sorteado e viraria mais uma forma fixa.
     */
    public static final class Sorteio {

        private long estado;

        public Sorteio(long semente) {
            this.estado = semente == 0 ? 1 : semente;
        }

        public int proximo() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;

            return (int) (estado >>> 32);
        }

        public double fracao() {
            return Integer.toUnsignedLong(proximo()) / 4294967296.0;
        }
    }

    /** Valores sorteados entre zero e um. O caso medio. */
    public static double[] sorteado(int n, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        double[] fluxo = new double[n];

        for (int i = 0; i < n; i++) fluxo[i] = sorteio.fracao();

        return fluxo;
    }

    /** CRESCENTE: a fila monotonica nunca descarta nada, e o minimo nunca muda de dono cedo. */
    public static double[] crescente(int n) {
        double[] fluxo = new double[n];

        for (int i = 0; i < n; i++) fluxo[i] = i;

        return fluxo;
    }

    /** DECRESCENTE: cada elemento novo descarta TODOS os anteriores da fila. */
    public static double[] decrescente(int n) {
        double[] fluxo = new double[n];

        for (int i = 0; i < n; i++) fluxo[i] = n - i;

        return fluxo;
    }

    /** Todos iguais: o caso em que nenhum desempate importa e nada e descartado. */
    public static double[] constante(int n, double valor) {
        double[] fluxo = new double[n];

        java.util.Arrays.fill(fluxo, valor);

        return fluxo;
    }

    /**
     * DENTE DE SERRA: sobe por um pedaco inteiro e despenca de uma vez.
     *
     * <p>E a forma que produz o PIOR PASSO da fila monotonica. Enquanto ela sobe, a fila
     * enche; quando despenca, um unico elemento descarta a fila toda de uma vez.
     */
    public static double[] denteDeSerra(int n, int dente) {
        double[] fluxo = new double[n];

        for (int i = 0; i < n; i++) fluxo[i] = i % dente;

        return fluxo;
    }

    /**
     * GRANDE E PEQUENO misturados, que e o fluxo que quebra a soma incremental.
     *
     * <p>Um de cada {@code deQuantos} vale um quatrilhao e o resto vale menos de um. Um
     * double tem cinquenta e tres bits de mantissa, o que da uns dezesseis digitos
     * decimais: somar 0,3 a 1e15 perde a terceira casa do 0,3 inteira, e subtrair o 1e15
     * depois nao devolve nada.
     *
     * <p>A primeira versao usava um bilhao, e com ela a deriva aparecia na sexta casa e
     * dava para confundir com arredondamento comum. A grandeza esta aqui para o efeito
     * ser inconfundivel, e ela nao e artificial: e a conta de somar centavos num total em
     * reais de uma empresa grande.
     */
    public static double[] grandeEPequeno(int n, int deQuantos, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        double[] fluxo = new double[n];

        for (int i = 0; i < n; i++) fluxo[i] = i % deQuantos == 0 ? 1e15 * (1 + sorteio.fracao()) : sorteio.fracao();

        return fluxo;
    }

    /** Todas as formas, na ordem em que as tabelas as mostram. */
    public static java.util.Map<String, double[]> todas(int n) {
        java.util.Map<String, double[]> mapa = new java.util.LinkedHashMap<>();

        mapa.put("sorteado", sorteado(n, 1));
        mapa.put("crescente", crescente(n));
        mapa.put("decrescente", decrescente(n));
        mapa.put("constante", constante(n, 7));
        mapa.put("dente de serra", denteDeSerra(n, 64));

        return mapa;
    }
}

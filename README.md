# janela

Agregados em janela deslizante do zero em Java 21: fila monotônica, duas pilhas, monte
com descarte tardio e árvore de segmentos, contra a varredura ingênua.

O repositório tem **dois juízes**, e eles são de naturezas diferentes:

Para o **mínimo**, o juiz é a varredura ingênua. Ela olha a janela inteira a cada passo,
custa n·w e não tem como estar errada. No mínimo não há arredondamento, então a
comparação é de igualdade bit por bit.

Para a **soma**, a varredura ingênua **não serve** de juiz, e essa é a parte interessante:
ela também é ponto flutuante e também arredonda. O juiz é a soma em `BigDecimal`, que não
arredonda.

## O custo não cresce com a janela

```
$ mvn exec:java -Dexec.args="custo"

     largura agregador                     operacoes       por elemento     pior passo
           8 varredura ingenua                349951               7.00              7
           8 fila monotonica                   99998               2.00              8
         512 varredura ingenua              25288879             505.78            511
         512 fila monotonica                   99993               2.00             14
        4096 varredura ingenua             187980975            3759.62           4095
        4096 fila monotonica                   99991               2.00             16
        4096 duas pilhas                      145057               2.90           4098
        4096 arvore de segmentos              600870              12.02          49999
```

A fila monotônica custa **2,00 operações por elemento** com janela de 8 e com janela de
4.096. O número não se mexe.

A árvore de segmentos custa log₂(w) por elemento: 4,12, 7,01, 9,91, 12,02 para janelas de
8, 64, 512 e 4.096. Ela é a única que responde qualquer intervalo, e não só o que está
deslizando, e esse é o preço da generalidade.

## O que o "O(1) amortizado" esconde

```
$ mvn exec:java -Dexec.args="passo"

  fluxo            agregador                    por elemento     pior passo   pior sobre a largura antes da primeira resposta
  crescente        fila monotonica                      1.99              2                   0.00                        512
  dente de serra   fila monotonica                      2.00             65                   0.13                        960
  dente de 512     fila monotonica                      1.99            513                   1.00                        512
  sorteado         duas pilhas                          2.98            514                   1.00                       1025
  crescente        duas pilhas                          2.98            514                   1.00                       1025
  dente de 512     duas pilhas                          2.98            514                   1.00                       1025
  sorteado         arvore de segmentos                  9.91          49999                  97.65                      50008
```

A fila monotônica custa **1,99** operações por elemento num dente de serra do tamanho da
janela, e **513** no passo em que o dente despenca. É a mesma linha.

Para um lote, a média é o que importa. Para uma interface com prazo **por evento**, é o
pior passo, e ele é w.

E os dois "O(1) amortizado" chegam lá por caminhos diferentes. O pior passo da fila
monotônica depende da forma do fluxo: é 2 num fluxo crescente, 65 num dente de 64 e 513
num dente de 512. O das duas pilhas é **514 em todo fluxo, sem exceção**: a viragem da
pilha não depende do que entra, só da janela.

A última coluna é outra pergunta: quantas operações antes da **primeira** resposta. Os
três de fluxo respondem depois de ver a janela. A árvore de segmentos precisa do vetor
inteiro, e num fluxo que não acabou isso quer dizer que ela não serve.

## A memória do descarte tardio

```
$ mvn exec:java -Dexec.args="memoria"

  fluxo            agregador                  maior guardado        sobre a largura
  sorteado         fila monotonica                        14                    0.2
  sorteado         duas pilhas                            63                    1.0
  sorteado         monte tardio                        43921                  686.3
  decrescente      fila monotonica                         1                    0.0
  decrescente      duas pilhas                            63                    1.0
  decrescente      monte tardio                        50000                  781.3
```

Jogar tudo num monte de mínimo e descartar o que expirou **na hora de ler** é a solução
de dez linhas, e ela funciona. O que ela não faz é tirar o que saiu da janela: tirar do
meio de um monte custa caro, então o lixo fica lá dentro até chegar ao topo.

Num fluxo decrescente o lixo nunca chega ao topo, porque o topo é sempre o elemento mais
novo. São **50.000 elementos guardados para responder uma janela de 64**.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `custo`, `passo`, `memoria`, `deriva` e `tudo`.

## O que as medidas me corrigiram

**Na soma, a varredura ingênua não é o juiz. Ela também erra.**

Eu tinha escrito a tabela da soma com a varredura ingênua do lado esquerdo, como juiz,
exatamente como na tabela do mínimo. No mínimo isso está certo: a resposta é um valor
**escolhido** do fluxo, não uma conta sobre eles, e não há o que arredondar.

Na soma há. Somar w números de grandezas diferentes em sequência arredonda, e a varredura
arredonda menos que a incremental só porque recomeça a conta a cada janela.

O juiz virou `BigDecimal`. Todo double é exatamente representável em BigDecimal, então
somar os w valores em BigDecimal dá a soma **exata daqueles bits**. É o único juiz desta
série que não é uma implementação concorrente: é a resposta.

```
$ mvn exec:java -Dexec.args="deriva"

  fluxo                  agregador                      maior desvio    desvio no fim    desvio relativo   por elemento   pior passo
  sorteado               varredura ingenua              0.000000e+00     0.000000e+00          0.000e+00          99.51          100
  sorteado               incremental                    0.000000e+00     0.000000e+00          0.000e+00           2.00          100
  um grande a cada 50    varredura ingenua              3.512794e+00     4.063914e-01          1.240e-15          99.51          100
  um grande a cada 50    incremental                    2.154961e+01     1.609361e+01          8.031e-15           2.00          100
  um grande a cada 50    duas pilhas                    3.593043e+00     4.063914e-01          1.230e-15           3.00          102
  um grande a cada 1000  incremental                    3.277236e+00     1.076156e+00          6.526e-02           2.00          100
  um grande a cada 1000  duas pilhas                    1.586851e+00     0.000000e+00          1.245e-15           3.00          102
```

Num fluxo sorteado, as quatro versões dão a soma **exata** e a tabela não separa nada. A
primeira versão dela usava só esse fluxo, e eu quase concluí que a soma incremental não
tem problema.

**A soma incremental erra 6,5% do valor da janela.**

Somar o que entra e subtrair o que sai é O(1) de verdade, sem amortização. E em ponto
flutuante o que se perde ao somar um valor pequeno a um grande não volta ao subtrair o
grande: 1e15 + 0,03 − 1e15 não dá 0,03, dá zero.

Quando a janela não tem nenhum valor grande, a soma vale umas dezenas, e o erro que
sobrou dos grandes anteriores vale **6,5% dela**. As duas pilhas, que nunca subtraem nada,
erram na décima quinta casa. São treze ordens de grandeza entre as duas colunas.

**E o erro acumula, que é outra coisa de arredondar.**

```
        janela      desvio da incremental     desvio das duas pilhas
             0               5.078433e-01               4.921567e-01
           100               1.184217e+00               1.842166e-01
          1000               2.483968e+00               1.603245e-02
         10000               1.195766e+01               4.234045e-02
         19900               1.609361e+01               4.063914e-01
```

Na janela zero as duas fazem a mesma conta e empatam. Da janela mil em diante a
incremental só sobe. Se fosse arredondamento, o desvio da última janela seria do tamanho
do da primeira.

**A primeira grandeza que escolhi era pequena demais para o efeito aparecer.** A primeira
versão do fluxo misturado usava um bilhão, e com ela a deriva aparecia na sexta casa
decimal, onde dá para confundir com arredondamento comum.

Um double tem 53 bits de mantissa, uns 16 dígitos decimais. Com 1e9, somar 0,03 ainda
guarda quase tudo; com 1e15, não guarda nada. A grandeza do teste virou 1e15, e ela não é
artificial: é a conta de somar centavos num total em reais de uma empresa grande.

**O monte com descarte tardio piora quando a janela DIMINUI.** Eu esperava o contrário.

O pior passo dele é **20.819 operações com janela de 8** e **151 com janela de 4.096**.
Com janela pequena, mais elementos expiram por passo e o lixo se junta no monte sem
chegar ao topo; quando chega, um único passo descarta a pilha inteira.

## Licença

MIT.

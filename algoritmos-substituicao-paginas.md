# Algoritmos de Substituição de Página — Documentação Conceitual

Este documento explica **a teoria** por trás dos três algoritmos de substituição de página implementados neste trabalho: **Segunda Chance**, **NRU** e **DCO** (algoritmo próprio do grupo).

A ideia aqui não é repetir o código — o código já está comentado passo a passo (`NRU.java`, `DCO.java`) e mostra *como* cada coisa é feita. Este documento existe pra explicar *por que* cada coisa é feita daquele jeito, para quem for defender o trabalho na arguição conseguir explicar o raciocínio por trás das escolhas, não só ler linha por linha.

---

## 1. Conceitos-base (pra quem está vendo isso pela primeira vez)

Antes de entrar em cada algoritmo, vale alinhar o vocabulário:

- **Memória virtual**: cada processo enxerga um espaço de endereços próprio, mas nem tudo dele precisa estar fisicamente na RAM o tempo todo. O sistema operacional só mantém na RAM as partes que estão sendo usadas.
- **Página**: um pedaço de memória do processo (nesse trabalho, identificado por um número — o `idPagina`).
- **Quadro (frame)**: um "espaço" físico na RAM onde uma página pode ser colocada. O arquivo de entrada define quantos quadros existem (`Quadros Disponiveis`).
- **Falta de página (Page Fault)**: acontece quando o processo tenta acessar uma página que não está na RAM naquele momento. O sistema precisa trazer essa página para algum quadro — e se todos os quadros já estiverem ocupados, precisa **escolher uma vítima** para sair e abrir espaço. É exatamente essa escolha de vítima que cada algoritmo faz de um jeito diferente.
- **Bit de Referência (R)**: fica em 1 quando a página foi acessada (lida ou escrita) recentemente. É a forma barata que o hardware tem de dizer "essa página está sendo usada" sem precisar guardar um timestamp exato.
- **Bit de Modificação (M)** — também chamado de *dirty bit*: fica em 1 quando a página foi **escrita**. Isso importa porque, se a página for removida da memória, uma página suja (M=1) precisa ser salva em disco antes de sumir (custo extra), enquanto uma página limpa (M=0) pode simplesmente ser descartada.
- **Localidade de referência**: o fenômeno de que, na prática, um processo tende a reacessar as mesmas poucas páginas repetidamente por um tempo, antes de "migrar" para outro conjunto de páginas. Todo algoritmo de substituição bom tenta explorar isso: manter na memória as páginas que fazem parte da localidade atual, e remover as que não fazem mais parte dela.

O problema que os três algoritmos resolvem é sempre o mesmo: **quando a memória está cheia e chega uma página nova, quem sai?** A diferença entre eles é só o critério de decisão.

---

## 2. Segunda Chance (algoritmo obrigatório)

### O problema que ele resolve

O algoritmo mais simples de todos é o **FIFO puro**: a primeira página que entrou na memória é a primeira a sair, sem olhar se ela está sendo muito usada ou não. O problema é óbvio — uma página pode ter entrado há muito tempo e ainda assim estar sendo usada o tempo inteiro (por exemplo, uma variável global acessada em todo loop do programa). O FIFO puro removeria essa página só porque ela é "velha", mesmo sendo importante. Isso gera faltas de página desnecessárias.

### A ideia do Segunda Chance

O Segunda Chance é o FIFO com uma correção de bom senso: **antes de remover a página mais antiga da fila, ele checa se ela foi usada recentemente (bit R).**

- Se a página mais antiga tem **R = 0** (não foi usada desde a última checagem): ela realmente não está sendo importante agora, então é removida — exatamente como no FIFO puro.
- Se a página mais antiga tem **R = 1** (foi usada recentemente): o algoritmo "perdoa" ela. Zera o bit R (para que da próxima vez ela seja avaliada do zero) e a manda para o **final da fila**, como se tivesse acabado de entrar na memória. Ela ganhou uma "segunda chance" de provar que ainda é útil.

Esse processo se repete — olhar a página mais antiga da fila, decidir entre remover ou dar segunda chance — até encontrar alguma com R = 0.

### Por que isso funciona

Pense na fila como um relógio: o algoritmo vai girando por ela, e toda página que "sobrevive" a uma rodada perde seu bit R (ele é zerado). Isso significa que **para uma página se manter viva na memória por muito tempo, ela precisa continuar sendo usada entre as checagens** — não basta ter sido usada uma vez há muito tempo. É uma aproximação simples e barata do LRU (*Least Recently Used*), sem precisar manter uma ordem exata de uso — só um bit por página.

### Ponto de atenção (pior caso)

Se **todas** as páginas da memória tiverem R = 1 no momento da falta, o algoritmo dá a volta completa na fila zerando o R de todo mundo, e acaba removendo a página que está na posição em que começou (a mais antiga), agora com R = 0. Ou seja, no pior caso ele degenera de volta para o comportamento do FIFO puro — mas isso só acontece quando *todas* as páginas realmente foram usadas recentemente, o que já é um sinal de que a memória está pequena para a carga de trabalho atual.

### Trade-offs

| Vantagem | Custo |
|---|---|
| Muito barato de implementar (só precisa do bit R e de uma fila) | Não distingue *quanto* uma página foi usada, só *se* foi usada desde a última checagem |
| Evita o problema óbvio do FIFO puro | Ainda é uma aproximação grosseira do uso real (LRU exato seria mais preciso, mas caro) |

---

## 3. NRU — Not Recently Used (algoritmo de escolha)

### A ideia central

Diferente do Segunda Chance, que só usa 1 bit (R), o NRU usa os **dois bits (R e M)** para separar as páginas residentes em **4 categorias de "quão ruim seria remover essa página agora"**. A vantagem sobre o Segunda Chance é considerar não só se a página foi usada, mas também se ela está suja (removê-la custaria uma escrita em disco).

As 4 classes, da melhor candidata a pior candidata para sair:

| Classe | R | M | Significado | Por que essa prioridade |
|:---:|:---:|:---:|---|---|
| **0** | 0 | 0 | Não usada recentemente, limpa | Melhor vítima: provavelmente não vai fazer falta, e sair não custa nada (não precisa gravar em disco) |
| **1** | 0 | 1 | Não usada recentemente, suja | Ainda uma boa vítima, mas sair custa uma escrita em disco |
| **2** | 1 | 0 | Usada recentemente, limpa | Evita-se remover: provavelmente ainda faz parte da localidade atual |
| **3** | 1 | 1 | Usada recentemente, suja | Pior vítima: perde-se localidade **e** ainda custa gravar em disco |

Quando precisa escolher uma vítima, o algoritmo olha a classe 0 primeiro; se tiver alguém lá, sorteia um deles. Se a classe 0 estiver vazia, olha a classe 1, e assim por diante.

**Por que sortear em vez de escolher de forma determinística?** Porque, dentro da mesma classe, o NRU não tem nenhuma informação adicional para desempatar (todas as páginas daquela classe são "igualmente boas ou ruins" do ponto de vista dele). Calcular um critério de desempate mais fino custaria mais caro — e o NRU é, por definição, um algoritmo que troca precisão por velocidade.

### Por que o bit R precisa ser "esquecido" de tempos em tempos

Esse é o detalhe mais importante para entender o NRU de verdade. Em um sistema real, o hardware **liga** o bit R sempre que a página é acessada, mas **nunca o desliga sozinho**. Se nada resetasse esse bit periodicamente, depois de um tempo de execução quase toda página do sistema teria R = 1 (foi acessada alguma vez desde que o processo começou) — e aí a classificação em 4 classes perderia todo o sentido, porque as classes 0 e 1 ficariam praticamente sempre vazias.

A solução é o sistema operacional programar um **temporizador (clock interrupt)** que, de tempos em tempos, percorre todas as páginas residentes e **zera o bit R de todas elas** (o bit M nunca é zerado nesse processo — ele só é zerado quando a página é de fato salva em disco). Com isso, R passa a significar "foi usada **desde o último tique do relógio**", que é uma informação bem mais útil do que "foi usada alguma vez na vida".

No simulador, esse "tique" é aproximado contando quantas referências já foram processadas (já que não temos timestamps reais em milissegundos) — mas o conceito é exatamente o mesmo de um SO de verdade.

### Trade-offs

| Vantagem | Custo |
|---|---|
| Considera tanto uso recente quanto o custo de gravar em disco | Só 4 níveis de granularidade — duas páginas na mesma classe são tratadas como idênticas, mesmo que uma tenha sido usada há 2 acessos e a outra há 200 |
| Muito barato: só precisa dos bits R/M e reset periódico | A escolha dentro da classe é aleatória, então não é 100% determinístico/reproduzível |

---

## 4. DCO — algoritmo próprio do grupo

> **Atenção:** o DCO **não é um algoritmo clássico de sistemas operacionais** — ele foi criado pelo grupo especificamente para este trabalho, então não existe literatura ou Tanenbaum pra consultar sobre ele. A explicação abaixo é a documentação oficial do algoritmo, com base no comportamento implementado em `DCO.java`.

### A ideia central

O NRU resolve o problema de "quem sai" olhando só o estado *atual* de cada página (seus bits R/M naquele instante). O DCO tenta capturar algo que o NRU não vê: **há quanto tempo, exatamente, uma página está sem ser usada** — não só "usou ou não usou desde o último tique", mas uma contagem contínua.

Para isso, cada página residente carrega um contador — o **tempo parado**:

- A cada novo acesso à memória (seja ele um hit ou uma falta), **todas as páginas que estão na memória** têm seu contador de tempo parado incrementado em 1 — exceto a página que acabou de ser acessada, cujo contador **volta para zero**, porque ela acabou de provar que ainda é relevante.
- Ou seja: o contador de uma página mede, em número de acessos, há quanto tempo ela não é a "protagonista" de um acesso.

### O critério de escolha de vítima

Quando é preciso abrir espaço, o DCO usa uma regra em duas camadas:

1. **Prioridade para páginas "estagnadas"**: existe um limite fixo (**25 acessos sem uso**, no código: `LIMITE_TEMPO_PARADO`). Qualquer página cujo contador já passou desse limite é tratada como candidata prioritária à remoção — a lógica é "se ninguém tocou nessa página nos últimos 25 acessos, ela quase certamente não faz mais parte da localidade atual do processo, então é seguro tirá-la".
2. **Empate / ninguém passou do limite ainda**: se nenhuma página residente ultrapassou o limite (ou seja, a memória inteira ainda está "quente"), o DCO recua para uma regra simples: remove a página com o **maior tempo parado entre todas**, mesmo que ainda esteja abaixo do limite. Alguém precisa sair mesmo assim, e a mais parada entre as que estão é a candidata mais razoável.

Na prática, essas duas regras colapsam numa única implementação: o algoritmo sempre escolhe a página com o **maior tempo parado**; o limite de 25 só é usado depois, para decidir a *mensagem de log* (se a remoção aconteceu porque a página "estava parada há muito tempo" ou porque "era a mais parada do grupo, mesmo sem estar tão parada assim").

### Por que isso é diferente do NRU e do Segunda Chance

- O **Segunda Chance** e o **NRU** enxergam cada página de forma binária (usou / não usou desde a última checagem). O **DCO enxerga um espectro contínuo**: uma página parada há 3 acessos e uma parada há 20 acessos são tratadas de forma diferente, mesmo que ambas tenham "R = 0" no sentido do NRU.
- Isso aproxima o DCO de uma ideia de **LRU (Least Recently Used)** — só que sem o custo de manter uma lista ordenada por uso exato (que seria caro de atualizar). Em vez de saber a ordem exata de todo mundo, o DCO só mantém um contador por página, o que é mais barato que um LRU exato, porém mais caro que o NRU (que só olha 2 bits).
- O **limite de 25** dá ao DCO um comportamento que nem o NRU nem o Segunda Chance têm: a capacidade de reconhecer **páginas efetivamente abandonadas** e priorizá-las de forma explícita, em vez de só relativa às outras. É um comportamento parecido com o que sistemas reais fazem ao identificar páginas "frias" para reclamação de memória (working-set / idle page tracking), simplificado para números redondos.

### Custo (complexidade)

A cada acesso processado, o DCO precisa **percorrer todas as páginas residentes** para incrementar o contador de cada uma — ou seja, o custo por acesso é proporcional ao número de quadros disponíveis (O(quadros)), diferente do NRU e do Segunda Chance, que fazem trabalho O(1) por acesso na maioria dos casos (só mexem na página envolvida) e só percorrem tudo no momento da falta. Isso é o preço pago pela granularidade extra: mais informação por página custa mais trabalho para manter essa informação atualizada.

### Em que cenários o DCO tende a se sair melhor

- **Cargas de trabalho com "picos" bem definidos**: quando o processo usa um grupo de páginas intensamente por um tempo e depois muda completamente de foco para outro grupo, o DCO consegue identificar rapidamente quais páginas do grupo antigo já passaram do limite e removê-las com confiança, enquanto o NRU só sabe dizer "não foi usada desde o último tique" — que pode ser uma janela de tempo curta demais para ter certeza de que a página realmente não vai mais ser usada.
- **Cargas de trabalho com páginas "esquecidas" por muito tempo**: páginas carregadas cedo e nunca mais tocadas (ex: uma rotina de inicialização) acumulam tempo parado alto e ficam marcadas como estagnadas de forma clara — o DCO tem mais confiança nessa decisão do que o NRU, que só vê um R=0 momentâneo (uma página pode ter R=0 nesse tique de clock mas ainda fazer parte da localidade atual só por coincidência de timing).

### Em que cenários o DCO tende a se sair pior (ou não trazer vantagem)

- **Cargas de trabalho muito uniformes/cíclicas**, onde todas as páginas são revisitadas com frequência parecida: nesse caso o contador de tempo parado de todo mundo fica sempre baixo e parecido, então o DCO acaba decidindo por uma diferença pequena de contador — sem trazer vantagem real sobre um algoritmo mais barato — mas ainda pagando o custo O(quadros) por acesso.
- **Memórias com muito poucos quadros**: com pouquíssimos quadros, o limite de 25 acessos raramente é atingido antes de uma página precisar sair de qualquer forma (a memória já força a saída antes disso), então a "camada de prioridade para estagnadas" quase nunca entra em ação de fato — o algoritmo se comporta, na prática, como "sempre remove a mais parada", perdendo parte da sua diferenciação.

---

## 5. Comparativo rápido

| | Segunda Chance | NRU | DCO |
|---|---|---|---|
| Informação usada | 1 bit (R) | 2 bits (R, M) | Contador numérico por página |
| Granularidade | Binária | 4 classes discretas | Contínua (quantos acessos sem uso) |
| Custo por acesso | O(1) | O(1) na maioria dos casos | O(quadros) — atualiza todo mundo a cada acesso |
| Reseta informação periodicamente? | Sim (bit R, ao passar pela fila) | Sim (bit R, via "tique de relógio") | Não reseta — o contador só zera quando a própria página é usada |
| Aproxima-se de qual ideia clássica? | LRU aproximado via fila circular | Prioridade por categoria (uso × sujeira) | LRU aproximado via contador direto |

---

## 6. Como usar este documento junto com o código

A sugestão é: leiam primeiro a seção do algoritmo aqui neste documento, entendam o **porquê** das decisões, e só depois abram `NRU.java` ou `DCO.java` para ver **onde** exatamente cada uma dessas decisões aparece no código (os comentários dentro dos métodos apontam, passo a passo, qual trecho corresponde a qual parte da teoria). Isso deve deixar bem mais fácil qualquer um do grupo responder "por que vocês fizeram assim?" na arguição, em vez de só saber recitar o que o código faz.

Se o Segunda Chance do grupo, quando implementado, tiver alguma variação em relação à descrição da Seção 2 (por exemplo, uma forma diferente de tratar o caso em que todas as páginas têm R=1), vale atualizar esta seção para refletir a implementação real.

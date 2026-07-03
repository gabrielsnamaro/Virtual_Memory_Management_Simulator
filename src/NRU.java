import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

/**
 * Implementação do algoritmo de substituição de páginas NRU (Not Recently Used).
 * <p>
 * <b>Classe independente:</b> esta classe não depende de nenhuma outra classe de
 * algoritmo (Segunda Chance, WSClock, MY, etc). Ela só conhece {@link Pagina},
 * {@link Acesso}, {@link Referencia} e {@link EstatisticasSimulacao}. Isso significa
 * que o Main só precisa chamar {@link #simular(List, int, int)} — se um dia
 * existir um bug na lógica do NRU, o conserto fica isolado aqui dentro, sem
 * risco de afetar os outros algoritmos.
 * </p>
 *
 * <h2>Conceito teórico do NRU</h2>
 * <p>
 * A ideia central do NRU é: não precisamos saber exatamente QUAL página é a
 * menos recentemente usada (isso seria caro de calcular, como no LRU exato).
 * Basta separar as páginas em "baldes" (classes) de prioridade, usando apenas
 * dois bits baratos de manter — R (referenciada) e M (modificada) — e escolher
 * uma vítima aleatória dentro do balde de menor prioridade que não estiver vazio.
 * </p>
 * <p>
 * As 4 classes possíveis, da melhor vítima (menos "perigosa" de remover) para a
 * pior, são:
 * <ul>
 *   <li><b>Classe 0</b> — R=0, M=0: não foi usada recentemente e está limpa.
 *       Remover custa pouco (nem precisa gravar em disco) e provavelmente não
 *       vai fazer falta tão cedo.</li>
 *   <li><b>Classe 1</b> — R=0, M=1: não foi usada recentemente, mas está suja.
 *       Ainda é uma boa vítima, só que remover custa uma escrita em disco.</li>
 *   <li><b>Classe 2</b> — R=1, M=0: foi usada recentemente, mas está limpa.
 *       Provavelmente vai ser acessada de novo em breve — evita-se remover.</li>
 *   <li><b>Classe 3</b> — R=1, M=1: foi usada recentemente e está suja. Pior
 *       cenário para remover (perde-se localidade E ainda custa uma escrita).</li>
 * </ul>
 * </p>
 * <p>
 * <b>Por que o bit R precisa ser "esquecido" de tempos em tempos?</b> Em um
 * sistema real, o bit R é setado pelo hardware sempre que a página é acessada
 * e NUNCA é limpo automaticamente. Se nada o resetasse, depois de um tempo
 * quase toda página do sistema teria R=1 (foi acessada alguma vez), e o NRU
 * degeneraria para "escolher qualquer página limpa ao acaso" — perdendo toda
 * a informação de recência. Por isso o sistema operacional programa um
 * temporizador (clock interrupt) que, periodicamente, percorre todas as
 * páginas residentes e zera o bit R de todas elas. Assim, R passa a significar
 * "foi usada DESDE o último tique do relógio", que é uma informação bem mais
 * útil sobre uso recente.
 * </p>
 * <p>
 * <b>Adaptação para este simulador:</b> como a entrada é uma lista de
 * referências (sem timestamps reais em milissegundos), aproximamos o "tique de
 * relógio" contando quantas referências já foram processadas. A cada
 * {@code intervaloClock} referências processadas, disparamos um reset do bit R
 * em todas as páginas residentes — exatamente como faria o timer interrupt de
 * um SO real. Essa é uma decisão de projeto explícita: caso o grupo prefira
 * outra forma de mapear "Tempo Clock;[ms]" do arquivo de entrada para esse
 * intervalo, basta trocar o valor passado no Main.
 * </p>
 */
public final class NRU {

    private static final Random RANDOM = new Random();

    /** Construtor privado — classe utilitária, não deve ser instanciada. */
    private NRU() {
    }

    /**
     * Sobrecarga de conveniência: usa {@code quadrosDisponiveis} como intervalo
     * padrão de clock (ou seja, um "tique" a cada N acessos, sendo N o número
     * de quadros). É apenas um valor default razoável — ajustem livremente
     * chamando a versão de 3 parâmetros com o intervalo que preferirem.
     *
     * @param referencias        cadeia de acessos à memória, na ordem em que ocorreram
     * @param quadrosDisponiveis número de quadros (frames) de memória física disponíveis
     * @return estatísticas da simulação
     */
    public static EstatisticasSimulacao simular(List<Referencia> referencias, int quadrosDisponiveis) {
        return simular(referencias, quadrosDisponiveis, quadrosDisponiveis);
    }

    /**
     * Executa a simulação completa do algoritmo NRU sobre a cadeia de
     * referências fornecida, incluindo o reset periódico do bit R.
     *
     * @param referencias        cadeia de acessos à memória, na ordem em que ocorreram
     * @param quadrosDisponiveis número de quadros (frames) de memória física disponíveis
     * @param intervaloClock     a cada quantas referências processadas ocorre um
     *                           "tique de relógio" que zera o bit R de todas as
     *                           páginas residentes (deve ser >= 1)
     * @return estatísticas da simulação (acessos, faltas, taxa de faltas e log)
     */
    public static EstatisticasSimulacao simular(List<Referencia> referencias, int quadrosDisponiveis, int intervaloClock) {
        if (intervaloClock < 1) {
            throw new IllegalArgumentException("intervaloClock deve ser >= 1");
        }

        // Memória física simulada: associa o id da página ao seu estado (bits R/M).
        // Um Map é usado porque precisamos localizar rapidamente "essa página já
        // está carregada?" a cada referência (equivalente a consultar a tabela
        // de páginas de um SO real).
        Map<Integer, Pagina> memoria = new LinkedHashMap<>();

        // Guarda apenas a ordem de chegada das páginas — não é usado pela lógica
        // de decisão do NRU (que é baseada em R/M, não em ordem), mas ajuda a
        // manter o log de simulação organizado e é útil para depuração.
        Queue<Integer> ordemCarregamento = new ArrayDeque<>();

        int totalAcessos = 0;
        int totalFaltas = 0;
        List<String> log = new ArrayList<>();

        for (Referencia ref : referencias) {
            totalAcessos++;
            int idPagina = ref.getIdPagina();
            Acesso modo = ref.getTipoAcesso();

            // ---- PASSO 1: a página já está em memória (HIT) ou não (FAULT)? ----
            if (memoria.containsKey(idPagina)) {
                // HIT: não há falta de página. Apenas atualizamos os bits R/M da
                // página, pois ela acabou de ser referenciada (e, se for escrita,
                // também modificada). Isso é o equivalente ao hardware setando os
                // bits automaticamente a cada acesso.
                memoria.get(idPagina).declararAcesso(modo);
                log.add(String.format("HIT   | pagina=%d modo=%s", idPagina, modo));
            } else {
                // FAULT: a página não está residente, então é preciso trazê-la
                // para a memória. Isso é uma Falta de Página (Page Fault).
                totalFaltas++;

                if (memoria.size() >= quadrosDisponiveis) {
                    // Não há quadro livre: é necessário escolher uma vítima entre
                    // as páginas residentes para abrir espaço. É exatamente aqui
                    // que a lógica de classes do NRU entra em ação.
                    int idVitima = escolherVitima(memoria);
                    memoria.remove(idVitima);
                    ordemCarregamento.remove(idVitima);
                    log.add(String.format("FALTA | pagina=%d (memoria cheia, vitima=%d)", idPagina, idVitima));
                } else {
                    // Ainda há quadro livre: nenhuma vítima precisa ser escolhida,
                    // a página simplesmente ocupa um quadro vazio.
                    log.add(String.format("FALTA | pagina=%d (quadro livre disponivel)", idPagina));
                }

                // A página entra em memória já com R=1 (acabou de ser referenciada)
                // e M de acordo com o tipo de acesso (leitura ou escrita).
                memoria.put(idPagina, new Pagina(modo));
                ordemCarregamento.add(idPagina);
            }

            // ---- PASSO 2: verifica se é hora de um "tique de relógio" ----
            // Isso simula o timer interrupt do SO: independentemente de ter
            // ocorrido HIT ou FALTA, a cada 'intervaloClock' acessos processados
            // o relógio dispara e o bit R de TODAS as páginas residentes é
            // zerado. O bit M NUNCA é zerado aqui — ele só seria limpo quando a
            // página fosse de fato salva em disco (o que não é o caso do NRU).
            if (totalAcessos % intervaloClock == 0) {
                for (Pagina pagina : memoria.values()) {
                    pagina.resetBitR();
                }
                log.add(String.format("CLOCK | tique de relogio: bit R zerado em %d pagina(s) residente(s)", memoria.size()));
            }
        }

        double taxaFaltas = totalAcessos == 0 ? 0.0 : (double) totalFaltas / totalAcessos;
        return new EstatisticasSimulacao("NRU", totalAcessos, totalFaltas, taxaFaltas, log);
    }

    /**
     * Classifica as páginas atualmente residentes em memória nas 4 classes do
     * NRU e sorteia uma vítima dentro da classe não vazia de menor índice
     * (menor índice = melhor candidata a ser removida).
     * <p>
     * A conversão de (R, M) para o número da classe é feita tratando R como o
     * bit mais significativo e M como o menos significativo:
     * {@code classe = (R << 1) | M}, o que gera exatamente os valores 0, 1, 2, 3
     * descritos na documentação da classe.
     * </p>
     *
     * @param memoria mapa de páginas atualmente carregadas (id -> Pagina)
     * @return o id da página escolhida para substituição
     * @throws IllegalStateException se a memória estiver vazia (não deveria ocorrer em uso normal)
     */
    @SuppressWarnings("unchecked")
    private static int escolherVitima(Map<Integer, Pagina> memoria) {
        // Um "balde" (lista) para cada uma das 4 classes possíveis.
        List<Integer>[] classes = new List[4];
        for (int i = 0; i < 4; i++) {
            classes[i] = new ArrayList<>();
        }

        // Passo 1: classificar cada página residente no balde correspondente.
        for (Map.Entry<Integer, Pagina> entry : memoria.entrySet()) {
            Pagina pagina = entry.getValue();
            int classe = (pagina.getBitR() << 1) | pagina.getBitM();
            classes[classe].add(entry.getKey());
        }

        // Passo 2: percorrer as classes da menor prioridade (0) para a maior (3)
        // e, assim que encontrar a primeira classe não vazia, sortear uma vítima
        // dentro dela. Não é preciso olhar as classes seguintes — a definição do
        // NRU é justamente "a primeira classe não vazia, na ordem 0..3, já basta".
        for (List<Integer> candidatos : classes) {
            if (!candidatos.isEmpty()) {
                return candidatos.get(RANDOM.nextInt(candidatos.size()));
            }
        }

        // Só chega aqui se 'memoria' estiver vazia, o que não deveria acontecer
        // pois esse método só é chamado quando memoria.size() >= quadrosDisponiveis > 0.
        throw new IllegalStateException("Nao ha paginas residentes para escolher vitima.");
    }
}

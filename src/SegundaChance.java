import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Implementação do algoritmo de substituição de páginas Segunda Chance
 * (obrigatório no Trabalho Prático 2).
 * <p>
 * <b>Classe independente:</b> assim como {@link NRU}, esta classe não depende
 * de nenhuma outra classe de algoritmo (NRU, WSClock, MY, etc). Ela só
 * conhece {@link Pagina}, {@link Acesso}, {@link Referencia} e
 * {@link EstatisticasSimulacao}. O Main só precisa chamar
 * {@link #simular(List, int)} — qualquer ajuste na lógica do Segunda Chance
 * fica isolado aqui dentro, sem risco de afetar os outros algoritmos.
 * </p>
 *
 * <h2>Conceito teórico do Segunda Chance</h2>
 * <p>
 * É uma modificação do FIFO clássico que tenta evitar o principal problema do
 * FIFO puro: substituir uma página só porque ela é a mais antiga, mesmo que
 * ainda esteja sendo usada com frequência. Para isso, as páginas residentes
 * são mantidas em uma fila que reflete a ordem de chegada na memória. Quando
 * é preciso escolher uma vítima, o algoritmo examina a página na cabeça da
 * fila (a mais antiga):
 * </p>
 * <ul>
 *   <li>Se o bit de referência <b>R = 0</b> (não foi acessada desde a última
 *       verificação), ela é removida imediatamente — é a vítima.</li>
 *   <li>Se <b>R = 1</b>, o algoritmo "perdoa" a página: zera o bit R e a
 *       move para o final da fila, como se tivesse acabado de chegar (dando
 *       a ela uma "segunda chance" antes de ser considerada vítima
 *       novamente). O exame então continua com a nova página na cabeça da
 *       fila.</li>
 * </ul>
 * <p>
 * No pior caso, se todas as páginas residentes tiverem R = 1, a fila dá uma
 * volta completa: cada página perde seu bit R e é reenfileirada, até que a
 * primeira página (agora com R = 0, pois já foi "perdoada" nesta mesma
 * varredura) seja finalmente escolhida como vítima. O algoritmo sempre
 * termina, pois há um número finito de páginas na memória.
 * </p>
 * <p>
 * <b>Diferença importante para o NRU:</b> aqui não existe um "tique de
 * relógio" periódico que zera o bit R de todas as páginas de uma vez. O bit R
 * só é zerado sob demanda, página por página, exatamente no momento em que
 * ela é examinada durante a busca por uma vítima.
 * </p>
 */
public final class SegundaChance {

    /** Construtor privado — classe utilitária, não deve ser instanciada. */
    private SegundaChance() {
    }

    /**
     * Executa a simulação completa do algoritmo Segunda Chance sobre a
     * cadeia de referências fornecida.
     *
     * @param referencias        cadeia de acessos à memória, na ordem em que ocorreram
     * @param quadrosDisponiveis número de quadros (frames) de memória física disponíveis
     * @return estatísticas da simulação (acessos, faltas, taxa de faltas e log)
     */
    public static EstatisticasSimulacao simular(List<Referencia> referencias, int quadrosDisponiveis) {
        // Memória física simulada: associa o id da página ao seu estado (bits R/M).
        Map<Integer, Pagina> memoria = new LinkedHashMap<>();

        // Fila que representa a ordem de chegada das páginas na memória. A
        // cabeça (poll) é a página mais antiga; novas páginas e páginas que
        // ganham "segunda chance" vão para o final (offer). Diferente do
        // NRU, aqui essa fila NÃO é só para log — ela é a estrutura de
        // decisão central do algoritmo.
        Queue<Integer> filaChegada = new ArrayDeque<>();

        int totalAcessos = 0;
        int totalFaltas = 0;
        List<String> log = new ArrayList<>();

        for (Referencia ref : referencias) {
            totalAcessos++;
            int idPagina = ref.getIdPagina();
            Acesso modo = ref.getTipoAcesso();

            // ---- PASSO 1: a página já está em memória (HIT) ou não (FAULT)? ----
            if (memoria.containsKey(idPagina)) {
                // HIT: apenas atualiza os bits R/M da página. A posição dela
                // na fila NÃO muda aqui — no Segunda Chance quem reordena a
                // fila é exclusivamente a rotina de substituição, ao
                // encontrar R=1 durante a varredura por uma vítima.
                memoria.get(idPagina).declararAcesso(modo);
                log.add(String.format("HIT   | pagina=%d modo=%s", idPagina, modo));
            } else {
                // FAULT: a página não está residente, é preciso trazê-la.
                totalFaltas++;

                if (memoria.size() >= quadrosDisponiveis) {
                    // Não há quadro livre: percorre a fila aplicando a regra
                    // do Segunda Chance até encontrar uma vítima.
                    int idVitima = escolherVitima(filaChegada, memoria, log);
                    memoria.remove(idVitima);
                    log.add(String.format("FALTA | pagina=%d (memoria cheia, vitima=%d)", idPagina, idVitima));
                } else {
                    // Ainda há quadro livre: nenhuma vítima precisa ser escolhida.
                    log.add(String.format("FALTA | pagina=%d (quadro livre disponivel)", idPagina));
                }

                // A página entra em memória já com R=1 (acabou de ser
                // referenciada) e M de acordo com o tipo de acesso, e vai
                // para o final da fila de chegada.
                memoria.put(idPagina, new Pagina(modo));
                filaChegada.offer(idPagina);
            }
        }

        double taxaFaltas = totalAcessos == 0 ? 0.0 : (double) totalFaltas / totalAcessos;
        return new EstatisticasSimulacao("Segunda Chance", totalAcessos, totalFaltas, taxaFaltas, log);
    }

    /**
     * Percorre a fila de chegada a partir da cabeça aplicando a regra do
     * Segunda Chance: se R=1, zera o bit e reenfileira no final (concedendo
     * a segunda chance, registrada no log); se R=0, essa é a página vítima.
     * <p>
     * Como toda página que ganha segunda chance é removida da cabeça e
     * reinserida no final, e existe pelo menos uma página na memória (a
     * memória está cheia quando este método é chamado), esse laço sempre
     * termina.
     * </p>
     *
     * @param filaChegada fila de ids na ordem de chegada (é modificada por este método)
     * @param memoria     mapa de páginas atualmente carregadas (id -> Pagina)
     * @param log         log da simulação, onde cada segunda chance concedida é registrada
     * @return o id da página escolhida para substituição
     */
    private static int escolherVitima(Queue<Integer> filaChegada, Map<Integer, Pagina> memoria, List<String> log) {
        while (true) {
            int idCandidato = filaChegada.poll();
            Pagina candidato = memoria.get(idCandidato);

            if (candidato.getBitR() == 1) {
                candidato.resetBitR();
                filaChegada.offer(idCandidato); // segunda chance: volta pro final
                log.add(String.format("CLOCK | segunda chance concedida a pagina=%d (R zerado, reenfileirada)", idCandidato));
            } else {
                return idCandidato; // vítima encontrada
            }
        }
    }
}
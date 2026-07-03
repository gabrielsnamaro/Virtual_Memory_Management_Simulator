import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

public final class NRU {

    private static final Random RANDOM = new Random();

    private NRU() {
    }

    /**
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

        Map<Integer, Pagina> memoria = new LinkedHashMap<>();


        Queue<Integer> ordemCarregamento = new ArrayDeque<>();

        int totalAcessos = 0;
        int totalFaltas = 0;
        List<String> log = new ArrayList<>();

        for (Referencia ref : referencias) {
            totalAcessos++;
            int idPagina = ref.getIdPagina();
            Acesso modo = ref.getTipoAcesso();

            if (memoria.containsKey(idPagina)) {

                memoria.get(idPagina).declararAcesso(modo);
                log.add(String.format("HIT   | pagina=%d modo=%s", idPagina, modo));
            } else {

                totalFaltas++;

                if (memoria.size() >= quadrosDisponiveis) {

                    int idVitima = escolherVitima(memoria);
                    memoria.remove(idVitima);
                    ordemCarregamento.remove(idVitima);
                    log.add(String.format("FALTA | pagina=%d (memoria cheia, vitima=%d)", idPagina, idVitima));
                } else {

                    log.add(String.format("FALTA | pagina=%d (quadro livre disponivel)", idPagina));
                }

                memoria.put(idPagina, new Pagina(modo));
                ordemCarregamento.add(idPagina);
            }

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
     *
     * @param memoria mapa de páginas atualmente carregadas (id -> Pagina)
     * @return o id da página escolhida para substituição
     * @throws IllegalStateException se a memória estiver vazia (não deveria ocorrer em uso normal)
     */
    @SuppressWarnings("unchecked")
    private static int escolherVitima(Map<Integer, Pagina> memoria) {

        List<Integer>[] classes = new List[4];
        for (int i = 0; i < 4; i++) {
            classes[i] = new ArrayList<>();
        }

        for (Map.Entry<Integer, Pagina> entry : memoria.entrySet()) {
            Pagina pagina = entry.getValue();
            int classe = (pagina.getBitR() << 1) | pagina.getBitM();
            classes[classe].add(entry.getKey());
        }

        for (List<Integer> candidatos : classes) {
            if (!candidatos.isEmpty()) {
                return candidatos.get(RANDOM.nextInt(candidatos.size()));
            }
        }

        throw new IllegalStateException("Nao ha paginas residentes para escolher vitima.");
    }
}

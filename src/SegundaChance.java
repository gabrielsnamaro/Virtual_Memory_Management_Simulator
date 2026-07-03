import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class SegundaChance {

    private SegundaChance() {
    }

    /**
     *
     * @param referencias        cadeia de acessos à memória, na ordem em que ocorreram
     * @param quadrosDisponiveis número de quadros (frames) de memória física disponíveis
     * @return estatísticas da simulação (acessos, faltas, taxa de faltas e log)
     */
    public static EstatisticasSimulacao simular(List<Referencia> referencias, int quadrosDisponiveis) {
        Map<Integer, Pagina> memoria = new LinkedHashMap<>();

        Queue<Integer> filaChegada = new ArrayDeque<>();

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

                    int idVitima = escolherVitima(filaChegada, memoria, log);
                    memoria.remove(idVitima);
                    log.add(String.format("FALTA | pagina=%d (memoria cheia, vitima=%d)", idPagina, idVitima));
                } else {
                    log.add(String.format("FALTA | pagina=%d (quadro livre disponivel)", idPagina));
                }

                memoria.put(idPagina, new Pagina(modo));
                filaChegada.offer(idPagina);
            }
        }

        double taxaFaltas = totalAcessos == 0 ? 0.0 : (double) totalFaltas / totalAcessos;
        return new EstatisticasSimulacao("Segunda Chance", totalAcessos, totalFaltas, taxaFaltas, log);
    }

    /**
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
                filaChegada.offer(idCandidato);
                log.add(String.format("CLOCK | segunda chance concedida a pagina=%d (R zerado, reenfileirada)", idCandidato));
            } else {
                return idCandidato;
            }
        }
    }
}
import java.util.List;

/**
 * Encapsula o resultado de uma simulação de substituição de páginas,
 * permitindo que o Main exiba as métricas de forma padronizada
 * independentemente do algoritmo executado (Segunda Chance, NRU, MY, etc.).
 */
public class EstatisticasSimulacao {

    private final String algoritmo;
    private final int totalAcessos;
    private final int totalFaltas;
    private final double taxaFaltas;
    private final List<String> log;

    /**
     * @param algoritmo    nome do algoritmo executado (ex: "NRU")
     * @param totalAcessos número total de referências processadas
     * @param totalFaltas  número absoluto de Page Faults ocorridos
     * @param taxaFaltas   totalFaltas / totalAcessos
     * @param log          histórico linha a linha da simulação (útil para depuração/relatório)
     */
    public EstatisticasSimulacao(String algoritmo, int totalAcessos, int totalFaltas,
                                  double taxaFaltas, List<String> log) {
        this.algoritmo = algoritmo;
        this.totalAcessos = totalAcessos;
        this.totalFaltas = totalFaltas;
        this.taxaFaltas = taxaFaltas;
        this.log = log;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public int getTotalAcessos() {
        return totalAcessos;
    }

    public int getTotalFaltas() {
        return totalFaltas;
    }

    public double getTaxaFaltas() {
        return taxaFaltas;
    }

    public List<String> getLog() {
        return log;
    }

    @Override
    public String toString() {
        return String.format(
                "Algoritmo: %s | Acessos: %d | Page Faults: %d | Taxa de Faltas: %.2f%%",
                algoritmo, totalAcessos, totalFaltas, taxaFaltas * 100
        );
    }
}
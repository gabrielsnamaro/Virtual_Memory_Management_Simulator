import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A ideia é que cada página que está na memória tem um contador que
 * mostra há quanto tempo ela não é usada. Toda vez que passa mais um acesso
 * e a página não foi chamada, esse contador sobe. Se a página for usada de
 * novo, o contador dela volta pra zero, porque ela mostrou que ainda é
 * importante.
 * Quando o contador de uma página passa de 25, ela é marcada como "parada
 * há muito tempo" e vira a primeira escolha para sair da memória quando
 * precisar abrir espaço para uma página nova.
 * Se, na hora de escolher quem sai, ninguém ainda passou de 25, a gente
 * simplesmente tira quem está parado há mais tempo entre todos — porque
 * alguém precisa sair mesmo assim.
 */
public final class DCO {

    /**
     * Depois de quantos acessos sem uso a página é considerada "parada há
     * muito tempo" e passa a ser prioridade para sair da memória.
     */
    private static final int LIMITE_TEMPO_PARADO = 25;

    private DCO() {
    }

    /**
     * Roda a simulação do algoritmo DCO: toda vez que precisa escolher
     * alguém para sair, ele olha todas as páginas que estão na memória
     * naquele momento e decide ali na hora.
     *
     * @param referencias        lista de acessos à memória, na ordem em que aconteceram
     * @param quadrosDisponiveis quantos espaços de memória existem disponíveis
     * @return o resultado da simulação (quantos acessos, quantas faltas, etc.)
     */
    public static EstatisticasSimulacao simular(List<Referencia> referencias, int quadrosDisponiveis) {
        // Aqui guardamos as páginas que estão na memória agora.
        Map<Integer, Pagina> memoria = new LinkedHashMap<>();

        // Aqui guardamos, para cada página que está na memória, há quantos
        // acessos ela não é usada.
        Map<Integer, Integer> tempoParado = new HashMap<>();

        int totalAcessos = 0;
        int totalFaltas = 0;
        List<String> log = new ArrayList<>();

        for (Referencia ref : referencias) {
            totalAcessos++;
            int idPagina = ref.getIdPagina();
            Acesso modo = ref.getTipoAcesso();

            // Passo 1: mais um acesso aconteceu, então soma 1 no tempo parado
            // de todo mundo que está na memória agora (a página que vai ser
            // usada agora também recebe +1, mas já vamos zerar ela no passo 2
            // se for o caso).
            for (Integer id : memoria.keySet()) {
                tempoParado.put(id, tempoParado.get(id) + 1);
            }

            if (memoria.containsKey(idPagina)) {
                // A página já está na memória: ela acabou de ser usada, então
                // o tempo parado dela volta para zero.
                memoria.get(idPagina).declararAcesso(modo);
                tempoParado.put(idPagina, 0);
                log.add(String.format("ACERTO | pagina=%d modo=%s (tempo parado zerado)", idPagina, modo));
            } else {
                // A página não está na memória: precisamos trazer ela, e isso
                // conta como uma falta.
                totalFaltas++;

                if (memoria.size() >= quadrosDisponiveis) {
                    // Não tem espaço livre, então precisamos escolher quem sai.
                    int idEscolhido = escolherQuemSai(memoria, tempoParado);
                    boolean passouDoLimite = tempoParado.get(idEscolhido) > LIMITE_TEMPO_PARADO;
                    memoria.remove(idEscolhido);
                    tempoParado.remove(idEscolhido);
                    log.add(String.format(
                            "FALTA | pagina=%d (sem espaco, saiu a pagina=%d, %s)",
                            idPagina, idEscolhido,
                            passouDoLimite ? "estava parada ha muito tempo" : "era a mais parada do grupo"));
                } else {
                    // Ainda tem espaço livre, ninguém precisa sair.
                    log.add(String.format("FALTA | pagina=%d (espaco livre disponivel)", idPagina));
                }

                // A página nova entra com o tempo parado zerado.
                memoria.put(idPagina, new Pagina(modo));
                tempoParado.put(idPagina, 0);
            }
        }

        double taxaFaltas = totalAcessos == 0 ? 0.0 : (double) totalFaltas / totalAcessos;
        return new EstatisticasSimulacao("DCO", totalAcessos, totalFaltas, taxaFaltas, log);
    }

    /**
     * Olha todas as páginas que estão na memória e decide qual delas deve
     * sair.
     * <p>
     * Primeiro procura alguma página que já passou do limite de tempo
     * parado. Se achar mais de uma nessa situação, escolhe a que está parada
     * há mais tempo entre elas. Se não achar nenhuma passada do limite,
     * escolhe simplesmente a que está parada há mais tempo, sem mais
     * critério nenhum.
     * </p>
     *
     * @param memoria     páginas que estão na memória agora
     * @param tempoParado quanto tempo cada página está sem ser usada
     * @return o id da página escolhida para sair
     */
    private static int escolherQuemSai(Map<Integer, Pagina> memoria, Map<Integer, Integer> tempoParado) {
        int idEscolhido = -1;
        int maiorTempo = -1;

        for (Integer id : memoria.keySet()) {
            int tempo = tempoParado.get(id);
            if (tempo > maiorTempo) {
                maiorTempo = tempo;
                idEscolhido = id;
            }
        }

        return idEscolhido;
    }
}
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Classe principal, só pra testar os algoritmos de um jeito simples.
 * <p>
 * Ela lê o arquivo "referencias.txt" (no formato pedido no trabalho),
 * monta a lista de acessos, e mostra um menu onde você escolhe qual
 * algoritmo quer rodar em cima daquela mesma carga de acessos.
 * </p>
 */
public class Main {

    /** Quantos quadros (espaços) de memória o arquivo de entrada informou. */
    private static int quadrosDisponiveis;

    /** O intervalo de clock informado no arquivo (usado só pelo NRU). */
    private static int tempoClock;

    public static void main(String[] args) {
        String caminhoArquivo = args.length > 0 ? args[0] : "referencias.txt";

        List<Referencia> referencias;
        try {
            referencias = lerReferencias(caminhoArquivo);
        } catch (IOException e) {
            System.out.println("Nao consegui ler o arquivo '" + caminhoArquivo + "': " + e.getMessage());
            System.out.println("Confira se o arquivo esta na mesma pasta do programa, ou passe o caminho como argumento.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;

        while (continuar) {
            System.out.println();
            System.out.println("===== MENU =====");
            System.out.println("1 - Rodar NRU");
            System.out.println("2 - Rodar DCO");
            System.out.println("3 - Rodar Segunda Chance");
            System.out.println("4 - Rodar os três e comparar");
            System.out.println("0 - Sair");
            System.out.print("Escolha: ");

            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1":
                    rodarEExibir("NRU", NRU.simular(referencias, quadrosDisponiveis, tempoClock), scanner);
                    break;

                case "2":
                    rodarEExibir("DCO", DCO.simular(referencias, quadrosDisponiveis), scanner);
                    break;

                case "3":
                    rodarEExibir("SegundaChance", SegundaChance.simular(referencias, quadrosDisponiveis), scanner);
                    break;

                case "4":
                    EstatisticasSimulacao nru = NRU.simular(referencias, quadrosDisponiveis, tempoClock);

                    EstatisticasSimulacao dco = DCO.simular(referencias, quadrosDisponiveis);

                    EstatisticasSimulacao segunda = SegundaChance.simular(referencias, quadrosDisponiveis);

                    compararResultados(nru, dco, segunda);

                    break;

                case "0":
                    
                    continuar = false;
                    System.out.println("Ate mais, Jpob!");
                    break;

                default:
                    System.out.println("Opcao invalida, tenta de novo.");
            }
        }


    }


    private static void imprimirCabecalho(String titulo) {

        System.out.println();
        System.out.println("==============================================================");
        System.out.printf("%34s%n", titulo);
        System.out.println("==============================================================");
    }

    private static void imprimirResumo(EstatisticasSimulacao resultado) {

        int hits = resultado.getTotalAcessos() - resultado.getTotalFaltas();

        System.out.println();

        System.out.println("+------------------------------------------------------+");
        System.out.printf("| %-28s %17s |%n", "Algoritmo", resultado.getAlgoritmo());
        System.out.printf("| %-28s %17d |%n", "Total de acessos", resultado.getTotalAcessos());
        System.out.printf("| %-28s %17d |%n", "Hits", hits);
        System.out.printf("| %-28s %17d |%n", "Page Faults", resultado.getTotalFaltas());
        System.out.printf("| %-28s %16.2f%% |%n", "Taxa de faltas", resultado.getTaxaFaltas() * 100);

        System.out.println("+------------------------------------------------------+");
    }

    private static void compararResultados(EstatisticasSimulacao nru, EstatisticasSimulacao dco, EstatisticasSimulacao segundaChance) {

        imprimirCabecalho("COMPARAÇÃO DOS ALGORITMOS");

        System.out.printf("%-20s %-10s %-10s %-12s%n",
                "Algoritmo",
                "Faults",
                "Hits",
                "Taxa");

        System.out.println("----------------------------------------------------------");

        imprimirLinhaComparacao(nru);
        imprimirLinhaComparacao(dco);
        imprimirLinhaComparacao(segundaChance);

        System.out.println("----------------------------------------------------------");

        EstatisticasSimulacao melhor = nru;

        if (dco.getTotalFaltas() < melhor.getTotalFaltas())
            melhor = dco;

        if (segundaChance.getTotalFaltas() < melhor.getTotalFaltas())
            melhor = segundaChance;

        System.out.println();
        System.out.println("Melhor algoritmo para esta carga:");
        System.out.println(">> " + melhor.getAlgoritmo());
    }

    private static void imprimirLinhaComparacao(EstatisticasSimulacao e) {

        int hits = e.getTotalAcessos() - e.getTotalFaltas();

        System.out.printf("%-20s %-10d %-10d %9.2f%%%n",
                e.getAlgoritmo(),
                e.getTotalFaltas(),
                hits,
                e.getTaxaFaltas() * 100);
    }

    /**
     * Mostra o resumo do resultado de uma simulação e pergunta se a pessoa
     * quer ver o log linha por linha também.
     *
     * @param nome       nome do algoritmo, só para identificar na saída
     * @param resultado  resultado retornado pelo método simular(...)
     */
    private static void rodarEExibir(String nome, EstatisticasSimulacao resultado, Scanner scanner) {

        imprimirCabecalho("RESULTADO - " + nome);

        imprimirResumo(resultado);

        System.out.println();

        System.out.print("Deseja visualizar o log detalhado? (S/N): ");

        String resposta = scanner.nextLine().trim();

        if (resposta.equalsIgnoreCase("S")) {

            System.out.println();

            System.out.println("============== LOG DA EXECUÇÃO ==============");

            int contador = 1;

            for (String linha : resultado.getLog()) {

                System.out.printf("[%03d] %s%n", contador++, linha);

            }

            System.out.println("=============================================");
        }

    }

    /**
     * Lê o arquivo de referências no formato do trabalho:
     * <pre>
     * Quadros Disponiveis;[Valor]
     * Tempo Clock;[Valor em ms]
     * ID Pagina;Tipo Acesso(R/W)
     * ID Pagina;Tipo Acesso(R/W)
     * ...
     * </pre>
     *
     * @param caminho caminho do arquivo a ser lido
     * @return lista de referências prontas para passar aos algoritmos
     * @throws IOException se o arquivo não existir ou não puder ser lido
     */
    private static List<Referencia> lerReferencias(String caminho) throws IOException {
        List<Referencia> referencias = new ArrayList<>();

        try (BufferedReader leitor = new BufferedReader(new FileReader(caminho))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty()) {
                    continue;
                }

                String[] partes = linha.split(";");

                if (partes[0].equalsIgnoreCase("Quadros Disponiveis")) {
                    quadrosDisponiveis = Integer.parseInt(partes[1].trim());
                } else if (partes[0].equalsIgnoreCase("Tempo Clock")) {
                    tempoClock = Integer.parseInt(partes[1].trim());
                } else {
                    // Linha de acesso: "idPagina;R" ou "idPagina;W"
                    int idPagina = Integer.parseInt(partes[0].trim());
                    String tipo = partes[1].trim().toUpperCase();

                    Acesso modo = tipo.equals("W") ? Acesso.MODIFICACAO : Acesso.REFERENCIA;
                    referencias.add(new Referencia(idPagina, modo));
                }
            }
        }

        // Se o arquivo nao trouxer "Tempo Clock", usamos os quadros disponiveis
        // como intervalo padrao (mesma logica do overload de conveniencia do NRU).
        if (tempoClock <= 0) {
            tempoClock = quadrosDisponiveis;
        }

        return referencias;
    }
}

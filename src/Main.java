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
            System.out.println("3 - Rodar os dois e comparar");
            System.out.println("0 - Sair");
            System.out.print("Escolha: ");

            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1":
                    rodarEExibir("NRU", NRU.simular(referencias, quadrosDisponiveis, tempoClock));
                    break;

                case "2":
                    rodarEExibir("DCO", DCO.simular(referencias, quadrosDisponiveis));
                    break;

                case "3":
                    EstatisticasSimulacao resultadoNru = NRU.simular(referencias, quadrosDisponiveis, tempoClock);
                    EstatisticasSimulacao resultadoDco = DCO.simular(referencias, quadrosDisponiveis);
                    rodarEExibir("NRU", resultadoNru);
                    rodarEExibir("DCO", resultadoDco);
                    break;

                case "0":
                    continuar = false;
                    System.out.println("Ate mais!");
                    break;

                default:
                    System.out.println("Opcao invalida, tenta de novo.");
            }
        }

        scanner.close();
    }

    /**
     * Mostra o resumo do resultado de uma simulação e pergunta se a pessoa
     * quer ver o log linha por linha também.
     *
     * @param nome       nome do algoritmo, só para identificar na saída
     * @param resultado  resultado retornado pelo método simular(...)
     */
    private static void rodarEExibir(String nome, EstatisticasSimulacao resultado) {
        System.out.println();
        System.out.println("--- Resultado " + nome + " ---");
        System.out.println(resultado);

        System.out.print("Ver o log detalhado? (s/n): ");
        Scanner scanner = new Scanner(System.in);
        String resposta = scanner.nextLine().trim().toLowerCase();

        if (resposta.equals("s")) {
            for (String linha : resultado.getLog()) {
                System.out.println(linha);
            }
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

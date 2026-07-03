public class Pagina {

    /**
     * Quantidade de bits utilizados para armazenar a idade da página.
     * Define o número de ciclos de clock que serão rastreados no histórico.
     * Valor fixo em 3, resultando em idade máxima 7 (binário 111).
     */
    private static final int QTD_BITS_IDADE = 3;

    /**
     * Bit de referência (R) – indica se a página foi acessada (leitura ou escrita)
     * no ciclo atual. Valor 1 significa acessada, 0 significa não acessada.
     */
    private int bitR;

    /**
     * Bit de modificação (M) – indica se a página foi alterada (escrita) desde
     * a última sincronização com o disco. Valor 1 significa modificada, 0 significa
     * inalterada.
     */
    private int bitM;

    /**
     * Idade da página, representada por um inteiro com até {@link #QTD_BITS_IDADE} bits.
     * O bit mais significativo (posição QTD_BITS_IDADE-1) reflete o bitR do ciclo atual
     * após a chamada de {@link #envelhecer()}. Os bits menos significativos guardam
     * o histórico dos ciclos anteriores.
     */
    private int idade;

    /**
     * Inicializa a página com o modo de acesso especificado.
     * <p>
     * Define os bits R e M conforme o modo, e inicializa a idade com o bitR no
     * bit mais significativo, representando o estado inicial da página.
     * </p>
     *
     * @param modo o modo de acesso inicial (GENERICO, REFERENCIA ou MODIFICACAO)
     */
    private void init(Acesso modo) {
        this.bitR = modo.getBitR();
        this.bitM = modo.getBitM();
        this.idade = this.bitR << (QTD_BITS_IDADE - 1);
    }

    /**
     * Construtor padrão – cria uma página com modo de acesso genérico
     * (sem referência nem modificação).
     */
    public Pagina() {
        init(Acesso.GENERICO);
    }

    /**
     * Construtor que permite especificar o modo de acesso inicial da página.
     *
     * @param modo o modo de acesso (REFERENCIA ou MODIFICACAO, ou GENERICO)
     */
    public Pagina(Acesso modo) {
        init(modo);
    }

    /**
     * Registra um acesso à página no modo especificado.
     * <p>
     * Atualiza os bits R e M de acordo com o modo (REFERENCIA ou MODIFICACAO).
     * Além disso, força o bit mais significativo da idade a ser 1, indicando que
     * a página foi referenciada neste ciclo de clock.
     * </p>
     * <p>
     * <b>Nota:</b> Este método deve ser chamado sempre que a página for referenciada
     * ou modificada, antes da chamada de {@link #envelhecer()}.
     * </p>
     *
     * @param modo o tipo de acesso realizado (REFERENCIA para leitura, MODIFICACAO para escrita)
     */
    public void declararAcesso(Acesso modo) {
        this.bitR = modo.getBitR();
        this.bitM = this.bitM | modo.getBitM();   // M é sticky: só liga, nunca desliga sozinho
        idade = idade | (1 << (QTD_BITS_IDADE - 1));
    }

    /**
     * Executa um ciclo de envelhecimento (aging) da página.
     * <p>
     * Este método deve ser chamado a cada ciclo de clock do sistema.
     * Ele realiza as seguintes operações:
     * <ol>
     *   <li>Desloca a idade atual um bit para a direita (descartando o bit mais antigo);</li>
     *   <li>Insere o bitR atual no bit mais significativo da idade;</li>
     *   <li>Reseta o bitR para 0, pois ele já foi consumido no histórico.</li>
     * </ol>
     * O bitM não é alterado automaticamente; deve ser resetado manualmente
     * quando a página for sincronizada com o disco.
     * </p>
     */
    public void envelhecer() {
        idade = (idade >> 1) | (bitR << (QTD_BITS_IDADE - 1));
        bitR = 0;
    }

    /**
     * Reseta o bit de referência (R) para 0.
     * <p>
     * Este método pode ser utilizado para limpar o bitR manualmente, por exemplo,
     * quando a página for selecionada para substituição ou após uma operação de
     * sincronização.
     * </p>
     */
    public void resetBitR() {
        this.bitR = 0;
    }

    /**
     * Reseta o bit de modificação (M) para 0.
     * <p>
     * Deve ser chamado após a página ser salva em disco, indicando que não há
     * mais alterações pendentes.
     * </p>
     */
    public void resetBitM() {
        this.bitM = 0;
    }

    /**
     * Retorna o valor atual do bit de referência (R).
     *
     * @return 1 se a página foi referenciada no ciclo atual, 0 caso contrário
     */
    public int getBitR() {
        return bitR;
    }

    /**
     * Retorna o valor atual do bit de modificação (M).
     *
     * @return 1 se a página foi modificada, 0 caso contrário
     */
    public int getBitM() {
        return bitM;
    }

    /**
     * Retorna o valor atual da idade da página.
     * <p>
     * O valor representa o histórico de acessos nos últimos N ciclos, onde N =
     * {@link #QTD_BITS_IDADE}. O bit mais significativo é o mais recente.
     * </p>
     *
     * @return a idade atual (valor inteiro entre 0 e 2^QTD_BITS_IDADE - 1)
     */
    public int getIdade() {
        return idade;
    }
}
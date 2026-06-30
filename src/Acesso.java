public enum Acesso {

    /**
     * Modo genérico – página sem acesso recente e sem modificação.
     * Bits: R=0, M=0.
     */
    GENERICO(0, 0),

    /**
     * Modo de referência – página foi apenas lida (acessada sem modificação).
     * Bits: R=1, M=0.
     */
    REFERENCIA(1, 0),

    /**
     * Modo de modificação – página foi escrita (acessada e modificada).
     * Bits: R=1, M=1.
     */
    MODIFICACAO(1, 1);

    /**
     * Bit de referência (R): 1 se a página foi acessada, 0 caso contrário.
     */
    private final int bitR;

    /**
     * Bit de modificação (M): 1 se a página foi modificada, 0 caso contrário.
     */
    private final int bitM;

    /**
     * Construtor privado do enum, que associa um par de bits a cada constante.
     *
     * @param bitR valor do bit de referência (0 ou 1)
     * @param bitM valor do bit de modificação (0 ou 1)
     */
    private Acesso(int bitR, int bitM) {
        this.bitR = bitR;
        this.bitM = bitM;
    }

    /**
     * Retorna o valor do bit de referência (R) associado ao modo de acesso.
     *
     * @return 1 se o modo indica referência, 0 caso contrário
     */
    public int getBitR() {
        return this.bitR;
    }

    /**
     * Retorna o valor do bit de modificação (M) associado ao modo de acesso.
     *
     * @return 1 se o modo indica modificação, 0 caso contrário
     */
    public int getBitM() {
        return this.bitM;
    }
}
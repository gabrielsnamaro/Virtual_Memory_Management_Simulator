public class Referencia {

    private final int idPagina;
    private final Acesso tipoAcesso;

    /**
     * Cria uma referência de acesso à memória.
     *
     * @param idPagina   identificador da página acessada
     * @param tipoAcesso modo de acesso (REFERENCIA para leitura, MODIFICACAO para escrita)
     */
    public Referencia(int idPagina, Acesso tipoAcesso) {
        this.idPagina = idPagina;
        this.tipoAcesso = tipoAcesso;
    }

    public int getIdPagina() {
        return idPagina;
    }

    public Acesso getTipoAcesso() {
        return tipoAcesso;
    }
}

/**
 * Representa uma única linha do arquivo referencias.txt: o id da página
 * acessada e o tipo de acesso realizado (leitura ou escrita).
 * <p>
 * Classe auxiliar mínima — se já existir algo equivalente no restante do
 * projeto (ex: gerado pelo parser do arquivo de entrada), utilizem essa
 * versão apenas como referência e ajustem os nomes conforme necessário.
 * </p>
 */
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
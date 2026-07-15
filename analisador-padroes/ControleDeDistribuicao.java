class ControleDeDistribuicao {
    private static ControleDeDistribuicao instanciaUnica;
    
    private ControleDeDistribuicao() {} 
    
    public static ControleDeDistribuicao obterControle() {
        if (instanciaUnica == null) {
            instanciaUnica = new ControleDeDistribuicao();
        }
        return instanciaUnica;
    }
}
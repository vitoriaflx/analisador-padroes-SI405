abstract class ProcessamentoDeCarga {
    
    public void executarExpedicao() {
        separarProdutos();
        carregarCaminhao();
        emitirNotaFiscal();
    }

    private void separarProdutos() {}
    private void emitirNotaFiscal() {}
    
    protected abstract void carregarCaminhao();
}
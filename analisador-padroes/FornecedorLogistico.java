interface Transporte {}
class CaminhaoBau implements Transporte {}

class FornecedorLogistico {
    
    public Transporte alocarVeiculo() {
        return new CaminhaoBau();
    }
}
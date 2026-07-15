package br.unicamp;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import java.io.File;

public class AnalisadorEstatico {
    public static void main(String[] args) {
        try {
            String caminho = "C:\\Users\\UNICAMP\\OneDrive\\Attachments\\DINAMICAP2\\analisador-padroes\\ControleDeDistribuicao.java";
            File arquivo = new File(caminho);
            
            CompilationUnit cu = StaticJavaParser.parse(arquivo);
            boolean encontrouAlgumPadrao = false;

            for (ClassOrInterfaceDeclaration classe : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                String nomeClasse = classe.getNameAsString();
                
                boolean temConstrutorPrivado = !classe.getConstructors().isEmpty() && 
                        classe.getConstructors().stream().anyMatch(c -> c.isPrivate());
                boolean temInstanciaEstatica = classe.getFields().stream()
                        .anyMatch(f -> f.isStatic() && f.isPrivate() && f.getElementType().toString().equals(nomeClasse));
                boolean temMetodoAcessoEstatico = classe.getMethods().stream()
                        .anyMatch(m -> m.isPublic() && m.isStatic() && m.getType().toString().equals(nomeClasse));

                if (temConstrutorPrivado && temInstanciaEstatica && temMetodoAcessoEstatico) {
                    encontrouAlgumPadrao = true;
                    System.out.println("[PADRÃO DETECTADO] Singleton");
                    System.out.println("Elementos identificados: Construtor privado, atributo estático do tipo '" + nomeClasse + "' e método estático de acesso encontrados.");
                    System.out.println("Vantagem neste contexto: Garante que apenas uma instância de '" + nomeClasse + "' exista, centralizando o controle e economizando memória caso seja um recurso compartilhado.");
                    System.out.println("Risco/desvantagem neste contexto: Introduz um estado global para '" + nomeClasse + "', o que pode dificultar testes unitários e acoplamento excessivo.");
                    System.out.println("--------------------------------------------------");
                }

                boolean temMetodoCriador = classe.getMethods().stream().anyMatch(m -> {
                    boolean retornaTipoDiferente = !m.getType().isVoidType() && !m.getType().toString().equals(nomeClasse);
                    boolean possuiReturnNew = m.getBody().isPresent() && m.getBody().get().getStatements().stream()
                            .anyMatch(stmt -> stmt.isReturnStmt() && stmt.asReturnStmt().getExpression().isPresent() 
                                    && stmt.asReturnStmt().getExpression().get().isObjectCreationExpr());
                    return retornaTipoDiferente && possuiReturnNew;
                });

                if (temMetodoCriador) {
                    encontrouAlgumPadrao = true;
                    System.out.println("[PADRÃO DETECTADO] Factory Method");
                    System.out.println("Elementos identificados: Método criador na classe '" + nomeClasse + "' que encapsula a instrução 'return new ...' para instanciar outro objeto.");
                    System.out.println("Vantagem neste contexto: A classe '" + nomeClasse + "' centraliza a lógica de criação, permitindo alterar a classe concreta instanciada sem quebrar o código cliente.");
                    System.out.println("Risco/desvantagem neste contexto: Pode aumentar a complexidade se houver apenas um produto simples a ser criado, gerando classes desnecessárias para a '" + nomeClasse + "'.");
                    System.out.println("--------------------------------------------------");
                }

                boolean ehClasseAbstrata = classe.isAbstract();
                boolean temMetodoAbstrato = classe.getMethods().stream().anyMatch(m -> m.isAbstract());
                boolean temMetodoConcreto = classe.getMethods().stream().anyMatch(m -> !m.isAbstract() && m.getBody().isPresent());

                if (ehClasseAbstrata && temMetodoAbstrato && temMetodoConcreto) {
                    encontrouAlgumPadrao = true;
                    System.out.println("[PADRÃO DETECTADO] Template Method");
                    System.out.println("Elementos identificados: Classe abstrata '" + nomeClasse + "' possuindo métodos abstratos e concretos.");
                    System.out.println("Vantagem neste contexto: A classe '" + nomeClasse + "' reaproveita o código dos passos em comum, garantindo que a ordem de execução não seja alterada acidentalmente.");
                    System.out.println("Risco/desvantagem neste contexto: O algoritmo dita regras rígidas. Pode ser inflexível se houver necessidade de mudar a ordem de execução no futuro para '" + nomeClasse + "'.");
                    System.out.println("--------------------------------------------------");
                }
            }
            
            if (!encontrouAlgumPadrao) {
                System.out.println("Nenhum padrão de projeto estrutural foi detectado neste arquivo.");
            }

        } catch (Exception e) {
            System.out.println("Erro na leitura: " + e.getMessage());
        }
    }
}
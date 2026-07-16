package br.unicamp;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnalisadorEstatico {

    public static void main(String[] args) {
        List<String> caminhos = new ArrayList<>();

        if (args.length > 0) {
            for (String arg : args) {
                caminhos.add(arg);
            }
        } else {
            caminhos.add("ControleDeDistribuicao.java");
            caminhos.add("FornecedorLogistico.java");
            caminhos.add("FluxoDeCarga.java");
        }

        for (String caminho : caminhos) {
            analisarArquivo(caminho);
        }
    }

    private static void analisarArquivo(String caminho) {
        File arquivo = resolverArquivo(caminho);

        if (arquivo == null) {
            System.out.println("Arquivo não encontrado: " + caminho);
            System.out.println("==================================================");
            return;
        }

        System.out.println("### Analisando: " + arquivo.getName());

        try {
            CompilationUnit cu = StaticJavaParser.parse(arquivo);
            boolean encontrouAlgumPadrao = false;

            for (ClassOrInterfaceDeclaration classe : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                encontrouAlgumPadrao |= detectarSingleton(classe);
                encontrouAlgumPadrao |= detectarFactoryMethod(classe);
                encontrouAlgumPadrao |= detectarTemplateMethod(classe);
            }

            if (!encontrouAlgumPadrao) {
                System.out.println("Nenhum padrão de projeto estrutural foi detectado neste arquivo.");
            }
        } catch (Exception e) {
            System.out.println("Erro na leitura: " + e.getMessage());
        }

        System.out.println("==================================================");
    }

    private static File resolverArquivo(String caminho) {
        File arquivo = new File(caminho);
        if (arquivo.exists()) {
            return arquivo;
        }
        File alternativo = new File("analisador-padroes" + File.separator + caminho);
        if (alternativo.exists()) {
            return alternativo;
        }
        return null;
    }

    private static boolean detectarSingleton(ClassOrInterfaceDeclaration classe) {
        String nomeClasse = classe.getNameAsString();

        boolean temConstrutorPrivado = !classe.getConstructors().isEmpty()
                && classe.getConstructors().stream().anyMatch(c -> c.isPrivate());

        FieldDeclaration campoAutoReferencia = classe.getFields().stream()
                .filter(f -> f.isStatic() && f.isPrivate()
                        && f.getElementType().toString().equals(nomeClasse))
                .findFirst().orElse(null);

        MethodDeclaration metodoAcesso = classe.getMethods().stream()
                .filter(m -> m.isPublic() && m.isStatic()
                        && m.getType().toString().equals(nomeClasse))
                .findFirst().orElse(null);

        if (!(temConstrutorPrivado && campoAutoReferencia != null && metodoAcesso != null)) {
            return false;
        }

        String nomeCampo = campoAutoReferencia.getVariable(0).getNameAsString();
        String nomeMetodo = metodoAcesso.getNameAsString();

        System.out.println("[PADRÃO DETECTADO] Singleton");
        System.out.println("Elementos identificados: Construtor privado, campo estático privado '" + nomeCampo
                + "' do tipo '" + nomeClasse + "' e método de acesso estático '" + nomeMetodo + "()'.");
        System.out.println("Vantagem neste contexto: O acesso à instância acontece exclusivamente por '" + nomeMetodo
                + "()', garantindo que exista uma única instância de '" + nomeClasse
                + "' compartilhada por todo o programa.");
        System.out.println("Risco/desvantagem neste contexto: O campo estático '" + nomeCampo
                + "' funciona como estado global de '" + nomeClasse
                + "', o que dificulta testes unitários (não é possível substituí-lo por um mock).");
        System.out.println("--------------------------------------------------");
        return true;
    }

    private static boolean detectarFactoryMethod(ClassOrInterfaceDeclaration classe) {
        String nomeClasse = classe.getNameAsString();

        for (MethodDeclaration m : classe.getMethods()) {
            String tipoRetorno = m.getType().toString();
            boolean retornaTipoDiferente = !m.getType().isVoidType() && !tipoRetorno.equals(nomeClasse);

            if (!retornaTipoDiferente || m.getBody().isEmpty()) {
                continue;
            }

            List<String> tiposCriados = new ArrayList<>();
            m.getBody().get().getStatements().stream()
                    .filter(stmt -> stmt.isReturnStmt() && stmt.asReturnStmt().getExpression().isPresent()
                            && stmt.asReturnStmt().getExpression().get().isObjectCreationExpr())
                    .forEach(stmt -> {
                        ObjectCreationExpr criacao = stmt.asReturnStmt().getExpression().get().asObjectCreationExpr();
                        tiposCriados.add(criacao.getType().asString());
                    });

            if (tiposCriados.isEmpty()) {
                continue;
            }

            String nomeMetodo = m.getNameAsString();
            String concretos = String.join(", ", tiposCriados);

            System.out.println("[PADRÃO DETECTADO] Factory Method");
            System.out.println("Elementos identificados: Método '" + nomeMetodo + "()' declara retorno abstrato '"
                    + tipoRetorno + "' mas instancia o tipo concreto '" + concretos + "' via 'return new'.");
            System.out.println("Vantagem neste contexto: O cliente depende apenas de '" + tipoRetorno
                    + "'; é possível trocar a implementação concreta '" + concretos + "' dentro de '" + nomeMetodo
                    + "()' sem alterar quem a consome.");
            System.out.println("Risco/desvantagem neste contexto: Como '" + nomeMetodo + "()' fixa o tipo '"
                    + concretos + "', qualquer nova variação exige alterar este método ou criar subclasses, "
                    + "o que pode ser exagerado se '" + tipoRetorno + "' tiver apenas uma implementação.");
            System.out.println("--------------------------------------------------");
            return true;
        }
        return false;
    }

    private static boolean detectarTemplateMethod(ClassOrInterfaceDeclaration classe) {
        String nomeClasse = classe.getNameAsString();

        if (!classe.isAbstract()) {
            return false;
        }

        List<String> metodosAbstratos = new ArrayList<>();
        int metodosConcretos = 0;

        for (MethodDeclaration m : classe.getMethods()) {
            if (m.isAbstract()) {
                metodosAbstratos.add(m.getNameAsString() + "()");
            } else if (m.getBody().isPresent()) {
                metodosConcretos++;
            }
        }

        if (metodosAbstratos.isEmpty() || metodosConcretos == 0) {
            return false;
        }

        String passosAbstratos = String.join(", ", metodosAbstratos);

        System.out.println("[PADRÃO DETECTADO] Template Method");
        System.out.println("Elementos identificados: Classe abstrata '" + nomeClasse + "' com " + metodosConcretos
                + " método(s) concreto(s) e " + metodosAbstratos.size() + " passo(s) abstrato(s): " + passosAbstratos
                + ".");
        System.out.println("Vantagem neste contexto: '" + nomeClasse + "' fixa o esqueleto do algoritmo nos "
                + metodosConcretos + " método(s) concreto(s) e reaproveita esse código em todas as subclasses.");
        System.out.println("Risco/desvantagem neste contexto: As subclasses são obrigadas a implementar '"
                + passosAbstratos + "' na ordem imposta por '" + nomeClasse
                + "'; mudar essa ordem exige alterar a classe base e afeta todas as subclasses.");
        System.out.println("--------------------------------------------------");
        return true;
    }
}
import core.Escalonador;
import core.Processo;
import core.ResultadoSimulacao;
import relatorio.RelatorioSimulacao;
import escalonadores.EscalonadorEFP;
import escalonadores.EscalonadorMultiplasFilas;
import escalonadores.EscalonadorRoundRobin;
import escalonadores.service.LeitorProcessosCsv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/** Ponto de entrada que permite escolher qual escalonador executar. */
public class Main {
    /** Exibe o menu até que o usuário escolha encerrar o programa. */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int opcao;

        do {
            exibirMenu();
            opcao = lerOpcao(scanner);

            switch (opcao) {
                case 1:
                    executarEscalonador(scanner, new EscalonadorRoundRobin());
                    break;
                case 2:
                    executarEscalonador(scanner, new EscalonadorMultiplasFilas());
                    break;
                case 3:
                    executarEscalonador(scanner, new EscalonadorEFP());
                    break;
                case 0:
                    System.out.println("Simulador encerrado.");
                    break;
                default:
                    System.out.println("Opção inválida. Escolha 1, 2, 3 ou 0.");
            }
            System.out.println();
        } while (opcao != 0);
    }

    /** Mostra as opções de escalonamento disponíveis no momento. */
    private static void exibirMenu() {
        System.out.println("========================================");
        System.out.println("     SIMULADOR DE ESCALONAMENTO");
        System.out.println("========================================");
        System.out.println("1 - Round Robin");
        System.out.println("2 - Múltiplas Filas");
        System.out.println("3 - Método proposto pelo grupo");
        System.out.println("0 - Sair");
        System.out.print("Escolha uma opção: ");
    }

    /** Lê uma opção numérica sem encerrar o programa caso o texto seja inválido. */
    private static int lerOpcao(Scanner scanner) {
        String texto = scanner.nextLine().trim();
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Lê o CSV pelo teclado, executa o algoritmo escolhido e imprime seu relatório.
     */
    private static void executarEscalonador(Scanner scanner, Escalonador escalonador) {
        Path caminho = lerCaminhoCsv(scanner);
        if (caminho == null)
            return;

        try {
            List<Processo> processos = new LeitorProcessosCsv().ler(caminho);
            ResultadoSimulacao resultado = escalonador.executar(processos);
            new RelatorioSimulacao().imprimir(escalonador, processos, resultado);
        } catch (IOException e) {
            System.out.println("Não foi possível ler o CSV: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("O CSV possui dados inválidos: " + e.getMessage());
        }
    }

    /**
     * Solicita o caminho do CSV e valida se ele aponta para um arquivo existente.
     */
    private static Path lerCaminhoCsv(Scanner scanner) {
        System.out.print("Informe o caminho do arquivo CSV: ");
        Path caminho = Paths.get(scanner.nextLine().trim());
        if (!Files.isRegularFile(caminho)) {
            System.out.println("Arquivo não encontrado: " + caminho);
            return null;
        }
        return caminho;
    }
}

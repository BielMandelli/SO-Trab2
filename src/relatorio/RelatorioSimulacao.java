package relatorio;

import core.Escalonador;
import core.Processo;
import core.ResultadoSimulacao;
import core.SegmentoGantt;

import java.util.List;
import java.util.Locale;

/** Apresenta Gantt, tabela de métricas e resumo para qualquer escalonador. */
public class RelatorioSimulacao {
    private static final Locale LOCALE_BRASIL = Locale.forLanguageTag("pt-BR");
    private static final String LINHA = "+---------+--------------------------+---------+---------+---------+----------+----------+------------+-----------+------------+";
    private static final String FORMATO_LINHA = "| %-7s | %-24s | %7d | %7d | %7d | %8d | %8d | %10d | %9d | %10d |%n";

    /** Imprime todos os resultados exigidos para um algoritmo executado. */
    public void imprimir(Escalonador escalonador, List<Processo> processos, ResultadoSimulacao resultado) {
        System.out.println("\n========== " + escalonador.getNome().toUpperCase() + " ==========");
        imprimirGantt(resultado.getGantt());
        imprimirTabela(processos);
        imprimirResumo(processos, resultado.getTrocasContexto());
    }

    /** Imprime cada faixa de execução no formato início-fim PID. */
    private void imprimirGantt(List<SegmentoGantt> gantt) {
        System.out.println("\nGantt (início-fim PID):");
        int segmentosNaLinha = 0;
        for (SegmentoGantt segmento : gantt) {
            System.out.print("[" + segmento + "] ");
            segmentosNaLinha++;
            if (segmentosNaLinha == 8) {
                System.out.println();
                segmentosNaLinha = 0;
            }
        }
        if (segmentosNaLinha != 0) System.out.println();
    }

    /** Imprime uma tabela alinhada com as métricas individuais de todos os processos. */
    private void imprimirTabela(List<Processo> processos) {
        System.out.println("\nMétricas por processo:");
        System.out.println(LINHA);
        System.out.printf("| %-7s | %-24s | %7s | %7s | %7s | %8s | %8s | %10s | %9s | %10s |%n",
                "PID", "Processo", "Chegada", "CPU", "E/S", "Início", "Fim", "Retorno", "Espera", "Resposta");
        System.out.println(LINHA);

        for (Processo processo : processos) {
            int retorno = processo.getConclusao() - processo.getChegada();
            int espera = retorno - processo.getCpuTotal() - processo.getTotalEs();
            int resposta = processo.getInicio() - processo.getChegada();
            System.out.printf(FORMATO_LINHA, processo.getPid(), processo.getNome(),
                    processo.getChegada(), processo.getCpuTotal(), processo.getTotalEs(),
                    processo.getInicio(), processo.getConclusao(), retorno, espera, resposta);
        }
        System.out.println(LINHA);
    }

    /** Calcula e exibe médias, trocas de contexto e duração total da simulação. */
    private void imprimirResumo(List<Processo> processos, int trocasContexto) {
        double somaEspera = 0;
        double somaRetorno = 0;
        double somaResposta = 0;
        int conclusaoFinal = 0;

        for (Processo processo : processos) {
            int retorno = processo.getConclusao() - processo.getChegada();
            int espera = retorno - processo.getCpuTotal() - processo.getTotalEs();
            int resposta = processo.getInicio() - processo.getChegada();
            somaEspera += espera;
            somaRetorno += retorno;
            somaResposta += resposta;
            conclusaoFinal = Math.max(conclusaoFinal, processo.getConclusao());
        }

        int quantidade = processos.size();
        System.out.println("\nResumo:");
        System.out.println("Trocas de contexto: " + trocasContexto);
        System.out.println("Tempo total da simulação: " + conclusaoFinal);
        System.out.printf(LOCALE_BRASIL, "Tempo médio de espera: %.2f%n", somaEspera / quantidade);
        System.out.printf(LOCALE_BRASIL, "Tempo médio de retorno: %.2f%n", somaRetorno / quantidade);
        System.out.printf(LOCALE_BRASIL, "Tempo médio de resposta: %.2f%n", somaResposta / quantidade);
    }
}

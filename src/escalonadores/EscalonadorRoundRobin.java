package escalonadores;

import core.Escalonador;
import core.Processo;
import core.ResultadoSimulacao;
import core.SegmentoGantt;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/** Implementa Round Robin com quantum fixo de quatro unidades de tempo. */
public class EscalonadorRoundRobin implements Escalonador {
    public static final int QUANTUM = 4;
    private final Random aleatorio;

    /** Cria o escalonador com uma semente fixa, deixando a simulação reproduzível. */
    public EscalonadorRoundRobin() { this(42L); }

    /** Cria o escalonador com uma semente escolhida para os sorteios de E/S. */
    public EscalonadorRoundRobin(long semente) { aleatorio = new Random(semente); }

    /** Executa o algoritmo, controlando chegadas, bloqueios por E/S e a fila de prontos. */
    @Override
    public ResultadoSimulacao executar(List<Processo> processos) {
        Queue<Processo> pendentes = new ArrayDeque<Processo>(processos);
        Queue<Processo> prontos = new ArrayDeque<Processo>();
        List<Processo> bloqueados = new ArrayList<Processo>();
        List<SegmentoGantt> gantt = new ArrayList<SegmentoGantt>();
        int tempo = 0, finalizados = 0, trocas = 0;
        String ultimoPid = null;

        while (finalizados < processos.size()) {
            adicionarChegadas(pendentes, prontos, tempo);
            desbloquearProcessos(bloqueados, prontos, tempo);
            if (prontos.isEmpty()) {
                tempo = proximoEvento(pendentes, bloqueados);
                ultimoPid = null;
                continue;
            }

            Processo processo = prontos.remove();
            processo.executar();
            processo.iniciar(tempo);
            if (ultimoPid != null && !ultimoPid.equals(processo.getPid())) trocas++;

            int inicioFatia = tempo;
            FatiaExecutada fatia = executarFatia(
                    processo, pendentes, prontos, bloqueados, inicioFatia
            );
            tempo += fatia.duracao;
            gantt.add(new SegmentoGantt(inicioFatia, tempo, processo.getPid()));
            ultimoPid = processo.getPid();

            if (processo.getCpuRestante() == 0) {
                processo.concluir(tempo);
                finalizados++;
            } else if (!fatia.interrompidaPorEs) {
                processo.tornarPronto();
                prontos.add(processo);
            }
        }
        return new ResultadoSimulacao(gantt, trocas);
    }

    /** Insere na fila todos os processos que chegaram até o instante informado. */
    private void adicionarChegadas(Queue<Processo> pendentes, Queue<Processo> prontos, int tempo) {
        while (!pendentes.isEmpty() && pendentes.peek().getChegada() <= tempo) {
            Processo processo = pendentes.remove();
            processo.tornarPronto();
            prontos.add(processo);
        }
    }

    /** Move para a fila de prontos os processos cujo tempo de bloqueio terminou. */
    private void desbloquearProcessos(List<Processo> bloqueados, Queue<Processo> prontos, int tempo) {
        for (Iterator<Processo> it = bloqueados.iterator(); it.hasNext();) {
            Processo processo = it.next();
            if (processo.getBloqueadoAte() <= tempo) {
                processo.tornarPronto();
                prontos.add(processo);
                it.remove();
            }
        }
    }

    /**
     * Executa uma fatia de CPU até o quantum, finalização ou solicitação de E/S.
     * A cada unidade de tempo, registra novas chegadas e desbloqueios antes de
     * reenfileirar o processo atual, preservando a ordem correta do Round Robin.
     */
    private FatiaExecutada executarFatia(Processo processo, Queue<Processo> pendentes,
                                         Queue<Processo> prontos, List<Processo> bloqueados,
                                         int inicioFatia) {
        int duracao = 0;
        while (duracao < QUANTUM && processo.getCpuRestante() > 0) {
            processo.consumirCpu();
            duracao++;

            int tempoAtual = inicioFatia + duracao;
            adicionarChegadas(pendentes, prontos, tempoAtual);
            desbloquearProcessos(bloqueados, prontos, tempoAtual);

            if (processo.getCpuRestante() > 0 && processo.possuiEs()
                    && aleatorio.nextDouble() < processo.getProbabilidadeEs()) {
                processo.bloquear(tempoAtual);
                bloqueados.add(processo);
                return new FatiaExecutada(duracao, true);
            }
        }
        return new FatiaExecutada(duracao, false);
    }

    /** Determina a próxima chegada ou término de E/S quando não há processo pronto. */
    private int proximoEvento(Queue<Processo> pendentes, List<Processo> bloqueados) {
        int proximo = pendentes.isEmpty() ? Integer.MAX_VALUE : pendentes.peek().getChegada();
        for (Processo processo : bloqueados) proximo = Math.min(proximo, processo.getBloqueadoAte());
        return proximo;
    }

    @Override public String getNome() { return "Round Robin"; }

    /** Transporta o resultado interno de uma fatia, sem expor detalhes fora do algoritmo. */
    private static class FatiaExecutada {
        private final int duracao;
        private final boolean interrompidaPorEs;

        /** Cria o resultado da fatia com sua duração e indicação de bloqueio. */
        private FatiaExecutada(int duracao, boolean interrompidaPorEs) {
            this.duracao = duracao;
            this.interrompidaPorEs = interrompidaPorEs;
        }
    }
}

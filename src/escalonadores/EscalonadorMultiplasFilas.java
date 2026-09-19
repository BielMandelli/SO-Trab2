package escalonadores;

import core.Escalonador;
import core.Processo;
import core.ResultadoSimulacao;
import core.SegmentoGantt;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

/**
 * Implementa Múltiplas Filas com Round Robin dentro de cada fila. */
public class EscalonadorMultiplasFilas implements Escalonador {
    public static final int[] QUANTUNS = {2, 4, 8};
    public static final int LIMITE_ESPERA = 30;

    private final Random aleatorio;

    private List<Deque<Processo>> filas;
    private Map<Processo, Integer> filaAtual;     // fila onde o processo está agora
    private Map<Processo, Integer> entradaPronto; // instante em que entrou (ou voltou) a pronto

    private enum Motivo { FIM, QUANTUM, ES, PREEMPCAO }

    /** Cria o escalonador com a mesma semente do Round Robin. */
    public EscalonadorMultiplasFilas() { this(42L); }

    /** Cria o escalonador com uma semente escolhida para os sorteios de E/S. */
    public EscalonadorMultiplasFilas(long semente) { aleatorio = new Random(semente); }

    /** Executa o algoritmo, controlando chegadas, bloqueios por E/S, aging e preempção. */
    @Override
    public ResultadoSimulacao executar(List<Processo> processos) {
        filas = new ArrayList<Deque<Processo>>();
        for (int i = 0; i < QUANTUNS.length; i++) filas.add(new ArrayDeque<Processo>());
        filaAtual = new HashMap<Processo, Integer>();
        entradaPronto = new HashMap<Processo, Integer>();

        Queue<Processo> pendentes = new ArrayDeque<Processo>(processos);
        List<Processo> bloqueados = new ArrayList<Processo>();
        List<SegmentoGantt> gantt = new ArrayList<SegmentoGantt>();
        int tempo = 0, finalizados = 0, trocas = 0;
        String ultimoPid = null;

        while (finalizados < processos.size()) {
            atualizarFilas(pendentes, bloqueados, tempo);
            int nivel = filaMaisPrioritaria();
            if (nivel < 0) {
                tempo = proximoEvento(pendentes, bloqueados);
                ultimoPid = null;
                continue;
            }

            Processo processo = filas.get(nivel).removeFirst();
            processo.executar();
            processo.iniciar(tempo);
            if (ultimoPid != null && !ultimoPid.equals(processo.getPid())) trocas++;

            int inicioFatia = tempo;
            FatiaExecutada fatia = executarFatia(processo, nivel, pendentes, bloqueados, inicioFatia);
            tempo += fatia.duracao;
            gantt.add(new SegmentoGantt(inicioFatia, tempo, processo.getPid()));
            ultimoPid = processo.getPid();

            switch (fatia.motivo) {
                case FIM:
                    processo.concluir(tempo);
                    finalizados++;
                    break;
                case QUANTUM:
                    // fim do quantum: volta ao FINAL da sua fila de origem
                    colocarNaFila(processo, tempo);
                    break;
                case PREEMPCAO:
                    // perdeu a CPU sem culpa: volta ao INÍCIO da fila em que estava
                    processo.tornarPronto();
                    entradaPronto.put(processo, tempo);
                    filas.get(nivel).addFirst(processo);
                    break;
                case ES:
                    // já foi bloqueado dentro de executarFatia
                    break;
            }
        }
        return new ResultadoSimulacao(gantt, trocas);
    }

    /** Fila de origem do processo, de acordo com o seu tipo. */
    private int filaBase(Processo processo) {
        String tipo = processo.getTipo() == null ? "" : processo.getTipo().trim().toLowerCase();
        if (tipo.equals("tempo_real") || tipo.equals("interativo")) return 0;
        if (tipo.equals("io_bound") || tipo.equals("misto")) return 1;
        return 2; // cpu_bound, batch e qualquer outro
    }

    /** Coloca o processo no fim da sua fila de origem e reinicia a contagem de espera. */
    private void colocarNaFila(Processo processo, int tempo) {
        int base = filaBase(processo);
        processo.tornarPronto();
        filaAtual.put(processo, base);
        entradaPronto.put(processo, tempo);
        filas.get(base).addLast(processo);
    }

    /** Aplica, em ordem, chegadas, desbloqueios e promoções por espera. */
    private void atualizarFilas(Queue<Processo> pendentes, List<Processo> bloqueados, int tempo) {
        adicionarChegadas(pendentes, tempo);
        desbloquearProcessos(bloqueados, tempo);
        envelhecer(tempo);
    }

    /** Insere nas filas todos os processos que chegaram até o instante informado. */
    private void adicionarChegadas(Queue<Processo> pendentes, int tempo) {
        while (!pendentes.isEmpty() && pendentes.peek().getChegada() <= tempo) {
            colocarNaFila(pendentes.remove(), tempo);
        }
    }

    /** Move para as filas de prontos os processos cujo bloqueio terminou. */
    private void desbloquearProcessos(List<Processo> bloqueados, int tempo) {
        for (Iterator<Processo> it = bloqueados.iterator(); it.hasNext();) {
            Processo processo = it.next();
            if (processo.getBloqueadoAte() <= tempo) {
                colocarNaFila(processo, tempo);
                it.remove();
            }
        }
    }

    /**
     * Estratégia contra inanição: quem espera mais de LIMITE_ESPERA na fila de
     * prontos sobe uma fila. Só é promovido uma vez por espera (enquanto estiver
     * na sua fila de origem); ao voltar da CPU retorna à fila de origem.
     */
    private void envelhecer(int tempo) {
        for (int nivel = 1; nivel < filas.size(); nivel++) {
            for (Iterator<Processo> it = filas.get(nivel).iterator(); it.hasNext();) {
                Processo processo = it.next();
                int espera = tempo - entradaPronto.get(processo);
                boolean naOrigem = filaAtual.get(processo) == filaBase(processo);
                if (naOrigem && espera > LIMITE_ESPERA) {
                    it.remove();
                    filaAtual.put(processo, nivel - 1);
                    filas.get(nivel - 1).addLast(processo);
                }
            }
        }
    }

    /** Índice da fila não vazia de maior prioridade, ou -1 se todas estiverem vazias. */
    private int filaMaisPrioritaria() {
        for (int i = 0; i < filas.size(); i++) if (!filas.get(i).isEmpty()) return i;
        return -1;
    }

    /** Indica se existe processo pronto em alguma fila mais prioritária que a informada. */
    private boolean existeMaisPrioritario(int nivel) {
        for (int i = 0; i < nivel; i++) if (!filas.get(i).isEmpty()) return true;
        return false;
    }

    /**
     * Executa uma fatia de CPU até: fim do processo, fim do quantum da fila,
     * solicitação de E/S ou preempção por processo de fila mais prioritária.
     */
    private FatiaExecutada executarFatia(Processo processo, int nivel, Queue<Processo> pendentes,
                                         List<Processo> bloqueados, int inicioFatia) {
        int quantum = QUANTUNS[nivel];
        int duracao = 0;
        while (duracao < quantum && processo.getCpuRestante() > 0) {
            processo.consumirCpu();
            duracao++;

            int tempoAtual = inicioFatia + duracao;
            atualizarFilas(pendentes, bloqueados, tempoAtual);

            if (processo.getCpuRestante() == 0) break;

            if (processo.possuiEs() && aleatorio.nextDouble() < processo.getProbabilidadeEs()) {
                processo.bloquear(tempoAtual);
                bloqueados.add(processo);
                return new FatiaExecutada(duracao, Motivo.ES);
            }

            if (duracao < quantum && existeMaisPrioritario(nivel)) {
                return new FatiaExecutada(duracao, Motivo.PREEMPCAO);
            }
        }
        return new FatiaExecutada(duracao, processo.getCpuRestante() == 0 ? Motivo.FIM : Motivo.QUANTUM);
    }

    /** Determina a próxima chegada ou término de E/S quando não há processo pronto. */
    private int proximoEvento(Queue<Processo> pendentes, List<Processo> bloqueados) {
        int proximo = pendentes.isEmpty() ? Integer.MAX_VALUE : pendentes.peek().getChegada();
        for (Processo processo : bloqueados) proximo = Math.min(proximo, processo.getBloqueadoAte());
        return proximo;
    }

    @Override public String getNome() { return "Múltiplas Filas"; }

    /** Transporta o resultado interno de uma fatia. */
    private static class FatiaExecutada {
        private final int duracao;
        private final Motivo motivo;

        private FatiaExecutada(int duracao, Motivo motivo) {
            this.duracao = duracao;
            this.motivo = motivo;
        }
    }
}
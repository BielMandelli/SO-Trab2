package roundrobin;

import core.Escalonador;
import core.Processo;
import core.ResultadoSimulacao;
import core.SegmentoGantt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

public class EscalonadorEFP implements Escalonador {
    private static final int QUANTUM = 4;
    private final Random aleatorio = new Random(42L);

    // Mapa para rastrear o exato momento em que o processo entrou na fila de
    // prontos
    private final Map<Processo, Integer> entradaPronto = new HashMap<>();

    @Override
    public ResultadoSimulacao executar(List<Processo> processos) {
        entradaPronto.clear();
        Queue<Processo> pendentes = new ArrayDeque<>(processos);
        List<Processo> prontos = new ArrayList<>();
        List<Processo> bloqueados = new ArrayList<>();
        List<SegmentoGantt> gantt = new ArrayList<>();

        int tempo = 0, finalizados = 0, trocas = 0;
        String ultimoPid = null;

        while (finalizados < processos.size()) {
            atualizarFilas(pendentes, prontos, bloqueados, tempo);

            if (prontos.isEmpty()) {
                tempo = proximoEvento(pendentes, bloqueados);
                ultimoPid = null;
                continue;
            }

            // Seleção baseada na Fórmula de Fadiga Ponderada
            Processo escolhido = null;
            double maiorNota = -Double.MAX_VALUE;

            for (Processo p : prontos) {
                int espera = tempo - entradaPronto.getOrDefault(p, tempo);

                // Fórmula: Tempo de Espera - (Prioridade * 10)
                double nota = espera - (p.getPrioridade() * 10.0);

                // Bónus de urgência para processos interativos ou de tempo real
                if ("interativo".equals(p.getTipo()) || "tempo_real".equals(p.getTipo())) {
                    nota += 15.0;
                }

                if (nota > maiorNota) {
                    maiorNota = nota;
                    escolhido = p;
                }
            }

            prontos.remove(escolhido);
            escolhido.executar();
            escolhido.iniciar(tempo);

            if (ultimoPid != null && !ultimoPid.equals(escolhido.getPid())) {
                trocas++;
            }

            int inicioFatia = tempo;
            int duracao = 0;
            boolean fezEs = false;

            while (duracao < QUANTUM && escolhido.getCpuRestante() > 0) {
                escolhido.consumirCpu();
                duracao++;
                int tempoAtual = inicioFatia + duracao;

                atualizarFilas(pendentes, prontos, bloqueados, tempoAtual);

                if (escolhido.getCpuRestante() == 0)
                    break;

                if (escolhido.possuiEs() && aleatorio.nextDouble() < escolhido.getProbabilidadeEs()) {
                    escolhido.bloquear(tempoAtual);
                    bloqueados.add(escolhido);
                    fezEs = true;
                    break;
                }
            }

            tempo += duracao;
            gantt.add(new SegmentoGantt(inicioFatia, tempo, escolhido.getPid()));
            ultimoPid = escolhido.getPid();

            if (escolhido.getCpuRestante() == 0) {
                escolhido.concluir(tempo);
                entradaPronto.remove(escolhido);
                finalizados++;
            } else if (!fezEs) {
                escolhido.tornarPronto();
                prontos.add(escolhido);
                entradaPronto.put(escolhido, tempo); // Reinicia a contagem de espera do zero
            } else {
                entradaPronto.remove(escolhido);
            }
        }
        return new ResultadoSimulacao(gantt, trocas);
    }

    private void atualizarFilas(Queue<Processo> pendentes, List<Processo> prontos, List<Processo> bloqueados,
            int tempo) {
        while (!pendentes.isEmpty() && pendentes.peek().getChegada() <= tempo) {
            Processo p = pendentes.remove();
            p.tornarPronto();
            prontos.add(p);
            entradaPronto.put(p, tempo);
        }

        for (Iterator<Processo> it = bloqueados.iterator(); it.hasNext();) {
            Processo p = it.next();
            if (p.getBloqueadoAte() <= tempo) {
                p.tornarPronto();
                prontos.add(p);
                entradaPronto.put(p, tempo);
                it.remove();
            }
        }
    }

    private int proximoEvento(Queue<Processo> pendentes, List<Processo> bloqueados) {
        int proximo = pendentes.isEmpty() ? Integer.MAX_VALUE : pendentes.peek().getChegada();
        for (Processo p : bloqueados)
            proximo = Math.min(proximo, p.getBloqueadoAte());
        return proximo;
    }

    @Override
    public String getNome() {
        return "Escalonador por Fadiga Ponderada (EFP)";
    }
}
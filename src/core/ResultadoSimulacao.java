package core;

import java.util.List;

/** Reúne os dados globais produzidos por qualquer algoritmo de escalonamento. */
public class ResultadoSimulacao {
    private final List<SegmentoGantt> gantt;
    private final int trocasContexto;

    public ResultadoSimulacao(List<SegmentoGantt> gantt, int trocasContexto) {
        this.gantt = gantt;
        this.trocasContexto = trocasContexto;
    }
    public List<SegmentoGantt> getGantt() { return gantt; }
    public int getTrocasContexto() { return trocasContexto; }
}

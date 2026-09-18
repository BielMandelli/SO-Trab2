package core;

/** Representa um intervalo contínuo de tempo em que um processo ocupou a CPU. */
public class SegmentoGantt {
    private final int inicio;
    private final int fim;
    private final String pid;

    public SegmentoGantt(int inicio, int fim, String pid) {
        this.inicio = inicio;
        this.fim = fim;
        this.pid = pid;
    }

    public int getInicio() { return inicio; }
    public int getFim() { return fim; }
    public String getPid() { return pid; }
    @Override public String toString() { return inicio + "-" + fim + " " + pid; }
}

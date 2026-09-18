package core;

/**
 * Guarda os dados de entrada e o estado de execução de um processo.
 * A classe é independente de algoritmo e serve aos três escalonadores.
 */
public class Processo {
    private final String pid;
    private final String nome;
    private final int chegada;
    private final int cpuTotal;
    private final int prioridade;
    private final String tipo;
    private final boolean possuiEs;
    private final double probabilidadeEs;
    private final int duracaoEs;
    private int cpuRestante;
    private int totalEs;
    private Integer inicio;
    private Integer conclusao;
    private Integer bloqueadoAte;
    private EstadoProcesso estado = EstadoProcesso.NOVO;

    public Processo(String pid, String nome, int chegada, int cpuTotal, int prioridade,
                    String tipo, boolean possuiEs, double probabilidadeEs, int duracaoEs) {
        this.pid = pid;
        this.nome = nome;
        this.chegada = chegada;
        this.cpuTotal = cpuTotal;
        this.prioridade = prioridade;
        this.tipo = tipo;
        this.possuiEs = possuiEs;
        this.probabilidadeEs = probabilidadeEs;
        this.duracaoEs = duracaoEs;
        this.cpuRestante = cpuTotal;
    }

    public String getPid() { return pid; }
    public String getNome() { return nome; }
    public int getChegada() { return chegada; }
    public int getCpuTotal() { return cpuTotal; }
    public int getPrioridade() { return prioridade; }
    public String getTipo() { return tipo; }
    public boolean possuiEs() { return possuiEs; }
    public double getProbabilidadeEs() { return probabilidadeEs; }
    public int getDuracaoEs() { return duracaoEs; }
    public int getCpuRestante() { return cpuRestante; }
    public int getTotalEs() { return totalEs; }
    public Integer getInicio() { return inicio; }
    public Integer getConclusao() { return conclusao; }
    public Integer getBloqueadoAte() { return bloqueadoAte; }
    public EstadoProcesso getEstado() { return estado; }

    /** Registra o primeiro uso da CPU, preservando o valor em chamadas posteriores. */
    public void iniciar(int tempo) {
        if (inicio == null) inicio = tempo;
    }

    /** Consome exatamente uma unidade do tempo de CPU restante. */
    public void consumirCpu() {
        cpuRestante--;
    }

    /** Coloca o processo na fila de prontos, inclusive ao retornar de E/S. */
    public void tornarPronto() {
        estado = EstadoProcesso.PRONTO;
        bloqueadoAte = null;
    }

    /** Marca o processo como aquele que está usando a CPU. */
    public void executar() {
        estado = EstadoProcesso.EXECUTANDO;
    }

    /** Bloqueia o processo pelo tempo configurado para sua operação de E/S. */
    public void bloquear(int tempoAtual) {
        estado = EstadoProcesso.BLOQUEADO;
        bloqueadoAte = tempoAtual + duracaoEs;
        totalEs += duracaoEs;
    }

    /** Marca o processo como finalizado e registra seu instante de término. */
    public void concluir(int tempo) {
        estado = EstadoProcesso.FINALIZADO;
        conclusao = tempo;
    }
}

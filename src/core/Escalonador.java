package core;

import java.util.List;

/** Interface que todos os três escalonadores do trabalho devem implementar. */
public interface Escalonador {
    ResultadoSimulacao executar(List<Processo> processos);
    String getNome();
}

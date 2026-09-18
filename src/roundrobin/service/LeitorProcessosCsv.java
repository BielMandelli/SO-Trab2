package roundrobin.service;

import core.Processo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Centraliza a leitura do arquivo de entrada para que todos os escalonadores
 * usem exatamente o mesmo conjunto de processos.
 */
public class LeitorProcessosCsv {
    /** Lê o CSV de entrada e devolve processos ordenados por tempo de chegada. */
    public List<Processo> ler(Path caminho) throws IOException {
        List<String> linhas = Files.readAllLines(caminho, StandardCharsets.UTF_8);
        List<Processo> processos = new ArrayList<Processo>();

        for (int i = 1; i < linhas.size(); i++) {
            String[] coluna = linhas.get(i).split(",", -1);
            if (coluna.length < 13) throw new IllegalArgumentException("Linha inválida: " + (i + 1));

            processos.add(new Processo(
                    coluna[0], coluna[1], Integer.parseInt(coluna[2]),
                    Integer.parseInt(coluna[3]), Integer.parseInt(coluna[4]), coluna[5],
                    "1".equals(coluna[6]), Double.parseDouble(coluna[7]), Integer.parseInt(coluna[9])
            ));
        }

        processos.sort(Comparator.comparingInt(Processo::getChegada));
        return processos;
    }
}

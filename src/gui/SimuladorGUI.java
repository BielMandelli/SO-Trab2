package gui;

import core.Escalonador;
import core.Processo;
import core.ResultadoSimulacao;
import core.SegmentoGantt;
import roundrobin.EscalonadorEFP;
import roundrobin.EscalonadorMultiplasFilas;
import roundrobin.EscalonadorRoundRobin;
import roundrobin.service.LeitorProcessosCsv;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class SimuladorGUI extends JFrame {
    private JComboBox<String> comboAlgoritmos;
    private JButton btnCarregar;
    private PainelGantt painelGantt;
    private JTable tabelaMetricas;
    private DefaultTableModel modeloTabela;

    // Rótulos para o resumo geral
    private JLabel lblTrocas, lblTempoTotal, lblMediaEspera, lblMediaRetorno, lblMediaResposta;

    // Mapa de cores partilhado entre o diagrama e a tabela
    private final Map<String, Color> coresProcessos = new HashMap<>();
    private final Random rand = new Random(42);

    private final Escalonador[] escalonadores = {
            new EscalonadorRoundRobin(),
            new EscalonadorMultiplasFilas(),
            new EscalonadorEFP()
    };

    public SimuladorGUI() {
        FlatDarkLaf.setup();

        setTitle("Simulador de Escalonamento de Processos");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- PAINEL SUPERIOR (Controles) ---
        JPanel painelControles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboAlgoritmos = new JComboBox<>();
        for (Escalonador e : escalonadores) {
            comboAlgoritmos.addItem(e.getNome());
        }

        btnCarregar = new JButton("Carregar CSV e Executar");
        btnCarregar.addActionListener(e -> carregarEExecutar());

        JButton btnLimparFiltro = new JButton("Limpar Seleção");
        btnLimparFiltro.addActionListener(e -> tabelaMetricas.clearSelection());

        painelControles.add(new JLabel("Algoritmo: "));
        painelControles.add(comboAlgoritmos);
        painelControles.add(btnCarregar);
        painelControles.add(btnLimparFiltro);
        add(painelControles, BorderLayout.NORTH);

        // --- PAINEL CENTRAL (Diagrama de Gantt) ---
        painelGantt = new PainelGantt(coresProcessos);
        painelGantt.setPreferredSize(new Dimension(800, 150));
        JScrollPane scrollGantt = new JScrollPane(painelGantt);
        scrollGantt.setBorder(BorderFactory.createTitledBorder("Diagrama de Gantt"));

        // --- PAINEL INFERIOR (Tabela e Resumo) ---
        JPanel painelInferior = new JPanel(new BorderLayout());

        String[] colunas = { "PID", "Processo", "Chegada", "CPU Total", "Total E/S", "Início", "Fim", "Retorno",
                "Espera", "Resposta" };
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaMetricas = new JTable(modeloTabela);

        // Renderizador para colorir as linhas da tabela
        tabelaMetricas.setDefaultRenderer(Object.class, new ProcessoColorRenderer());

        // Listener para destacar o processo no diagrama ao clicar na tabela
        tabelaMetricas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int linhaSelecionada = tabelaMetricas.getSelectedRow();
                if (linhaSelecionada >= 0) {
                    String pidSelecionado = (String) tabelaMetricas.getValueAt(linhaSelecionada, 0);
                    painelGantt.setProcessoDestacado(pidSelecionado);
                } else {
                    painelGantt.setProcessoDestacado(null);
                }
            }
        });

        JScrollPane scrollTabela = new JScrollPane(tabelaMetricas);
        scrollTabela.setBorder(BorderFactory.createTitledBorder("Métricas por Processo"));
        painelInferior.add(scrollTabela, BorderLayout.CENTER);

        // Rodapé de Resumo Geral
        JPanel painelResumo = new JPanel(new GridLayout(1, 5, 10, 0));
        painelResumo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lblTrocas = new JLabel("Trocas de Contexto: -");
        lblTempoTotal = new JLabel("Tempo Total: -");
        lblMediaEspera = new JLabel("Média Espera: -");
        lblMediaRetorno = new JLabel("Média Retorno: -");
        lblMediaResposta = new JLabel("Média Resposta: -");

        painelResumo.add(lblTrocas);
        painelResumo.add(lblTempoTotal);
        painelResumo.add(lblMediaEspera);
        painelResumo.add(lblMediaRetorno);
        painelResumo.add(lblMediaResposta);
        painelInferior.add(painelResumo, BorderLayout.SOUTH);

        // --- SPLIT PANE (Divisor ajustável) ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollGantt, painelInferior);
        splitPane.setDividerLocation(250);
        add(splitPane, BorderLayout.CENTER);
    }

    private void carregarEExecutar() {
        JFileChooser fileChooser = new JFileChooser(new File("."));
        fileChooser.setDialogTitle("Selecione o arquivo CSV de processos");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path caminho = fileChooser.getSelectedFile().toPath();
            int indiceEscolhido = comboAlgoritmos.getSelectedIndex();
            Escalonador escalonador = escalonadores[indiceEscolhido];

            try {
                List<Processo> processos = new LeitorProcessosCsv().ler(caminho);
                ResultadoSimulacao resultado = escalonador.executar(processos);

                gerarCoresProcessos(processos);
                painelGantt.setResultado(resultado);
                atualizarTabela(processos);
                atualizarResumo(processos, resultado);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao processar arquivo: " + ex.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void gerarCoresProcessos(List<Processo> processos) {
        coresProcessos.clear();
        for (Processo p : processos) {
            // Gera cores pastéis para que o texto a preto seja legível no interior
            coresProcessos.put(p.getPid(), new Color(
                    rand.nextFloat() * 0.4f + 0.6f,
                    rand.nextFloat() * 0.4f + 0.6f,
                    rand.nextFloat() * 0.4f + 0.6f));
        }
    }

    private void atualizarTabela(List<Processo> processos) {
        tabelaMetricas.clearSelection();
        modeloTabela.setRowCount(0);
        for (Processo p : processos) {
            int retorno = p.getConclusao() - p.getChegada();
            int espera = retorno - p.getCpuTotal() - p.getTotalEs();
            int resposta = p.getInicio() - p.getChegada();

            modeloTabela.addRow(new Object[] {
                    p.getPid(), p.getNome(), p.getChegada(), p.getCpuTotal(), p.getTotalEs(),
                    p.getInicio(), p.getConclusao(), retorno, espera, resposta
            });
        }
    }

    private void atualizarResumo(List<Processo> processos, ResultadoSimulacao resultado) {
        double somaEspera = 0, somaRetorno = 0, somaResposta = 0;
        int tempoFinal = 0;

        for (Processo p : processos) {
            int retorno = p.getConclusao() - p.getChegada();
            int espera = retorno - p.getCpuTotal() - p.getTotalEs();
            int resposta = p.getInicio() - p.getChegada();

            somaEspera += espera;
            somaRetorno += retorno;
            somaResposta += resposta;
            tempoFinal = Math.max(tempoFinal, p.getConclusao());
        }

        int qtd = processos.size();
        lblTrocas.setText("Trocas de Contexto: " + resultado.getTrocasContexto());
        lblTempoTotal.setText("Tempo Total: " + tempoFinal);
        lblMediaEspera.setText(String.format(Locale.getDefault(), "Média Espera: %.2f", somaEspera / qtd));
        lblMediaRetorno.setText(String.format(Locale.getDefault(), "Média Retorno: %.2f", somaRetorno / qtd));
        lblMediaResposta.setText(String.format(Locale.getDefault(), "Média Resposta: %.2f", somaResposta / qtd));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimuladorGUI().setVisible(true));
    }

    /**
     * Renderizador customizado para pintar as linhas da tabela de acordo com o
     * Gantt
     */
    /**
     * Renderizador customizado para pintar APENAS a coluna do PID de acordo com o
     * Gantt
     */
    private class ProcessoColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else {
                if (column == 0) {
                    // Pinta o fundo e força o texto a preto apenas na coluna do PID (índice 0)
                    String pid = (String) table.getValueAt(row, 0);
                    c.setBackground(coresProcessos.getOrDefault(pid, table.getBackground()));
                    c.setForeground(Color.BLACK);
                } else {
                    // O resto da tabela mantém as cores nativas do FlatDarkLaf
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            }
            return c;
        }
    }
}

/** Componente customizado para desenhar o Gantt visualmente */
class PainelGantt extends JPanel {
    private ResultadoSimulacao resultado;
    private final Map<String, Color> coresProcessos;
    private final double PIXELS_POR_TEMPO = 15.0;
    private String processoDestacado = null; // PID a ser realçado (se aplicável)

    public PainelGantt(Map<String, Color> coresProcessos) {
        this.coresProcessos = coresProcessos;
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setResultado(ResultadoSimulacao resultado) {
        this.resultado = resultado;
        this.processoDestacado = null;

        if (resultado != null && !resultado.getGantt().isEmpty()) {
            int tempoFinal = resultado.getGantt().get(resultado.getGantt().size() - 1).getFim();
            int larguraNecessaria = 60 + (int) (tempoFinal * PIXELS_POR_TEMPO);
            setPreferredSize(new Dimension(larguraNecessaria, 180));
            revalidate();
        }
        repaint();
    }

    public void setProcessoDestacado(String pid) {
        this.processoDestacado = pid;
        repaint(); // Redesenha com o novo filtro
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (resultado == null || resultado.getGantt().isEmpty())
            return null;

        int margemX = 20;
        int margemY = 40;
        int alturaBarra = 40;

        if (e.getY() < margemY || e.getY() > margemY + alturaBarra)
            return null;

        double mouseTempo = (e.getX() - margemX) / PIXELS_POR_TEMPO;

        for (SegmentoGantt seg : resultado.getGantt()) {
            if (mouseTempo >= seg.getInicio() && mouseTempo < seg.getFim()) {
                return String.format(
                        "<html><div style='padding: 5px;'><b>PID:</b> %s<br><b>Início:</b> %d<br><b>Fim:</b> %d<br><b>Duração:</b> %d u.t.</div></html>",
                        seg.getPid(), seg.getInicio(), seg.getFim(), (seg.getFim() - seg.getInicio()));
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (resultado == null || resultado.getGantt().isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<SegmentoGantt> segmentos = resultado.getGantt();
        int tempoFinal = segmentos.get(segmentos.size() - 1).getFim();

        int margemX = 20;
        int margemY = 40;
        int alturaBarra = 40;

        // Extrai as cores de texto adaptadas ao tema escuro
        Color corTextoExterior = UIManager.getColor("Label.foreground");
        if (corTextoExterior == null)
            corTextoExterior = Color.WHITE;

        for (int i = 0; i < segmentos.size(); i++) {
            SegmentoGantt seg = segmentos.get(i);
            int x = margemX + (int) (seg.getInicio() * PIXELS_POR_TEMPO);
            int w = Math.max(1, (int) ((seg.getFim() - seg.getInicio()) * PIXELS_POR_TEMPO));

            // Lógica de Destaque / Filtragem
            boolean destacar = (processoDestacado == null || processoDestacado.equals(seg.getPid()));

            if (destacar) {
                g2.setColor(coresProcessos.getOrDefault(seg.getPid(), Color.GRAY));
            } else {
                // Se não for o processo selecionado na tabela, desenha com cinzento muito
                // escuro
                g2.setColor(new Color(60, 60, 60));
            }

            g2.fillRect(x, margemY, w, alturaBarra);

            // Borda do bloco (preta para contraste)
            g2.setColor(Color.BLACK);
            g2.drawRect(x, margemY, w, alturaBarra);

            // Texto interno (apenas se for o bloco destacado e couber no espaço)
            if (destacar && w > 35) {
                g2.setColor(Color.BLACK); // Força preto dentro do bloco pastel
                FontMetrics fm = g2.getFontMetrics();
                int textX = x + (w - fm.stringWidth(seg.getPid())) / 2;
                int textY = margemY + ((alturaBarra - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(seg.getPid(), textX, textY);
            }

            // Marcadores de tempo inferiores (em Cascata para não sobrepor)
            int nivel = i % 4;
            int offsetTextoTempo = 20 + (nivel * 18);

            g2.setColor(new Color(100, 100, 100)); // Linha guia discreta
            g2.drawLine(x, margemY + alturaBarra, x, margemY + alturaBarra + offsetTextoTempo - 12);

            g2.setColor(corTextoExterior); // Texto visível no tema escuro
            g2.drawString(String.valueOf(seg.getInicio()), x - 5, margemY + alturaBarra + offsetTextoTempo);
        }

        // Marca do tempo final a vermelho
        int xFinal = margemX + (int) (tempoFinal * PIXELS_POR_TEMPO);
        g2.setColor(new Color(255, 85, 85));
        g2.drawLine(xFinal, margemY + alturaBarra, xFinal, margemY + alturaBarra + 20);
        g2.drawString(String.valueOf(tempoFinal), xFinal - 5, margemY + alturaBarra + 35);
    }
}
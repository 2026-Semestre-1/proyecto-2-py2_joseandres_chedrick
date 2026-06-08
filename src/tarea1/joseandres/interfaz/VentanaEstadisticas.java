/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.interfaz;
import tarea1.joseandres.proceso.BCP;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import tarea1.joseandres.proceso.EstadisticasProceso;
/**
 *
 * @author chedr
 */
public class VentanaEstadisticas extends JDialog {
 
    // ── Paleta oscura coherente con SimuladorGUI ──────────────────────────
    private static final Color C_FONDO        = new Color(18,  18,  18);
    private static final Color C_PANEL        = new Color(28,  28,  36);
    private static final Color C_BORDE        = new Color(70,  70,  90);
    private static final Color C_HEADER       = new Color(40,  44,  60);
    private static final Color C_TEXTO        = new Color(220, 220, 230);
    private static final Color C_ACENTO       = new Color(100, 160, 255);
    private static final Color C_VERDE        = new Color(80,  220, 120);
    private static final Color C_AMARILLO     = new Color(255, 210,  80);
    private static final Color C_ROJO         = new Color(255, 100,  90);
    private static final Color C_TERMINADO    = new Color(70,  70,  80);
    private static final Color C_ERROR        = new Color(120,  35,  35);
 
    private final long momentoInicial;
    private final List<BCP> procesos;
 
    public VentanaEstadisticas(JFrame parent, List<BCP> procesos, long momentoInicial) {
        super(parent, "Estadísticas de Ejecución", true);
        this.procesos       = procesos;
        this.momentoInicial = momentoInicial;
 
        setSize(1100, 660);
        setMinimumSize(new Dimension(900, 500));
        setLocationRelativeTo(parent);
        setBackground(C_FONDO);
        getContentPane().setBackground(C_FONDO);
        setLayout(new BorderLayout(0, 0));
 
        add(crearEncabezado(),      BorderLayout.NORTH);
        add(crearPanelTabla(),      BorderLayout.CENTER);
        add(crearPanelResumen(),    BorderLayout.SOUTH);
    }
 
    // =========================================================================
    // ENCABEZADO
    // =========================================================================
    private JPanel crearEncabezado() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_HEADER);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, C_ACENTO));
        panel.setPreferredSize(new Dimension(0, 64));
 
        JLabel titulo = new JLabel("  📊  Reporte de Estadísticas del Planificador");
        titulo.setFont(new Font("Monospaced", Font.BOLD, 18));
        titulo.setForeground(C_ACENTO);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
 
        JLabel subtitulo = new JLabel("Procesos finalizados: " + procesos.size() + "   ");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitulo.setForeground(new Color(150, 150, 170));
 
        panel.add(titulo,    BorderLayout.CENTER);
        panel.add(subtitulo, BorderLayout.EAST);
        return panel;
    }
 
    // =========================================================================
    // TABLA PRINCIPAL
    // =========================================================================
    private JScrollPane crearPanelTabla() {
        String[] columnas = {
            "PID", "Nombre", "Estado",
            "T. Llegada (s)", "T. Inicio (s)", "T. Final (s)",
            "Tr (s)", "Ts (s)", "Tr / Ts"
        };
 
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
 
        for (BCP p : procesos) {
            EstadisticasProceso e = p.construirDatosEstadísticos(momentoInicial);
            String ratioStr = (e.ts > 0)
                    ? String.format("%.3f", e.ratio)
                    : "N/A";
            modelo.addRow(new Object[]{
                e.id,
                e.nombre,
                e.estado,
                e.tLlegada,
                e.tInicio,
                e.tFinal,
                e.tr,
                e.ts,
                ratioStr
            });
        }
 
        JTable tabla = new JTable(modelo);
        tabla.setBackground(C_PANEL);
        tabla.setForeground(C_TEXTO);
        tabla.setGridColor(new Color(50, 50, 65));
        tabla.setSelectionBackground(new Color(60, 80, 120));
        tabla.setSelectionForeground(Color.WHITE);
        tabla.setRowHeight(28);
        tabla.setFont(new Font("Monospaced", Font.PLAIN, 13));
        tabla.setShowVerticalLines(true);
        tabla.setIntercellSpacing(new Dimension(6, 2));
 
        // Header
        JTableHeader header = tabla.getTableHeader();
        header.setBackground(C_HEADER);
        header.setForeground(C_ACENTO);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 32));
        header.setReorderingAllowed(false);
 
        // Anchos de columna
        int[] anchos = {45, 220, 90, 110, 110, 110, 80, 80, 80};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
 
        // Renderer con colores por estado y ratio
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                c.setFont(tabla.getFont());
 
                String estado = t.getValueAt(row, 2).toString();
 
                // Color base por estado
                if (!sel) {
                    if ("TERMINADO".equalsIgnoreCase(estado)) {
                        c.setBackground(row % 2 == 0 ? C_PANEL : new Color(32, 32, 42));
                        c.setForeground(C_TEXTO);
                    } else if ("ERROR".equalsIgnoreCase(estado)) {
                        c.setBackground(C_ERROR);
                        c.setForeground(new Color(255, 160, 150));
                    } else {
                        c.setBackground(C_PANEL);
                        c.setForeground(C_TEXTO);
                    }
                }
 
                // Columna Estado con color propio
                if (col == 2 && !sel) {
                    if ("TERMINADO".equalsIgnoreCase(estado)) {
                        c.setForeground(C_VERDE);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if ("ERROR".equalsIgnoreCase(estado)) {
                        c.setForeground(C_ROJO);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    }
                }
 
                // Columna Tr/Ts: color por eficiencia
                if (col == 8 && val != null && !val.toString().equals("N/A") && !sel) {
                    try {
                        double ratio = Double.parseDouble(val.toString());
                        if (ratio < 0.3)       c.setForeground(C_VERDE);
                        else if (ratio < 0.6)  c.setForeground(C_AMARILLO);
                        else                   c.setForeground(C_ROJO);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } catch (NumberFormatException ignored) {}
                }
 
                // Alineación numérica para columnas 0 y 3-8
                if (col == 0 || col >= 3) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                }
 
                return c;
            }
        });
 
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(C_PANEL);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }
 
    // =========================================================================
    // PANEL DE RESUMEN (promedios)
    // =========================================================================
    private JPanel crearPanelResumen() {
        // Calcular promedios
        double sumTr = 0, sumTs = 0, sumRatio = 0;
        int    contRatio = 0;
 
        for (BCP p : procesos) {
            EstadisticasProceso e = p.construirDatosEstadísticos(momentoInicial);
            sumTr += e.tr;
            sumTs += e.ts;
            if (e.ts > 0) { sumRatio += e.ratio; contRatio++; }
        }
 
        int n = procesos.isEmpty() ? 1 : procesos.size();
        double avgTr    = sumTr / n;
        double avgTs    = sumTs / n;
        double avgRatio = contRatio > 0 ? sumRatio / contRatio : 0;
 
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 32, 12));
        panel.setBackground(C_HEADER);
        panel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, C_BORDE));
 
        panel.add(crearTarjetaResumen("Promedio Tr",    String.format("%.2f s", avgTr),    C_ACENTO));
        panel.add(crearTarjetaResumen("Promedio Ts",    String.format("%.2f s", avgTs),    C_AMARILLO));
        panel.add(crearTarjetaResumen("Promedio Tr/Ts", String.format("%.3f",   avgRatio), avgRatio < 0.4 ? C_VERDE : C_ROJO));
        panel.add(crearTarjetaResumen("Procesos OK",
                String.valueOf(procesos.stream().filter(p -> "TERMINADO".equalsIgnoreCase(p.estado)).count()),
                C_VERDE));
        panel.add(crearTarjetaResumen("Procesos ERROR",
                String.valueOf(procesos.stream().filter(p -> "ERROR".equalsIgnoreCase(p.estado)).count()),
                C_ROJO));
 
        // Botón cerrar
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setBackground(new Color(60, 60, 75));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setFocusPainted(false);
        btnCerrar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCerrar.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btnCerrar.addActionListener(e -> dispose());
        panel.add(btnCerrar);
 
        return panel;
    }
 
    private JPanel crearTarjetaResumen(String etiqueta, String valor, Color colorValor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDE, 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
 
        JLabel lblEtiqueta = new JLabel(etiqueta);
        lblEtiqueta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblEtiqueta.setForeground(new Color(150, 150, 170));
        lblEtiqueta.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font("Monospaced", Font.BOLD, 20));
        lblValor.setForeground(colorValor);
        lblValor.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        card.add(lblEtiqueta);
        card.add(Box.createVerticalStrut(4));
        card.add(lblValor);
        return card;
    }
 
    // =========================================================================
    // MÉTODO ESTÁTICO DE CONVENIENCIA — llamar desde SimuladorGUI
    // =========================================================================
    public static void mostrar(JFrame parent, List<BCP> procesos, long momentoInicial) {
        if (procesos == null || procesos.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Aún no hay procesos finalizados.",
                    "Sin datos", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        VentanaEstadisticas ventana = new VentanaEstadisticas(parent, procesos, momentoInicial);
        ventana.setVisible(true);
    }
}

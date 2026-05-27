package tarea1.joseandres.interfaz;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import tarea1.joseandres.cpu.Cpu;
import tarea1.joseandres.memoria.Memoria;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.disco.Disco;
import tarea1.joseandres.dispacher.Dispatcher;
import tarea1.joseandres.kernel.Kernel;
import static tarea1.joseandres.loader.Loader.traducirInstruccion;

public class SimuladorGUI extends JFrame {

    private Kernel kernel;
    private Memoria memoria;
    private Disco disco;
    private Dispatcher dispatcher;
    private Cpu cpu;

    private BCP bcpActual;
    private Timer timerSimulacion;

    private int tamanoRamConfig;
    private int porcentajeKernelConfig;
    private int tamanoDiscoConfig;
    private int porcentajeIndiceDiscoConfig;

    private int pidActualVisual = 1;
    private final Map<Integer, Color> coloresPID = new HashMap<>();

    private DefaultTableModel modeloMemoria, modeloDisco, modeloProcesos;
    private JTable tablaMemoriaFisica;
    private JTable tablaDisco;
    private JTable tablaProcesos;

    private ColorRowRenderer renderizadorMemoria;
    private ProcesoTableRenderer renderizadorProcesos;

    private JLabel lblPC, lblIR, lblAC, lblAX, lblBX, lblCX, lblDX;

    private JTextArea areaTerminal;
    private JTextField campoEntrada;
    private JLabel lblPromptEntrada;
    private JButton btnEnviarEntrada;
    private JSlider sliderVelocidad;

    private String ultimaEntrada = null;
    private boolean esperandoEntrada = false;

    private final Map<Integer, Color> coloresProcesos = new HashMap<>();
    private final Color[] paletaProcesos = {
        new Color(100, 150, 255),
        new Color(255, 120, 120),
        new Color(120, 255, 120),
        new Color(200, 140, 255),
        new Color(255, 180, 90)
    };

    private final Color COLOR_FONDO = new Color(24, 24, 24);
    private final Color COLOR_PANEL = new Color(36, 36, 36);
    private final Color COLOR_BORDE = new Color(90, 90, 90);
    private final Color COLOR_KERNEL = new Color(55, 70, 90);
    private final Color COLOR_LIBRE = new Color(30, 30, 30);
    private final Color COLOR_TEXTO_LIBRE = new Color(57, 255, 20);

    public SimuladorGUI(int tamanoRam, int porcentajeKernel, int tamanoDisco, int porcentajeIndiceDisco) {
        this.tamanoRamConfig = tamanoRam;
        this.porcentajeKernelConfig = porcentajeKernel;
        this.tamanoDiscoConfig = tamanoDisco;
        this.porcentajeIndiceDiscoConfig = porcentajeIndiceDisco;

        this.kernel = new Kernel(tamanoRam, tamanoDisco, porcentajeKernel, porcentajeIndiceDisco);
        this.memoria = kernel.getRam();
        this.disco = kernel.getDisco();

        this.dispatcher = new Dispatcher(this.memoria);
        this.cpu = new Cpu(this.memoria, this.dispatcher, this.disco, this.kernel, this);

        this.renderizadorMemoria = new ColorRowRenderer(memoria.getInicioUsuario(), memoria.getInicioUsuario());
        this.renderizadorProcesos = new ProcesoTableRenderer();

        configurarVentana();
        actualizarTablas();
        actualizarLabelsBCP();
    }

    private void configurarVentana() {
        setTitle("S.O. MiniPC - Gestión de Procesos | Jose Andrés Solano");
        setSize(1280, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout(10, 10));

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelBotones.setBackground(COLOR_PANEL);

        JButton btnCargar = new JButton("Cargar ASM");
        JButton btnPaso = new JButton("Paso a Paso");
        JButton btnTodo = new JButton("Ejecutar Todo");
        JButton btnLimpiar = new JButton("Reset");

        estilizarBotonSimple(btnCargar);
        estilizarBotonSimple(btnPaso);
        estilizarBotonSimple(btnTodo);
        estilizarBotonSimple(btnLimpiar);

        sliderVelocidad = new JSlider(100, 2000, 1000);
        sliderVelocidad.setBackground(COLOR_PANEL);
        sliderVelocidad.setForeground(Color.WHITE);
        sliderVelocidad.setInverted(true);

        JLabel lblVelocidad = new JLabel("Velocidad:");
        lblVelocidad.setForeground(Color.WHITE);

        btnCargar.addActionListener(e -> menuCargarArchivo());
        btnPaso.addActionListener(e -> ejecutarPasoAPaso());
        btnTodo.addActionListener(e -> alternarEjecucionAutomatica());
        btnLimpiar.addActionListener(e -> limpiarSistema());

        panelBotones.add(btnCargar);
        panelBotones.add(btnPaso);
        panelBotones.add(btnTodo);
        panelBotones.add(lblVelocidad);
        panelBotones.add(sliderVelocidad);
        panelBotones.add(btnLimpiar);

        add(panelBotones, BorderLayout.NORTH);

        JPanel panelCentral = new JPanel(new GridBagLayout());
        panelCentral.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);

        modeloMemoria = new DefaultTableModel(new String[]{"Dir", "Contenido"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaMemoriaFisica = crearTablaOscura(modeloMemoria);
        tablaMemoriaFisica.setDefaultRenderer(Object.class, renderizadorMemoria);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.30;
        gbc.weighty = 1.0;
        panelCentral.add(crearPanelConTitulo(new JScrollPane(tablaMemoriaFisica), "MEMORIA RAM (FÍSICA)"), gbc);

        modeloDisco = new DefaultTableModel(new String[]{"Sector", "Dato"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        modeloProcesos = new DefaultTableModel(new String[]{"ID", "Nombre", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaDisco = crearTablaOscura(modeloDisco);
        tablaProcesos = crearTablaOscura(modeloProcesos);
        tablaProcesos.setDefaultRenderer(Object.class, renderizadorProcesos);

        JPanel panelMedio = new JPanel(new GridLayout(2, 1, 0, 10));
        panelMedio.setOpaque(false);
        panelMedio.add(crearPanelConTitulo(new JScrollPane(tablaDisco), "DISCO DURO"));
        panelMedio.add(crearPanelConTitulo(new JScrollPane(tablaProcesos), "PROCESOS"));

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.35;
        panelCentral.add(panelMedio, gbc);

        JPanel panelDerecho = new JPanel(new BorderLayout(0, 10));
        panelDerecho.setOpaque(false);

        JPanel panelRegistros = new JPanel();
        panelRegistros.setLayout(new BoxLayout(panelRegistros, BoxLayout.Y_AXIS));
        panelRegistros.setBackground(new Color(25, 25, 25));

        Font f = new Font("Monospaced", Font.BOLD, 14);
        lblPC = crearLabelRegistro("PC", "---", f, Color.CYAN);
        lblIR = crearLabelRegistro("IR", "---", f, Color.WHITE);
        lblAC = crearLabelRegistro("AC", "---", f, Color.GREEN);
        lblAX = crearLabelRegistro("AX", "---", f, Color.ORANGE);
        lblBX = crearLabelRegistro("BX", "---", f, Color.ORANGE);
        lblCX = crearLabelRegistro("CX", "---", f, Color.ORANGE);
        lblDX = crearLabelRegistro("DX", "---", f, Color.ORANGE);

        panelRegistros.add(lblPC);
        panelRegistros.add(lblIR);
        panelRegistros.add(new JSeparator());
        panelRegistros.add(lblAC);
        panelRegistros.add(lblAX);
        panelRegistros.add(lblBX);
        panelRegistros.add(lblCX);
        panelRegistros.add(lblDX);

        areaTerminal = new JTextArea();
        areaTerminal.setBackground(Color.BLACK);
        areaTerminal.setForeground(Color.GREEN);
        areaTerminal.setEditable(false);
        areaTerminal.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaTerminal.setLineWrap(true);
        areaTerminal.setWrapStyleWord(true);

        lblPromptEntrada = new JLabel("Esperando instrucción de entrada...");
        lblPromptEntrada.setForeground(Color.WHITE);
        lblPromptEntrada.setFont(new Font("SansSerif", Font.BOLD, 12));

        campoEntrada = new JTextField();
        campoEntrada.setBackground(new Color(20, 20, 20));
        campoEntrada.setForeground(Color.WHITE);
        campoEntrada.setCaretColor(Color.WHITE);
        campoEntrada.setEnabled(false);

        btnEnviarEntrada = new JButton("Enviar");
        btnEnviarEntrada.setEnabled(false);
        estilizarBotonSimple(btnEnviarEntrada);

        btnEnviarEntrada.addActionListener(e -> enviarEntradaTerminal());
        campoEntrada.addActionListener(e -> enviarEntradaTerminal());

        JPanel panelEntrada = new JPanel(new BorderLayout(8, 8));
        panelEntrada.setBackground(new Color(25, 25, 25));
        panelEntrada.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelEntrada.add(lblPromptEntrada, BorderLayout.NORTH);
        panelEntrada.add(campoEntrada, BorderLayout.CENTER);
        panelEntrada.add(btnEnviarEntrada, BorderLayout.EAST);

        JPanel panelTerminalCompleto = new JPanel(new BorderLayout(0, 8));
        panelTerminalCompleto.setOpaque(false);
        panelTerminalCompleto.add(new JScrollPane(areaTerminal), BorderLayout.CENTER);
        panelTerminalCompleto.add(panelEntrada, BorderLayout.SOUTH);

        panelDerecho.add(crearPanelConTitulo(panelRegistros, "ESTADO CPU"), BorderLayout.NORTH);
        panelDerecho.add(crearPanelConTitulo(panelTerminalCompleto, "TERMINAL"), BorderLayout.CENTER);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.35;
        panelCentral.add(panelDerecho, gbc);

        add(panelCentral, BorderLayout.CENTER);

        JLabel lblFooter = new JLabel(" TEC | Sistemas Operativos | Jose Andrés Solano Vargas ");
        lblFooter.setForeground(Color.GRAY);
        add(lblFooter, BorderLayout.SOUTH);
    }

    private void estilizarBotonSimple(JButton btn) {
        btn.setBackground(new Color(70, 70, 70));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
    }

    private void alternarEjecucionAutomatica() {
        if (timerSimulacion != null && timerSimulacion.isRunning()) {
            timerSimulacion.stop();
            imprimirEnTerminal("Ejecución automática detenida.");
            return;
        }

        timerSimulacion = new Timer(sliderVelocidad.getValue(), e -> {
            boolean continua = ejecutarPasoAPaso();
             if (cpu.estaEsperandoEntradaInt09()) {
                timerSimulacion.stop();
                return;
            }
            
            if (!continua) {
                timerSimulacion.stop();
            }
        });

        timerSimulacion.start();
        imprimirEnTerminal("Ejecución automática iniciada.");
    }

    private boolean ejecutarPasoAPaso() {
        if (cpu == null) {
            return false;
        }

        boolean continua = cpu.ejecutarSiguientePaso();

        bcpActual = cpu.getProcesoActual();

        actualizarLabelsBCP();
        actualizarTablas();

        // Si el CPU quedó esperando entrada, no seguimos corriendo automático
        if (cpu.estaEsperandoEntradaInt09()) {
            return true;
        }

        if (!continua) {
            imprimirEnTerminal("No hay más procesos para ejecutar.");
        } else if (bcpActual != null && !cpu.estaEsperandoEntradaInt09()) {
            imprimirEnTerminal("Ejecutando PID " + bcpActual.id + " - " + bcpActual.nombreProceso);
        }
        

        return continua;
    }

    private void menuCargarArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] archivos = chooser.getSelectedFiles();

            for (File f : archivos) {
                boolean cargado = kernel.cargarProceso(f.getAbsolutePath());

                if (cargado) {
                    imprimirEnTerminal("CARGADO: " + f.getName());
                } else {
                    imprimirEnTerminal("ERROR al cargar: " + f.getName());
                    JOptionPane.showMessageDialog(
                            this,
                            "Memoria insuficiente para cargar: " + f.getName(),
                            "Error de carga",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

            bcpActual = cpu.getProcesoActual();
            actualizarTablas();
            actualizarLabelsBCP();
        }
    }

    public void imprimirEnTerminal(String texto) {
        areaTerminal.append("> " + texto + "\n");
        areaTerminal.setCaretPosition(areaTerminal.getDocument().getLength());
    }

    public void solicitarEntrada(String mensaje) {
        esperandoEntrada = true;
        ultimaEntrada = null;

        lblPromptEntrada.setText(mensaje);
        campoEntrada.setText("");
        campoEntrada.setEnabled(true);
        btnEnviarEntrada.setEnabled(true);
        campoEntrada.requestFocus();

        imprimirEnTerminal("[ENTRADA] " + mensaje);
    }

    private void enviarEntradaTerminal() {
        if (!esperandoEntrada) {
            return;
        }

        String texto = campoEntrada.getText().trim();

        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Debe ingresar un valor.",
                    "Entrada vacía",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        ultimaEntrada = texto;
        esperandoEntrada = false;

        imprimirEnTerminal("[USUARIO] " + texto);

        campoEntrada.setText("");
        campoEntrada.setEnabled(false);
        btnEnviarEntrada.setEnabled(false);
        lblPromptEntrada.setText("Esperando instrucción de entrada...");

        //Procesa de inmediato la entrada enviada
        ejecutarPasoAPaso();
    }

    public String consumirEntrada() {
        String entrada = ultimaEntrada;
        ultimaEntrada = null;
        return entrada;
    }

    public boolean hayEntradaDisponible() {
        return ultimaEntrada != null;
    }

    public boolean estaEsperandoEntrada() {
        return esperandoEntrada;
    }

    private void actualizarLabelsBCP() {
        bcpActual = cpu.getProcesoActual();

        if (bcpActual != null) {
            lblPC.setText("PC: " + String.format("%03d", bcpActual.PC));
            lblIR.setText("IR: " + (bcpActual.IR == null ? "---" : traducirInstruccion(bcpActual.IR)));
            lblAC.setText("AC: " + bcpActual.AC);
            lblAX.setText("AX: " + bcpActual.AX);
            lblBX.setText("BX: " + bcpActual.BX);
            lblCX.setText("CX: " + bcpActual.CX);
            lblDX.setText("DX: " + bcpActual.DX);
        } else {
            lblPC.setText("PC: ---");
            lblIR.setText("IR: ---");
            lblAC.setText("AC: ---");
            lblAX.setText("AX: ---");
            lblBX.setText("BX: ---");
            lblCX.setText("CX: ---");
            lblDX.setText("DX: ---");
        }
    }
    //Actualiza la tabla del disco
    private void actualizarTablas() {
        modeloMemoria.setRowCount(0);
        for (int i = 0; i < memoria.getTamanoTotal(); i++) {
            String raw = memoria.leerCelda(i);
            String traducida = traducirInstruccion(raw);
            modeloMemoria.addRow(new Object[]{i, traducida});
        }

        modeloDisco.setRowCount(0);
        for (int i = 0; i < disco.getTamanoTotal(); i++) {
            String dato = disco.leer(i);

            if (i < disco.getEspacioIndice()) {
                modeloDisco.addRow(new Object[]{i, dato});
            } else {
                modeloDisco.addRow(new Object[]{i, traducirInstruccion(dato)});
            }
        }

        modeloProcesos.setRowCount(0);
        for (BCP p : kernel.getListaProcesos()) {
            obtenerColorProceso(p.id);
            modeloProcesos.addRow(new Object[]{p.id, p.nombreProceso, p.estado});
        }

        int direccionActual = cpu.getDireccionIRActual();

        if (direccionActual < 0) {
            direccionActual = memoria.getInicioUsuario();
        }

        renderizadorMemoria.setConfig(direccionActual, memoria.getInicioUsuario());

        tablaMemoriaFisica.repaint();
        tablaProcesos.repaint();
    }

    private JTable crearTablaOscura(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setBackground(new Color(30, 30, 30));
        tabla.setForeground(Color.WHITE);
        tabla.setGridColor(new Color(60, 60, 60));
        tabla.setSelectionBackground(new Color(70, 70, 70));
        tabla.setSelectionForeground(Color.WHITE);
        tabla.setRowHeight(24);
        tabla.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(50, 50, 50));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));

        return tabla;
    }

    private JPanel crearPanelConTitulo(Component comp, String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                titulo
        );
        tb.setTitleColor(Color.LIGHT_GRAY);
        p.setBorder(tb);
        p.add(comp);
        return p;
    }

    private JLabel crearLabelRegistro(String reg, String val, Font f, Color c) {
        JLabel l = new JLabel(reg + ": " + val);
        l.setFont(f);
        l.setForeground(c);
        return l;
    }

    private void limpiarSistema() {
        if (timerSimulacion != null && timerSimulacion.isRunning()) {
            timerSimulacion.stop();
        }

        dispose();

        SwingUtilities.invokeLater(() -> {
            SimuladorGUI nueva = new SimuladorGUI(
                    tamanoRamConfig,
                    porcentajeKernelConfig,
                    tamanoDiscoConfig,
                    porcentajeIndiceDiscoConfig
            );
            nueva.setVisible(true);
        });
    }

    private Color obtenerColorProceso(int pid) {
        if (pid <= 0) {
            return new Color(220, 220, 220);
        }

        if (!coloresProcesos.containsKey(pid)) {
            int index = (pid - 1) % paletaProcesos.length;
            coloresProcesos.put(pid, paletaProcesos[index]);
        }

        return coloresProcesos.get(pid);
    }

    private Color colorSuave(Color base) {
        int r = (base.getRed() + 255) / 2;
        int g = (base.getGreen() + 255) / 2;
        int b = (base.getBlue() + 255) / 2;
        return new Color(r, g, b);
    }

    private BCP buscarProcesoPorDireccion(int direccion) {
        for (BCP p : kernel.getListaProcesos()) {
            if (perteneceAlProceso(p, direccion)) {
                return p;
            }
        }
        return null;
    }

    private boolean perteneceAlProceso(BCP proceso, int direccion) {
        Integer inicio = obtenerValorEnteroCampo(proceso,
                "baseMemoria", "inicioMemoria", "direccionBase", "base", "inicio");

        Integer tamano = obtenerValorEnteroCampo(proceso,
                "tamanoProceso", "limiteMemoria", "longitudProceso", "size", "tamano", "alcance");

        if (inicio != null && tamano != null) {
            return direccion >= inicio && direccion < (inicio + tamano);
        }

        Integer fin = obtenerValorEnteroCampo(proceso,
                "finMemoria", "direccionFin", "limiteSuperior", "topeMemoria");

        if (inicio != null && fin != null) {
            return direccion >= inicio && direccion <= fin;
        }

        return false;
    }

    private Integer obtenerValorEnteroCampo(Object obj, String... nombresPosibles) {
        Class<?> clase = obj.getClass();

        for (String nombre : nombresPosibles) {
            try {
                Field campo = clase.getDeclaredField(nombre);
                campo.setAccessible(true);
                Object valor = campo.get(obj);

                if (valor instanceof Integer) {
                    return (Integer) valor;
                }

                if (valor != null) {
                    return Integer.parseInt(valor.toString());
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    class ColorRowRenderer extends DefaultTableCellRenderer {
        private int pcActual;
        private int limiteKernel;

        public ColorRowRenderer(int pc, int limite) {
            this.pcActual = pc;
            this.limiteKernel = limite;
        }

        public void setConfig(int pc, int limite) {
            this.pcActual = pc;
            this.limiteKernel = limite;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (row < limiteKernel) {
                c.setBackground(new Color(255, 255, 200));
                c.setForeground(Color.BLACK);
                c.setFont(c.getFont().deriveFont(Font.PLAIN));
            } else {
                BCP procesoDeFila = buscarProcesoPorDireccion(row);

                if (procesoDeFila != null) {
                    Color colorPid = obtenerColorProceso(procesoDeFila.id);

                    if (bcpActual != null && procesoDeFila.id == bcpActual.id && row == pcActual) {
                        c.setBackground(colorPid);
                        c.setForeground(Color.BLACK);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setBackground(colorSuave(colorPid));
                        c.setForeground(Color.BLACK);
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    }
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
            }

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            }

            return c;
        }
    }

    class ProcesoTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );

            try {
                int pid = Integer.parseInt(table.getValueAt(row, 0).toString());
                String estado = table.getValueAt(row, 2).toString();

                Color colorPid = obtenerColorProceso(pid);

                if ("EJECUCION".equalsIgnoreCase(estado) || "EJECUCION (CPU)".equalsIgnoreCase(estado)) {
                    c.setBackground(colorPid);
                    c.setForeground(Color.BLACK);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if ("ERROR".equalsIgnoreCase(estado)) {
                    c.setBackground(new Color(170, 60, 60));
                    c.setForeground(Color.WHITE);
                } else if ("TERMINADO".equalsIgnoreCase(estado)) {
                    c.setBackground(new Color(90, 90, 90));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(colorSuave(colorPid));
                    c.setForeground(Color.BLACK);
                }
            } catch (Exception e) {
                c.setBackground(new Color(30, 30, 30));
                c.setForeground(Color.WHITE);
            }

            if (isSelected) {
                c.setBackground(c.getBackground().darker());
                c.setForeground(Color.WHITE);
            }

            return c;
        }
    }

    private Color obtenerColorPorPID(int pid) {
        switch (pid) {
            case 1:
                return new Color(255, 255, 120);
            case 2:
                return new Color(255, 120, 120);
            case 3:
                return new Color(120, 255, 120);
            case 4:
                return new Color(200, 140, 255);
            case 5:
                return new Color(255, 180, 90);
            default:
                return new Color(173, 216, 230);
        }
    }
}
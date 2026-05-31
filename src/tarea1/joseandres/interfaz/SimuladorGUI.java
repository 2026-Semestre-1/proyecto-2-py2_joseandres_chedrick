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
import java.util.List;

import tarea1.joseandres.cpu.Cpu;
import tarea1.joseandres.memoria.Memoria;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.disco.Disco;
import tarea1.joseandres.dispacher.Dispatcher;
import tarea1.joseandres.kernel.Kernel;
import static tarea1.joseandres.loader.Loader.traducirInstruccion;
import tarea1.joseandres.memoria.Particion;

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

    private String tipoMemoriaConfig;
    private int cantParticionesConfig;
    private int[] tamanosParticionesConfig;

    private int pidActualVisual = 1;
    private final Map<Integer, Color> coloresPID = new HashMap<>();

    private DefaultTableModel modeloMemoria, modeloDisco, modeloProcesos, modeloParticiones;
    private JTable tablaMemoriaFisica;
    private JTable tablaDisco;
    private JTable tablaProcesos;
    private JTable tablaParticiones;

    private ColorRowRenderer renderizadorMemoria;
    private ProcesoTableRenderer renderizadorProcesos;
    private ParticionTableRenderer renderizadorParticiones;

    private JLabel lblPC, lblIR, lblAC, lblAX, lblBX, lblCX, lblDX;

    private JTextArea areaTerminal;
    private JTextField campoEntrada;
    private JLabel lblPromptEntrada;
    private JButton btnEnviarEntrada;
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

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public SimuladorGUI(Kernel kernel) {
        this.kernel = kernel;
        this.memoria = kernel.getRam();
        this.disco = kernel.getDisco();

        this.tamanoRamConfig = this.memoria.getTamanoTotal();
        this.tamanoDiscoConfig = this.disco.getTamanoTotal();
        this.porcentajeKernelConfig = (int) ((this.memoria.getInicioUsuario() * 100.0) / this.tamanoRamConfig);
        this.porcentajeIndiceDiscoConfig = (int) ((this.disco.getEspacioIndice() * 100.0) / this.tamanoDiscoConfig);

        // 🔄 DETECCIÓN ROBUSTA DE CONFIGURACIÓN DE MEMORIA
        try {
            Field fieldTipo = kernel.getClass().getDeclaredField("tipoMemoria");
            fieldTipo.setAccessible(true);
            this.tipoMemoriaConfig = (String) fieldTipo.get(kernel);
        } catch (Exception e) {
            this.tipoMemoriaConfig = kernel.getMemoriaFija().getParticiones().isEmpty() ? "DINAMICA" : "FIJA_IGUAL";
        }

        if (kernel.getMemoriaFija() != null && !kernel.getMemoriaFija().getParticiones().isEmpty()) {
            this.cantParticionesConfig = kernel.getMemoriaFija().getParticiones().size();
            this.tamanosParticionesConfig = new int[this.cantParticionesConfig];
            for (int i = 0; i < this.cantParticionesConfig; i++) {
                this.tamanosParticionesConfig[i] = kernel.getMemoriaFija().getParticiones().get(i).getTamano();
            }
        } else {
            this.cantParticionesConfig = 0;
            this.tamanosParticionesConfig = new int[0];
        }

        this.dispatcher = new Dispatcher(this.memoria);
        this.cpu = new Cpu(this.memoria, this.dispatcher, this.disco, this.kernel, this);

        this.renderizadorMemoria = new ColorRowRenderer(memoria.getInicioUsuario(), memoria.getInicioUsuario());
        this.renderizadorProcesos = new ProcesoTableRenderer();
        this.renderizadorParticiones = new ParticionTableRenderer();

        configurarVentana();
        actualizarTablas();
        actualizarLabelsBCP();
    }

    // =========================================================================
    // CONFIGURACIÓN DE VENTANA
    // =========================================================================
    private void configurarVentana() {
        setTitle("S.O. MiniPC - Gestión de Procesos | Jose Andrés Solano");

        setSize(1600, 900);
        setMinimumSize(new Dimension(1400, 750));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout(8, 8));

        add(crearBarraHerramientas(), BorderLayout.NORTH);

        JPanel panelCentral = new JPanel(new GridBagLayout());
        panelCentral.setOpaque(false);
        panelCentral.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.weightx = 0.17;
        panelCentral.add(crearPanelRAM(), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.17;
        panelCentral.add(crearPanelDisco(), gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.38;
        panelCentral.add(crearPanelProcesoYParticiones(), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.28;
        panelCentral.add(crearPanelCpuYTerminal(), gbc);

        add(panelCentral, BorderLayout.CENTER);

        JLabel lblFooter = new JLabel("  TEC | Sistemas Operativos | Jose Andrés Solano Vargas  ");
        lblFooter.setForeground(Color.GRAY);
        lblFooter.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
        add(lblFooter, BorderLayout.SOUTH);
    }

    private JPanel crearBarraHerramientas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDE));

        JButton btnCargar = new JButton("⬆  Cargar ASM");
        JButton btnPaso = new JButton("⏭  Paso a Paso");
        JButton btnTodo = new JButton("▶  Ejecutar Todo");
        JButton btnLimpiar = new JButton("↺  Reset");

        estilizarBotonSimple(btnCargar);
        estilizarBotonSimple(btnPaso);
        estilizarBotonSimple(btnTodo);
        estilizarBotonSimple(btnLimpiar);

        btnCargar.addActionListener(e -> menuCargarArchivo());
        btnPaso.addActionListener(e -> ejecutarPasoAPaso());
        btnTodo.addActionListener(e -> alternarEjecucionAutomatica());
        btnLimpiar.addActionListener(e -> limpiarSistema());

        panel.add(btnCargar);
        panel.add(btnPaso);
        panel.add(btnTodo);
        panel.add(Box.createHorizontalStrut(16));
        panel.add(btnLimpiar);

        return panel;
    }

    private JPanel crearPanelRAM() {
        modeloMemoria = new DefaultTableModel(new String[]{"Dir", "Contenido"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaMemoriaFisica = crearTablaOscura(modeloMemoria);
        tablaMemoriaFisica.setDefaultRenderer(Object.class, renderizadorMemoria);

        JScrollPane scroll = new JScrollPane(tablaMemoriaFisica);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return crearPanelConTitulo(scroll, "MEMORIA RAM  (FÍSICA)");
    }

    private JPanel crearPanelDisco() {
        modeloDisco = new DefaultTableModel(new String[]{"Sector", "Dato"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaDisco = crearTablaOscura(modeloDisco);

        JScrollPane scroll = new JScrollPane(tablaDisco);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return crearPanelConTitulo(scroll, "DISCO DURO");
    }

    private JPanel crearPanelProcesoYParticiones() {
        modeloProcesos = new DefaultTableModel(new String[]{"ID", "Nombre", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaProcesos = crearTablaOscura(modeloProcesos);
        tablaProcesos.setDefaultRenderer(Object.class, renderizadorProcesos);

        JScrollPane scrollProcesos = new JScrollPane(tablaProcesos);
        scrollProcesos.getVerticalScrollBar().setUnitIncrement(12);

        // 🔄 CONFIGURACIÓN INICIAL DE LAS COLUMNAS MUTANTES SEGÚN EL MODO ACTIVO
        String[] cabeceras = "DINAMICA".equalsIgnoreCase(tipoMemoriaConfig)
                ? new String[]{"Bloque Dinámico", "Inicio", "Fin", "Tamaño", "Estado", "Atributo", "-"}
                : new String[]{"Partición", "Inicio", "Fin", "Tamaño", "Estado", "Utilizado", "Desperdicio"};

        modeloParticiones = new DefaultTableModel(cabeceras, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaParticiones = crearTablaOscura(modeloParticiones);
        tablaParticiones.setDefaultRenderer(Object.class, renderizadorParticiones);

        tablaParticiones.setRowHeight(24);
        tablaParticiones.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tablaParticiones.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tablaParticiones.getTableHeader().setPreferredSize(new Dimension(0, 26));

        tablaParticiones.getColumnModel().getColumn(0).setPreferredWidth(110);
        tablaParticiones.getColumnModel().getColumn(1).setPreferredWidth(55);
        tablaParticiones.getColumnModel().getColumn(2).setPreferredWidth(55);
        tablaParticiones.getColumnModel().getColumn(3).setPreferredWidth(85);
        tablaParticiones.getColumnModel().getColumn(4).setPreferredWidth(180);
        tablaParticiones.getColumnModel().getColumn(5).setPreferredWidth(110);
        tablaParticiones.getColumnModel().getColumn(6).setPreferredWidth(110);

        tablaParticiones.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollParticiones = new JScrollPane(tablaParticiones,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollParticiones.getVerticalScrollBar().setUnitIncrement(16);
        scrollParticiones.getHorizontalScrollBar().setUnitIncrement(20);

        String tituloSeccion = "DINAMICA".equalsIgnoreCase(tipoMemoriaConfig) ? "MAPA DE BLOQUES (DINÁMICO)" : "MAPA DE PARTICIONES";

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                crearPanelConTitulo(scrollProcesos, "PROCESOS EN COLA"),
                crearPanelConTitulo(scrollParticiones, tituloSeccion));
        split.setResizeWeight(0.40);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setBackground(COLOR_FONDO);
        split.setOpaque(false);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(split, BorderLayout.CENTER);
        return contenedor;
    }

    private JPanel crearPanelCpuYTerminal() {
        JPanel panelRegistros = new JPanel(new GridLayout(7, 1, 0, 2));
        panelRegistros.setBackground(new Color(22, 22, 30));
        panelRegistros.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        Font f = new Font("Monospaced", Font.BOLD, 14);
        lblPC = crearLabelRegistro("PC", "---", f, new Color(0, 220, 255));
        lblIR = crearLabelRegistro("IR", "---", f, Color.WHITE);
        lblAC = crearLabelRegistro("AC", "---", f, new Color(80, 255, 120));
        lblAX = crearLabelRegistro("AX", "---", f, new Color(255, 180, 60));
        lblBX = crearLabelRegistro("BX", "---", f, new Color(255, 180, 60));
        lblCX = crearLabelRegistro("CX", "---", f, new Color(255, 180, 60));
        lblDX = crearLabelRegistro("DX", "---", f, new Color(255, 180, 60));

        for (JLabel l : new JLabel[]{lblPC, lblIR, lblAC, lblAX, lblBX, lblCX, lblDX}) {
            panelRegistros.add(l);
        }

        JPanel cpuWrapper = crearPanelConTitulo(panelRegistros, "ESTADO CPU");
        cpuWrapper.setPreferredSize(new Dimension(0, 200));

        areaTerminal = new JTextArea();
        areaTerminal.setBackground(new Color(8, 8, 8));
        areaTerminal.setForeground(new Color(57, 255, 20));
        areaTerminal.setEditable(false);
        areaTerminal.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaTerminal.setLineWrap(true);
        areaTerminal.setWrapStyleWord(true);

        lblPromptEntrada = new JLabel("Esperando instrucción de entrada...");
        lblPromptEntrada.setForeground(Color.WHITE);
        lblPromptEntrada.setFont(new Font("SansSerif", Font.BOLD, 12));

        campoEntrada = new JTextField();
        campoEntrada.setBackground(new Color(18, 18, 18));
        campoEntrada.setForeground(Color.WHITE);
        campoEntrada.setCaretColor(Color.WHITE);
        campoEntrada.setEnabled(false);

        btnEnviarEntrada = new JButton("Enviar");
        btnEnviarEntrada.setEnabled(false);
        estilizarBotonSimple(btnEnviarEntrada);
        btnEnviarEntrada.addActionListener(e -> enviarEntradaTerminal());
        campoEntrada.addActionListener(e -> enviarEntradaTerminal());

        JPanel panelEntrada = new JPanel(new BorderLayout(6, 6));
        panelEntrada.setBackground(new Color(22, 22, 22));
        panelEntrada.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        panelEntrada.add(lblPromptEntrada, BorderLayout.NORTH);
        panelEntrada.add(campoEntrada, BorderLayout.CENTER);
        panelEntrada.add(btnEnviarEntrada, BorderLayout.EAST);

        JPanel terminalWrapper = new JPanel(new BorderLayout(0, 6));
        terminalWrapper.setOpaque(false);
        terminalWrapper.add(new JScrollPane(areaTerminal), BorderLayout.CENTER);
        terminalWrapper.add(panelEntrada, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                cpuWrapper,
                crearPanelConTitulo(terminalWrapper, "TERMINAL"));
        split.setResizeWeight(0.22);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setOpaque(false);
        split.setBackground(COLOR_FONDO);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(split, BorderLayout.CENTER);
        return contenedor;
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
        timerSimulacion = new Timer(600, e -> {
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
            for (File f : chooser.getSelectedFiles()) {
                boolean cargado = kernel.cargarProceso(f.getAbsolutePath());
                if (cargado) {
                    imprimirEnTerminal("CARGADO: " + f.getName());
                } else {
                    imprimirEnTerminal("ERROR al cargar: " + f.getName());
                    JOptionPane.showMessageDialog(this,
                            "Memoria insuficiente para cargar: " + f.getName(),
                            "Error de carga", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Debe ingresar un valor.", "Entrada vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ultimaEntrada = texto;
        esperandoEntrada = false;
        imprimirEnTerminal("[USUARIO] " + texto);
        campoEntrada.setText("");
        campoEntrada.setEnabled(false);
        btnEnviarEntrada.setEnabled(false);
        lblPromptEntrada.setText("Esperando instrucción de entrada...");
        ejecutarPasoAPaso();
    }

    public String consumirEntrada() {
        String e = ultimaEntrada;
        ultimaEntrada = null;
        return e;
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

    // =========================================================================
    // 🔄 RE-RENDERIZADO DE TABLAS COMPATIBLE CON AMBOS MODOS
    // =========================================================================
    private void actualizarTablas() {
        if (memoria == null || disco == null || kernel == null) {
            return;
        }

        // 1. RAM
        modeloMemoria.setRowCount(0);
        for (int i = 0; i < memoria.getTamanoTotal(); i++) {
            modeloMemoria.addRow(new Object[]{i, traducirInstruccion(memoria.leerCelda(i))});
        }

        // 2. Disco
        modeloDisco.setRowCount(0);
        for (int i = 0; i < disco.getTamanoTotal(); i++) {
            String dato = disco.leer(i);
            modeloDisco.addRow(new Object[]{i, (i < disco.getEspacioIndice()) ? dato : traducirInstruccion(dato)});
        }

        // 3. Procesos
        modeloProcesos.setRowCount(0);
        if (kernel.getListaProcesos() != null) {
            for (BCP p : kernel.getListaProcesos()) {
                if (p != null) {
                    obtenerColorProceso(p.id);
                    modeloProcesos.addRow(new Object[]{p.id, p.nombreProceso, p.estado});
                }
            }
        }

        // 4. MAPEO INTEGRADO: PARTICIONES FIJAS VS BLOQUES DINÁMICOS
        modeloParticiones.setRowCount(0);

        if ("DINAMICA".equalsIgnoreCase(tipoMemoriaConfig)) {
            // 💡 ENRUTAMIENTO DINÁMICO: Consume directamente la lista mutante de MemoriaDinamica
            List<Particion> bloquesDinamicos = kernel.getMemoriaDinamica().getBloques();
            if (bloquesDinamicos != null) {
                for (Particion b : bloquesDinamicos) {
                    int inicio = b.getInicio();
                    int tamano = b.getTamano();
                    String estado = b.isLibre() ? "LIBRE (Hueco disponible)" : "OCUPADO - PID: " + (b.getProceso() != null ? b.getProceso().id : "?");
                    String atributo = b.isLibre() ? "Bloque Limpio" : "Proceso Activo: " + (b.getProceso() != null ? b.getProceso().nombreProceso : "-");

                    modeloParticiones.addRow(new Object[]{
                        "Bloque #" + b.getNumero(),
                        inicio,
                        inicio + tamano - 1,
                        tamano + " celdas",
                        estado,
                        atributo,
                        "---" // Columna inútil en dinámico
                    });
                }
            }
        } else {
            // 💡 ENRUTAMIENTO FIJO ORIGINAL
            List<Particion> listaParticiones = kernel.getMemoriaFija().getParticiones();
            if (listaParticiones != null && !listaParticiones.isEmpty()) {
                for (Particion p : listaParticiones) {
                    int inicio = p.getInicio();
                    int tamano = p.getTamano();
                    String estado;
                    String utilizado;
                    String desperdicio;

                    if (p.isLibre()) {
                        estado = "LIBRE (Vacía)";
                        utilizado = "---";
                        desperdicio = "---";
                    } else {
                        int pid = (p.getProceso() != null) ? p.getProceso().id : -1;
                        estado = "OCUPADA - PID: " + (pid > 0 ? pid : "?");

                        if (p.getProceso() != null) {
                            int usado = p.getProceso().getAlcance();
                            int fragInt = tamano - usado;
                            double pctUtil = (tamano > 0) ? ((double) usado / tamano * 100) : 0;
                            double pctDesp = (tamano > 0) ? ((double) fragInt / tamano * 100) : 0;
                            utilizado = String.format("%d celdas (%.1f%%)", usado, pctUtil);
                            desperdicio = String.format("%d celdas (%.1f%%)", fragInt, pctDesp);
                        } else {
                            utilizado = "?";
                            desperdicio = "?";
                        }
                    }

                    modeloParticiones.addRow(new Object[]{
                        "Partición #" + p.getNumero(),
                        inicio,
                        inicio + tamano - 1,
                        tamano + " celdas",
                        estado,
                        utilizado,
                        desperdicio
                    });
                }
            }
        }

        // Renderizador RAM
        int dirActual = -1;
        if (cpu != null) {
            try {
                dirActual = cpu.getDireccionIRActual();
            } catch (Exception ignored) {
            }
        }
        if (dirActual < 0) {
            dirActual = memoria.getInicioUsuario();
        }
        renderizadorMemoria.setConfig(dirActual, memoria.getInicioUsuario());

        tablaMemoriaFisica.repaint();
        tablaProcesos.repaint();
        tablaParticiones.repaint();
    }

    private JTable crearTablaOscura(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setBackground(new Color(28, 28, 28));
        tabla.setForeground(Color.WHITE);
        tabla.setGridColor(new Color(55, 55, 55));
        tabla.setSelectionBackground(new Color(70, 70, 70));
        tabla.setSelectionForeground(Color.WHITE);
        tabla.setRowHeight(22);
        tabla.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tabla.setIntercellSpacing(new Dimension(4, 2));

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(45, 45, 60));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 28));

        return tabla;
    }

    private JPanel crearPanelConTitulo(Component comp, String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)), titulo);
        tb.setTitleColor(new Color(200, 200, 200));
        tb.setTitleFont(new Font("SansSerif", Font.BOLD, 11));
        p.setBorder(tb);
        p.add(comp);
        return p;
    }

    private JLabel crearLabelRegistro(String reg, String val, Font f, Color c) {
        JLabel l = new JLabel(reg + ": " + val);
        l.setFont(f);
        l.setForeground(c);
        l.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        return l;
    }

    private void limpiarSistema() {
        if (timerSimulacion != null && timerSimulacion.isRunning()) {
            timerSimulacion.stop();
        }
        dispose();
        SwingUtilities.invokeLater(() -> {
            Kernel nuevoKernel = new Kernel(
                    tamanoRamConfig, tamanoDiscoConfig,
                    porcentajeKernelConfig, porcentajeIndiceDiscoConfig,
                    tipoMemoriaConfig, cantParticionesConfig, tamanosParticionesConfig);
            SimuladorGUI nueva = new SimuladorGUI(nuevoKernel);
            nueva.setLocationRelativeTo(null);
            nueva.setVisible(true);
        });
    }

    private Color obtenerColorProceso(int pid) {
        if (pid <= 0) {
            return new Color(220, 220, 220);
        }
        if (!coloresProcesos.containsKey(pid)) {
            coloresProcesos.put(pid, paletaProcesos[(pid - 1) % paletaProcesos.length]);
        }
        return coloresProcesos.get(pid);
    }

    private Color colorSuave(Color base) {
        return new Color((base.getRed() + 255) / 2, (base.getGreen() + 255) / 2, (base.getBlue() + 255) / 2);
    }

    private BCP buscarProcesoPorDireccion(int direccion) {
        for (BCP p : kernel.getListaProcesos()) {
            if (perteneceAlProceso(p, direccion)) {
                return p;
            }
        }
        return null;
    }

    private boolean belongsToProcess(BCP process, int address) {
        return perteneceAlProceso(process, address);
    }

    private boolean perteneceAlProceso(BCP proceso, int direccion) {
        Integer inicio = obtenerValorEnteroCampo(proceso, "baseMemoria", "inicioMemoria", "direccionBase", "base", "inicio");
        Integer tamano = obtenerValorEnteroCampo(proceso, "tamanoProceso", "limiteMemoria", "longitudProceso", "size", "tamano", "alcance");
        if (inicio != null && tamano != null) {
            return direccion >= inicio && direccion < (inicio + tamano);
        }
        Integer fin = obtenerValorEnteroCampo(proceso, "finMemoria", "direccionFin", "limiteSuperior", "topeMemoria");
        if (inicio != null && fin != null) {
            return direccion >= inicio && direccion <= fin;
        }
        return false;
    }

    private Integer obtenerValorEnteroCampo(Object obj, String... nombres) {
        for (String nombre : nombres) {
            try {
                Field campo = obj.getClass().getDeclaredField(nombre);
                campo.setAccessible(true);
                Object valor = campo.get(obj);
                if (valor instanceof Integer) {
                    return (Integer) valor;
                }
                if (valor != null) {
                    return Integer.parseInt(valor.toString());
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // =========================================================================
    // RENDERIZADORES PERSONALIZADOS
    // =========================================================================
    class ColorRowRenderer extends DefaultTableCellRenderer {

        private int pcActual, limiteKernel;

        ColorRowRenderer(int pc, int limite) {
            this.pcActual = pc;
            this.limiteKernel = limite;
        }

        void setConfig(int pc, int limite) {
            this.pcActual = pc;
            this.limiteKernel = limite;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(table.getFont());

            if (row < limiteKernel) {
                c.setBackground(COLOR_KERNEL);
                c.setForeground(Color.WHITE);
                return c;
            }

            BCP proc = buscarProcesoPorDireccion(row);
            if (proc != null) {
                Color colorPid = obtenerColorProceso(proc.id);
                int relPos = row - proc.getDireccionBase();
                if (relPos >= 0 && relPos < proc.getAlcance()) {
                    if (bcpActual != null && proc.id == bcpActual.id && row == pcActual) {
                        c.setBackground(colorPid.brighter());
                        c.setForeground(Color.BLACK);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setBackground(colorSuave(colorPid));
                        c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setBackground(new Color(45, 40, 40));
                    c.setForeground(new Color(230, 100, 100));
                    if (column == 1) {
                        setText("❌ [Frag. Interna - Bloque de PID " + proc.id + "]");
                    }
                }
            } else {
                c.setBackground(COLOR_LIBRE);
                c.setForeground(COLOR_TEXTO_LIBRE);
            }

            if (isSelected) {
                c.setBackground(c.getBackground().darker());
                c.setForeground(Color.WHITE);
            }
            return c;
        }
    }

    class ProcesoTableRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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

    class ParticionTableRenderer extends DefaultTableCellRenderer {

        ParticionTableRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

            try {
                String estadoStr = table.getValueAt(row, 4).toString();

                if (estadoStr.contains("LIBRE")) {
                    c.setBackground(new Color(28, 48, 28));
                    c.setForeground(new Color(80, 255, 80));
                    c.setFont((column == 4)
                            ? c.getFont().deriveFont(Font.BOLD)
                            : table.getFont());

                } else {
                    String[] partes = estadoStr.split("PID: ");
                    Color base = (partes.length > 1)
                            ? colorSuave(obtenerColorProceso(Integer.parseInt(partes[1].trim())))
                            : new Color(60, 40, 40);
                    Color fg = (partes.length > 1) ? new Color(20, 20, 20) : Color.WHITE;

                    // 🔄 FILTRO DE SEGURIDAD PARA MODO DINÁMICO
                    if ("DINAMICA".equalsIgnoreCase(tipoMemoriaConfig)) {
                        c.setBackground(base);
                        c.setForeground(fg);
                        return c;
                    }

                    // Columna "Desperdicio" (6): tono rojizo si hay fragmentación real (Solo Fijo)
                    if (column == 6 && partes.length > 1) {
                        String desp = table.getValueAt(row, 6).toString();
                        if (!desp.equals("---") && !desp.startsWith("0 ")) {
                            c.setBackground(new Color(90, 35, 35));
                            c.setForeground(new Color(255, 120, 120));
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                            if (isSelected) {
                                c.setBackground(c.getBackground().darker());
                            }
                            return c;
                        }
                    }

                    // Columna "Utilizado" (5): tono verde si aprovechamiento alto (Solo Fijo)
                    if (column == 5 && partes.length > 1) {
                        String util = table.getValueAt(row, 5).toString();
                        if (util.contains("100.0%")) {
                            c.setBackground(new Color(28, 60, 28));
                            c.setForeground(new Color(80, 255, 80));
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                            if (isSelected) {
                                c.setBackground(c.getBackground().darker());
                            }
                            return c;
                        }
                    }

                    c.setBackground(base);
                    c.setForeground(fg);
                    c.setFont((column == 4)
                            ? c.getFont().deriveFont(Font.BOLD)
                            : table.getFont());
                }
            } catch (Exception e) {
                c.setBackground(new Color(35, 35, 35));
                c.setForeground(Color.WHITE);
                c.setFont(table.getFont());
            }

            if (isSelected) {
                c.setBackground(c.getBackground().darker());
                c.setForeground(Color.WHITE);
            }
            return c;
        }
    }
}
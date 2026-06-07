package tarea1.joseandres.interfaz;

import ThreadUtils.ProcesoCargaCallback;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tarea1.joseandres.cpu.Cpu;
import tarea1.joseandres.memoria.Memoria;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.disco.Disco;
import tarea1.joseandres.dispacher.Dispatcher;
import tarea1.joseandres.kernel.Kernel;
import static tarea1.joseandres.loader.Loader.traducirInstruccion;
import tarea1.joseandres.memoria.Particion;
import tarea1.joseandres.memoria.MemoriaPaginada;
import tarea1.joseandres.memoria.TablaPaginas;
import ThreadUtils.CronometroThread;
import ThreadUtils.ObservadorCargaThread;
import tarea1.joseandres.algoritmos.PlanificadorFCFS;
import tarea1.joseandres.algoritmos.PlanificadorHRRN;
import tarea1.joseandres.algoritmos.PlanificadorRoundRobin;
import tarea1.joseandres.algoritmos.PlanificadorSJF;

public class SimuladorGUI extends JFrame {

    // =========================================================================
    // ESTADO DEL SISTEMA 2.0
    // =========================================================================
    private Kernel kernel;
    private Memoria memoria;
    private Disco disco;
    private Dispatcher dispatcher;

    // --- MULTIHILO: lista de CPUs activas y sus hilos ---
    private final List<Cpu>    cpus    = new ArrayList<>();
    private final List<Thread> hilosCpu = new ArrayList<>();

    // Cantidad de CPUs que el usuario seleccionó en el ComboBox (se inicializa desde el JSON)
    private int cantidadCpusActiva = 2;

    private BCP bcpActual;
    private Timer timerSimulacion;

    // --- Config persistida para el Reset ---
    private int    tamanoRamConfig;
    private int    porcentajeKernelConfig;
    private int    tamanoDiscoConfig;
    private int    porcentajeIndiceDiscoConfig;
    private String tipoMemoriaConfig;
    private int    cantParticionesConfig;
    private int[]  tamanosParticionesConfig;

    // Paginación
    private int tamanoPagina = 0;

    // Tabla de páginas visual
    private DefaultTableModel modeloTablaPaginas;
    private JTable            tablaPaginasVisual;
    private JPanel            panelTablaPaginas;
    private CronometroThread cronometro;
    private ObservadorCargaThread observador;
    private List<Object[]> configuracionProcesos = new ArrayList<>();
    JComboBox comboQuantum = new JComboBox<>(new Integer[]{1, 2, 3, 4});
    JComboBox comboAlgoritmo = new JComboBox<>(new String[]{"FCFS", "SRT", "SJF", "RR", "HRRN", "SRR", "Lottery"});

    private int pidActualVisual = 1;
    private final Map<Integer, Color> coloresPID      = new HashMap<>();
    private final Map<Integer, Color> coloresProcesos  = new HashMap<>();
    private final Color[] paletaProcesos = {
        new Color(100, 150, 255),
        new Color(255, 120, 120),
        new Color(120, 255, 120),
        new Color(200, 140, 255),
        new Color(255, 180,  90)
    };

    // =========================================================================
    // MODELOS Y TABLAS
    // =========================================================================
    private DefaultTableModel modeloMemoria, modeloDisco, modeloProcesos, modeloParticiones;
    private JTable tablaMemoriaFisica, tablaDisco, tablaProcesos, tablaParticiones;

    private ColorRowRenderer       renderizadorMemoria;
    private ProcesoTableRenderer   renderizadorProcesos;
    private ParticionTableRenderer renderizadorParticiones;

    // =========================================================================
    // PANELES DE CPU  (arreglo 4 paneles, índices 0-3)
    //
    //  cpuPanelRegistros[i][j]  → JLabel del registro j en la CPU i
    //      j = 0 → PID/Nombre
    //      j = 1 → PC
    //      j = 2 → IR
    //      j = 3 → AC
    //      j = 4 → AX
    //      j = 5 → BX
    //      j = 6 → CX
    //      j = 7 → DX
    // =========================================================================
    private static final int MAX_CPUS = 4;
    private static final int REGS_POR_CPU = 8;   // PID, PC, IR, AC, AX, BX, CX, DX

    /** cpuPanelRegistros[cpuId][registroIdx] */
    private JLabel[][] cpuPanelRegistros;

    /** Panel contenedor de cada CPU (para activar/desactivar visualmente) */
    private JPanel[] cpuPanelContenedor;

    // =========================================================================
    // TERMINAL Y ENTRADA
    // =========================================================================
    private JComboBox<Integer> comboCpus;
    private JTextArea  areaTerminal;
    private JTextField campoEntrada;
    private JLabel     lblPromptEntrada;
    private JButton    btnEnviarEntrada;
    private String     ultimaEntrada   = null;
    private boolean    esperandoEntrada = false;

    // =========================================================================
    // COLORES GLOBALES
    // =========================================================================
    private final Color COLOR_FONDO        = new Color(24,  24,  24);
    private final Color COLOR_PANEL        = new Color(36,  36,  36);
    private final Color COLOR_BORDE        = new Color(90,  90,  90);
    private final Color COLOR_KERNEL       = new Color(55,  70,  90);
    private final Color COLOR_LIBRE        = new Color(30,  30,  30);
    private final Color COLOR_TEXTO_LIBRE  = new Color(57, 255,  20);
    private final Color COLOR_CPU_INACTIVA = new Color(45,  45,  45);

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public SimuladorGUI(Kernel kernel, int cantidadCpusJson) {
        this.kernel   = kernel;
        this.memoria  = kernel.getRam();
        this.disco    = kernel.getDisco();

        this.tamanoRamConfig        = this.memoria.getTamanoTotal();
        this.tamanoDiscoConfig      = this.disco.getTamanoTotal();
        this.porcentajeKernelConfig = (int)((this.memoria.getInicioUsuario() * 100.0) / this.tamanoRamConfig);
        this.porcentajeIndiceDiscoConfig = (int)((this.disco.getEspacioIndice() * 100.0) / this.tamanoDiscoConfig);

        // Detectar tipo de memoria
        try {
            Field fieldTipo = kernel.getClass().getDeclaredField("tipoMemoria");
            fieldTipo.setAccessible(true);
            this.tipoMemoriaConfig = (String) fieldTipo.get(kernel);
        } catch (Exception e) {
            this.tipoMemoriaConfig = kernel.getMemoriaFija().getParticiones().isEmpty() ? "DINAMICA" : "FIJA_IGUAL";
        }

        if (kernel.getMemoriaFija() != null && !kernel.getMemoriaFija().getParticiones().isEmpty()) {
            this.cantParticionesConfig    = kernel.getMemoriaFija().getParticiones().size();
            this.tamanosParticionesConfig = new int[this.cantParticionesConfig];
            for (int i = 0; i < this.cantParticionesConfig; i++) {
                this.tamanosParticionesConfig[i] = kernel.getMemoriaFija().getParticiones().get(i).getTamano();
            }
        } else {
            this.cantParticionesConfig    = 0;
            this.tamanosParticionesConfig = new int[0];
        }

        // Leer tamaño de página desde MemoriaPaginada (fallback seguro: 16)
        try {
            tarea1.joseandres.memoria.MemoriaPaginada mp = this.kernel.getMemoriaPaginada();
            this.tamanoPagina = (mp != null) ? mp.getTamanoPagina() : 16;
        } catch (Exception e) {
            this.tamanoPagina = 16; // Fallback por defecto si no está activa
            System.out.println("GUI: Usando tamaño de página 16 por defecto.");
        }

        this.dispatcher = new Dispatcher(this.memoria);

        this.renderizadorMemoria     = new ColorRowRenderer(memoria.getInicioUsuario(), memoria.getInicioUsuario());
        this.renderizadorProcesos    = new ProcesoTableRenderer();
        this.renderizadorParticiones = new ParticionTableRenderer();

        // Reservamos arreglos para los 4 paneles (se llenan en crearPanelCpuYTerminal)
        cpuPanelRegistros  = new JLabel[MAX_CPUS][REGS_POR_CPU];
        cpuPanelContenedor = new JPanel[MAX_CPUS];

        // Valor leído del JSON → limitar entre 1 y MAX_CPUS
        this.cantidadCpusActiva = Math.max(1, Math.min(MAX_CPUS, cantidadCpusJson));

        configurarVentana();
        actualizarTablas();
        actualizarPanelesCpu();
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
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.insets  = new Insets(4, 4, 4, 4);
        gbc.gridy   = 0;
        gbc.weighty = 1.0;

        gbc.gridx   = 0; gbc.weightx = 0.15;
        panelCentral.add(crearPanelRAM(), gbc);

        gbc.gridx   = 1; gbc.weightx = 0.15;
        panelCentral.add(crearPanelDisco(), gbc);

        gbc.gridx   = 2; gbc.weightx = 0.36;
        panelCentral.add(crearPanelProcesoYParticiones(), gbc);

        gbc.gridx   = 3; gbc.weightx = 0.34;
        panelCentral.add(crearPanelCpuYTerminal(), gbc);

        add(panelCentral, BorderLayout.CENTER);

        JLabel lblFooter = new JLabel("  TEC | Sistemas Operativos | Jose Andrés Solano Vargas  ");
        lblFooter.setForeground(Color.GRAY);
        lblFooter.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
        add(lblFooter, BorderLayout.SOUTH);
    }

    // =========================================================================
    // BARRA DE HERRAMIENTAS (con ComboBox de CPUs)
    // =========================================================================
    private JPanel crearBarraHerramientas() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDE));

        JButton btnCargar  = new JButton("Cargar ASM");
        //JButton btnPaso    = new JButton("Paso a Paso");
        //JButton btnTodo    = new JButton("Ejecutar Todo");
        JButton btnIniciar = new JButton("Iniciar CPUs");
       // JButton btnLimpiar = new JButton("Reset");

        estilizarBotonSimple(btnCargar);
       // estilizarBotonSimple(btnPaso);
       // estilizarBotonSimple(btnTodo);
        estilizarBotonSimple(btnIniciar);
        //estilizarBotonSimple(btnLimpiar);

        // --- ComboBox de cantidad de CPUs ---
        JLabel lblCpus = new JLabel("Núcleos:");
        lblCpus.setForeground(Color.WHITE);
        lblCpus.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel lblQuantum = new JLabel("Quantum (Para algoritmos que apliquen):");
        lblQuantum.setForeground(Color.WHITE);
        lblQuantum.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        
        comboQuantum.setBackground(new Color(60, 60, 60));
        comboQuantum.setForeground(Color.WHITE);
        comboQuantum.setMaximumSize(new Dimension(60, 28));
        comboQuantum.setFont(new Font("SansSerif", Font.BOLD, 12));

        comboCpus = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        comboCpus.setSelectedItem(cantidadCpusActiva);        // Defecto: viene del JSON
        comboCpus.setBackground(new Color(60, 60, 60));
        comboCpus.setForeground(Color.WHITE);
        comboCpus.setMaximumSize(new Dimension(60, 28));
        comboCpus.setFont(new Font("SansSerif", Font.BOLD, 12));
        comboCpus.addActionListener(e -> {
            cantidadCpusActiva = (Integer) comboCpus.getSelectedItem();
            aplicarEstadoVisualPanelesCpu();
        });
        JLabel lblAlgoritmo = new JLabel("Algoritmo planificador:");
        lblAlgoritmo.setForeground(Color.WHITE);
        lblAlgoritmo.setFont(new Font("SansSerif", Font.BOLD, 12));

        
        comboAlgoritmo.setBackground(new Color(60, 60, 60));
        comboAlgoritmo.setForeground(Color.WHITE);
        comboAlgoritmo.setFont(new Font("SansSerif", Font.BOLD, 12));
        comboAlgoritmo.setPreferredSize(new Dimension(110, 28));

        // Acción opcional: imprimir en terminal cuando cambia el algoritmo
        comboAlgoritmo.addActionListener(e ->
            this.setearAlgoritmo((String) comboAlgoritmo.getSelectedItem(), (int) comboQuantum.getSelectedItem())
        );
        comboQuantum.addActionListener(e ->
            this.setearAlgoritmo((String) comboAlgoritmo.getSelectedItem(), (int) comboQuantum.getSelectedItem())
        );

        btnCargar.addActionListener(e -> menuCargarArchivo());
       // btnPaso.addActionListener(e -> ejecutarPasoAPaso());
       // btnTodo.addActionListener(e -> alternarEjecucionAutomatica());
        btnIniciar.addActionListener(e -> iniciarCpusEnHilos());
       // btnLimpiar.addActionListener(e -> limpiarSistema());

        panel.add(btnCargar);
       // panel.add(btnPaso);
       // panel.add(btnTodo);
        panel.add(Box.createHorizontalStrut(16));
        panel.add(lblCpus);
        panel.add(comboCpus);
        panel.add(btnIniciar);
        panel.add(Box.createHorizontalStrut(16));
        panel.add(lblAlgoritmo);
        panel.add(comboAlgoritmo);
        panel.add(lblQuantum);
        panel.add(comboQuantum);
       // panel.add(btnLimpiar);

        return panel;
    }
    private void setearAlgoritmo(String algoritmo, int quantum){
        switch(algoritmo){
            case "FCFS":
                this.kernel.colocarEstrategia(new PlanificadorFCFS(),new PlanificadorFCFS());
                break;
            case "HRRN":
                this.kernel.colocarEstrategia(new PlanificadorHRRN(),new PlanificadorHRRN());
                break;
            case "SJF":
                this.kernel.colocarEstrategia(new PlanificadorSJF(),new PlanificadorSJF());
                break;
            case "RR":
                this.kernel.colocarEstrategia(new PlanificadorRoundRobin(quantum), new PlanificadorRoundRobin(quantum));
            default:
                this.kernel.colocarEstrategia(new PlanificadorFCFS(), new PlanificadorFCFS());
                break;
        }
    }
    private void configurarAlgoritmo() {
        String algoritmo = (String) comboAlgoritmo.getSelectedItem();
        int quantum = (int) comboQuantum.getSelectedItem(); // quantum por defecto; podrías exponerlo en la UI

        tarea1.joseandres.estrategia.EstrategiaPlanificacion estrategia;
        tarea1.joseandres.estrategia.AlgoritmoPlanificador   descripcion;

        switch (algoritmo) {
            case "FCFS": {
                tarea1.joseandres.algoritmos.PlanificadorFCFS fcfs =
                        new tarea1.joseandres.algoritmos.PlanificadorFCFS();
                estrategia  = fcfs;
                descripcion = fcfs;
                break;
            }
            case "SJF": {
                tarea1.joseandres.algoritmos.PlanificadorSJF sjf =
                        new tarea1.joseandres.algoritmos.PlanificadorSJF();
                estrategia  = sjf;
                descripcion = sjf;
                break;
            }
            case "HRRN": {
                tarea1.joseandres.algoritmos.PlanificadorHRRN hrrn =
                        new tarea1.joseandres.algoritmos.PlanificadorHRRN();
                estrategia  = hrrn;
                descripcion = hrrn;
                break;
            }
            case "RR": {
                tarea1.joseandres.algoritmos.PlanificadorRoundRobin rr =
                        new tarea1.joseandres.algoritmos.PlanificadorRoundRobin(quantum);
                estrategia  = rr;
                descripcion = rr;
                break;
            }
            case "SRT": {
                tarea1.joseandres.algoritmos.PlanificadorSRT srt =
                        new tarea1.joseandres.algoritmos.PlanificadorSRT();
                estrategia  = srt;
                descripcion = srt;
                quantum     = 1;
                break;
            }
            case "SRR": {
                tarea1.joseandres.algoritmos.PlanificadorSRR srr =
                        new tarea1.joseandres.algoritmos.PlanificadorSRR(quantum);
                estrategia  = srr;
                descripcion = srr;
                break;
            }
            case "Lottery": {
                tarea1.joseandres.algoritmos.PlanificadorLottery lottery =
                        new tarea1.joseandres.algoritmos.PlanificadorLottery(quantum);
                estrategia  = lottery;
                descripcion = lottery;
                break;
            }
            
            default: {
                // Fallback a FCFS
                tarea1.joseandres.algoritmos.PlanificadorFCFS fcfs =
                        new tarea1.joseandres.algoritmos.PlanificadorFCFS();
                estrategia  = fcfs;
                descripcion = fcfs;
            }
        }

        // Inyectar en el Scheduler del Kernel
        kernel.getScheduler().setEstrategia(estrategia, descripcion);

        // Informar a cada CPU si es apropiativo y con qué quantum
        boolean apropiativo = descripcion.esApropiativo;
        for (Cpu cpu : cpus) {
            cpu.setConfiguracionPlanificacion(apropiativo, quantum);
        }

        imprimirEnTerminal("⚙ Algoritmo: " + algoritmo
                + (apropiativo ? " | Quantum: " + quantum : " | No apropiativo"));
    }

    // =========================================================================
    // PANEL RAM
    // =========================================================================
    private JPanel crearPanelRAM() {
        modeloMemoria = new DefaultTableModel(new String[]{"Dir", "Contenido"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaMemoriaFisica = crearTablaOscura(modeloMemoria);
        tablaMemoriaFisica.setDefaultRenderer(Object.class, renderizadorMemoria);

        JScrollPane scroll = new JScrollPane(tablaMemoriaFisica);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return crearPanelConTitulo(scroll, "MEMORIA RAM  (FÍSICA)");
    }

    // =========================================================================
    // PANEL DISCO
    // =========================================================================
    private JPanel crearPanelDisco() {
        modeloDisco = new DefaultTableModel(new String[]{"Sector", "Dato"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaDisco = crearTablaOscura(modeloDisco);

        JScrollPane scroll = new JScrollPane(tablaDisco);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return crearPanelConTitulo(scroll, "DISCO DURO");
    }

    // =========================================================================
    // PANEL PROCESOS + PARTICIONES
    // =========================================================================
    private JPanel crearPanelProcesoYParticiones() {
        modeloProcesos = new DefaultTableModel(new String[]{"ID", "Nombre", "Estado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaProcesos = crearTablaOscura(modeloProcesos);
        tablaProcesos.setDefaultRenderer(Object.class, renderizadorProcesos);

        JScrollPane scrollProcesos = new JScrollPane(tablaProcesos);
        scrollProcesos.getVerticalScrollBar().setUnitIncrement(12);

        String[] cabeceras = "DINAMICA".equalsIgnoreCase(tipoMemoriaConfig)
                ? new String[]{"Bloque Dinámico", "Inicio", "Fin", "Tamaño", "Estado", "Atributo", "-"}
                : new String[]{"Partición", "Inicio", "Fin", "Tamaño", "Estado", "Utilizado", "Desperdicio"};

        modeloParticiones = new DefaultTableModel(cabeceras, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
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

        String tituloSeccion = "DINAMICA".equalsIgnoreCase(tipoMemoriaConfig)
                ? "MAPA DE BLOQUES (DINÁMICO)" : "MAPA DE PARTICIONES";

        // ── Tabla de páginas (visible solo si esPaginado) ────────────────
        modeloTablaPaginas = new DefaultTableModel(
                new String[]{"Página", "Marco Físico", "Dirección Física"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaPaginasVisual = crearTablaOscura(modeloTablaPaginas);
        tablaPaginasVisual.setRowHeight(20);
        tablaPaginasVisual.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tablaPaginasVisual.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        // Colorear filas alternadas
        tablaPaginasVisual.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                c.setForeground(new Color(180, 230, 255));
                c.setBackground(row % 2 == 0 ? new Color(28, 36, 48) : new Color(22, 28, 38));
                if (sel) { c.setBackground(new Color(60, 90, 130)); }
                return c;
            }
        });

        JScrollPane scrollPaginas = new JScrollPane(tablaPaginasVisual);
        scrollPaginas.getVerticalScrollBar().setUnitIncrement(12);

        panelTablaPaginas = crearPanelConTitulo(scrollPaginas, "TABLA DE PÁGINAS  (seleccione un proceso)");
        panelTablaPaginas.setVisible(false); // oculto hasta que se seleccione un proceso paginado

        // Listener: al hacer clic en la tabla de procesos, refrescar tabla de páginas
        tablaProcesos.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int fila = tablaProcesos.getSelectedRow();
            if (fila < 0) { panelTablaPaginas.setVisible(false); return; }
            try {
                int pid = Integer.parseInt(modeloProcesos.getValueAt(fila, 0).toString());
                BCP proc = kernel.getListaProcesos().stream()
                        .filter(p -> p != null && p.id == pid).findFirst().orElse(null);
                if (proc != null && proc.esPaginado && proc.getTablaPaginas() != null) {
                    refrescarTablaPaginas(proc);
                    panelTablaPaginas.setVisible(true);
                } else {
                    panelTablaPaginas.setVisible(false);
                }
            } catch (Exception ignored) { panelTablaPaginas.setVisible(false); }
        });

        // ── Split principal: procesos / particiones / tabla páginas ──────
        JSplitPane splitSuperior = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                crearPanelConTitulo(scrollProcesos, "PROCESOS EN COLA"),
                crearPanelConTitulo(scrollParticiones, tituloSeccion));
        splitSuperior.setResizeWeight(0.40);
        splitSuperior.setDividerSize(5);
        splitSuperior.setBorder(null);
        splitSuperior.setBackground(COLOR_FONDO);
        splitSuperior.setOpaque(false);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                splitSuperior, panelTablaPaginas);
        split.setResizeWeight(0.72);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setBackground(COLOR_FONDO);
        split.setOpaque(false);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(split, BorderLayout.CENTER);
        return contenedor;
    }

    /** Rellena modeloTablaPaginas con los datos de la tabla de páginas del BCP. */
    private void refrescarTablaPaginas(BCP proc) {
        modeloTablaPaginas.setRowCount(0);
        tarea1.joseandres.memoria.TablaPaginas tablaProc = proc.getTablaPaginas();
        if (tablaProc == null) return;
        java.util.Map<Integer, Integer> mapaPaginas = tablaProc.getTabla();
        if (mapaPaginas == null) return;
        try {
            int tp = kernel.getMemoriaPaginada().getTamanoPagina();
            for (java.util.Map.Entry<Integer, Integer> entrada : mapaPaginas.entrySet()) {
                int pagina   = entrada.getKey();
                int marco    = entrada.getValue();
                int dirFisica = marco * tp;
                String dirHex = "0x" + Integer.toHexString(dirFisica).toUpperCase();
                modeloTablaPaginas.addRow(new Object[]{
                    "Página " + pagina,
                    "Marco  " + marco,
                    dirFisica + "  (" + dirHex + ")"
                });
            }
        } catch (Exception e) {
            // Fallback: si no hay MemoriaPaginada activa, mostrar sin dirección física
            for (java.util.Map.Entry<Integer, Integer> entrada : mapaPaginas.entrySet()) {
                modeloTablaPaginas.addRow(new Object[]{
                    "Página " + entrada.getKey(),
                    "Marco  " + entrada.getValue(),
                    "---"
                });
            }
        }
        // Actualizar título del panel con PID y nombre del proceso
        TitledBorder tb = (TitledBorder) panelTablaPaginas.getBorder();
        if (tb != null) tb.setTitle("TABLA DE PÁGINAS  — PID " + proc.id + " · " + proc.nombreProceso);
        panelTablaPaginas.repaint();
    }

    // =========================================================================
    // PANEL CPU (4 sub-paneles en GridLayout 2x2) + TERMINAL
    // =========================================================================
    private JPanel crearPanelCpuYTerminal() {

        // --- Grid 2x2 con los 4 paneles de CPU ---
        JPanel gridCpus = new JPanel(new GridLayout(2, 2, 4, 4));
        gridCpus.setOpaque(false);

        for (int id = 0; id < MAX_CPUS; id++) {
            JPanel panelCpu = crearSubPanelCpu(id);
            cpuPanelContenedor[id] = panelCpu;
            gridCpus.add(panelCpu);
        }

        // Estado visual inicial según cantidadCpusActiva (defecto 2)
        aplicarEstadoVisualPanelesCpu();

        JPanel cpuWrapper = crearPanelConTitulo(gridCpus, "ESTADO CPUs");

        // --- Terminal ---
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
        panelEntrada.add(campoEntrada,     BorderLayout.CENTER);
        panelEntrada.add(btnEnviarEntrada, BorderLayout.EAST);

        JPanel terminalWrapper = new JPanel(new BorderLayout(0, 6));
        terminalWrapper.setOpaque(false);
        terminalWrapper.add(new JScrollPane(areaTerminal), BorderLayout.CENTER);
        terminalWrapper.add(panelEntrada, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                cpuWrapper,
                crearPanelConTitulo(terminalWrapper, "TERMINAL"));
        split.setResizeWeight(0.45);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setOpaque(false);
        split.setBackground(COLOR_FONDO);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(split, BorderLayout.CENTER);
        return contenedor;
    }

    /**
     * Crea un sub-panel compacto para una CPU individual.
     * Llena cpuPanelRegistros[cpuId][0..7].
     */
    private JPanel crearSubPanelCpu(int cpuId) {
        JPanel panel = new JPanel(new GridLayout(REGS_POR_CPU, 1, 0, 1));
        panel.setBackground(new Color(22, 22, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        Font f     = new Font("Monospaced", Font.BOLD, 11);
        Color cPID = new Color(255, 255, 100);
        Color cPC  = new Color(0,   220, 255);
        Color cIR  = Color.WHITE;
        Color cAC  = new Color(80,  255, 120);
        Color cReg = new Color(255, 180,  60);

        cpuPanelRegistros[cpuId][0] = crearLabelRegistro("PID", "IDLE", f, cPID);
        cpuPanelRegistros[cpuId][1] = crearLabelRegistro("PC",  "---",  f, cPC);
        cpuPanelRegistros[cpuId][2] = crearLabelRegistro("IR",  "---",  f, cIR);
        cpuPanelRegistros[cpuId][3] = crearLabelRegistro("AC",  "---",  f, cAC);
        cpuPanelRegistros[cpuId][4] = crearLabelRegistro("AX",  "---",  f, cReg);
        cpuPanelRegistros[cpuId][5] = crearLabelRegistro("BX",  "---",  f, cReg);
        cpuPanelRegistros[cpuId][6] = crearLabelRegistro("CX",  "---",  f, cReg);
        cpuPanelRegistros[cpuId][7] = crearLabelRegistro("DX",  "---",  f, cReg);

        for (JLabel lbl : cpuPanelRegistros[cpuId]) {
            panel.add(lbl);
        }

        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                "CPU " + (cpuId + 1));
        tb.setTitleColor(new Color(180, 210, 255));
        tb.setTitleFont(new Font("SansSerif", Font.BOLD, 11));
        panel.setBorder(tb);

        return panel;
    }

    // =========================================================================
    // LÓGICA DE ACTIVACIÓN VISUAL DE PANELES
    // =========================================================================
    /**
     * Pinta los paneles activos/inactivos según cantidadCpusActiva.
     */
    private void aplicarEstadoVisualPanelesCpu() {
        for (int id = 0; id < MAX_CPUS; id++) {
            boolean activa = (id < cantidadCpusActiva);
            JPanel panel = cpuPanelContenedor[id];

            if (activa) {
                panel.setBackground(new Color(22, 22, 30));
                TitledBorder tb = BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(80, 80, 80)),
                        "CPU " + (id + 1));
                tb.setTitleColor(new Color(180, 210, 255));
                tb.setTitleFont(new Font("SansSerif", Font.BOLD, 11));
                panel.setBorder(tb);
            } else {
                panel.setBackground(COLOR_CPU_INACTIVA);
                TitledBorder tb = BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(60, 60, 60)),
                        "CPU " + (id + 1) + "  [APAGADA]");
                tb.setTitleColor(new Color(100, 100, 100));
                tb.setTitleFont(new Font("SansSerif", Font.BOLD, 11));
                panel.setBorder(tb);
                // Apagar labels
                for (JLabel lbl : cpuPanelRegistros[id]) {
                    lbl.setForeground(new Color(70, 70, 70));
                    String texto = lbl.getText().split(":")[0];
                    lbl.setText(texto + ": ---");
                }
            }
            panel.repaint();
        }
    }

    // =========================================================================
    // INICIO DE HILOS CONCURRENTES
    // =========================================================================
    /**
     * Instancia y arranca en hilos independientes la cantidad de CPUs elegida.
     * Se puede llamar desde el botón "🚀 Iniciar CPUs".
     */
    private void iniciarCpusEnHilos() {
        if (timerSimulacion != null && timerSimulacion.isRunning()) timerSimulacion.stop();
        for (Cpu c : cpus)        c.setCorriendo(false);
        for (Thread t : hilosCpu) t.interrupt();
        if (cronometro != null)   cronometro.interrupt();
        if (observador != null)   observador.interrupt();

        int cpusAInterpretar = (Integer) comboCpus.getSelectedItem();
        cpus.clear();
        hilosCpu.clear();

        for (int i = 0; i < cpusAInterpretar; i++) {
            tarea1.joseandres.memoria.MemoriaPaginada mpCpu = null;
            try { mpCpu = this.kernel.getMemoriaPaginada(); } catch (Exception _ignored) {}

            Cpu nuevaCpu = new Cpu(i, this.kernel, this.memoria, this.dispatcher, this.disco, mpCpu);
            nuevaCpu.setDelayReloj(1200);
            nuevaCpu.setCorriendo(true);

            final int cpuId  = i;
            final Cpu cpuRef = nuevaCpu;
            nuevaCpu.setPasoCallback((id, bcp) -> {
                bcpActual = bcp;
                actualizarPanelCpu(id, bcp);
                int dirIR = cpuRef.getDireccionIRActual();
                renderizadorMemoria.setPuntero(id, dirIR);
                tablaMemoriaFisica.repaint();
                if (dirIR >= 0 && dirIR < tablaMemoriaFisica.getRowCount())
                    tablaMemoriaFisica.scrollRectToVisible(tablaMemoriaFisica.getCellRect(dirIR, 0, true));
                if (bcp != null)
                    actualizarEstadoProcesoPorId(bcp.id, "EJECUCION (CPU " + cpuId + ")");
                tablaProcesos.repaint();
                tablaParticiones.repaint();
                if (dirIR % 4 == 0) actualizarTablas();
            });

            cpus.add(nuevaCpu);
        }

        configurarAlgoritmo();

        for (int i = 0; i < cpus.size(); i++) {
            Thread hilo = new Thread(cpus.get(i), "CPU-" + i);
            hilosCpu.add(hilo);
            hilo.start();
            imprimirEnTerminal("CPU " + (i + 1) + " iniciada en hilo independiente.");
        }

        aplicarEstadoVisualPanelesCpu();

        // Cronómetro + Observador
        if (!configuracionProcesos.isEmpty()) {
            ProcesoCargaCallback callback = (nombre, ruta) -> {
                boolean cargado = kernel.cargarProceso(ruta);
                if (cargado) {
                    imprimirEnTerminal("CARGADO: " + nombre);
                } else {
                    imprimirEnTerminal("ERROR al cargar: " + nombre);
                    JOptionPane.showMessageDialog(this,
                            "Memoria insuficiente para cargar: " + nombre,
                            "Error de carga", JOptionPane.ERROR_MESSAGE);
                }
                actualizarTablas();
            };
            cronometro = new CronometroThread();
            observador = new ObservadorCargaThread(configuracionProcesos, callback);
            cronometro.agregarObserver(observador);
            observador.setDaemon(true);
            cronometro.setDaemon(true);
            observador.start();
            cronometro.start();
            imprimirEnTerminal("▶ Cronómetro iniciado junto con las CPUs.");
        }
    }

    /**
     * Busca el proceso con ese PID en la tabla de procesos y actualiza su columna Estado
     * directamente en el modelo para que el renderizador lo pinte al instante.
     */
    private void actualizarEstadoProcesoPorId(int pid, String nuevoEstado) {
        for (int fila = 0; fila < modeloProcesos.getRowCount(); fila++) {
            Object valorPid = modeloProcesos.getValueAt(fila, 0);
            if (valorPid != null && Integer.parseInt(valorPid.toString()) == pid) {
                modeloProcesos.setValueAt(nuevoEstado, fila, 2); // columna 2 = Estado
                return;
            }
        }
    }

    // =========================================================================
    // ACTUALIZACIÓN DE PANELES CPU  (llamado desde Swing EDT o Timer)
    // =========================================================================
    /**
     * Refresca los labels de todas las CPUs activas con el BCP que tienen asignado.
     * Llamar desde el Timer o después de cada paso.
     */
    public void actualizarPanelesCpu() {
        for (int id = 0; id < MAX_CPUS; id++) {
            if (id >= cpus.size()) {
                // Panel inactivo: no tocar
                continue;
            }
            BCP bcp = cpus.get(id).getProcesoActual();
            actualizarPanelCpu(id, bcp);
        }
    }

    /**
     * Actualiza un panel de CPU individual con los datos de un BCP.
     * Diseñado para ser llamado desde Dispatcher u otros componentes.
     *
     * @param cpuId  índice de la CPU (0-3)
     * @param bcp    BCP actualmente en esa CPU, o null si está IDLE
     */
    public void actualizarPanelCpu(int cpuId, BCP bcp) {
        if (cpuId < 0 || cpuId >= MAX_CPUS) return;

        SwingUtilities.invokeLater(() -> {
            JLabel[] lbls = cpuPanelRegistros[cpuId];
            if (bcp != null) {
                lbls[0].setText("PID: " + bcp.id + " · " + bcp.nombreProceso);
                lbls[1].setText("PC:  " + String.format("%03d", bcp.PC));
                lbls[2].setText("IR:  " + (bcp.IR == null ? "---" : traducirInstruccion(bcp.IR)));
                lbls[3].setText("AC:  " + bcp.AC);
                lbls[4].setText("AX:  " + bcp.AX);
                lbls[5].setText("BX:  " + bcp.BX);
                lbls[6].setText("CX:  " + bcp.CX);
                lbls[7].setText("DX:  " + bcp.DX);
            } else {
                lbls[0].setText("PID: IDLE");
                lbls[1].setText("PC:  ---");
                lbls[2].setText("IR:  ---");
                lbls[3].setText("AC:  ---");
                lbls[4].setText("AX:  ---");
                lbls[5].setText("BX:  ---");
                lbls[6].setText("CX:  ---");
                lbls[7].setText("DX:  ---");
            }
        });
    }

    // =========================================================================
    // EJECUCIÓN PASO A PASO Y AUTOMÁTICA (modo un solo hilo, compatibilidad)
    // =========================================================================
    private void alternarEjecucionAutomatica() {
        if (timerSimulacion != null && timerSimulacion.isRunning()) {
            timerSimulacion.stop();
            imprimirEnTerminal("Ejecución automática detenida.");
            return;
        }
        timerSimulacion = new Timer(600, e -> {
            boolean continua = ejecutarPasoAPaso();
            if (!continua) timerSimulacion.stop();
        });
        timerSimulacion.start();
        imprimirEnTerminal("Ejecución automática iniciada.");
    }

    private boolean ejecutarPasoAPaso() {
        if (cpus.isEmpty()) {
            // Compatibilidad: crear CPU 0 on-demand en modo paso a paso
            tarea1.joseandres.memoria.MemoriaPaginada mp0 = null;
            try { mp0 = kernel.getMemoriaPaginada(); } catch (Exception _ig) {}
            Cpu cpu0 = new Cpu(0, kernel, memoria, dispatcher, disco, mp0);
            cpus.add(cpu0);
        }

        Cpu cpu = cpus.get(0);
        boolean continua = cpu.ejecutarSiguientePaso();
        bcpActual = cpu.getProcesoActual();

        actualizarPanelCpu(0, bcpActual);
        actualizarTablas();

        if (!continua) {
            imprimirEnTerminal("No hay más procesos para ejecutar.");
        } else if (bcpActual != null) {
            imprimirEnTerminal("Ejecutando PID " + bcpActual.id + " - " + bcpActual.nombreProceso);
        }
        return continua;
    }

    // =========================================================================
    // CARGA DE ARCHIVOS hay que venir aquí
    // =========================================================================
    private void menuCargarArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] archivosSeleccionados = chooser.getSelectedFiles();

            // ── Ventana de configuración ──────────────────────────────────────
            JDialog dialogo = new JDialog(this, "Configuración de procesos", true);
            dialogo.setLayout(new BorderLayout(10, 10));

            JLabel titulo = new JLabel("Configura el tiempo de llegada de cada proceso");
            titulo.setFont(new Font("SansSerif", Font.BOLD, 14));
            titulo.setHorizontalAlignment(SwingConstants.CENTER);
            titulo.setBorder(BorderFactory.createEmptyBorder(12, 10, 4, 10));
            dialogo.add(titulo, BorderLayout.NORTH);

            JPanel panelArchivos = new JPanel(new GridLayout(archivosSeleccionados.length, 2, 10, 8));
            panelArchivos.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

            JSpinner[] spinners = new JSpinner[archivosSeleccionados.length];
            for (int i = 0; i < archivosSeleccionados.length; i++) {
                JLabel nombreLabel = new JLabel(archivosSeleccionados[i].getName());
                nombreLabel.setToolTipText(archivosSeleccionados[i].getAbsolutePath());

                SpinnerNumberModel modelo = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
                spinners[i] = new JSpinner(modelo);
                spinners[i].setPreferredSize(new Dimension(80, 28));

                panelArchivos.add(nombreLabel);
                panelArchivos.add(spinners[i]);
            }

            JScrollPane scroll = new JScrollPane(panelArchivos);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            dialogo.add(scroll, BorderLayout.CENTER);

            JButton btnConfirmar = new JButton("Confirmar configuración");
            JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
            panelBoton.add(btnConfirmar);
            panelBoton.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            dialogo.add(panelBoton, BorderLayout.SOUTH);

            // ── Al confirmar: solo guardar la configuración ───────────────────
            btnConfirmar.addActionListener(e -> {
                // Acumular en la lista persistente (permite cargar varias veces)
                for (int i = 0; i < archivosSeleccionados.length; i++) {
                    configuracionProcesos.add(new Object[]{
                            archivosSeleccionados[i].getName(),
                            archivosSeleccionados[i].getAbsolutePath(),
                            (int) spinners[i].getValue()
                    });
                    imprimirEnTerminal("EN COLA: " + archivosSeleccionados[i].getName()
                            + " → llegada en segundo " + (int) spinners[i].getValue()
                            + " (" + CronometroThread.formatear((int) spinners[i].getValue()) + ")");
                }
                dialogo.dispose();
                imprimirEnTerminal("✔ Configuración guardada. Pulsa 'Iniciar CPUs' para comenzar.");
            });

            dialogo.pack();
            dialogo.setMinimumSize(new Dimension(420, 200));
            dialogo.setLocationRelativeTo(this);
            dialogo.setVisible(true);
        }
}

    // =========================================================================
    // TERMINAL
    // =========================================================================
    public void imprimirEnTerminal(String texto) {
        SwingUtilities.invokeLater(() -> {
            areaTerminal.append("> " + texto + "\n");
            areaTerminal.setCaretPosition(areaTerminal.getDocument().getLength());
        });
    }

    public void solicitarEntrada(String mensaje) {
        esperandoEntrada = true;
        ultimaEntrada    = null;
        lblPromptEntrada.setText(mensaje);
        campoEntrada.setText("");
        campoEntrada.setEnabled(true);
        btnEnviarEntrada.setEnabled(true);
        campoEntrada.requestFocus();
        imprimirEnTerminal("[ENTRADA] " + mensaje);
    }

    private void enviarEntradaTerminal() {
        if (!esperandoEntrada) return;
        String texto = campoEntrada.getText().trim();
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un valor.", "Entrada vacía", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ultimaEntrada    = texto;
        esperandoEntrada = false;
        imprimirEnTerminal("[USUARIO] " + texto);
        campoEntrada.setText("");
        campoEntrada.setEnabled(false);
        btnEnviarEntrada.setEnabled(false);
        lblPromptEntrada.setText("Esperando instrucción de entrada...");
        ejecutarPasoAPaso();
    }

    public String  consumirEntrada()        { String e = ultimaEntrada; ultimaEntrada = null; return e; }
    public boolean hayEntradaDisponible()   { return ultimaEntrada != null; }
    public boolean estaEsperandoEntrada()   { return esperandoEntrada; }

    // =========================================================================
    // ACTUALIZACIÓN DE TABLAS
    // =========================================================================
    private void actualizarTablas() {
        if (memoria == null || disco == null || kernel == null) return;

        // RAM
        modeloMemoria.setRowCount(0);
        for (int i = 0; i < memoria.getTamanoTotal(); i++) {
            modeloMemoria.addRow(new Object[]{i, traducirInstruccion(memoria.leerCelda(i))});
        }

        // Disco
        modeloDisco.setRowCount(0);
        for (int i = 0; i < disco.getTamanoTotal(); i++) {
            String dato = disco.leer(i);
            modeloDisco.addRow(new Object[]{i, (i < disco.getEspacioIndice()) ? dato : traducirInstruccion(dato)});
        }

        // Procesos
        modeloProcesos.setRowCount(0);
        if (kernel.getListaProcesos() != null) {
            for (BCP p : kernel.getListaProcesos()) {
                if (p != null) {
                    obtenerColorProceso(p.id);
                    modeloProcesos.addRow(new Object[]{p.id, p.nombreProceso, p.estado});
                }
            }
        }

        // Particiones / bloques dinámicos / marcos de paginación
        modeloParticiones.setRowCount(0);
        if (tipoMemoriaConfig != null && tipoMemoriaConfig.toUpperCase().startsWith("PAGIN")) {
            // Mostrar mapa de marcos del bitmap
            try {
                tarea1.joseandres.memoria.MemoriaPaginada mp = kernel.getMemoriaPaginada();
                if (mp != null) {
                    boolean[] bitmap = mp.getBitmap();
                    int tp           = mp.getTamanoPagina();
                    if (bitmap != null) {
                        for (int i = 0; i < bitmap.length; i++) {
                            String estadoMarco = bitmap[i] ? "OCUPADO" : "LIBRE";
                            int dirInicio = i * tp;
                            int dirFin    = dirInicio + tp - 1;
                            modeloParticiones.addRow(new Object[]{
                                "Marco #" + i,
                                tp + " celdas",
                                String.format("%d - %d", dirInicio, dirFin),
                                estadoMarco
                            });
                        }
                    }
                }
            } catch (Exception ignored) {}
        } else if ("DINAMICA".equalsIgnoreCase(tipoMemoriaConfig)) {
            List<Particion> bloques = kernel.getMemoriaDinamica().getBloques();
            if (bloques != null) {
                for (Particion b : bloques) {
                    int inicio = b.getInicio(), tamano = b.getTamano();
                    String estado   = b.isLibre() ? "LIBRE (Hueco disponible)" : "OCUPADO - PID: " + (b.getProceso() != null ? b.getProceso().id : "?");
                    String atributo = b.isLibre() ? "Bloque Limpio" : "Proceso Activo: " + (b.getProceso() != null ? b.getProceso().nombreProceso : "-");
                    modeloParticiones.addRow(new Object[]{"Bloque #" + b.getNumero(), inicio, inicio + tamano - 1, tamano + " celdas", estado, atributo, "---"});
                }
            }
        } else {
            List<Particion> lista = kernel.getMemoriaFija().getParticiones();
            if (lista != null && !lista.isEmpty()) {
                for (Particion p : lista) {
                    int inicio = p.getInicio(), tamano = p.getTamano();
                    String estado, utilizado, desperdicio;
                    if (p.isLibre()) {
                        estado = "LIBRE (Vacía)"; utilizado = "---"; desperdicio = "---";
                    } else {
                        int pid  = (p.getProceso() != null) ? p.getProceso().id : -1;
                        estado   = "OCUPADA - PID: " + (pid > 0 ? pid : "?");
                        if (p.getProceso() != null) {
                            int usado    = p.getProceso().getAlcance();
                            int fragInt  = tamano - usado;
                            double pUtil = tamano > 0 ? (double) usado / tamano * 100 : 0;
                            double pDesp = tamano > 0 ? (double) fragInt / tamano * 100 : 0;
                            utilizado    = String.format("%d celdas (%.1f%%)", usado,   pUtil);
                            desperdicio  = String.format("%d celdas (%.1f%%)", fragInt, pDesp);
                        } else { utilizado = "?"; desperdicio = "?"; }
                    }
                    modeloParticiones.addRow(new Object[]{"Partición #" + p.getNumero(), inicio, inicio + tamano - 1, tamano + " celdas", estado, utilizado, desperdicio});
                }
            }
        }

        // Renderizador RAM: actualiza el puntero de CADA CPU activa individualmente
        renderizadorMemoria.setConfig(memoria.getInicioUsuario(), memoria.getInicioUsuario());
        for (int ci = 0; ci < cpus.size(); ci++) {
            try {
                int dir = cpus.get(ci).getDireccionIRActual();
                if (dir >= 0) renderizadorMemoria.setPuntero(ci, dir);
            } catch (Exception ignored) {}
        }

        tablaMemoriaFisica.repaint();
        tablaProcesos.repaint();
        tablaParticiones.repaint();
    }

    // =========================================================================
    // RESET
    // =========================================================================
    private void limpiarSistema() {
        if (timerSimulacion != null && timerSimulacion.isRunning()) timerSimulacion.stop();
        for (Cpu c : cpus) c.setCorriendo(false);
        for (Thread t : hilosCpu) t.interrupt();
        dispose();
        SwingUtilities.invokeLater(() -> {
            Kernel nuevoKernel = new Kernel(
                    tamanoRamConfig, tamanoDiscoConfig,
                    porcentajeKernelConfig, porcentajeIndiceDiscoConfig,
                    tipoMemoriaConfig, cantParticionesConfig, tamanosParticionesConfig,
                    tamanoPagina);
            SimuladorGUI nueva = new SimuladorGUI(nuevoKernel, cantidadCpusActiva);
            nueva.setLocationRelativeTo(null);
            nueva.setVisible(true);
        });
    }

    // =========================================================================
    // UTILS DE UI
    // =========================================================================
    private void estilizarBotonSimple(JButton btn) {
        btn.setBackground(new Color(70, 70, 70));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
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

    private Color obtenerColorProceso(int pid) {
        if (pid <= 0) return new Color(220, 220, 220);
        if (!coloresProcesos.containsKey(pid))
            coloresProcesos.put(pid, paletaProcesos[(pid - 1) % paletaProcesos.length]);
        return coloresProcesos.get(pid);
    }

    private Color colorSuave(Color base) {
        return new Color((base.getRed() + 255) / 2, (base.getGreen() + 255) / 2, (base.getBlue() + 255) / 2);
    }

    private BCP buscarProcesoPorDireccion(int direccion) {
        for (BCP p : kernel.getListaProcesos()) {
            if (perteneceAlProceso(p, direccion)) return p;
        }
        return null;
    }

    private boolean perteneceAlProceso(BCP proceso, int direccion) {
        Integer inicio = obtenerValorEnteroCampo(proceso, "baseMemoria", "inicioMemoria", "direccionBase", "base", "inicio");
        Integer tamano = obtenerValorEnteroCampo(proceso, "tamanoProceso", "limiteMemoria", "longitudProceso", "size", "tamano", "alcance");
        if (inicio != null && tamano != null) return direccion >= inicio && direccion < (inicio + tamano);
        Integer fin = obtenerValorEnteroCampo(proceso, "finMemoria", "direccionFin", "limiteSuperior", "topeMemoria");
        if (inicio != null && fin != null) return direccion >= inicio && direccion <= fin;
        return false;
    }

    private Integer obtenerValorEnteroCampo(Object obj, String... nombres) {
        for (String nombre : nombres) {
            try {
                Field campo = obj.getClass().getDeclaredField(nombre);
                campo.setAccessible(true);
                Object valor = campo.get(obj);
                if (valor instanceof Integer) return (Integer) valor;
                if (valor != null) return Integer.parseInt(valor.toString());
            } catch (Exception ignored) {}
        }
        return null;
    }

    // =========================================================================
    // RENDERIZADORES PERSONALIZADOS
    // =========================================================================
    class ColorRowRenderer extends DefaultTableCellRenderer {
        private int limiteKernel;
        // cpuId -> dirección IR actual de esa CPU (su "puntero" en la RAM)
        private final java.util.Map<Integer, Integer> punterosPC = new java.util.HashMap<>();

        ColorRowRenderer(int pc, int limite) {
            this.limiteKernel = limite;
            punterosPC.put(0, pc);
        }

        /** Actualiza el puntero de una CPU específica */
        void setPuntero(int cpuId, int direccionIR) {
            punterosPC.put(cpuId, direccionIR);
        }

        /** Mantiene compatibilidad con llamadas anteriores (CPU 0) */
        void setConfig(int pc, int limite) {
            this.limiteKernel = limite;
            punterosPC.put(0, pc);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(table.getFont());

            // Zona de kernel: color fijo azulado
            if (row < limiteKernel) {
                c.setBackground(COLOR_KERNEL); c.setForeground(Color.WHITE); return c;
            }

            // ¿Esta fila es el PC activo de alguna CPU?
            int cpuEjecutando = -1;
            for (java.util.Map.Entry<Integer, Integer> entry : punterosPC.entrySet()) {
                if (entry.getValue() != null && entry.getValue() == row) {
                    cpuEjecutando = entry.getKey();
                    break;
                }
            }

            BCP proc = buscarProcesoPorDireccion(row);
            if (proc != null) {
                Color colorPid = obtenerColorProceso(proc.id);
                int relPos = row - proc.getDireccionBase();
                if (relPos >= 0 && relPos < proc.getAlcance()) {
                    if (cpuEjecutando >= 0) {
                        // Fila activa: resaltado brillante + etiqueta de CPU
                        c.setBackground(colorPid.brighter());
                        c.setForeground(Color.BLACK);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                        if (column == 1) {
                            String contenido = (value != null ? value.toString() : "");
                            setText("▶ CPU" + (cpuEjecutando + 1) + "  " + contenido);
                        }
                    } else {
                        // Fila ocupada por proceso pero no es el PC activo
                        c.setBackground(colorSuave(colorPid)); c.setForeground(Color.BLACK);
                    }
                } else {
                    // Fragmentación interna
                    c.setBackground(new Color(45, 40, 40)); c.setForeground(new Color(230, 100, 100));
                    if (column == 1) setText("❌ [Frag. Interna - Bloque de PID " + proc.id + "]");
                }
            } else {
                c.setBackground(COLOR_LIBRE); c.setForeground(COLOR_TEXTO_LIBRE);
            }

            if (isSelected) { c.setBackground(c.getBackground().darker()); c.setForeground(Color.WHITE); }
            return c;
        }
    }

    class ProcesoTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            try {
                int    pid    = Integer.parseInt(table.getValueAt(row, 0).toString());
                String estado = table.getValueAt(row, 2).toString();
                Color  colorPid = obtenerColorProceso(pid);
                if ("EJECUCION".equalsIgnoreCase(estado) || estado.toUpperCase().startsWith("EJECUCION (CPU")) {
                    c.setBackground(colorPid); c.setForeground(Color.BLACK); c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if ("ERROR".equalsIgnoreCase(estado)) {
                    c.setBackground(new Color(170, 60, 60)); c.setForeground(Color.WHITE);
                } else if ("TERMINADO".equalsIgnoreCase(estado)) {
                    c.setBackground(new Color(90, 90, 90)); c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(colorSuave(colorPid)); c.setForeground(Color.BLACK);
                }
            } catch (Exception e) {
                c.setBackground(new Color(30, 30, 30)); c.setForeground(Color.WHITE);
            }
            if (isSelected) { c.setBackground(c.getBackground().darker()); c.setForeground(Color.WHITE); }
            return c;
        }
    }

    class ParticionTableRenderer extends DefaultTableCellRenderer {
        ParticionTableRenderer() { setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8)); }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

            try {
                String estadoStr = table.getValueAt(row, 4).toString();
                if (estadoStr.contains("LIBRE")) {
                    c.setBackground(new Color(28, 48, 28)); c.setForeground(new Color(80, 255, 80));
                    c.setFont((column == 4) ? c.getFont().deriveFont(Font.BOLD) : table.getFont());
                } else {
                    String[] partes = estadoStr.split("PID: ");
                    Color base = (partes.length > 1)
                            ? colorSuave(obtenerColorProceso(Integer.parseInt(partes[1].trim())))
                            : new Color(60, 40, 40);
                    Color fg = (partes.length > 1) ? new Color(20, 20, 20) : Color.WHITE;

                    if ("DINAMICA".equalsIgnoreCase(tipoMemoriaConfig)) {
                        c.setBackground(base); c.setForeground(fg); return c;
                    }

                    if (column == 6 && partes.length > 1) {
                        String desp = table.getValueAt(row, 6).toString();
                        if (!desp.equals("---") && !desp.startsWith("0 ")) {
                            c.setBackground(new Color(90, 35, 35)); c.setForeground(new Color(255, 120, 120));
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                            if (isSelected) c.setBackground(c.getBackground().darker());
                            return c;
                        }
                    }

                    if (column == 5 && partes.length > 1) {
                        String util = table.getValueAt(row, 5).toString();
                        if (util.contains("100.0%")) {
                            c.setBackground(new Color(28, 60, 28)); c.setForeground(new Color(80, 255, 80));
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                            if (isSelected) c.setBackground(c.getBackground().darker());
                            return c;
                        }
                    }

                    c.setBackground(base); c.setForeground(fg);
                    c.setFont((column == 4) ? c.getFont().deriveFont(Font.BOLD) : table.getFont());
                }
            } catch (Exception e) {
                c.setBackground(new Color(35, 35, 35)); c.setForeground(Color.WHITE); c.setFont(table.getFont());
            }

            if (isSelected) { c.setBackground(c.getBackground().darker()); c.setForeground(Color.WHITE); }
            return c;
        }
    }
}

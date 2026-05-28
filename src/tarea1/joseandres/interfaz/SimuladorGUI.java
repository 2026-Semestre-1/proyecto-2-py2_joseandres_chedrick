package tarea1.joseandres.interfaz;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
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

    // Panel de fragmentación
    private JPanel panelFragmentacion;

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
        this.cpu = new Cpu(this.memoria, this.dispatcher, this.kernel, this);

        this.renderizadorMemoria = new ColorRowRenderer(memoria.getInicioUsuario(), memoria.getInicioUsuario());
        this.renderizadorProcesos = new ProcesoTableRenderer();

        configurarVentana();
        actualizarTablas();
        actualizarLabelsBCP();
    }

    // =========================================================================
    // CONFIGURACIÓN DE VENTANA
    // =========================================================================
    private void configurarVentana() {
        setTitle("S.O. MiniPC - Gestión de Procesos | Jose Andrés Solano");
        setSize(1600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout(10, 10));

        // ── Barra de botones ─────────────────────────────────────────────────
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

        // ── Panel central con GridBagLayout ──────────────────────────────────
        JPanel panelCentral = new JPanel(new GridBagLayout());
        panelCentral.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);

        // ----- Columna 0: Memoria RAM ----------------------------------------
        modeloMemoria = new DefaultTableModel(new String[] { "Dir", "Contenido" }, 0) {
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
        gbc.weightx = 0.20;
        gbc.weighty = 1.0;
        panelCentral.add(crearPanelConTitulo(new JScrollPane(tablaMemoriaFisica), "MEMORIA RAM (FÍSICA)"), gbc);

        // ----- Columna 1: Panel de Fragmentación -----------------------------
        panelFragmentacion = new JPanel(new BorderLayout(0, 8));
        panelFragmentacion.setOpaque(false);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.16;
        panelCentral.add(crearPanelConTitulo(panelFragmentacion, "FRAGMENTACIÓN"), gbc);

        // ----- Columna 2: Disco + Procesos -----------------------------------
        modeloDisco = new DefaultTableModel(new String[] { "Sector", "Dato" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        modeloProcesos = new DefaultTableModel(new String[] { "ID", "Nombre", "Estado" }, 0) {
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

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.30;
        panelCentral.add(panelMedio, gbc);

        // ----- Columna 3: Estado CPU + Terminal ------------------------------
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

        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.34;
        panelCentral.add(panelDerecho, gbc);

        add(panelCentral, BorderLayout.CENTER);

        JLabel lblFooter = new JLabel(" TEC | Sistemas Operativos | Jose Andrés Solano Vargas ");
        lblFooter.setForeground(Color.GRAY);
        add(lblFooter, BorderLayout.SOUTH);
    }

    // =========================================================================
    // PANEL DE FRAGMENTACIÓN
    // =========================================================================
    private void actualizarPanelFragmentacion() {
        panelFragmentacion.removeAll();

        int total = memoria.getTamanoTotal();
        int limKernel = memoria.getInicioUsuario();
        final int ALTO_CELDA = 18; // px por celda — más grueso para distinguir mejor
        final int ANCHO_BLOQUE = 52;

        // ── Panel interior que contiene bloques + etiquetas ──────────────────
        // Altura total = una fila por celda
        int alturaTotal = total * ALTO_CELDA;

        JPanel filasPanel = new JPanel(null); // layout absoluto
        filasPanel.setBackground(new Color(28, 28, 28));
        filasPanel.setPreferredSize(new Dimension(ANCHO_BLOQUE + 38, alturaTotal));

        for (int i = 0; i < total; i++) {
            final int idx = i;
            int y = i * ALTO_CELDA;

            // Color del bloque
            Color colorBloque;
            String etiqueta;
            if (i < limKernel) {
                colorBloque = new Color(55, 70, 130);
                etiqueta = "K";
            } else {
                BCP dueño = buscarProcesoPorDireccion(i);
                if (dueño != null && !dueño.estado.equals("TERMINADO")) {
                    colorBloque = obtenerColorProceso(dueño.id);
                    etiqueta = "P" + dueño.id;
                } else {
                    colorBloque = new Color(55, 55, 55);
                    etiqueta = "";
                }
            }

            // Bloque de color
            final Color colorFinal = colorBloque;
            final String etqFinal = etiqueta;
            JPanel bloque = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(colorFinal);
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    // borde inferior para separar celdas
                    g2.setColor(new Color(0, 0, 0, 100));
                    g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

                    // etiqueta dentro del bloque
                    if (!etqFinal.isEmpty()) {
                        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g2.setColor(new Color(0, 0, 0, 160));
                        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                        FontMetrics fm = g2.getFontMetrics();
                        int tx = (getWidth() - fm.stringWidth(etqFinal)) / 2;
                        int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                        g2.drawString(etqFinal, tx, ty);
                    }

                    // línea de PC actual
                    if (bcpActual != null && bcpActual.PC == idx) {
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                    }
                }
            };
            bloque.setBounds(0, y, ANCHO_BLOQUE, ALTO_CELDA);
            bloque.setOpaque(false);
            filasPanel.add(bloque);

            // Etiqueta de dirección a la derecha del bloque
            JLabel lblDir = new JLabel(String.valueOf(i));
            lblDir.setFont(new Font("Monospaced", Font.PLAIN, 9));
            lblDir.setForeground(new Color(160, 160, 160));
            lblDir.setBounds(ANCHO_BLOQUE + 3, y + 1, 34, ALTO_CELDA - 2);
            filasPanel.add(lblDir);
        }

        // ── Scroll sobre el panel de bloques ─────────────────────────────────
        JScrollPane scrollBloques = new JScrollPane(filasPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollBloques.setBorder(null);
        scrollBloques.getViewport().setBackground(new Color(28, 28, 28));
        scrollBloques.getVerticalScrollBar().setUnitIncrement(ALTO_CELDA);

        // ── Leyenda ──────────────────────────────────────────────────────────
        JPanel leyenda = new JPanel();
        leyenda.setLayout(new BoxLayout(leyenda, BoxLayout.Y_AXIS));
        leyenda.setBackground(new Color(36, 36, 36));
        leyenda.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titLeyenda = new JLabel("Leyenda");
        titLeyenda.setForeground(Color.LIGHT_GRAY);
        titLeyenda.setFont(new Font("SansSerif", Font.BOLD, 11));
        titLeyenda.setAlignmentX(Component.LEFT_ALIGNMENT);
        leyenda.add(titLeyenda);
        leyenda.add(Box.createVerticalStrut(8));

        leyenda.add(crearItemLeyenda(new Color(55, 70, 130), "Kernel",
                "dir 0–" + (limKernel - 1)));
        leyenda.add(Box.createVerticalStrut(5));

        leyenda.add(crearItemLeyenda(new Color(55, 55, 55), "Libre", ""));
        leyenda.add(Box.createVerticalStrut(5));

        for (BCP p : kernel.getListaProcesos()) {
            if (!p.estado.equals("TERMINADO")) {
                Color c = obtenerColorProceso(p.id);
                int base = p.getDireccionBase();
                int fin = base + p.getAlcance() - 1;
                leyenda.add(crearItemLeyenda(c,
                        "PID " + p.id + " — " + p.nombreProceso,
                        "dir " + base + "–" + fin));
                leyenda.add(Box.createVerticalStrut(5));
            }
        }

        leyenda.add(Box.createVerticalGlue());

        JScrollPane scrollLeyenda = new JScrollPane(leyenda);
        scrollLeyenda.setBorder(null);
        scrollLeyenda.setOpaque(false);
        scrollLeyenda.getViewport().setOpaque(false);

        // ── Contenedor interno ────────────────────────────────────────────────
        JPanel contenedor = new JPanel(new BorderLayout(6, 0));
        contenedor.setOpaque(false);
        contenedor.add(scrollBloques, BorderLayout.WEST);
        contenedor.add(scrollLeyenda, BorderLayout.CENTER);

        panelFragmentacion.add(contenedor, BorderLayout.CENTER);
        panelFragmentacion.revalidate();
        panelFragmentacion.repaint();
    }

    private JPanel crearItemLeyenda(Color color, String nombre, String rango) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cuadro = new JPanel();
        cuadro.setBackground(color);
        cuadro.setPreferredSize(new Dimension(12, 12));
        cuadro.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 90), 1));

        JLabel lblNombre = new JLabel(nombre);
        lblNombre.setForeground(Color.WHITE);
        lblNombre.setFont(new Font("SansSerif", Font.PLAIN, 11));

        item.add(cuadro);
        item.add(lblNombre);

        if (rango != null && !rango.isEmpty()) {
            JLabel lblRango = new JLabel(rango);
            lblRango.setForeground(new Color(150, 150, 150));
            lblRango.setFont(new Font("SansSerif", Font.PLAIN, 10));
            item.add(lblRango);
        }

        return item;
    }

    // =========================================================================
    // EJECUCIÓN
    // =========================================================================
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
            if (!continua) {
                timerSimulacion.stop();
            }
        });

        timerSimulacion.start();
        imprimirEnTerminal("Ejecución automática iniciada.");
    }

    private boolean ejecutarPasoAPaso() {
        if (cpu == null)
            return false;

        boolean continua = cpu.ejecutarSiguientePaso();
        bcpActual = cpu.getProcesoActual();
        actualizarLabelsBCP();
        actualizarTablas();

        if (!continua) {
            imprimirEnTerminal("No hay más procesos para ejecutar.");
        } else if (bcpActual != null) {
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

    // =========================================================================
    // TERMINAL
    // =========================================================================
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
        if (!esperandoEntrada)
            return;

        String texto = campoEntrada.getText().trim();
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un valor.",
                    "Entrada vacía", JOptionPane.WARNING_MESSAGE);
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

    // =========================================================================
    // ACTUALIZAR UI
    // =========================================================================
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

        if (panelFragmentacion != null) {
            actualizarPanelFragmentacion();
        }
    }

    private void actualizarTablas() {
        modeloMemoria.setRowCount(0);
        for (int i = 0; i < memoria.getTamanoTotal(); i++) {
            String raw = memoria.leerCelda(i);
            String traducida = traducirInstruccion(raw);
            modeloMemoria.addRow(new Object[] { i, traducida });
        }

        modeloDisco.setRowCount(0);
        for (int i = 0; i < disco.getTamanoTotal(); i++) {
            String dato = disco.leer(i);
            if (i < disco.getEspacioIndice()) {
                modeloDisco.addRow(new Object[] { i, dato });
            } else {
                modeloDisco.addRow(new Object[] { i, traducirInstruccion(dato) });
            }
        }

        modeloProcesos.setRowCount(0);
        for (BCP p : kernel.getListaProcesos()) {
            obtenerColorProceso(p.id);
            modeloProcesos.addRow(new Object[] { p.id, p.nombreProceso, p.estado });
        }

        int direccionActual = cpu.getDireccionIRActual();
        if (direccionActual < 0) {
            direccionActual = memoria.getInicioUsuario();
        }

        renderizadorMemoria.setConfig(direccionActual, memoria.getInicioUsuario());

        tablaMemoriaFisica.repaint();
        tablaProcesos.repaint();

        actualizarPanelFragmentacion();
    }

    // =========================================================================
    // HELPERS DE UI
    // =========================================================================
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
                BorderFactory.createLineBorder(Color.GRAY), titulo);
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
                    tamanoRamConfig, porcentajeKernelConfig,
                    tamanoDiscoConfig, porcentajeIndiceDiscoConfig);
            nueva.setVisible(true);
        });
    }

    private Color obtenerColorProceso(int pid) {
        if (pid <= 0)
            return new Color(220, 220, 220);
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
            if (perteneceAlProceso(p, direccion))
                return p;
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
                if (valor instanceof Integer)
                    return (Integer) valor;
                if (valor != null)
                    return Integer.parseInt(valor.toString());
            } catch (Exception e) {
                /* ignorar */ }
        }
        return null;
    }

    // =========================================================================
    // RENDERERS
    // =========================================================================
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

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

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
                    table, value, isSelected, hasFocus, row, column);

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
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.cpu;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import tarea1.joseandres.kernel.Kernel;
import tarea1.joseandres.memoria.Memoria;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.dispacher.Dispatcher;
import tarea1.joseandres.disco.Disco;
import tarea1.joseandres.uitls.Errors;
import javax.swing.JOptionPane;
import tarea1.joseandres.interfaz.SimuladorGUI;

/**
 *
 * @author joses
 */
public class Cpu implements Runnable {

    private int cpuId;
    private boolean corriendo = false;
    private int delayReloj = 600;

    private Memoria memoria;
    private Dispatcher dispatcher;

    private Map<String, Instruccion> operaciones;
    private Map<String, Integer> pesosInstrucciones;
    private Map<String, IntHandler> interrupciones;
    private Map<Integer, Int21Handler> int21Handlers;
    private Map<String, String> archivos = new HashMap<>();

    private BCP procesoActual;
    private Disco disco;
    private Kernel kernel;
    private Component uiParent;
    private boolean saltoRealizado = false;
    private int cantidadRestante;
    private int quantumActivo;
    
    private tarea1.joseandres.memoria.MemoriaPaginada memoriaPaginada;

    public interface PasoCallback {
        void onPasoEjecutado(int cpuId, BCP bcp);
    }
    private PasoCallback pasoCallback = null;

    public void setPasoCallback(PasoCallback cb) { this.pasoCallback = cb; }

    private boolean esperandoEntradaInt09 = false;
    private String registroDestinoInt09 = "";
    private int direccionIRActual = 0;

    private boolean esApropiativo = false;
    private int quantumMaximo = 5;


    public Cpu(int cpuId, Kernel kernel, Memoria memoria, Dispatcher dispatcher, Disco disco, tarea1.joseandres.memoria.MemoriaPaginada memoriaPaginada, JFrame uiParent) {
        this.cpuId = cpuId;
        this.kernel = kernel;
        this.memoria = memoria;
        this.dispatcher = dispatcher;
        this.disco = disco;
        this.uiParent = uiParent;
     
        this.memoriaPaginada = memoriaPaginada; 
        this.corriendo = false;

        this.operaciones = new HashMap<>();
        this.pesosInstrucciones = new HashMap<>();
        this.interrupciones = new HashMap<>();
        this.int21Handlers = new HashMap<>();

        inicializarOperacion();
        inicializarPeso();
        inicializarInterrupcion();
        inicializarInt21Handler();
    }

    // ==========================================================
    // DICCIONARIO DE INSTRUCCIONES
    // ==========================================================
    private void inicializarOperacion() {
        operaciones.put("00001", new Load());
        operaciones.put("00010", new Store());
        operaciones.put("00011", new Move());
        operaciones.put("00100", new Sub());
        operaciones.put("00101", new Add());
        operaciones.put("00110", new Inc());
        operaciones.put("00111", new Dec());
        operaciones.put("01000", new Jump());
        operaciones.put("01001", new Compare());
        operaciones.put("01010", new JumpEqual());
        operaciones.put("01011", new JumpNotEqual());
        operaciones.put("01100", new Push());
        operaciones.put("01101", new Pop());
        operaciones.put("01110", new Param());
        operaciones.put("01111", new IntInstruction());
        operaciones.put("10000", new Swap());
    }

    private void inicializarPeso() {
        pesosInstrucciones.put("00001", 1); // LOAD
        pesosInstrucciones.put("00010", 1); // STORE
        pesosInstrucciones.put("00011", 1); // MOV
        pesosInstrucciones.put("00100", 1); // SUB
        pesosInstrucciones.put("00101", 1); // ADD
        pesosInstrucciones.put("00110", 1); // INC
        pesosInstrucciones.put("00111", 1); // DEC
        pesosInstrucciones.put("01000", 1); // JMP
        pesosInstrucciones.put("01001", 1); // CMP
        pesosInstrucciones.put("01010", 1); // JE
        pesosInstrucciones.put("01011", 1); // JNE
        pesosInstrucciones.put("01100", 1); // PUSH
        pesosInstrucciones.put("01101", 1); // POP
        pesosInstrucciones.put("01110", 1); // PARAM
        pesosInstrucciones.put("01111", 1); // INT
        pesosInstrucciones.put("10000", 1); // SWAP
    }

    private void inicializarInterrupcion() {
        interrupciones.put("10H", this::manejarInt10H);
        interrupciones.put("21H", this::manejarInt21H);
    }

    private void inicializarInt21Handler() {
        int21Handlers.put(9, (bcp, mem) -> {
            int direccionMemoria = bcp.DX;
            StringBuilder cadena = new StringBuilder();

            while (true) {
                String celda = mem.leerCelda(direccionMemoria);
                if (celda == null || celda.equals("$") || celda.isEmpty()) break;
                cadena.append(celda).append(" ");
                direccionMemoria++;
            }

            String salida = cadena.toString().trim();
            System.out.println("SALIDA [INT 21H / 09H]: " + salida);

            if (uiParent instanceof SimuladorGUI) {
                ((SimuladorGUI) uiParent).imprimirEnTerminal("21H -> " + salida);
            }
        });
    }

    public BCP getProcesoActual() { return this.procesoActual; }
    public void setProcesoActual(BCP bcp) { this.procesoActual = bcp; }
    public int getDireccionIRActual() { return direccionIRActual; }
    public void setCorriendo(boolean corriendo) { this.corriendo = corriendo; }
    public void setDelayReloj(int ms) { this.delayReloj = ms; }
    public void setConfiguracionPlanificacion(boolean esApropiativo, int quantum) {
        this.esApropiativo = esApropiativo;
        this.quantumMaximo = quantum;
    }
    public boolean estaEsperandoEntradaInt09() { return esperandoEntradaInt09; }

    private int obtenerRegistros(String nombreRegistro, BCP bcp) {
        switch (nombreRegistro.trim().toUpperCase()) {
            case "AX": return bcp.AX;
            case "BX": return bcp.BX;
            case "CX": return bcp.CX;
            case "DX": return bcp.DX;
            case "AC": return bcp.AC;
            case "AH": return bcp.AH;
            case "AL": return bcp.AL;
            default: throw new IllegalArgumentException("Registro desconocido: " + nombreRegistro);
        }
    }

    private void escribirRegistro(String nombreRegistro, int valor, BCP bcp) {
        switch (nombreRegistro.trim().toUpperCase()) {
            case "AX": bcp.AX = valor; break;
            case "BX": bcp.BX = valor; break;
            case "CX": bcp.CX = valor; break;
            case "DX": bcp.DX = valor; break;
            case "AH": bcp.AH = valor; break;
            case "AL": bcp.AL = valor; break;
            case "AC": bcp.AC = valor; break;
            default: throw new IllegalArgumentException("Registro desconocido: " + nombreRegistro);
        }
    }

    private boolean esRegistroValido(String nombreRegistro) {
        if (nombreRegistro == null) return false;
        switch (nombreRegistro.trim().toUpperCase()) {
            case "AX": case "BX": case "CX": case "DX":
            case "AC": case "AH": case "AL": return true;
            default: return false;
        }
    }

    private int parseValor(String texto) {
        texto = texto.trim().toUpperCase();
        try {
            if (texto.endsWith("H")) {
                return Integer.parseInt(texto.replace("H", ""), 16);
            } else {
                return Integer.parseInt(texto);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido: " + texto);
        }
    }

    private int obtenerPesoInstruccion(String[] partes) {
        if (partes == null || partes.length == 0) return 1;
        String opcode = partes[0];
        if ("01111".equals(opcode)) {
            if (partes.length > 1) {
                String tipo = partes[1].trim().toUpperCase();
                switch (tipo) {
                    case "10H": return 1;
                    default:    return 1;
                }
            }
        }
        return pesosInstrucciones.getOrDefault(opcode, 1);
    }

    public interface Instruccion { void ejecutar(String[] operandos, BCP bcp, Memoria memoria); }
    public interface IntHandler { void ejecutar(BCP bcp, Memoria memoria); }
    public interface Int21Handler { void ejecutar(BCP bcp, Memoria memoria); }

    // ==========================================================
    // IMPLEMENTACIONES DE INSTRUCCIONES
    // ==========================================================
    public class Load implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("LOAD requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro)) throw new IllegalArgumentException("Registro inválido en LOAD: " + registro);
            bcp.AC = obtenerRegistros(registro, bcp);
            System.out.println("LOAD ejecutado: " + registro + " (" + bcp.AC + ") -> AC");
        }
    }

    public class Store implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("STORE requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro)) throw new IllegalArgumentException("Registro inválido en STORE: " + registro);
            escribirRegistro(registro, bcp.AC, bcp);
            System.out.println("STORE ejecutado: AC (" + bcp.AC + ") -> " + registro);
        }
    }

    public class Move implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 3) throw new IllegalArgumentException("Formato MOVE incorrecto en PC: " + bcp.PC);
            String destino = operandos[1];
            if (!esRegistroValido(destino)) throw new IllegalArgumentException("Registro inválido en MOV: " + destino);
            int valor = esRegistroValido(operandos[2]) ? obtenerRegistros(operandos[2], bcp) : parseValor(operandos[2]);
            escribirRegistro(destino, valor, bcp);
            System.out.println("MOV ejecutado -> " + destino + " = " + valor);
        }
    }

    public class Sub implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("SUB requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro)) throw new IllegalArgumentException("Registro inválido en SUB: " + registro);
            bcp.AC = bcp.AC - obtenerRegistros(registro, bcp);
            System.out.println("SUB ejecutado: AC - " + registro + " = " + bcp.AC);
        }
    }

    public class Add implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("ADD requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro)) throw new IllegalArgumentException("Registro inválido en ADD: " + registro);
            bcp.AC = bcp.AC + obtenerRegistros(registro, bcp);
            System.out.println("ADD ejecutado: AC + " + registro + " = " + bcp.AC);
        }
    }

    public class Push implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("PUSH requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro)) throw new IllegalArgumentException("Registro invalido en PUSH: " + registro);
            int valor = obtenerRegistros(registro, bcp);
            if (!bcp.push(valor)) throw new IllegalArgumentException("Stack Overflow en PUSH.");
            System.out.println("PUSH ejecutado: " + registro + " (" + valor + ") -> pila");
        }
    }

    public class Pop implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("POP requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro)) throw new IllegalArgumentException("Registro inválido en POP: " + registro);
            int valor = bcp.pop();
            if (valor == -999) throw new IllegalArgumentException("Stack Underflow en POP.");
            escribirRegistro(registro, valor, bcp);
            System.out.println("POP ejecutado: pila -> " + registro + " (" + valor + ")");
        }
    }

    public class Param implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("PARAM requiere al menos un valor.");
            int cantidad = operandos.length - 1;
            if (cantidad > 3) throw new IllegalArgumentException("PARAM acepta máximo 3 parámetros.");
            for (int i = 1; i < operandos.length; i++) {
                int valor;
                try {
                    valor = Integer.parseInt(operandos[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Parámetro inválido en PARAM: " + operandos[i]);
                }
                if (!bcp.push(valor)) throw new IllegalArgumentException("Stack Overflow en PARAM.");
            }
            System.out.println("PARAM ejecutado: se cargaron " + cantidad + " valores en pila.");
        }
    }

    public class Compare implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 3) throw new IllegalArgumentException("CMP requiere dos registros.");
            String reg1 = operandos[1]; String reg2 = operandos[2];
            if (!esRegistroValido(reg1)) throw new IllegalArgumentException("Registro inválido en CMP: " + reg1);
            if (!esRegistroValido(reg2)) throw new IllegalArgumentException("Registro inválido en CMP: " + reg2);
            int valor1 = obtenerRegistros(reg1, bcp);
            int valor2 = obtenerRegistros(reg2, bcp);
            bcp.flagIgual = (valor1 == valor2);
            System.out.println("CMP ejecutado: " + reg1 + " (" + valor1 + ") vs " + reg2 + " (" + valor2 + ") -> flagIgual=" + bcp.flagIgual);
        }
    }

    public class Jump implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("JMP requiere dirección/desplazamiento.");
            bcp.PC = parseValor(operandos[1]);
            saltoRealizado = true;
            System.out.println("JMP ejecutado: PC forzado a " + bcp.PC);
        }
    }

    public class JumpEqual implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("JE requiere dirección.");
            if (bcp.flagIgual) {
                bcp.PC = parseValor(operandos[1]);
                saltoRealizado = true;
                System.out.println("JE ejecutado: Salto tomado a PC " + bcp.PC);
            } else {
                System.out.println("JE saltado: flagIgual es false.");
            }
        }
    }

    public class JumpNotEqual implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("JNE requiere dirección.");
            if (!bcp.flagIgual) {
                bcp.PC = parseValor(operandos[1]);
                saltoRealizado = true;
                System.out.println("JNE ejecutado: Salto tomado a PC " + bcp.PC);
            } else {
                System.out.println("JNE saltado: flagIgual es true.");
            }
        }
    }

    public class Inc implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            String reg = (operandos.length >= 2) ? operandos[1] : "AX";
            if (!esRegistroValido(reg)) throw new IllegalArgumentException("Registro inválido en INC: " + reg);
            int val = obtenerRegistros(reg, bcp) + 1;
            escribirRegistro(reg, val, bcp);
            System.out.println("INC ejecutado: " + reg + " ahora vale " + val);
        }
    }

    public class Dec implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("DEC requiere un registro.");
            String reg = operandos[1];
            if (!esRegistroValido(reg)) throw new IllegalArgumentException("Registro inválido en DEC: " + reg);
            int val = obtenerRegistros(reg, bcp) - 1;
            escribirRegistro(reg, val, bcp);
            System.out.println("DEC ejecutado: " + reg + " ahora vale " + val);
        }
    }

    public class Swap implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 3) throw new IllegalArgumentException("Formato SWAP incorrecto en PC: " + bcp.PC);
            String reg1 = operandos[1]; String reg2 = operandos[2];
            if (!esRegistroValido(reg1) || !esRegistroValido(reg2)) throw new IllegalArgumentException("Registro inválido en SWAP");
            int val1 = obtenerRegistros(reg1, bcp); int val2 = obtenerRegistros(reg2, bcp);
            escribirRegistro(reg1, val2, bcp); escribirRegistro(reg2, val1, bcp);
            System.out.println("SWAP ejecutado -> " + reg1 + " <-> " + reg2);
        }
    }

    public class IntInstruction implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) throw new IllegalArgumentException("INT requiere un código de interrupción.");
            String tipo = operandos[1].trim().toUpperCase();
            IntHandler handler = interrupciones.get(tipo);
            if (handler == null) throw new IllegalArgumentException("Interrupción INT no soportada: " + tipo);
            handler.ejecutar(bcp, memoria);
        }
    }

    private void manejarInt10H(BCP bcp, Memoria memoria) {
        String mensaje = String.valueOf(bcp.DX);
        if (uiParent instanceof SimuladorGUI) {
            ((SimuladorGUI) uiParent).imprimirEnTerminal(" 10H -> " + mensaje);
        } else {
            System.out.println("INT 10H ejecutado: salida en pantalla -> DX = " + mensaje);
        }
    }

    private void manejarInt21H(BCP bcp, Memoria memoria) {
        Int21Handler handler = int21Handlers.get(bcp.AH);
        if (handler != null) {
            handler.ejecutar(bcp, memoria);
        } else {
            System.out.println("Servicio de INT 21H no soportado: AH=" + bcp.AH);
        }
    }

    @Override
    public void run() {
        this.corriendo = true;
        System.out.println("CPU " + cpuId + ": Hilo encendido y listo.");

        while (corriendo) {
        try {
            // 1. Ejecutamos el paso de inmediato
            boolean ejecuto = ejecutarSiguientePaso();

            // 2. Notificamos a la interfaz gráfica de forma segura en el EDT
            if (pasoCallback != null) {
                final BCP bcpSnapshot = procesoActual;
                final int idSnapshot  = cpuId;
                javax.swing.SwingUtilities.invokeLater(
                    () -> pasoCallback.onPasoEjecutado(idSnapshot, bcpSnapshot)
                );
            }

            // 3. Manejo eficiente de los tiempos de espera (Delays)
            if (ejecuto) {
                // Si la CPU ejecutó una instrucción, espera el tiempo del ciclo de reloj configurado
                Thread.sleep(delayReloj);
            } else {
                // Si NO ejecutó nada (porque el Kernel no devolvió procesos y la CPU quedó ociosa),
                // duerme un tiempo muy corto (ej. 100ms) para no saturar el procesador real de la computadora,
                // pero permitiendo reaccionar de forma inmediata apenas se cargue un proceso en la interfaz.
                Thread.sleep(100);
            }

        } catch (InterruptedException e) {
            System.out.println("CPU " + cpuId + " detenida.");
            this.corriendo = false;
        }
    }
}
//AQUI ES DONDE DEBO IMPLEMENTAR LOS ALGORITMOS EXPROPIATIVOS.
    
    
    public boolean ejecutarSiguientePaso() {
        if (procesoActual == null) {
            procesoActual = kernel.solicitarSiguienteProceso(this.cpuId);
            if (procesoActual == null) {
                System.out.println("CPU " + cpuId + ": No hay procesos pendientes.");
                return false;
            }
            dispatcher.despachar(procesoActual, this.cpuId);
        }

        if (procesoActual.estado.equals("TERMINADO") || procesoActual.estado.equals("ERROR")) {
            SimuladorGUI gui = (SimuladorGUI)this.uiParent;
            procesoActual.tiempoFinal = System.currentTimeMillis();
            gui.agregarProcesoListaFinalizados(procesoActual);
            procesoActual = kernel.solicitarSiguienteProceso(this.cpuId);
            if (procesoActual == null) return false;
            dispatcher.despachar(procesoActual, this.cpuId);
        }

        boolean esPaginado = (procesoActual.getDireccionBase() == -1); 
        boolean finAlcanzado = false;

        if (esPaginado) {
            if (procesoActual.PC >= procesoActual.getAlcance() || procesoActual.rafagaRestante <= 0) {
                finAlcanzado = true;
            }
        } else {
            int limiteProceso = procesoActual.getDireccionBase() + procesoActual.getAlcance();
            if (procesoActual.PC >= limiteProceso || procesoActual.rafagaRestante <= 0) {
                finAlcanzado = true;
            }
        }

        if (finAlcanzado) {
            int pidTerminado = procesoActual.id;
            procesoActual.estado = "TERMINADO";
            procesoActual.tiempoFinal = System.currentTimeMillis();
            procesoActual.IR = "END";
            SimuladorGUI gui = (SimuladorGUI)this.uiParent;
            procesoActual.tiempoFinal = System.currentTimeMillis();
            gui.agregarProcesoListaFinalizados(procesoActual);

            dispatcher.actualizarBcpEnKernel(procesoActual);
            kernel.finalizarProceso(procesoActual);

            BCP siguiente = kernel.solicitarSiguienteProceso(this.cpuId);
            if (siguiente != null) {
                procesoActual = siguiente;
                dispatcher.despachar(procesoActual, this.cpuId);
                return true;
            } else {
                procesoActual = null;
                return false;
            }
        }

        direccionIRActual = procesoActual.PC;
        String instruccionCompleta;

        if (esPaginado) {
            int direccionVirtual = procesoActual.PC;
            int direccionFisicaReal = this.memoriaPaginada.traducirDireccionVirtual(procesoActual, direccionVirtual);
            
            if (direccionFisicaReal == -1) {
                return marcarErrorProceso("Fallo de Página / Violación de Memoria Virtual en PC: " + direccionVirtual);
            }
            
           
            instruccionCompleta = memoria.leerCelda(direccionFisicaReal); 
        } else {
            instruccionCompleta = memoria.leerSeguro(procesoActual.PC, procesoActual.getDireccionBase(), procesoActual.getAlcance());
        }
        
        procesoActual.IR = instruccionCompleta;

        if (instruccionCompleta == null
                || instruccionCompleta.trim().toUpperCase().startsWith("END")
                || instruccionCompleta.equals("00000")) {
            procesoActual.estado = "TERMINADO";
            procesoActual.tiempoFinal = System.currentTimeMillis();
            SimuladorGUI gui = (SimuladorGUI)this.uiParent;
            procesoActual.tiempoFinal = System.currentTimeMillis();
            gui.agregarProcesoListaFinalizados(procesoActual);
            dispatcher.actualizarBcpEnKernel(procesoActual);
            kernel.finalizarProceso(procesoActual);

            BCP siguiente = kernel.solicitarSiguienteProceso(this.cpuId);
            if (siguiente != null) {
                procesoActual = siguiente;
                dispatcher.despachar(procesoActual, this.cpuId);
                return true;
            } else {
                procesoActual = null;
                return false;
            }
        }

        String[] partes = instruccionCompleta.split("\\s+");
        String opcode = partes[0].toUpperCase();

        Instruccion instr = operaciones.get(opcode);
        if (instr == null) {
            return marcarErrorProceso("Opcode inválido -> " + opcode);
        }

        try {
            saltoRealizado = false;
            instr.ejecutar(partes, procesoActual, memoria);

            if (!esperandoEntradaInt09) {
                int peso = obtenerPesoInstruccion(partes);
                procesoActual.ciclosConsumidos += peso;
                //procesoActual.rafagaEjecutada++;
                //procesoActual.rafagaRestante--;
            }
        } catch (Exception e) {
            return marcarErrorProceso("Error en ejecución: " + e.getMessage());
        }

        if (esperandoEntradaInt09) {
            dispatcher.actualizarBcpEnKernel(procesoActual);
            return true;
        }

        if (!saltoRealizado) {
            procesoActual.PC++;
        }

        dispatcher.actualizarBcpEnKernel(procesoActual);
/**
        if (this.kernel.getScheduler().getEstrategia().esApropiativo
                && procesoActual.rafagaEjecutada >= this.kernel.getScheduler().getEstrategia().quantum
                && procesoActual.rafagaRestante > 0) {
            procesoActual.rafagaEjecutada = 0;
            kernel.devolverAColaListos(procesoActual);
            procesoActual = null;
        }
        
         **/
         
        if (this.kernel.getScheduler().getEstrategia().esApropiativo){
            if(procesoActual.rafagaEjecutada >= this.kernel.getScheduler().getEstrategia().quantum){
                procesoActual.rafagaEjecutada = 0;
                if(procesoActual.rafagaRestante > 0){
                    kernel.devolverAColaListos(procesoActual);
                    procesoActual = null;
                }
            }else{
                procesoActual.rafagaEjecutada++;
                procesoActual.rafagaRestante--;
            }
        }
        if (procesoActual != null){
            dispatcher.actualizarBcpEnKernel(procesoActual);
        }

        return true;
    }

    private boolean marcarErrorProceso(String mensaje) {
        procesoActual.estado = "ERROR";
        procesoActual.tiempoFinal = System.currentTimeMillis();
        dispatcher.actualizarBcpEnKernel(procesoActual);
        kernel.finalizarProceso(procesoActual);
        procesoActual = null;

        Errors.logError(mensaje);
        if (uiParent != null) {
            Errors.mostrarErrorVisual(uiParent, "Error de ejecución", mensaje);
        }
        return false;
    }
    public void setearQuantumMaximo(int q){
        this.quantumMaximo = q;
    }
}
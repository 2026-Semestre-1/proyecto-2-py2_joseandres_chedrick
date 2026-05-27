/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.cpu;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
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
public class Cpu {

    private Memoria memoria;
    private Dispatcher dispatcher; // Para actualizar la RAM del Kernel

    private Map<String, Instruccion> operaciones;
    private Map<String, Integer> pesosInstrucciones; //Inst, pesos
    private Map<String, IntHandler> interrupciones; //INT-> interrupcuines
    private Map<Integer, Int21Handler> int21Handlers;//Guardaremos los atributos de 21H
    private Map<String, String> archivos = new HashMap<>(); //Guardaremos los archivos  filesystem

    private BCP procesoActual;
    private Disco disco;
    private Kernel kernel;
    private Component uiParent;//Conectamos a la CPU para mandar errores visuales de registros.
    private boolean saltoRealizado = false; //Permite saber si se realizo un JMP
    private boolean esperandoEntradaInt09 = false;//Para entrada de teclado.

    private int direccionIRActual = -1; //Guardamos indice de IR

    public Cpu(Memoria memoria, Dispatcher dispatcher, Disco disco, Kernel kernel, Component uiParent) {
        this.memoria = memoria;
        this.dispatcher = dispatcher;
        this.disco = disco;
        this.kernel = kernel;
        this.uiParent = uiParent;

        //=============================================================
        //==============Registramos las operaciones=====================
        //============================================================
        operaciones = new HashMap<>();
        // Registramos con los nuevos opcodes de 5 bits
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

        //=============================================================
        //==============Manejo de instrucciones INT=========================
        //=============================================================
        interrupciones = new HashMap<>();
        interrupciones.put("20H", this::manejarInt20H);
        interrupciones.put("10H", this::manejarInt10H);
        interrupciones.put("09H", this::manejarInt09H);
        interrupciones.put("21H", this::manejarInt21H);

        //=============================================================
        //==============Manejo de instrucciones 21H=========================
        //=============================================================
        //Usamos decimal.
        int21Handlers = new HashMap<>();

        int21Handlers.put(60, this::crearArchivo);     // 3CH
        int21Handlers.put(64, this::escribirArchivo);  // 40H
        int21Handlers.put(77, this::leerArchivo);      // 4DH
        int21Handlers.put(65, this::eliminarArchivo);  // 41H
        int21Handlers.put(61, this::abrirArchivo);     // 3DH

        //=============================================================
        //==============Registramos sus pesos=========================
        //=============================================================
        pesosInstrucciones = new HashMap<>();
        pesosInstrucciones.put("00001", 2); // LOAD
        pesosInstrucciones.put("00010", 2); // STORE
        pesosInstrucciones.put("00011", 1); // MOV
        pesosInstrucciones.put("00100", 3); // SUB
        pesosInstrucciones.put("00101", 3); // ADD
        pesosInstrucciones.put("00110", 1); // INC
        pesosInstrucciones.put("00111", 1); // DEC
        pesosInstrucciones.put("01000", 2); // JMP
        pesosInstrucciones.put("01001", 2); // CMP
        pesosInstrucciones.put("01010", 2); // JE
        pesosInstrucciones.put("01011", 2); // JNE
        pesosInstrucciones.put("01100", 1); // PUSH
        pesosInstrucciones.put("01101", 1); // POP
        pesosInstrucciones.put("01110", 3); // PARAM
        pesosInstrucciones.put("01111", 2); // INT 
        pesosInstrucciones.put("10000", 1); // SWAP
    }

    //getter
    public BCP getProcesoActual() {
        return this.procesoActual;
    }

    /**
     * Permite que el simulador le asigne un nuevo trabajo a la CPU. El
     * Dispatcher se encarga de cambiar el estado y registrarlo en el Kernel.
     */
    public void setProcesoActual(BCP bcp) {
        this.procesoActual = bcp;
    }

    public int getDireccionIRActual() {
        return direccionIRActual;
    }

    //Metodo  para leer valor de un registro por nombre 
    private int obtenerRegistros(String nombreRegistro, BCP bcp) {
        switch (nombreRegistro.trim().toUpperCase()) {
            case "AX":
                return bcp.AX;
            case "BX":
                return bcp.BX;
            case "CX":
                return bcp.CX;
            case "DX":
                return bcp.DX;
            case "AC":
                return bcp.AC;
            case "AH":
                return bcp.AH;
            case "AL":
                return bcp.AL;
            default:
                throw new IllegalArgumentException("Registro desconocido: " + nombreRegistro);
        }
    }

    //mMetodo para escribir un valor a un registro por nombre
    private void escribirRegistro(String nombreRegistro, int valor, BCP bcp) {
        switch (nombreRegistro.trim().toUpperCase()) {
            case "AX":
                bcp.AX = valor;
                break;
            case "BX":
                bcp.BX = valor;
                break;
            case "CX":
                bcp.CX = valor;
                break;
            case "DX":
                bcp.DX = valor;
                break;
            case "AH":
                bcp.AH = valor;
                break;
            case "AL":
                bcp.AL = valor;
                break;
            case "AC":
                bcp.AC = valor;
                break;
            default:
                throw new IllegalArgumentException("Registro desconocido: " + nombreRegistro);
        }
    }

    //Clase para errores
    private boolean esRegistroValido(String nombreRegistro) {
        if (nombreRegistro == null) {
            return false;
        }

        switch (nombreRegistro.trim().toUpperCase()) {
            case "AX":
            case "BX":
            case "CX":
            case "DX":
            case "AC":
            case "AH":
            case "AL":
                return true;
            default:
                return false;
        }
    }

    //Switch necesario para validar el peso de 01111->INT
    private int obtenerPesoInstruccion(String[] partes) {
        if (partes == null || partes.length == 0) {
            return 1;
        }

        String opcode = partes[0];

        //  INT cambia segun el operando
        if ("01111".equals(opcode)) {
            if (partes.length > 1) {
                String tipoInterrupcion = partes[1].trim().toUpperCase();

                switch (tipoInterrupcion) {
                    case "20H":
                        return 2; // finaliza programa
                    case "10H":
                        return 2; // imprimimos pantalla
                    case "09H":
                        return 3; // Entrada de teclado
                    case "21H":
                        return 5; // manejo de archivos
                    case "3":
                        return 3;
                    default:
                        return 2;
                }
            }
        }

        return pesosInstrucciones.getOrDefault(opcode, 1); //Devolvemos el INT
    }

    ////////////////////////////////////////////////////////////////////////////////
    //PATRON DE DISENO STRATEGY. Evitamos hacer puros ifs y whiles
     ////////////////////////////////////////////////////////////////////////////////
   
     
     public interface Instruccion {

        void ejecutar(String[] operandos, BCP bcp, Memoria memoria);
    }

    //====================Constructor para los eventos de INT======================
    public interface IntHandler {

        void ejecutar(BCP bcp, Memoria memoria);
    }

    //===============Constructor para los eventos de 21Handler======================
    public interface Int21Handler {

        void ejecutar(BCP bcp, Memoria memoria);
    }

    public class Load implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("LOAD requiere un registro.");
            }
            // operandos[1] es el registro de donde viene el dato (ej: "AX")
            String registro = operandos[1];

            if (!esRegistroValido(registro)) {
                throw new IllegalArgumentException("Registro inválido en LOAD: " + registro);
            }

            // El AC recibe el valor que hay en ese registro
            bcp.AC = obtenerRegistros(registro, bcp);
            System.out.println("LOAD ejecutado: " + registro + " (" + bcp.AC + ") -> AC");
        }
    }

    public class Store implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("STORE requiere un registro.");
            }
            // operandos[1] es el registro donde vamos a guardar (ej: "DX")
            String registro = operandos[1];
            if (!esRegistroValido(registro)) {
                throw new IllegalArgumentException("Registro inválido en STORE: " + registro);
            }

            // Escribimos en el registro lo que hay actualmente en el AC
            escribirRegistro(registro, bcp.AC, bcp);

            System.out.println("STORE ejecutado: AC (" + bcp.AC + ") -> " + registro);
        }
    }

    public class Move implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            // operandos[1] = "AX"
            // operandos[2] = "5"
            if (operandos.length < 3) {
                throw new IllegalArgumentException("Formato MOVE incorrecto en PC: " + bcp.PC);
            }
            String registro = operandos[1];
            if (!esRegistroValido(registro)) {
                throw new IllegalArgumentException("Registro inválido en MOV: " + registro);
            }
            int valor = parseValor(operandos[2]);

            escribirRegistro(registro, valor, bcp);
            System.out.println("MOV ejecutado -> " + registro + " = " + valor);
        }
    }

    public class Sub implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("SUB requiere un registro.");
            }
            String registro = operandos[1];
            if (!esRegistroValido(registro)) {
                throw new IllegalArgumentException("Registro inválido en SUB: " + registro);
            }
            int valorReg = obtenerRegistros(registro, bcp);
            bcp.AC = bcp.AC - valorReg;
            System.out.println("SUB ejecutado: AC - " + registro + " = " + bcp.AC);
        }
    }

    public class Add implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("ADD requiere un registro.");
            }

            String registro = operandos[1];
            if (!esRegistroValido(registro)) {
                throw new IllegalArgumentException("Registro inválido en ADD: " + registro);
            }
            int valorReg = obtenerRegistros(registro, bcp);
            bcp.AC = bcp.AC + valorReg;
            System.out.println("ADD ejecutado: AC + " + registro + " = " + bcp.AC);
        }
    }

    ///////////////////////////Registros E/S/////////////////////////
    public class Push implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("PUSH requiere un registro.");
            }

            String registro = operandos[1];

            if (!esRegistroValido(registro)) {
                throw new IllegalArgumentException("Registro invalido en PUSH: " + registro);
            }

            int valor = obtenerRegistros(registro, bcp);

            boolean ok = bcp.push(valor);
            if (!ok) {
                throw new IllegalArgumentException("Stack Overflow en PUSH.");
            }

            System.out.println("PUSH ejecutado: " + registro + " (" + valor + ") -> pila");
        }
    }

    //Saca de la cola y se gaurda en BX
    public class Pop implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("POP requiere un registro.");
            }

            String registro = operandos[1];

            if (!esRegistroValido(registro)) {
                throw new IllegalArgumentException("Registro inválido en POP: " + registro);
            }

            int valor = bcp.pop();

            if (valor == -999) {
                throw new IllegalArgumentException("Stack Underflow en POP.");
            }

            escribirRegistro(registro, valor, bcp);

            System.out.println("POP ejecutado: pila -> " + registro + " (" + valor + ")");
        }
    }

    //Trae valores que se guardan en la pila
    public class Param implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("PARAM requiere al menos un valor.");
            }

            int cantidad = operandos.length - 1;

            if (cantidad > 3) {
                throw new IllegalArgumentException("PARAM acepta máximo 3 parámetros.");
            }

            for (int i = 1; i < operandos.length; i++) {
                int valor;
                try {
                    valor = Integer.parseInt(operandos[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Parámetro inválido en PARAM: " + operandos[i]);
                }

                boolean ok = bcp.push(valor);
                if (!ok) {
                    throw new IllegalArgumentException("Stack Overflow en PARAM.");
                }
            }

            System.out.println("PARAM ejecutado: se cargaron " + cantidad + " valores en pila.");
        }
    }

    public class Compare implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 3) {
                throw new IllegalArgumentException("CMP requiere dos registros.");
            }

            String reg1 = operandos[1];
            String reg2 = operandos[2];

            if (!esRegistroValido(reg1)) {
                throw new IllegalArgumentException("Registro inválido en CMP: " + reg1);
            }

            if (!esRegistroValido(reg2)) {
                throw new IllegalArgumentException("Registro inválido en CMP: " + reg2);
            }

            int valor1 = obtenerRegistros(reg1, bcp);
            int valor2 = obtenerRegistros(reg2, bcp);

            bcp.flagIgual = (valor1 == valor2);

            System.out.println("CMP ejecutado: " + reg1 + " (" + valor1 + ") vs "
                    + reg2 + " (" + valor2 + ") -> flagIgual=" + bcp.flagIgual);
        }
    }

    //Salta por si solo
    public class Jump implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("JMP requiere un desplazamiento.");
            }

            int desplazamiento;
            try {
                desplazamiento = Integer.parseInt(operandos[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Desplazamiento inválido en JMP: " + operandos[1]);
            }

            bcp.PC = bcp.PC + desplazamiento;
            saltoRealizado = true;

            System.out.println("JMP ejecutado: salto de " + desplazamiento + " -> nuevo PC " + bcp.PC);
        }
    }

    //Salta si CMP retorna true.
    public class JumpEqual implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("JE requiere un desplazamiento.");
            }

            int desplazamiento;
            try {
                desplazamiento = Integer.parseInt(operandos[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Desplazamiento inválido en JE: " + operandos[1]);
            }

            if (bcp.flagIgual) {
                bcp.PC = bcp.PC + desplazamiento;
                saltoRealizado = true;
                System.out.println("JE ejecutado: salto de " + desplazamiento + " -> nuevo PC " + bcp.PC);
            } else {
                System.out.println("JE no ejecutado: flagIgual=false");
            }
        }
    }

    //Salta si CMP retorna false.
    public class JumpNotEqual implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2) {
                throw new IllegalArgumentException("JNE requiere un desplazamiento.");
            }

            int desplazamiento;
            try {
                desplazamiento = Integer.parseInt(operandos[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Desplazamiento inválido en JNE: " + operandos[1]);
            }

            if (!bcp.flagIgual) {
                bcp.PC = bcp.PC + desplazamiento;
                saltoRealizado = true;
                System.out.println("JNE ejecutado: salto de " + desplazamiento + " -> nuevo PC " + bcp.PC);
            } else {
                System.out.println("JNE no ejecutado: flagIgual=true");
            }
        }
    }

    public class Inc implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {

            if (operandos.length < 2) {
                throw new IllegalArgumentException("Formato INC incorrecto en PC: " + bcp.PC);
            }

            String reg = operandos[1];

            if (!esRegistroValido(reg)) {
                throw new IllegalArgumentException("Registro inválido en INC: " + reg);
            }

            int valor = obtenerRegistros(reg, bcp);
            valor++;

            escribirRegistro(reg, valor, bcp);

            System.out.println("INC ejecutado -> " + reg + " = " + valor);
        }
    }

    public class Dec implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {

            if (operandos.length < 2) {
                throw new IllegalArgumentException("Formato DEC incorrecto en PC: " + bcp.PC);
            }

            String reg = operandos[1];

            if (!esRegistroValido(reg)) {
                throw new IllegalArgumentException("Registro inválido en DEC: " + reg);
            }

            int valor = obtenerRegistros(reg, bcp);
            valor--;

            escribirRegistro(reg, valor, bcp);

            System.out.println("DEC ejecutado -> " + reg + " = " + valor);
        }
    }

    public class Swap implements Instruccion {

        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {

            if (operandos.length < 3) {
                throw new IllegalArgumentException("Formato SWAP incorrecto en PC: " + bcp.PC);
            }

            String reg1 = operandos[1];
            String reg2 = operandos[2];

            if (!esRegistroValido(reg1) || !esRegistroValido(reg2)) {
                throw new IllegalArgumentException("Registro inválido en SWAP");
            }

            int val1 = obtenerRegistros(reg1, bcp);
            int val2 = obtenerRegistros(reg2, bcp);

            escribirRegistro(reg1, val2, bcp);
            escribirRegistro(reg2, val1, bcp);

            System.out.println("SWAP ejecutado -> " + reg1 + " <-> " + reg2);
        }
    }

    //==========================================================================================
    //==================================INTERRUPCIONES==========================================
    //==========================================================================================
    //Int. Tenemos el switch para caso 20H
    public class IntInstruction implements Instruccion {

        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {

            if (operandos.length < 2) {
                throw new IllegalArgumentException("INT requiere un código de interrupción.");
            }

            String tipo = operandos[1].trim().toUpperCase();

            IntHandler handler = interrupciones.get(tipo);

            if (handler == null) {
                throw new IllegalArgumentException("Interrupción INT no soportada: " + tipo);
            }

            handler.ejecutar(bcp, memoria);
        }
    }

    //clases aparte del hashmap
    private void manejarInt20H(BCP bcp, Memoria memoria) {
        bcp.estado = "TERMINADO";
        bcp.tiempoFinal = System.currentTimeMillis();
        bcp.IR = "00000";
        kernel.finalizarProceso(procesoActual); // libera memoria

        System.out.println("INT 20H ejecutado: proceso finalizado.");
    }

    private void manejarInt10H(BCP bcp, Memoria memoria) {
        String mensaje = String.valueOf(bcp.DX);

        if (uiParent instanceof SimuladorGUI) {
            SimuladorGUI gui = (SimuladorGUI) uiParent;
            gui.imprimirEnTerminal(" 10H -> " + mensaje);
        } else {
            System.out.println("INT 10H ejecutado: salida en pantalla -> DX = " + mensaje);
        }
    }

    //Solicitamos entrada al usuario.
    private void manejarInt09H(BCP bcp, Memoria memoria) {

        if (!(uiParent instanceof SimuladorGUI)) {
            throw new IllegalArgumentException("No hay interfaz disponible para INT 09H.");
        }

        SimuladorGUI gui = (SimuladorGUI) uiParent;

        // Primera vez 
        if (!esperandoEntradaInt09) {
            gui.solicitarEntrada("INT 09H -> Ingrese un número entre 0 y 255:");
            esperandoEntradaInt09 = true;
            return;
        }

        // Si  no hay entrada → seguir esperando
        if (!gui.hayEntradaDisponible()) {
            return;
        }

        // SIEMPRE consumimos la entrada
        String entrada = gui.consumirEntrada();

        int valor;

        try {
            valor = Integer.parseInt(entrada.trim());
        } catch (NumberFormatException e) {

            gui.imprimirEnTerminal("[ERROR] Entrada inválida. Debe ser un número.");
            esperandoEntradaInt09 = true;

            //  Volvemos a pedir entrada correctamente
            gui.solicitarEntrada("INT 09H -> Ingrese un número entre 0 y 255:");

            return;
        }

        if (valor < 0 || valor > 255) {

            gui.imprimirEnTerminal("[ERROR] El número debe estar entre 0 y 255.");

            gui.solicitarEntrada("INT 09H -> Ingrese un número entre 0 y 255:");

            return;
        }

        //  Entrada exito
        bcp.DX = valor;
        esperandoEntradaInt09 = false;

        gui.imprimirEnTerminal("[OK] Entrada guardada en DX = " + valor);
    }

    //Aqui manejaremos, crear archivo, seleccionar, etc
    private void manejarInt21H(BCP bcp, Memoria memoria) {
        int operacion = bcp.AH;

        Int21Handler handler = int21Handlers.get(operacion);

        if (handler == null) {
            throw new IllegalArgumentException("INT 21H operación no soportada en AH: " + operacion);
        }

        handler.ejecutar(bcp, memoria);
    }

    private void crearArchivo(BCP bcp, Memoria memoria) {
        SimuladorGUI gui = (SimuladorGUI) uiParent;

        String nombre = "21H_File_" + bcp.DX;

        int direccion = disco.crearArchivoEnDisco(nombre);

        if (direccion == -1) {
            throw new IllegalArgumentException("No hay espacio en disco para crear el archivo: " + nombre);
        }

        gui.imprimirEnTerminal("[FS] Archivo creado: " + nombre + " en " + direccion);
    }

    private void escribirArchivo(BCP bcp, Memoria memoria) {
        SimuladorGUI gui = (SimuladorGUI) uiParent;

        String nombre = "21H_File_" + bcp.DX;
        int valor = bcp.AL;

        int indice = disco.buscarIndiceArchivo(nombre);

        if (indice == -1) {
            throw new IllegalArgumentException("Archivo no esxiste: " + nombre);
        }

        int inicio = disco.obtenerInicioDesdeIndice(indice);
        int fin = disco.obtenerFinDesdeIndice(indice);

        int nuevaPos = fin + 1;

        if (nuevaPos >= disco.getTamanoTotal()) {
            throw new IllegalArgumentException("Disco lleno");
        }

        disco.escribirContenidoArchivo(nombre, String.valueOf(valor));

        // actualizar fin en índice
        disco.actualizarRangoArchivo(indice, inicio, nuevaPos);

        gui.imprimirEnTerminal("[FS] Escrito en " + nombre + ": " + valor);
    }

    //hACEMOS LA LECTURA DE DISCO
    private void leerArchivo(BCP bcp, Memoria memoria) {
        SimuladorGUI gui = (SimuladorGUI) uiParent;

        String nombre = "21H_File_" + bcp.DX;

        String contenido = disco.leerContenidoArchivo(nombre);

        if (contenido == null) {
            throw new IllegalArgumentException("Archivo no existe: " + nombre);
        }

        if (contenido.isEmpty()) {
            bcp.AL = 0;
            gui.imprimirEnTerminal("[FS] Archivo vacío: " + nombre);
            return;
        }

        try {
            bcp.AL = Integer.parseInt(contenido.trim());
        } catch (NumberFormatException e) {
            bcp.AL = 0;
        }

        gui.imprimirEnTerminal("[FS] Leído de " + nombre + ": " + contenido);
    }

    //eLIMINAMOS EL ARCHIVO CREADO SI EXISTE
    private void eliminarArchivo(BCP bcp, Memoria memoria) {
        SimuladorGUI gui = (SimuladorGUI) uiParent;

        String nombre = "21H_File_" + bcp.DX;

        boolean ok = disco.eliminarArchivo(nombre);

        if (!ok) {
            throw new IllegalArgumentException("Archivo no existe: " + nombre);
        }

        gui.imprimirEnTerminal("[FS] Archivo eliminado: " + nombre);
    }

    private void abrirArchivo(BCP bcp, Memoria memoria) {
        SimuladorGUI gui = (SimuladorGUI) uiParent;

        String nombre = "21H_File_" + bcp.DX;

        if (!disco.existeArchivo(nombre)) {
            throw new IllegalArgumentException("Archivo no existe: " + nombre);
        }

        gui.imprimirEnTerminal("[FS] Archivo abierto: " + nombre);
    }

    //CLASE CREADA EN SU TOTALIDAD CON CHATGPT
    /**
     * Este es el método que llamará tu botón "Next Step" en la Interfaz. Recibe
     * el proceso que el Scheduler/Dispatcher decidió poner en CPU.
     */
    public boolean ejecutarSiguientePaso() {

        // 1. Si no hay proceso actual, intentamos pedir uno al kernel
        if (procesoActual == null) {
            procesoActual = kernel.solicitarSiguienteProceso();

            if (procesoActual == null) {
                System.out.println("CPU: No hay procesos pendientes.");
                return false;
            }

            dispatcher.despachar(procesoActual);
        }

        // 2. Si el proceso actual ya terminó o falló, pedimos otro
        if (procesoActual.estado.equals("TERMINADO") || procesoActual.estado.equals("ERROR")) {
            procesoActual = kernel.solicitarSiguienteProceso();
            if (procesoActual == null) {
                return false;
            }
            dispatcher.despachar(procesoActual);
        }

        // 3. VALIDAR FIN REAL DEL PROCESO POR RANGO
        int limiteProceso = procesoActual.getDireccionBase() + procesoActual.getAlcance();

        if (procesoActual.PC >= limiteProceso) {
            int pidTerminado = procesoActual.id;

            procesoActual.estado = "TERMINADO";
            procesoActual.tiempoFinal = System.currentTimeMillis();
            procesoActual.IR = "00000";
            if (!saltoRealizado) {
                procesoActual.PC++;
            }
            saltoRealizado = false;
            dispatcher.actualizarBcpEnKernel(procesoActual);

            kernel.finalizarProceso(procesoActual);

            BCP siguiente = kernel.solicitarSiguienteProceso();

            if (siguiente != null) {
                procesoActual = siguiente;
                dispatcher.despachar(procesoActual);

                System.out.println("CPU: Finalizado PID " + pidTerminado
                        + ". Iniciando PID " + procesoActual.id);
                return true;
            } else {
                procesoActual = null;
                System.out.println("CPU: Finalizado PID " + pidTerminado
                        + ". No hay más procesos. Sistema en IDLE.");
                return false;
            }
        }
        // 3. FETCH
        direccionIRActual = procesoActual.PC;
        String instruccionCompleta = memoria.leerCelda(procesoActual.PC);
        procesoActual.IR = instruccionCompleta;

        // 4. Si encontramos fin de programa
        if (instruccionCompleta == null || instruccionCompleta.equals("00000")) {
            int pidTerminado = procesoActual.id;

            procesoActual.estado = "TERMINADO";
            procesoActual.tiempoFinal = System.currentTimeMillis();
            dispatcher.actualizarBcpEnKernel(procesoActual);

            // Ahora le avisamos al kernel.
            kernel.finalizarProceso(procesoActual);
            BCP siguiente = kernel.solicitarSiguienteProceso();

            if (siguiente != null) {
                procesoActual = siguiente;
                dispatcher.despachar(procesoActual);

                System.out.println("CPU: Finalizado PID " + pidTerminado
                        + ". Iniciando PID " + procesoActual.id);
                return true;
            } else {
                procesoActual = null;
                System.out.println("CPU: Finalizado PID " + pidTerminado
                        + ". No hay más procesos. Sistema en IDLE.");
                return false;
            }
        }

        // 5. DECODE
        String[] partes = instruccionCompleta.split("\\s+");
        String opcode = partes[0]; //Partes de registro

        // 6. EXECUTE
        Instruccion instr = operaciones.get(opcode);

        if (instr == null) {
            return marcarErrorProceso("Opcode inválido -> " + opcode);
        }

        try {
            instr.ejecutar(partes, procesoActual, memoria);

            // Solo sumar peso si la instrucción no quedó esperando entrada
            if (!esperandoEntradaInt09) {
                int peso = obtenerPesoInstruccion(partes);
                procesoActual.ciclosConsumidos += peso;

                System.out.println("CPU: instrucción " + opcode + " con peso " + peso
                        + ". Total acumulado del proceso: " + procesoActual.ciclosConsumidos);
            }

        } catch (IllegalArgumentException e) {
            return marcarErrorProceso(e.getMessage());
        } catch (ArithmeticException e) {
            return marcarErrorProceso(e.getMessage());
        } catch (Exception e) {
            return marcarErrorProceso("Error inesperado en ejecución: " + e.getMessage());
        }

        if (esperandoEntradaInt09) {
            // NO avanzamos el PC
            dispatcher.actualizarBcpEnKernel(procesoActual);
            return true;
        }

        procesoActual.PC++;
        dispatcher.actualizarBcpEnKernel(procesoActual);
        return true;
    }

    //==========================================================================================
    //==================================UTILS==========================================
    //==========================================================================================
    private boolean marcarErrorProceso(String mensaje) {
        procesoActual.estado = "ERROR";
        procesoActual.tiempoFinal = System.currentTimeMillis();

        dispatcher.actualizarBcpEnKernel(procesoActual);
        // Liberamos memoria del proceso con error
        kernel.finalizarProceso(procesoActual);
        // Lo sacamos de CPU para que entre el siguiente en el proximo paso
        procesoActual = null;

        //Error de GUI
        Errors.logError(mensaje);

        if (uiParent != null) {
            Errors.mostrarErrorVisual(
                    uiParent,
                    "Error de ejecución",
                    mensaje
            );
        }

        return false;
    }

    public boolean estaEsperandoEntradaInt09() {
        return esperandoEntradaInt09;
    }

    private int parseValor(String texto) {
        texto = texto.trim().toUpperCase();

        try {
            if (texto.endsWith("H")) {
                // Hexadecimal tipo 3CH
                return Integer.parseInt(texto.replace("H", ""), 16);
            } else {
                // Decimal normal
                return Integer.parseInt(texto);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido: " + texto);
        }
    }

}

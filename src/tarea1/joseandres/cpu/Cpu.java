package tarea1.joseandres.cpu;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import tarea1.joseandres.kernel.Kernel;
import tarea1.joseandres.memoria.Memoria;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.dispacher.Dispatcher;
import tarea1.joseandres.uitls.Errors;
import tarea1.joseandres.interfaz.SimuladorGUI;

public class Cpu {

    private Memoria memoria;
    private Dispatcher dispatcher;

    private Map<String, Instruccion> operaciones;
    private Map<String, Integer> pesosInstrucciones;
    private Map<String, IntHandler> interrupciones;

    private BCP procesoActual;
    private Kernel kernel;
    private Component uiParent;
    private boolean saltoRealizado = false;

    private int direccionIRActual = -1;

    public Cpu(Memoria memoria, Dispatcher dispatcher, Kernel kernel, Component uiParent) {
        this.memoria = memoria;
        this.dispatcher = dispatcher;
        this.kernel = kernel;
        this.uiParent = uiParent;

        operaciones = new HashMap<>();
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

        // Solo 20H (finalizar) y 10H (imprimir)
        interrupciones = new HashMap<>();
        interrupciones.put("20H", this::manejarInt20H);
        interrupciones.put("10H", this::manejarInt10H);

        pesosInstrucciones = new HashMap<>();
        pesosInstrucciones.put("00001", 1);
        pesosInstrucciones.put("00010", 1);
        pesosInstrucciones.put("00011", 1);
        pesosInstrucciones.put("00100", 1);
        pesosInstrucciones.put("00101", 1);
        pesosInstrucciones.put("00110", 1);
        pesosInstrucciones.put("00111", 1);
        pesosInstrucciones.put("01000", 1);
        pesosInstrucciones.put("01001", 1);
        pesosInstrucciones.put("01010", 1);
        pesosInstrucciones.put("01011", 1);
        pesosInstrucciones.put("01100", 1);
        pesosInstrucciones.put("01101", 1);
        pesosInstrucciones.put("01110", 1);
        pesosInstrucciones.put("01111", 1);
        pesosInstrucciones.put("10000", 1);
    }

    public BCP getProcesoActual() {
        return this.procesoActual;
    }

    public void setProcesoActual(BCP bcp) {
        this.procesoActual = bcp;
    }

    public int getDireccionIRActual() {
        return direccionIRActual;
    }

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

    private boolean esRegistroValido(String nombreRegistro) {
        if (nombreRegistro == null)
            return false;
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

    private int obtenerPesoInstruccion(String[] partes) {
        if (partes == null || partes.length == 0)
            return 1;

        String opcode = partes[0];

        // INT solo tiene 20H y 10H ahora
        if ("01111".equals(opcode) && partes.length > 1) {
            switch (partes[1].trim().toUpperCase()) {
                case "20H":
                    return 2;
                case "10H":
                    return 2;
                default:
                    return 2;
            }
        }

        return pesosInstrucciones.getOrDefault(opcode, 1);
    }

    public interface Instruccion {
        void ejecutar(String[] operandos, BCP bcp, Memoria memoria);
    }

    public interface IntHandler {
        void ejecutar(BCP bcp, Memoria memoria);
    }

    public class Load implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("LOAD requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro))
                throw new IllegalArgumentException("Registro inválido en LOAD: " + registro);
            bcp.AC = obtenerRegistros(registro, bcp);
            System.out.println("LOAD ejecutado: " + registro + " (" + bcp.AC + ") -> AC");
        }
    }

    public class Store implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("STORE requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro))
                throw new IllegalArgumentException("Registro inválido en STORE: " + registro);
            escribirRegistro(registro, bcp.AC, bcp);
            System.out.println("STORE ejecutado: AC (" + bcp.AC + ") -> " + registro);
        }
    }

    public class Move implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 3)
                throw new IllegalArgumentException("Formato MOVE incorrecto en PC: " + bcp.PC);

            String destino = operandos[1];
            if (!esRegistroValido(destino))
                throw new IllegalArgumentException("Registro inválido en MOV: " + destino);

            int valor;

            // Si el segundo operando es un registro, leemos su valor
            // Si no, lo tratamos como número literal
            if (esRegistroValido(operandos[2])) {
                valor = obtenerRegistros(operandos[2], bcp);
            } else {
                valor = parseValor(operandos[2]);
            }

            escribirRegistro(destino, valor, bcp);
            System.out.println("MOV ejecutado -> " + destino + " = " + valor);
        }
    }

    public class Sub implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("SUB requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro))
                throw new IllegalArgumentException("Registro inválido en SUB: " + registro);
            bcp.AC = bcp.AC - obtenerRegistros(registro, bcp);
            System.out.println("SUB ejecutado: AC - " + registro + " = " + bcp.AC);
        }
    }

    public class Add implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("ADD requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro))
                throw new IllegalArgumentException("Registro inválido en ADD: " + registro);
            bcp.AC = bcp.AC + obtenerRegistros(registro, bcp);
            System.out.println("ADD ejecutado: AC + " + registro + " = " + bcp.AC);
        }
    }

    public class Push implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("PUSH requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro))
                throw new IllegalArgumentException("Registro invalido en PUSH: " + registro);
            if (!bcp.push(obtenerRegistros(registro, bcp)))
                throw new IllegalArgumentException("Stack Overflow en PUSH.");
            System.out.println("PUSH ejecutado: " + registro + " -> pila");
        }
    }

    public class Pop implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("POP requiere un registro.");
            String registro = operandos[1];
            if (!esRegistroValido(registro))
                throw new IllegalArgumentException("Registro inválido en POP: " + registro);
            int valor = bcp.pop();
            if (valor == -999)
                throw new IllegalArgumentException("Stack Underflow en POP.");
            escribirRegistro(registro, valor, bcp);
            System.out.println("POP ejecutado: pila -> " + registro + " (" + valor + ")");
        }
    }

    public class Param implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("PARAM requiere al menos un valor.");
            int cantidad = operandos.length - 1;
            if (cantidad > 3)
                throw new IllegalArgumentException("PARAM acepta máximo 3 parámetros.");
            for (int i = 1; i < operandos.length; i++) {
                try {
                    if (!bcp.push(Integer.parseInt(operandos[i])))
                        throw new IllegalArgumentException("Stack Overflow en PARAM.");
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Parámetro inválido en PARAM: " + operandos[i]);
                }
            }
            System.out.println("PARAM ejecutado: " + cantidad + " valores en pila.");
        }
    }

    public class Compare implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 3)
                throw new IllegalArgumentException("CMP requiere dos registros.");
            String reg1 = operandos[1], reg2 = operandos[2];
            if (!esRegistroValido(reg1))
                throw new IllegalArgumentException("Registro inválido en CMP: " + reg1);
            if (!esRegistroValido(reg2))
                throw new IllegalArgumentException("Registro inválido en CMP: " + reg2);
            int v1 = obtenerRegistros(reg1, bcp), v2 = obtenerRegistros(reg2, bcp);
            bcp.flagIgual = (v1 == v2);
            System.out.println("CMP: " + reg1 + "=" + v1 + " vs " + reg2 + "=" + v2 + " -> " + bcp.flagIgual);
        }
    }

    public class Jump implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("JMP requiere un desplazamiento.");
            try {
                int d = Integer.parseInt(operandos[1]);
                bcp.PC = bcp.PC + d;
                saltoRealizado = true;
                System.out.println("JMP ejecutado: desplazamiento " + d + " -> PC=" + bcp.PC);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Desplazamiento inválido en JMP: " + operandos[1]);
            }
        }
    }

    public class JumpEqual implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("JE requiere un desplazamiento.");
            try {
                int d = Integer.parseInt(operandos[1]);
                if (bcp.flagIgual) {
                    bcp.PC = bcp.PC + d;
                    saltoRealizado = true;
                }
                System.out.println("JE: flagIgual=" + bcp.flagIgual + " -> PC=" + bcp.PC);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Desplazamiento inválido en JE: " + operandos[1]);
            }
        }
    }

    public class JumpNotEqual implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("JNE requiere un desplazamiento.");
            try {
                int d = Integer.parseInt(operandos[1]);
                if (!bcp.flagIgual) {
                    bcp.PC = bcp.PC + d;
                    saltoRealizado = true;
                }
                System.out.println("JNE: flagIgual=" + bcp.flagIgual + " -> PC=" + bcp.PC);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Desplazamiento inválido en JNE: " + operandos[1]);
            }
        }
    }

    public class Inc implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("Formato INC incorrecto en PC: " + bcp.PC);
            String reg = operandos[1];
            if (!esRegistroValido(reg))
                throw new IllegalArgumentException("Registro inválido en INC: " + reg);
            int valor = obtenerRegistros(reg, bcp) + 1;
            escribirRegistro(reg, valor, bcp);
            System.out.println("INC ejecutado -> " + reg + " = " + valor);
        }
    }

    public class Dec implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("Formato DEC incorrecto en PC: " + bcp.PC);
            String reg = operandos[1];
            if (!esRegistroValido(reg))
                throw new IllegalArgumentException("Registro inválido en DEC: " + reg);
            int valor = obtenerRegistros(reg, bcp) - 1;
            escribirRegistro(reg, valor, bcp);
            System.out.println("DEC ejecutado -> " + reg + " = " + valor);
        }
    }

    public class Swap implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 3)
                throw new IllegalArgumentException("Formato SWAP incorrecto en PC: " + bcp.PC);
            String reg1 = operandos[1], reg2 = operandos[2];
            if (!esRegistroValido(reg1) || !esRegistroValido(reg2))
                throw new IllegalArgumentException("Registro inválido en SWAP");
            int v1 = obtenerRegistros(reg1, bcp), v2 = obtenerRegistros(reg2, bcp);
            escribirRegistro(reg1, v2, bcp);
            escribirRegistro(reg2, v1, bcp);
            System.out.println("SWAP ejecutado -> " + reg1 + " <-> " + reg2);
        }
    }

    public class IntInstruction implements Instruccion {
        @Override
        public void ejecutar(String[] operandos, BCP bcp, Memoria memoria) {
            if (operandos.length < 2)
                throw new IllegalArgumentException("INT requiere un código de interrupción.");
            String tipo = operandos[1].trim().toUpperCase();
            IntHandler handler = interrupciones.get(tipo);
            if (handler == null)
                throw new IllegalArgumentException("Interrupción INT no soportada: " + tipo);
            handler.ejecutar(bcp, memoria);
        }
    }

    private void manejarInt20H(BCP bcp, Memoria memoria) {
        bcp.estado = "TERMINADO";
        bcp.tiempoFinal = System.currentTimeMillis();
        bcp.IR = "00000";
        kernel.finalizarProceso(procesoActual);
        System.out.println("INT 20H ejecutado: proceso finalizado.");
    }

    private void manejarInt10H(BCP bcp, Memoria memoria) {
        String mensaje = String.valueOf(bcp.DX);
        if (uiParent instanceof SimuladorGUI) {
            ((SimuladorGUI) uiParent).imprimirEnTerminal(" 10H -> " + mensaje);
        } else {
            System.out.println("INT 10H: DX = " + mensaje);
        }
    }

    public boolean ejecutarSiguientePaso() {

        if (procesoActual == null) {
            procesoActual = kernel.solicitarSiguienteProceso();
            if (procesoActual == null) {
                System.out.println("CPU: No hay procesos pendientes.");
                return false;
            }
            dispatcher.despachar(procesoActual);
        }

        if (procesoActual.estado.equals("TERMINADO") || procesoActual.estado.equals("ERROR")) {
            procesoActual = kernel.solicitarSiguienteProceso();
            if (procesoActual == null)
                return false;
            dispatcher.despachar(procesoActual);
        }

        int limiteProceso = procesoActual.getDireccionBase() + procesoActual.getAlcance();

        if (procesoActual.PC >= limiteProceso) {
            int pidTerminado = procesoActual.id;
            procesoActual.estado = "TERMINADO";
            procesoActual.tiempoFinal = System.currentTimeMillis();
            procesoActual.IR = "00000";
            if (!saltoRealizado)
                procesoActual.PC++;
            saltoRealizado = false;
            dispatcher.actualizarBcpEnKernel(procesoActual);
            kernel.finalizarProceso(procesoActual);

            BCP siguiente = kernel.solicitarSiguienteProceso();
            if (siguiente != null) {
                procesoActual = siguiente;
                dispatcher.despachar(procesoActual);
                System.out.println("CPU: Finalizado PID " + pidTerminado + ". Iniciando PID " + procesoActual.id);
                return true;
            } else {
                procesoActual = null;
                System.out.println("CPU: Finalizado PID " + pidTerminado + ". Sistema en IDLE.");
                return false;
            }
        }

        direccionIRActual = procesoActual.PC;
        String instruccionCompleta = memoria.leerCelda(procesoActual.PC);
        procesoActual.IR = instruccionCompleta;

        if (instruccionCompleta == null || instruccionCompleta.equals("00000")) {
            int pidTerminado = procesoActual.id;
            procesoActual.estado = "TERMINADO";
            procesoActual.tiempoFinal = System.currentTimeMillis();
            dispatcher.actualizarBcpEnKernel(procesoActual);
            kernel.finalizarProceso(procesoActual);

            BCP siguiente = kernel.solicitarSiguienteProceso();
            if (siguiente != null) {
                procesoActual = siguiente;
                dispatcher.despachar(procesoActual);
                System.out.println("CPU: Finalizado PID " + pidTerminado + ". Iniciando PID " + procesoActual.id);
                return true;
            } else {
                procesoActual = null;
                System.out.println("CPU: Finalizado PID " + pidTerminado + ". Sistema en IDLE.");
                return false;
            }
        }

        String[] partes = instruccionCompleta.split("\\s+");
        String opcode = partes[0];

        Instruccion instr = operaciones.get(opcode);
        if (instr == null)
            return marcarErrorProceso("Opcode inválido -> " + opcode);

        try {
            instr.ejecutar(partes, procesoActual, memoria);
            int peso = obtenerPesoInstruccion(partes);
            procesoActual.ciclosConsumidos += peso;
            System.out.println("CPU: instrucción " + opcode + " peso=" + peso
                    + " total=" + procesoActual.ciclosConsumidos);
        } catch (IllegalArgumentException | ArithmeticException e) {
            return marcarErrorProceso(e.getMessage());
        } catch (Exception e) {
            return marcarErrorProceso("Error inesperado: " + e.getMessage());
        }

        procesoActual.PC++;
        dispatcher.actualizarBcpEnKernel(procesoActual);
        return true;
    }

    private boolean marcarErrorProceso(String mensaje) {
        procesoActual.estado = "ERROR";
        procesoActual.tiempoFinal = System.currentTimeMillis();
        dispatcher.actualizarBcpEnKernel(procesoActual);
        kernel.finalizarProceso(procesoActual);
        procesoActual = null;
        Errors.logError(mensaje);
        if (uiParent != null)
            Errors.mostrarErrorVisual(uiParent, "Error de ejecución", mensaje);
        return false;
    }

    private int parseValor(String texto) {
        texto = texto.trim().toUpperCase();
        try {
            return texto.endsWith("H")
                    ? Integer.parseInt(texto.replace("H", ""), 16)
                    : Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido: " + texto);
        }
    }
}
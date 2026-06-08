/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.proceso;

/**
 *
 * @author joses
 */
public class BCP {
    
    public int cpuAsignada = -1;       // ID de la CPU que lo ejecuta (-1 si está libre)
    public int tiquetesLoteria = 0;    // Para el algoritmo de Lottery
    
    public int rafagaTotal = 0;        // Tamaño total en instrucciones
    public int rafagaRestante = 0;     // Cuántas instrucciones le quedan
    public int rafagaEjecutada = 0;    // Cuántas instrucciones lleva en la ráfaga actual

    // Identificador y estado
    public String nombreProceso;
    public int id; // Id para cada proceso
    // Estado del proceso 
    public String estado; // "EJECUTANDO", "TERMINADO", "ERROR")
   
    // Registros de Control 
    public int PC;      // Program Counter (Puntero a la instruccion actual)
    public String IR;   // Instruction Register (Guarda el texto de la instrucciion) eje: 011 BX 10

    // Registros de Datos (Los que guardan valores)
    public int AC; // Acumulador (Resultado de ADD, SUB, etc.)
    public int AX; // Registro general A
    public int BX; // Registro general B
    public int CX; // Registro general C
    public int DX; // Registro general D
    // REGISTROS ESPECIALES PARA INT
    public int AH;
    public int AL;
    // Gestion de memoria
    public int direccionBase; // Donde estara el inicio de la RAM fisica
    public int alcance; // Cuanto abarca para no salir del rango

    //  Atributo para la tabla de páginas del proceso en modo paginado
    private tarea1.joseandres.memoria.TablaPaginas tablaPaginas;
    public boolean esPaginado = false;

    // Pila
    public int[] pila = new int[5];
    public int SP = -1; // iniciamos en vacio stackPointer

    // Estadistica
    public long tiempoLlegada;
    public long tiempoInicio;
    public long tiempoFinal;
    public int ciclosConsumidos; // Pesos.
    public boolean flagIgual; // Bandera pensada para hacer las comparaciones (CMP, JE y JNE).

    // Bloque Control de Proceso -> BCP
    public BCP(int id, String nombre, int direccionBase, int alcance) {
        this.id = id;
        this.nombreProceso = nombre;
        this.direccionBase = direccionBase;
        this.alcance = alcance;
        this.flagIgual = false;
        this.tiempoLlegada = System.currentTimeMillis();
        this.ciclosConsumidos = 0;
        
        this.estado = "NUEVO"; // Estado correcto mientras está en disco
     
         
        // Si base es -1 (Paginado), el PC virtual inicia en 0. Si no, toma la dirección física base.
        this.PC = (direccionBase == -1) ? 0 : direccionBase;
        
        this.rafagaTotal = alcance;     // El tamaño del archivo cargado determina sus instrucciones totales
        this.rafagaRestante = alcance;  // Al inicio le queda todo por ejecutar
        this.rafagaEjecutada = 0;       
        this.cpuAsignada = -1;          // Libre
        this.tiquetesLoteria = 0;       

        // Inicialización de Registros
        this.AC = 0;
        this.AX = 0;
        this.BX = 0;
        this.CX = 0;
        this.DX = 0;
        this.AH = 0;
        this.AL = 0;
        this.IR = "00000"; 

        this.SP = -1;
    }

    // Metodo Push
    public boolean push(int valor) {
        if (SP < 4) {
            pila[++SP] = valor;
            return true;
        }
        return false; // Stack Overflow
    }

    // Metodo Pop
    public int pop() {
        if (SP >= 0) {
            return pila[SP--];
        }
        return -999; 
    }
    
    // GETTERS Y SETTERS
    public int getDireccionBase() {
        return direccionBase;
    }

    public void setDireccionBase(int direccionBase) {
        this.direccionBase = direccionBase;
        // Solo alteramos el PC si no es un esquema virtual/paginado
        if (direccionBase != -1) {
            this.PC = direccionBase;
        }
    }

    public int getAlcance() {
        return alcance;
    }


    public tarea1.joseandres.memoria.TablaPaginas getTablaPaginas() {
        return this.tablaPaginas;
    }

    public void setTablaPaginas(tarea1.joseandres.memoria.TablaPaginas tabla) {
        this.tablaPaginas = tabla;
    }
    public EstadisticasProceso construirDatosEstadísticos(long momentoInicial) {
       
        long tLlegada = (tiempoLlegada - momentoInicial) / 1000;
        long tInicio  = (tiempoInicio  - momentoInicial) / 1000;
        long tFinal   = (tiempoFinal   - momentoInicial) / 1000;

        long   tr    = tInicio - tLlegada;
        long   ts    = tFinal  - tLlegada;
        double ratio = (ts > 0) ? (double) tr / ts : 0.0;

        StringBuilder stackView = new StringBuilder("[");
        for (int i = 0; i < pila.length; i++) {
            stackView.append(pila[i]);
            if (i < pila.length - 1) stackView.append(", ");
        }
        stackView.append("]");

        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println(String.format("PROCESO: %-15s | ID: %d | ESTADO: %s", nombreProceso, id, estado));
        System.out.println(String.format("REGISTROS -> PC: %03d | IR: %-10s | AC: %d", PC, IR, AC));
        System.out.println(String.format("GENERALES -> AX: %d | BX: %d | CX: %d | DX: %d", AX, BX, CX, DX));
        System.out.println(String.format("STACK (SP: %d) -> %s", SP, stackView.toString()));
        System.out.println(String.format("MEMORIA   -> Base: %d | Alcance: %d", direccionBase, alcance));
        System.out.println(String.format("TIEMPOS   -> Llegada: %ds | Inicio: %ds | Final: %ds", tLlegada, tInicio, tFinal));
        System.out.println(String.format("MÉTRICAS  -> Tr: %ds | Ts: %ds | Tr/Ts: %.3f", tr, ts, ratio));
        System.out.println("---------------------------------------------------------------------------------------");

        return new EstadisticasProceso(
                nombreProceso, id, estado,
                PC, IR, AC,
                AX, BX, CX, DX,
                SP, stackView.toString(),
                direccionBase, alcance,
                tLlegada, tInicio, tFinal,
                tr, ts, ratio
        );
    }

    public void mostrarRegistros() {
        StringBuilder stackView = new StringBuilder("[");
        for (int i = 0; i < pila.length; i++) {
            stackView.append(pila[i]);
            if (i < pila.length - 1) {
                stackView.append(", ");
            }
        }
        stackView.append("]");

        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println(String.format("PROCESO: %-15s | ID: %d | ESTADO: %s", nombreProceso, id, estado));
        System.out.println(String.format("REGISTROS -> PC: %03d | IR: %-10s | AC: %d", PC, IR, AC));
        System.out.println(String.format("GENERALES -> AX: %d | BX: %d | CX: %d | DX: %d", AX, BX, CX, DX));
        System.out.println(String.format("STACK (SP: %d) -> %s", SP, stackView.toString()));
        System.out.println(String.format("MEMORIA   -> Base: %d | Alcance: %d", direccionBase, alcance));
        System.out.println(String.format("TIEMPOS   -> INICIO: %d | FINAL: %d", this.tiempoInicio, this.tiempoFinal));
        System.out.println("---------------------------------------------------------------------------------------");
    }
}
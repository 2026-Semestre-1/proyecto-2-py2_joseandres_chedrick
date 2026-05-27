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

    // Identificador y estado
    public String nombreProceso;
    public int id; // Id para cada proceso
    // Estado del proceso 
    public String estado; // "EJECUTANDO", "TERMINADO", "ERROR")
   
    
    // Registros de Control 
    public int PC;      // Prog ram Counter (Puntero a la instruccion actual)
    public String IR;   // Instruction Register (Guarda el texto de la instrucciion) eje: 011 BX 10

    // Registros de Datos (Los que guardan valores
    public int AC; // Acumulador (Resultado de ADD, SUB, etc.)
    public int AX; // Registro general A
    public int BX; // Registro general B
    public int CX; // Registro general C
    public int DX; // Registro general D
    //REGISTROS ESPECIALES PARA INT
    public int AH;
    public int AL;
    //Gestion de memoria
    public int direccionBase; //Donde estara el inicio de la RAM fisica
    public int alcance; // Cuanto abarca para no salir del rango

    //Pila
    public int[] pila = new int[5];
    public int SP = -1; // iniciamos en vacio stackPointer

    //Estadistica
    public long tiempoLlegada;
    public long tiempoInicio;
    public long tiempoFinal;
    public int ciclosConsumidos;//Pesos.
    public boolean flagIgual;//Bandera pensada para hacer ldas comparaciones(CMP, JE y JNE).

    //Bloque Control de Proceso -> BCP
    public BCP(int id, String nombre, int direccionBase, int alcance) {
        this.id = id;
        this.nombreProceso = nombre;
        this.direccionBase = direccionBase;
        this.alcance = alcance;
        this.flagIgual = false;
        // Capturamos el tiempo de creacio para las stats
        this.tiempoLlegada = System.currentTimeMillis();
        this.ciclosConsumidos = 0;
        
        this.estado = "NUEVO"; // Estado correcto mientras está en disco
     
        // PC 
        this.PC = direccionBase;

        // Inicialización de Registros
        this.AC = 0;
        this.AX = 0;
        this.BX = 0;
        this.CX = 0;
        this.DX = 0;
        this.AH = 0;
        this.AL = 0;
        this.IR = "00000"; // Ahora usamos 5 bits 

        // La pila inicia vacía 
        this.SP = -1;

      
        
        // Capturamos el tiempo de creación para estadísticas
        this.tiempoLlegada = System.currentTimeMillis();
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
    
    
    //(GETTERS Y SETTERS) 

    public int getDireccionBase() {
        return direccionBase;
    }

    public void setDireccionBase(int direccionBase) {
        this.direccionBase = direccionBase;
        this.PC = direccionBase;
    }

    public int getAlcance() {
        return alcance;
    }

    public void mostrarRegistros() {
        // 1. Construimos una representación visual de la pila (ej: [5, 10, 0, 0, 0])
        StringBuilder stackView = new StringBuilder("[");
        for (int i = 0; i < pila.length; i++) {
            stackView.append(pila[i]);
            if (i < pila.length - 1) {
                stackView.append(", ");
            }
        }
        stackView.append("]");

        // 2. Imprimimos toda la información relevante del proceso
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println(String.format("PROCESO: %-15s | ID: %d | ESTADO: %s", nombreProceso, id, estado));
        System.out.println(String.format("REGISTROS -> PC: %03d | IR: %-10s | AC: %d", PC, IR, AC));
        System.out.println(String.format("GENERALES -> AX: %d | BX: %d | CX: %d | DX: %d", AX, BX, CX, DX));
        System.out.println(String.format("STACK (SP: %d) -> %s", SP, stackView.toString()));
        System.out.println(String.format("MEMORIA   -> Base: %d | Alcance: %d", direccionBase, alcance));
        System.out.println("---------------------------------------------------------------------------------------");
    }

}

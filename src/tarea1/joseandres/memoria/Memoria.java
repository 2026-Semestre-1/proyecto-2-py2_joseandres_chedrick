package tarea1.joseandres.memoria;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author joses
 * Dentro de la clase memoria debemos tener los siguientes cuidados:
 * Ahora la memoria viene dada por config json
 * (0-19)-> Sistema Operativo.
 * (20-99) -> Usuario.
 * Ademas debemos validar que no se hagan escrituras fuera de rango
 */
public class Memoria {
    private String[] memoria;
    private int tamanoTotal;
    private int inicioUsuario;

    // Constructor que acepta  el tamaño segun el config.json
    public Memoria(int tamano, int porcentajeKernel) {
        this.tamanoTotal = tamano;
        //Y ahora el % viene dado por cnfig.json
        this.inicioUsuario = (int) (tamano * (porcentajeKernel / 100.0));
        
        memoria = new String[tamanoTotal];
        
        // Inicializamos con ceros para evitar NullPointer
        for(int i = 0; i < tamanoTotal; i++){
            memoria[i] = "00000";
        }
    }
    
   
    // --- MÉTODOS DE GESTIÓN (Para el Kernel/Orquestador) AYuda con chat---
    
    /**
     * Busca un bloque de celdas contiguas vacías en la zona de usuario.
     * Esto permite que la arquitectura soporte FCFS, SJF, etc., al permitir 
     * cargar procesos en diferentes "huecos".
     */
    public int buscarEspacioDisponible(int tamanoRequerido) {
        int celdasSeguidas = 0;
        for (int i = inicioUsuario; i < tamanoTotal; i++) {
            if (memoria[i].equals("00000")) {
                celdasSeguidas++;
                if (celdasSeguidas == tamanoRequerido) {
                    return (i - tamanoRequerido + 1); // Retorna la Dirección Base
                }
            } else {
                celdasSeguidas = 0;
            }
        }
        return -1; // No hay espacio suficiente para este proceso
    }
    
    //Metoodo para proteccion de memoria
    /**
     * Valida que el proceso solo escriba dentro de su "Alcance".
     * Se usa la Dirección Base y el Alcance guardados en el BCP.
     */
    public boolean escribirSeguro(int direccionFisica, String dato, int base, int alcance) {
        // Calculo: Base <= Dirección < (Base + Alcance)
        if (direccionFisica >= base && direccionFisica < (base + alcance)) {
            if (direccionFisica >= inicioUsuario && direccionFisica < tamanoTotal) {
                memoria[direccionFisica] = dato;
                return true;
            }
        }
        // Aquí podrías disparar una interrupción de error de memoria en el futuro
        System.err.println("Error: Violación de acceso a memoria en posición " + direccionFisica);
        return false;
    }
    
    //Escribimos en la zona del kernel
    public void escribirEnKernel(int direccion, String dato) {
        if (direccion >= 0 && direccion < inicioUsuario) {
            memoria[direccion] = dato;
        }
    }
    
    public String leerCelda(int direccion) {
        if (direccion >= 0 && direccion < tamanoTotal) {
            return memoria[direccion];
        }
        return "00000";
    }
    
    
    /**
     * Lectura con protección (opcional, para la CPU).
     * Valida que un proceso no intente leer código de otro proceso o del Kernel.
     */
    public String leerSeguro(int direccionFisica, int base, int alcance) {
        if (direccionFisica >= base && direccionFisica < (base + alcance)) {
            return memoria[direccionFisica];
        }
        System.err.println("Error: Violación de lectura en posición " + direccionFisica);
        return "00000";
    }
    
    // Método para que el Dispatcher y la GUI sepan dónde termina el Kernel
    public int getInicioUsuario() {
        return this.inicioUsuario;
    }
    
    // Para que la GUI sepa cuántas filas dibujar
    public int getTamanoTotal() {
        return this.tamanoTotal;
    }
    
    public void liberarCelda(int direccion) {
    if (direccion >= inicioUsuario && direccion < tamanoTotal) {
        memoria[direccion] = "00000";
    }
}
    
    
   

}
    
    
    
    
    
 
    


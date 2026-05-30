/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.administrador;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author joses
 * Los procesos que esten listos para trabajar los agregamos a la readyQueque
 */
public class Scheduler {
    private List<BCP> readyQueue;
    private EstrategiaPlanificacion estrategia;
    
    public Scheduler() {
        this.readyQueue = new ArrayList<>();
    }
    // Aquí inyectas la estrategia (FCFS, SJF, etc.)
    public void setEstrategia(EstrategiaPlanificacion estrategia) {
        this.estrategia = estrategia;
    }

    public void agregarProceso(BCP proceso) {
        proceso.estado = "PREPARADO";
        this.readyQueue.add(proceso);
    }

    public BCP obtenerSiguiente() {
        if (estrategia == null) {
            System.err.println("Error: No se ha configurado una estrategia de planificación.");
            return null;
        }
        //Usamos FCFS
        BCP seleccionado = estrategia.seleccionarSiguiente(readyQueue);
        
        // Si se seleccionó uno, lo sacamos de la cola de listos
        if (seleccionado != null) {
            readyQueue.remove(seleccionado);
        }
        return seleccionado;
    }

    public List<BCP> getReadyQueue() {
        return readyQueue;
    }
}
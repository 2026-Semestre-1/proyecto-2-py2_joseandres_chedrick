/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.administrador;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;
import java.util.ArrayList;
import java.util.List;
import tarea1.joseandres.estrategia.AlgoritmoPlanificador;
/**
 *
 * @author joses
 * Los procesos que esten listos para trabajar los agregamos a la readyQueque
 */
public class Scheduler {
    private List<BCP> readyQueue;
    private EstrategiaPlanificacion estrategia;
    private AlgoritmoPlanificador algoritmo;
    
    public Scheduler() {
        this.readyQueue = new ArrayList<>();
    }
    
    public void setEstrategia(EstrategiaPlanificacion estrategia, AlgoritmoPlanificador algoritmo) {
        this.estrategia = estrategia;
        this.algoritmo = algoritmo;
    }

    public synchronized void agregarProceso(BCP proceso) {
        proceso.estado = "PREPARADO";
        this.readyQueue.add(proceso);
    }

    public synchronized BCP obtenerSiguiente() {
        if (estrategia == null) {
            System.err.println("Error: No se ha configurado una estrategia de planificación.");
            return null;
        }

        BCP seleccionado = estrategia.seleccionarSiguiente(readyQueue);

        if (seleccionado != null) {
            readyQueue.remove(seleccionado);
        }
        return seleccionado;
    }

    public List<BCP> getReadyQueue() {
        return readyQueue;
    }

    public AlgoritmoPlanificador getEstrategia() {
        return this.algoritmo;
    }
    
}
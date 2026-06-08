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
 * @author joses Los procesos que esten listos para trabajar los agregamos a la
 * readyQueque
 */
public class Scheduler {

    //private List<BCP> readyQueue;
    private List<List<BCP>> colasListosPorCpu;
    private EstrategiaPlanificacion estrategia;
    private AlgoritmoPlanificador algoritmo;
    private int cantidadCpus;

    public Scheduler(int cantidadCpus) {
        this.colasListosPorCpu = new ArrayList<>();
        inicializarColas(cantidadCpus);

    }

    //Inicializa las colas
    public void inicializarColas(int cantidadCpus) {
        this.cantidadCpus = cantidadCpus;

        if (this.colasListosPorCpu == null) {
            this.colasListosPorCpu = new ArrayList<>();
        } else {
            this.colasListosPorCpu.clear(); // Ahora sí es seguro limpiar porque sabemos que no es null
        }

        for (int i = 0; i < cantidadCpus; i++) {
            this.colasListosPorCpu.add(new ArrayList<BCP>());
        }
        System.out.println("SCHEDULER: Inicializadas " + cantidadCpus + " colas independientes de ejecución.");
    }

    public void setEstrategia(EstrategiaPlanificacion estrategia, AlgoritmoPlanificador algoritmo) {
        this.estrategia = estrategia;
        this.algoritmo = algoritmo;
    }

    public synchronized void agregarProceso(BCP proceso, int cpuId) {
        if (cpuId < 0 || cpuId >= colasListosPorCpu.size()) {
            cpuId = 0;
        }
        proceso.estado = "PREPARADO";
        proceso.cpuAsignada = cpuId;
        this.colasListosPorCpu.get(cpuId).add(proceso);
    }

    public synchronized BCP obtenerSiguiente(int cpuId) {
        if (estrategia == null) {
            System.err.println("SCHEDULER Error: No se ha configurado una estrategia de planificación.");
            return null;
        }
        if (cpuId < 0 || cpuId >= colasListosPorCpu.size()) {
            return null;
        }

        List<BCP> subCola = this.colasListosPorCpu.get(cpuId);
        if (subCola.isEmpty()) {
            return null; // No hay procesos listos para esta CPU en específico
        }

        BCP seleccionado = estrategia.seleccionarSiguiente(subCola);

        if (seleccionado != null) {
            subCola.remove(seleccionado);
            seleccionado.estado = "EJECUCION (CPU " + cpuId + ")";
        }

        return seleccionado;
    }

    /**
     * Retorna el ID de la CPU que tiene menos procesos listos acumulados
     * (Balanceo Least Loaded).
     */
    public int obtenerCpuMenorCarga() {
        int mejorCpu = 0;
        int menorCarga = Integer.MAX_VALUE;
        for (int i = 0; i < cantidadCpus; i++) {
            int cargaActual = colasListosPorCpu.get(i).size();
            if (cargaActual < menorCarga) {
                menorCarga = cargaActual;
                mejorCpu = i;
            }
        }
        return mejorCpu;
    }

    /**
     * Retorna cuántos procesos tiene cargados una CPU específica en su lista de
     * listos.
     */
    public int getCargaCpu(int cpuId) {
        if (cpuId < 0 || cpuId >= colasListosPorCpu.size()) {
            return 0;
        }
        return colasListosPorCpu.get(cpuId).size();
    }

    public List<BCP> getReadyQueue() {
        return colasListosPorCpu.isEmpty() ? new ArrayList<>() : colasListosPorCpu.get(0);
    }

    public List<List<BCP>> getColasListosPorCpu() {
        return this.colasListosPorCpu;
    }

    public AlgoritmoPlanificador getEstrategia() {
        return this.algoritmo;
    }

    public void setCantidadCpus(int cantidadCpus) {
        this.cantidadCpus = cantidadCpus;
        inicializarColas(cantidadCpus); 
    }
     
}

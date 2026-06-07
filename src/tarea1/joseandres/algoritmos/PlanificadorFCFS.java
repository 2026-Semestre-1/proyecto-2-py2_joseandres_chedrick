/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import java.util.List;
import java.util.Comparator;
import tarea1.joseandres.estrategia.AlgoritmoPlanificador;
/**
 *
 * @author joses
 * Algoritmo 1 -> El FCFS primer algoritmo y el unico a utilizarse para este py
 * Elegimos la que lleve mas tiempo esperando solo con los tiemposLLegada
 */
public class PlanificadorFCFS extends AlgoritmoPlanificador implements EstrategiaPlanificacion {
 
    public PlanificadorFCFS() {
        this.esApropiativo = false;
        this.quantum = 0; // no usa quantum
    }
 
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        if (listos == null || listos.isEmpty()) return null;
        // El primero en llegar es el primero en ser atendido
        return listos.get(0);
    }
}
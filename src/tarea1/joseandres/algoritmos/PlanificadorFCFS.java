/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import java.util.List;
import java.util.Comparator;
/**
 *
 * @author joses
 * Algoritmo 1 -> El FCFS primer algoritmo y el unico a utilizarse para este py
 * Elegimos la que lleve mas tiempo esperando solo con los tiemposLLegada
 */
public class PlanificadorFCFS implements EstrategiaPlanificacion {
    public BCP seleccionarSiguiente(List<BCP> listos) {
        if (listos == null || listos.isEmpty()) return null;

        // Ordenamos por tiempo de llegada por si entraron varios a la vez
        return listos.stream()
                     .min(Comparator.comparingLong(p -> p.tiempoLlegada))
                     .orElse(null);
    }
}

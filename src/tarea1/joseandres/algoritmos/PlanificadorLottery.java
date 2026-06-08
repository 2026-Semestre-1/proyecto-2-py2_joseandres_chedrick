/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;

import java.util.List;
import java.util.Random;
import tarea1.joseandres.estrategia.AlgoritmoPlanificador;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;

/**
 *
 * @author chedr
 */
public class PlanificadorLottery extends AlgoritmoPlanificador implements EstrategiaPlanificacion {
 
    private static final int TIQUETES_DEFAULT = 10; // si un proceso tiene 0
    private final Random rng = new Random();
 
    public PlanificadorLottery(int quantum) {
        this.esApropiativo = true;
        this.quantum = quantum;
    }
 
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        if (listos == null || listos.isEmpty()) return null;
 
        // Calcular total de tiquetes
        int totalTiquetes = 0;
        for (BCP bcp : listos) {
            int t = bcp.tiquetesLoteria > 0 ? bcp.tiquetesLoteria : TIQUETES_DEFAULT;
            totalTiquetes += t;
        }
 
        // Sortear un tiquete ganador
        int ganador = rng.nextInt(totalTiquetes);
 
        // Recorrer la lista hasta encontrar al dueño del tiquete ganador
        int acumulado = 0;
        BCP seleccionado = listos.get(listos.size() - 1); // fallback al último
        for (BCP bcp : listos) {
            int t = bcp.tiquetesLoteria > 0 ? bcp.tiquetesLoteria : TIQUETES_DEFAULT;
            acumulado += t;
            if (ganador < acumulado) {
                seleccionado = bcp;
                break;
            }
        }
 
        if (seleccionado.rafagaRestante <= 0) {
            listos.remove(seleccionado);
            return seleccionarSiguiente(listos);
        }
 
        seleccionado.ciclosConsumidos += this.quantum;
 
        // Rotar al final
        listos.remove(seleccionado);
        listos.add(seleccionado);
 
        return seleccionado;
    }
}

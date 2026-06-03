/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;

import java.util.Comparator;
import java.util.List;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;

/**
 *
 * @author chedr
 */
public class PlanificadorSJF implements EstrategiaPlanificacion {
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        return listos.stream()
                     .min(Comparator.comparingLong(p -> p.alcance))
                     .orElse(null);
    }
    
}

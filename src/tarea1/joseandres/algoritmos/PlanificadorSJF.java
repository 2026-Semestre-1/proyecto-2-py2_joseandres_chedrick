/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;

import java.util.Comparator;
import java.util.List;
import tarea1.joseandres.estrategia.AlgoritmoPlanificador;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;
//pr
/**
 *
 * @author chedr
 */
public class PlanificadorSJF extends AlgoritmoPlanificador implements EstrategiaPlanificacion {
 
    public PlanificadorSJF() {
        this.esApropiativo = false;
        this.quantum = 0;
    }
 
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        if (listos == null || listos.isEmpty()) return null;
 
        BCP menor = listos.get(0);
        for (BCP bcp : listos) {
            if (bcp.rafagaTotal < menor.rafagaTotal) {
                menor = bcp;
            }
        }
        return menor;
    }
}
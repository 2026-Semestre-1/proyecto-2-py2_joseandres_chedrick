/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;
//<>
import java.util.List;
import tarea1.joseandres.estrategia.AlgoritmoPlanificador;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;
//hr
/**
 *
 * @author chedr
 */
public class PlanificadorHRRN extends AlgoritmoPlanificador implements EstrategiaPlanificacion {
 
    public PlanificadorHRRN() {
        this.esApropiativo = false;
        this.quantum = 0;
    }
 
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        if (listos == null || listos.isEmpty()) return null;
 
        long ahora = System.currentTimeMillis();
        BCP mejor = null;
        double mejorRatio = -1;
 
        for (BCP bcp : listos) {
            if (bcp.rafagaTotal <= 0) continue;
 
            double espera = (double)(ahora - bcp.tiempoLlegada) / 1000.0;
            double ratio  = (espera + bcp.rafagaTotal) / (double) bcp.rafagaTotal;
 
            if (ratio > mejorRatio) {
                mejorRatio = ratio;
                mejor = bcp;
            }
        }
        return mejor;
    }
}

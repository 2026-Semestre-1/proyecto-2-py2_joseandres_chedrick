/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;

import java.util.ArrayList;
import java.util.List;
import tarea1.joseandres.estrategia.AlgoritmoPlanificador;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;

/**
 *
 * @author chedr
 */
public class PlanificadorSRR extends AlgoritmoPlanificador implements EstrategiaPlanificacion {
 
    // Un proceso pasa a "aceptado" cuando su tiempoEspera (en ciclos)
    // supera este umbral. Ajustable.
    private static final int UMBRAL_AGING = 5;
 
    public PlanificadorSRR(int quantum) {
        this.esApropiativo = true;
        this.quantum = quantum;
    }
 
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        if (listos == null || listos.isEmpty()) return null;
 
        // Separar en dos grupos usando ciclosConsumidos como indicador de "edad"
        List<BCP> aceptados = new ArrayList<>();
        List<BCP> nuevos    = new ArrayList<>();
 
        for (BCP bcp : listos) {
            if (bcp.ciclosConsumidos >= UMBRAL_AGING) {
                aceptados.add(bcp);
            } else {
                nuevos.add(bcp);
            }
        }
 
        // Prioridad: aceptados primero; si no hay, atender nuevos
        List<BCP> grupo = aceptados.isEmpty() ? nuevos : aceptados;
 
        BCP candidato = grupo.get(0);
        if (candidato.rafagaRestante <= 0) {
            listos.remove(candidato);
            return seleccionarSiguiente(listos); // recursión para saltar terminados
        }
 
        candidato.ciclosConsumidos += this.quantum;
 
        // Rotar dentro de la lista global manteniendo el orden relativo
        listos.remove(candidato);
        listos.add(candidato);
 
        return candidato;
    }
}

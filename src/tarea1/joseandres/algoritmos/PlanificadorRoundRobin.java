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
//<>
/**
 *
 * @author chedr
 */
public class PlanificadorRoundRobin extends AlgoritmoPlanificador implements EstrategiaPlanificacion {
 
    public PlanificadorRoundRobin(int quantum) {
        this.esApropiativo = true;
        this.quantum = quantum;
    }
 
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        if (listos == null || listos.isEmpty()) return null;
 
        // Buscar el primero que todavía tenga instrucciones
        while (!listos.isEmpty()) {
            BCP candidato = listos.get(0);
            if (candidato.rafagaRestante > 0) {
                candidato.ciclosConsumidos += this.quantum;
                reestructurarLista(listos); // va al final de la cola
                return candidato;
            } else {
                listos.remove(0); // ya terminó, sacarlo
            }
        }
        return null;
    }
 
    private void reestructurarLista(List<BCP> listos) {
        if (listos == null || listos.size() <= 1) return;
        BCP primero = listos.remove(0);
        listos.add(primero);
    }
}

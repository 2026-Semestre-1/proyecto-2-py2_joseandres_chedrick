/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;

import java.util.List;
import tarea1.joseandres.estrategia.AlgoritmoPlanificador;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;

/**
 *
 * @author chedr
 */
public class PlanificadorSRT extends AlgoritmoPlanificador implements EstrategiaPlanificacion {
 
    public PlanificadorSRT() {
        this.esApropiativo = true;
        this.quantum = 1;
    }
 
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        if (listos == null || listos.isEmpty()) return null;
 
        // Eliminar procesos ya terminados
        listos.removeIf(bcp -> bcp.rafagaRestante <= 0);
 
        if (listos.isEmpty()) return null;
 
        // Ordenar de menor a mayor rafagaRestante → el más corto queda en [0]
        reestructurarLista(listos);
 
        return listos.get(0);
    }
 
    private void reestructurarLista(List<BCP> listos) {
        listos.sort((a, b) -> Integer.compare(a.rafagaRestante, b.rafagaRestante));
    }
}
 

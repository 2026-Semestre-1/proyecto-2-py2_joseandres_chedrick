/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;
//<>
import java.util.List;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;

/**
 *
 * @author chedr
 */
public class PlanificadorHRRN implements EstrategiaPlanificacion {
    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        int rr = -200;
        BCP procesoSiguiente = null;
        for(BCP proceso : listos){
            long tiempoEspera = (System.currentTimeMillis() - proceso.tiempoLlegada);
            int rrActual = (int) ((tiempoEspera + proceso.alcance) / proceso.alcance);
            if (rr < rrActual){
                rr = rrActual;
                procesoSiguiente = proceso;
            }
        }
        return procesoSiguiente;
    }
}

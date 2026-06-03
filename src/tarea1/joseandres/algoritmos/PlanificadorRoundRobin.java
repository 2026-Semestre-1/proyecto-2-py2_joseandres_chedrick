/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.algoritmos;
//<>
import java.util.Comparator;
import java.util.List;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.proceso.BCP;

/**
 *
 * @author chedr
 */
public class PlanificadorRoundRobin implements EstrategiaPlanificacion {
    int quantum;

    @Override
    public BCP seleccionarSiguiente(List<BCP> listos) {
        BCP elemento = null;
        try{
            if (listos.get(0).ciclosConsumidos < listos.get(0).alcance){
                listos.get(0).ciclosConsumidos = listos.get(0).ciclosConsumidos + this.quantum;
                elemento = listos.get(0);
                this.reestructurarLista(listos);
            }else{
                listos.remove(0);
                this.seleccionarSiguiente(listos);
            }
        }catch(NullPointerException e){
            System.out.println("No hay elementos que obtener");
        }finally{
            return elemento;
        }
        
    }
    private List<BCP> reestructurarLista(List<BCP> listos) {
        if (listos == null || listos.size() <= 1) {
            return listos;
        }

        BCP primerElemento = listos.get(0);
        listos.remove(0);
        listos.add(primerElemento);

        return listos;
    }
}

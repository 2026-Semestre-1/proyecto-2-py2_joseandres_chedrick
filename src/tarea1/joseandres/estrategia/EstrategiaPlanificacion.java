/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.estrategia;
import java.util.List;
import tarea1.joseandres.proceso.BCP;


/**
 *
 * @author joses
 * Usamos el patron strategy para implementar los algoritmos a futuro.
 * Todos son capaces de recibir una lista d eprocesos y devolver uno.
 */
public interface EstrategiaPlanificacion {
    BCP seleccionarSiguiente(List<BCP> listos);
}

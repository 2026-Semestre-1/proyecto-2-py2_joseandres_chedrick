/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.memoria;

import java.util.HashMap;
import java.util.Map;
import tarea1.joseandres.proceso.BCP;

/**
 * Diccionario de tradu de Direcciones Virtuales a Marcos de RAM Reales.
 * @author Jose Andrés Solano Vargas
 */
public class TablaPaginas {
    
    // Mapea: Número de Pag Virtual -> Numero de Marco Fisico en RAM
    private final Map<Integer, Integer> tabla;
    private final int tamanoPagina;

    public TablaPaginas(int tamanoPagina) {
        this.tabla = new HashMap<>();
        this.tamanoPagina = tamanoPagina;
    }

    /**
     * Registra en que marco fisico se guardo una pagina del proceso.
     */
    public void registrarPagina(int paginaVirtual, int marcoFisico) {
        tabla.put(paginaVirtual, marcoFisico);
    }

    /**
     * Traduce una dirección virtual (el PC del proceso) a la celda física real de la RAM.
     */
    public int traducirDireccion(int direccionVirtual) {
        int numeroPagina = direccionVirtual / tamanoPagina;
        int desplazamiento = direccionVirtual % tamanoPagina;

        if (!tabla.containsKey(numeroPagina)) {
            // Si la CPU pide algo que no está mapeado, es un fallo de página o violación de segmento
            return -1; 
        }

        int marcoFisico = tabla.get(numeroPagina);
        
        // Fórmula matemática de paginación pura: (Marco * Tam) + Offset
        return (marcoFisico * tamanoPagina) + desplazamiento;
    }
    
    
    public int traducirDireccionVirtual(BCP proceso, int direccionVirtual) {
        if (proceso == null) {
            return -1;
        }

        // Recuperamos la TablaPaginas específica que tiene asignada este BCP
        TablaPaginas tablaProceso = proceso.getTablaPaginas(); 
        
        if (tablaProceso == null) {
            return -1; // El proceso no está inicializado en modo paginado
        }

        // Delegamos la traducción matemática al método que acabas de crear
        return tablaProceso.traducirDireccion(direccionVirtual);
    }


    public Map<Integer, Integer> getTabla() {
        return tabla;
    }

    public void limpiar() {
        this.tabla.clear();
    }
}

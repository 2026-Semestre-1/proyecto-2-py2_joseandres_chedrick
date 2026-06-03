/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.memoria;

import java.util.ArrayList;
import java.util.List;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.uitls.Errors;

/**
 * Gestor de Memoria No Contigua por Paginación Pura con Mapa de Bits.
 *
 * @author Jose Andrés Solano Vargas
 */
public class MemoriaPaginada {

    private final Memoria memoriaReal;
    private final boolean[] tablaBitsMarcos; // El MAPA DE BITS solicitado por el profe
    private final int tamanoPagina;          // Tamaño de cada frame (ej. 16)
    private final int totalMarcos;           // Cantidad total de frames en el hardware

    public MemoriaPaginada(Memoria memoriaReal, int tamanoPagina) {
        this.memoriaReal = memoriaReal;
        this.tamanoPagina = tamanoPagina;

        int tamanoTotalRAM = memoriaReal.getTamanoTotal();
        this.totalMarcos = tamanoTotalRAM / tamanoPagina;
        this.tablaBitsMarcos = new boolean[totalMarcos]; // Inicia todo en false (Libre)

        // Bloquean marcos que le pertenecen al Kernel de forma física fija si es necesario
        int inicioUsuario = memoriaReal.getInicioUsuario();
        int marcosKernel = (int) Math.ceil((double) inicioUsuario / tamanoPagina);

        for (int i = 0; i < marcosKernel; i++) {
            tablaBitsMarcos[i] = true; // Reservados para el Sistema Operativo
        }

        System.out.println("\n=== HARDWARE: CONFIGURACIÓN DE MEMORIA PAGINADA ===");
        System.out.println(" -> Total RAM: " + tamanoTotalRAM + " celdas.");
        System.out.println(" -> Tamaño de Página establecido: " + tamanoPagina + " celdas.");
        System.out.println(" -> Total Marcos de Hardware creados: " + totalMarcos);
        System.out.println(" -> Marcos reservados para el Kernel: " + marcosKernel);
        System.out.println("===================================================\n");
    }

    /**
     * Asigna marcos dispersos de la RAM a un proceso bajo demanda (No Contigua)
     */
    public boolean asignarProcesoPaginado(BCP proceso, List<String> instruccionesAsm) {
        int celdasNecesarias = instruccionesAsm.size();
        // Calcular cuántas páginas ocupa el script (División entera hacia arriba)
        int paginasRequeridas = (int) Math.ceil((double) celdasNecesarias / tamanoPagina);

        // Contamos cuántos marcos libres reales nos quedan en el mapa de bits
        int marcosLibres = 0;
        for (boolean ocupado : tablaBitsMarcos) {
            if (!ocupado) {
                marcosLibres++;
            }
        }

        if (marcosLibres < paginasRequeridas) {
            System.out.println("KERNEL-PAGINACIÓN: No hay suficientes marcos libres para " + proceso.nombreProceso
                    + " (Requiere: " + paginasRequeridas + " marcos, Libres: " + marcosLibres + ")");
            return false; // Al disco en ESPERA_ADMISION
        }

        // Hay espacio, entonces Activamos la paginación en el BCP
        proceso.esPaginado = true;
        proceso.setTablaPaginas(new TablaPaginas(tamanoPagina));
        proceso.alcance = celdasNecesarias;
        proceso.PC = 0; // REGLA DE ORO: Su PC virtual arranca en 0

        // Buscamos marcos libres y repartir el código línea por línea en la RAM real
        int paginaActual = 0;
        int lineaInstruccion = 0;

        List<Integer> marcosAsignados = new ArrayList<>();

        for (int marco = 0; marco < totalMarcos && paginaActual < paginasRequeridas; marco++) {
            if (!tablaBitsMarcos[marco]) { // Si el bit está en false (libre)

                tablaBitsMarcos[marco] = true; // Setear bit a 1 (ocupado)
                proceso.getTablaPaginas().registrarPagina(paginaActual, marco);
                marcosAsignados.add(marco);

                // Copia las líneas que quepan en este marco físico en la RAM real
                int baseFisicaMarco = marco * tamanoPagina;
                for (int offset = 0; offset < tamanoPagina && lineaInstruccion < celdasNecesarias; offset++) {
                    String lineaCodigo = instruccionesAsm.get(lineaInstruccion);

                    // Inyectamos directo en el storage de tu RAM real usando la celda física exacta
                    memoriaReal.escribirCeldaDirecta(baseFisicaMarco + offset, lineaCodigo);
                    lineaInstruccion++;
                }
                paginaActual++;
            }
        }

        System.out.println("\n>>> PROCESO ADMITIDO EN HARDWARE (PAGINACIÓN NO CONTIGUA) <<<");
        System.out.println("MEMORIA: '" + proceso.nombreProceso + "' -> Segmentado en " + paginasRequeridas + " páginas lógicas.");
        System.out.println("MAPA BITS: Marcos físicos asignados -> " + marcosAsignados.toString());
        System.out.println("-----------------------------------------------------------------------");

        return true;
    }

    /**
     * Traduce una dirección virtual consultando la Tabla de Páginas asignada
     * específicamente al BCP del proceso.
     */
    public int traducirDireccionVirtual(tarea1.joseandres.proceso.BCP proceso, int direccionVirtual) {
        if (proceso == null) {
            return -1;
        }

        // Recuperamos la TablaPaginas que ahora almacena el BCP corregido
        tarea1.joseandres.memoria.TablaPaginas tablaProceso = proceso.getTablaPaginas();

        if (tablaProceso == null) {
            System.err.println("Error: El proceso PID " + proceso.id + " no tiene una Tabla de Páginas asignada.");
            return -1;
        }

        // Delegamos la traducción matemática al método 'traducirDireccion' que ya creaste
        return tablaProceso.traducirDireccion(direccionVirtual);
    }

    /**
     * Libera los marcos del mapa de bits y limpia la RAM real con "00000"
     */
    public void liberarProcesoPaginado(BCP proceso) {
        //  Usamos el getter público getTablaPaginas() en lugar del atributo privado
        if (proceso == null || proceso.getTablaPaginas() == null) return;

        //  De igual forma accedemos mediante el getter
        for (int marcoFisico : proceso.getTablaPaginas().getTabla().values()) {
            
            // Apagamos el bit del hardware en el mapa de bits
            tablaBitsMarcos[marcoFisico] = false; 
            
            // Limpiamos físicamente las celdas de ese marco en la RAM real
            int baseFisicaMarco = marcoFisico * tamanoPagina;
            for (int offset = 0; offset < tamanoPagina; offset++) {
                memoriaReal.liberarCelda(baseFisicaMarco + offset);
            }
        }
        
        System.out.println("KERNEL-PAGINACIÓN: RAM y Mapa de Bits liberados para el PID: " + proceso.id);
        
        // Limpieza final usando el getter
        proceso.getTablaPaginas().limpiar();
    }
    
    public boolean[] getBitmap() {
        return this.tablaBitsMarcos;
    }

    /**
     * Devuelve el tamaño configurado para cada página física del sistema.
     * @return Cantidad de celdas por página.
     */
    public int getTamanoPagina() {
        return this.tamanoPagina;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.kernel;

/**
 *
 * @author joses
 */
import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import tarea1.joseandres.memoria.Memoria;
import tarea1.joseandres.disco.Disco;
import tarea1.joseandres.administrador.Scheduler;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.loader.Loader;
import tarea1.joseandres.algoritmos.PlanificadorFCFS;

//Gestionamos los recusos y los procesos. Decidimos si un proceso entra o no al sistema
//Usamos como base la .config 
public class Kernel {
    private Memoria ram;
    private Disco disco;
    private Scheduler scheduler;
    private Loader loader;
    private int contadorProcesos = 0;
    private static final int MAX_PROCESOS_ACTIVOS = 5;
    
    private java.util.List<BCP> listaProcesos = new java.util.ArrayList<>(); //Nuestra lista de procesos
    private Queue<BCP> colaEsperaAdmision = new LinkedList<>();

    //Contructor
    public Kernel(int tamanoRam, int tamanoDisco, int porcentajeKernel, int porcentajeIndiceDisco) {
        this.ram = new Memoria(tamanoRam, porcentajeKernel);
        this.disco = new Disco(tamanoDisco, porcentajeIndiceDisco);
        this.loader = new Loader(this.disco);
        this.scheduler = new Scheduler();
        this.scheduler.setEstrategia(new PlanificadorFCFS());
        
    }
    
       
    public BCP solicitarSiguienteProceso() {
        // Pedimos siguiente proceso, usa estrategia
        return scheduler.obtenerSiguiente(); 
    }
    
    
    public Memoria getRam() {
        return ram;
    }

    public Disco getDisco() {
        return disco;
    }
    //Mostramos 
    public Scheduler getScheduler() {
        return scheduler;
    }
    public java.util.List<BCP> getListaProcesos() {
        return listaProcesos;
    }
    /**
     * Orquestador de la carga: Nuevo -> Disco -> RAM -> BCP -> Ready
     */
     /**
     * Carga el archivo al disco siempre.
     * Si hay cupo, lo admite al sistema y lo pasa a PREPARADO.
     * Si no hay cupo, queda en ESPERA_ADMISION.
     */
    public boolean cargarProceso(String rutaAsm) {
        int tamanoReal = loader.cargaArchivoADisco(rutaAsm);

        if (tamanoReal == -1) {
            System.err.println("KERNEL: Error al traducir/cargar el archivo en disco: " + rutaAsm);
            return false;
        }

        File archivo = new File(rutaAsm);

        // Si ya está el máximo de admitidos, queda en espera
        if (contarProcesosActivos() >= MAX_PROCESOS_ACTIVOS) {
            BCP enEspera = new BCP(contadorProcesos++, archivo.getName(), -1, tamanoReal);
            enEspera.estado = "Nuevo";
            listaProcesos.add(enEspera);
            colaEsperaAdmision.offer(enEspera);

            System.out.println("KERNEL: " + archivo.getName() + " cargado a disco, pero quedó en ESPERA_ADMISION.");
            return true;
        }

        // Si hay cupo, intentamos admitirlo de una vez
        return admitirProcesoDesdeDisco(archivo.getName(), tamanoReal);
    }
    
    
    /**
     * Cuando termina un proceso, libera su memoria y trata de promover
     * uno desde la cola de espera.
     */
    public void finalizarProceso(BCP proceso) {
        if (proceso == null) return;

        liberarMemoriaProceso(proceso);
        intentarPromoverDesdeEspera();
    }

    /**
     * Admite un proceso que este guardado en disco:
     * lo copia a RAM, crea BCP y lo mete a READY/PREPARADO.
     */
    private boolean admitirProcesoDesdeDisco(String nombreArchivo, int tamanoReal) {
        int direccionBase = ram.buscarEspacioDisponible(tamanoReal);

        if (direccionBase == -1) {
            System.err.println("KERNEL: No hay espacio suficiente en RAM para admitir " + nombreArchivo);
            return false;
        }

        int direccionInicioEnDisco = disco.getDireccionInicioArchivo(nombreArchivo);

        if (direccionInicioEnDisco == -1) {
            System.err.println("KERNEL: No se encontró " + nombreArchivo + " en el índice del disco.");
            return false;
        }

        for (int i = 0; i < tamanoReal; i++) {
            String instruccion = disco.leer(direccionInicioEnDisco + i);
            ram.escribirSeguro(direccionBase + i, instruccion, direccionBase, tamanoReal);
        }

        BCP nuevoProceso = new BCP(contadorProcesos++, nombreArchivo, direccionBase, tamanoReal);
        scheduler.agregarProceso(nuevoProceso);
        listaProcesos.add(nuevoProceso);

        System.out.println("SISTEMA: Proceso " + nombreArchivo + " admitido en base " + direccionBase);
        return true;
    }
    
    
     /**
     * Toma el primero de la cola de espera si hay cupo y RAM.
     */
    public void intentarPromoverDesdeEspera() {
        if (colaEsperaAdmision.isEmpty()) return;
        if (contarProcesosActivos() >= MAX_PROCESOS_ACTIVOS) return;

        BCP esperando = colaEsperaAdmision.peek();
        if (esperando == null) return;

        int direccionBase = ram.buscarEspacioDisponible(esperando.getAlcance());
        if (direccionBase == -1) {
            System.out.println("KERNEL: Hay procesos en espera, pero todavía no hay RAM suficiente.");
            return;
        }

        int direccionInicioEnDisco = disco.getDireccionInicioArchivo(esperando.nombreProceso);
        if (direccionInicioEnDisco == -1) {
            System.err.println("KERNEL: No se encontró en disco el proceso en espera " + esperando.nombreProceso);
            colaEsperaAdmision.poll();
            esperando.estado = "ERROR";
            return;
        }

        for (int i = 0; i < esperando.getAlcance(); i++) {
            String instruccion = disco.leer(direccionInicioEnDisco + i);
            ram.escribirSeguro(direccionBase + i, instruccion, direccionBase, esperando.getAlcance());
        }

        esperando.setDireccionBase(direccionBase);
        scheduler.agregarProceso(esperando); // lo deja en PREPARADO
        colaEsperaAdmision.poll();

        System.out.println("KERNEL: Proceso " + esperando.nombreProceso + " promovido desde ESPERA_ADMISION a PREPARADO.");
    }

    /**
     * Cuenta procesos admitidos al sistema.
     */
    private int contarProcesosActivos() {
        int total = 0;

        for (BCP p : listaProcesos) {
            if (p.estado.equals("PREPARADO")
                    || p.estado.equals("EJECUCION (CPU)")
                    || p.estado.equals("ESPERA")) {
                total++;
            }
        }

        return total;
    }

    /**
     * Libera la memoria del proceso terminado/error.
     */
    private void liberarMemoriaProceso(BCP proceso) {
        if (proceso.getDireccionBase() == -1) return;

        int base = proceso.getDireccionBase();
        int alcance = proceso.getAlcance();

        for (int i = 0; i < alcance; i++) {
            ram.liberarCelda(base + i);
        }
        // NO borrar\ la base, solo liberar memoria
        proceso.setDireccionBase(-1);
    }
    
    
}
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
import tarea1.joseandres.memoria.MemoriaFija;

//Gestionamos los recusos y los procesos. Decidimos si un proceso entra o no al sistema
//Usamos como base la .config 
public class Kernel {

    private Memoria ram;
    private Disco disco;
    private Scheduler scheduler;
    private Loader loader;

    //MemoriaFija
    private MemoriaFija memoriaFija;
    private String algoritmoAsignacion;

    private int contadorProcesos = 0;
    private static final int MAX_PROCESOS_ACTIVOS = 5;

    private java.util.List<BCP> listaProcesos = new java.util.ArrayList<>(); //Nuestra lista de procesos
    private Queue<BCP> colaEsperaAdmision = new LinkedList<>();

   

   
    public Kernel(int tamanoRam, int tamanoDisco, int porcentajeKernel, int porcentajeIndiceDisco, String tipoMemoria, int cantParticiones, int[] tamanosParticiones) {

        // Inicialización de los componentes base
         System.out.println("El tipo de memoria leído es: " + tipoMemoria);
        this.ram = new Memoria(tamanoRam, porcentajeKernel);
        this.disco = new Disco(tamanoDisco, porcentajeIndiceDisco);
        this.loader = new Loader(this.disco);
        this.scheduler = new Scheduler();
        this.scheduler.setEstrategia(new PlanificadorFCFS());

        // Inicializamos el gestor de memoria fija pasándole la RAM
        this.memoriaFija = new MemoriaFija(this.ram);
        //Guardamos el algoritmo
        this.algoritmoAsignacion = algoritmoAsignacion;

     
        if ("FIJA_IGUAL".equalsIgnoreCase(tipoMemoria)) {
        this.memoriaFija.crearParticionesIguales(cantParticiones);
       
        System.out.println("KERNEL: Memoria Fija Igual inicializada con " + cantParticiones + " particiones.");
    } else if ("FIJA_VARIABLE".equalsIgnoreCase(tipoMemoria)) {
        this.memoriaFija.crearParticionesVariables(tamanosParticiones);
        System.out.println("KERNEL: Memoria Fija Variable inicializada correctamente.");
    }
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

    public Scheduler getScheduler() {
        return scheduler;
    }

    public java.util.List<BCP> getListaProcesos() {
        return listaProcesos;
    }

    // Getter para que la GUI o el Main puedan inspeccionar el estado lógico de las particiones
    public MemoriaFija getMemoriaFija() {
        return memoriaFija;
    }

    /**
     * Carga el archivo al disco siempre. Si hay cupo, lo admite al sistema y lo
     * pasa a PREPARADO. Si no hay cupo, queda en ESPERA_ADMISION.
     */
    public boolean cargarProceso(String rutaAsm) {
        int tamanoReal = loader.cargaArchivoADisco(rutaAsm);

        if (tamanoReal == -1) {
            System.err.println("KERNEL: Error al traducir/cargar el archivo en disco: " + rutaAsm);
            return false;
        }

        File archivo = new File(rutaAsm);

        // Si ya está el máximo de admitidos activos, va directo a la cola de espera de admisión
        if (contarProcesosActivos() >= MAX_PROCESOS_ACTIVOS) {
            BCP enEspera = new BCP(contadorProcesos++, archivo.getName(), -1, tamanoReal);
            enEspera.estado = "Nuevo";
            listaProcesos.add(enEspera);
            colaEsperaAdmision.offer(enEspera);

            System.out.println("KERNEL: " + archivo.getName() + " cargado a disco, pero quedó en ESPERA_ADMISION por límite de grado multiprogramación.");
            return true;
        }

        // Si hay cupo en el grado de multiprogramación, intentamos admitirlo de una vez
        return admitirProcesoDesdeDisco(archivo.getName(), tamanoReal);
    }

    /**
     * Cuando termina un proceso, libera su memoria y trata de promover uno
     * desde la cola de espera.
     */
    public void finalizarProceso(BCP proceso) {
        if (proceso == null) {
            return;
        }

        liberarMemoriaProceso(proceso);
        intentarPromoverDesdeEspera();
    }

    /**
     * Admite un proceso que está guardado en disco: Utiliza memoriaFija para
     * buscar una partición libre, calcula dirección física, copia a la RAM
     * física, inicializa BCP y lo mete a READY/PREPARADO.
     */
    private boolean admitirProcesoDesdeDisco(String nombreArchivo, int tamanoReal) {
        // Creamos un BCP temporal para evaluar si cabe en alguna partición
        BCP provisional = new BCP(contadorProcesos, nombreArchivo, -1, tamanoReal);

        //  Best-Fit según el JSON
        boolean exitoAsignacion = memoriaFija.asignarProceso(provisional, this.algoritmoAsignacion);

        if (!exitoAsignacion) {
            System.err.println("KERNEL: No se encontró partición fija disponible o suficientemente grande para admitir " + nombreArchivo);
            return false;
        }

        // Si tuvo éxito, 'provisional' ya tiene asignada la dirección base inicial de la partición fija
        int direccionBase = provisional.getDireccionBase();
        contadorProcesos++; // Consumimos el ID oficial

        int direccionInicioEnDisco = disco.getDireccionInicioArchivo(nombreArchivo);

        if (direccionInicioEnDisco == -1) {
            System.err.println("KERNEL: No se encontró " + nombreArchivo + " en el índice del disco.");
            // Si falla el disco por alguna razón catastrófica, liberamos la partición lógica que reservamos
            memoriaFija.liberarParticionPorProceso(provisional);
            return false;
        }

        // Transferimos las instrucciones binarias del Disco a la RAM real usando la base calculada
        for (int i = 0; i < tamanoReal; i++) {
            String instruccion = disco.leer(direccionInicioEnDisco + i);
            ram.escribirSeguro(direccionBase + i, instruccion, direccionBase, tamanoReal);
        }

        scheduler.agregarProceso(provisional);
        listaProcesos.add(provisional);

        System.out.println("SISTEMA: Proceso " + nombreArchivo + " admitido en Partición Fija con base física " + direccionBase);
        return true;
    }

    /**
     * Toma el primero de la cola de espera si hay cupo y particiones libres en
     * la RAM.
     */
    public void intentarPromoverDesdeEspera() {
        if (colaEsperaAdmision.isEmpty()) {
            return;
        }
        if (contarProcesosActivos() >= MAX_PROCESOS_ACTIVOS) {
            return;
        }

        BCP esperando = colaEsperaAdmision.peek();
        if (esperando == null) {
            return;
        }

        // Evaluamos si hay espacio en alguna partición para el proceso en espera
        boolean exitoAsignacion = memoriaFija.asignarProceso(esperando, this.algoritmoAsignacion);
        if (!exitoAsignacion) {
            System.out.println("KERNEL: Hay procesos en cola de admisión, pero no hay ninguna partición fija libre que se ajuste a su tamaño.");
            return;
        }

        // Si se asignó la partición con éxito, extraemos la dirección física base establecida en el BCP
        int direccionBase = esperando.getDireccionBase();

        int direccionInicioEnDisco = disco.getDireccionInicioArchivo(esperando.nombreProceso);
        if (direccionInicioEnDisco == -1) {
            System.err.println("KERNEL: No se encontró en disco el proceso en espera " + esperando.nombreProceso);
            colaEsperaAdmision.poll();
            esperando.estado = "ERROR";
            memoriaFija.liberarParticionPorProceso(esperando); // Deshacemos reserva lógica
            return;
        }

        // Transferimos desde el disco a las celdas físicas reales mapeadas de la partición
        for (int i = 0; i < esperando.getAlcance(); i++) {
            String instruccion = disco.leer(direccionInicioEnDisco + i);
            ram.escribirSeguro(direccionBase + i, instruccion, direccionBase, esperando.getAlcance());
        }

        scheduler.agregarProceso(esperando); // Cambia su estado a PREPARADO y entra a la cola del Scheduler
        colaEsperaAdmision.poll(); // Lo extraemos oficialmente de la cola de espera

        System.out.println("KERNEL: Proceso " + esperando.nombreProceso + " promovido desde ESPERA_ADMISION a la partición fija con base: " + direccionBase);
    }

    /**
     * Cuenta procesos activos admitidos en el sistema.
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
     * Libera completamente los recursos de hardware y el control del software
     * administrador.
     */
    private void liberarMemoriaProceso(BCP proceso) {
        if (proceso.getDireccionBase() == -1) {
            return;
        }

        int base = proceso.getDireccionBase();
        int alcance = proceso.getAlcance();

        // Devolvemos las celdas utilizadas en la RAM real a "00000"
        for (int i = 0; i < alcance; i++) {
            ram.liberarCelda(base + i);
        }

        //  Le avisamos al gestor de particiones fijas que deje libre el slot
        memoriaFija.liberarParticionPorProceso(proceso);

        // Modificamos el BCP para reflejar que ya no reside en memoria física
        proceso.setDireccionBase(-1);
        System.out.println("KERNEL: Recursos de hardware y partición lógica liberados exitosamente para el proceso " + proceso.nombreProceso);
    }
}

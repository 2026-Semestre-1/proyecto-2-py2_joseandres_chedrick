package tarea1.joseandres.kernel;

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
import tarea1.joseandres.memoria.MemoriaDinamica;


public class Kernel {

    private Memoria ram;
    private Disco disco;
    private Scheduler scheduler;
    private Loader loader;

    private MemoriaFija memoriaFija;
    private MemoriaDinamica memoriaDinamica;
    private String tipoMemoria;
    private String algoritmoAsignacion;

    private int contadorProcesos = 0;
    private static final int MAX_PROCESOS_ACTIVOS = 5;

    private java.util.List<BCP> listaProcesos = new java.util.ArrayList<>(); // lista global de procesos
    private Queue<BCP> colaEsperaAdmision = new LinkedList<>();

    public Kernel(int tamanoRam, int tamanoDisco, int porcentajeKernel, int porcentajeIndiceDisco, String tipoMemoria, int cantParticiones, int[] tamanosParticiones) {

        //  Guardamos el tipo de memoria globalmente para que no sea null
        System.out.println("El tipo de memoria leído es: " + tipoMemoria);
        this.tipoMemoria = tipoMemoria; 
        
        this.ram = new Memoria(tamanoRam, porcentajeKernel);
        this.disco = new Disco(tamanoDisco, porcentajeIndiceDisco);
        this.loader = new Loader(this.disco);
        this.scheduler = new Scheduler();
        this.scheduler.setEstrategia(new PlanificadorFCFS());

        // Inicializamos ambos enviando la RAM real
        this.memoriaFija = new MemoriaFija(this.ram);
        this.memoriaDinamica = new MemoriaDinamica(this.ram);
        
        // Guardamos el algoritmo de asignación
        this.algoritmoAsignacion = algoritmoAsignacion;

        inicializarGestorMemoria(cantParticiones, tamanosParticiones);
    }

  
    private void inicializarGestorMemoria(int cantParticiones, int[] tamanosParticiones) {
        String tipoLimpio = (this.tipoMemoria != null) ? this.tipoMemoria.trim().toUpperCase() : "";

        if (tipoLimpio.contains("IGUAL")) {
            this.memoriaFija.crearParticionesIguales(cantParticiones);
            System.out.println("KERNEL: Memoria Fija Igual inicializada con " + cantParticiones + " particiones.");
        } else if (tipoLimpio.contains("VARIABLE")) {
            this.memoriaFija.crearParticionesVariables(tamanosParticiones);
            System.out.println("KERNEL: Memoria Fija Variable inicializada correctamente.");
        } else if (tipoLimpio.contains("DINAM")) {
            this.memoriaDinamica.inicializarMemoriaDinamica();
            System.out.println("KERNEL: Memoria Dinámica (Best-Fit) inicializada correctamente.");
        }
    }

   
    private boolean asignarMemoriaGestor(BCP proceso) {
        String tipoLimpio = (this.tipoMemoria != null) ? this.tipoMemoria.trim().toUpperCase() : "";

        if (tipoLimpio.contains("DINAM")) {
            return memoriaDinamica.asignarProceso(proceso);
        } else {
            return memoriaFija.asignarProceso(proceso, this.algoritmoAsignacion);
        }
    }
    
  
    private void liberarMemoriaGestor(BCP proceso) {
        String tipoLimpio = (this.tipoMemoria != null) ? this.tipoMemoria.trim().toUpperCase() : "";

        if (tipoLimpio.contains("DINAM")) {
            memoriaDinamica.liberarBloquePorProceso(proceso);
        } else {
            memoriaFija.liberarParticionPorProceso(proceso);
        }
    }

    public BCP solicitarSiguienteProceso() {
        return scheduler.obtenerSiguiente();
    }

    public Memoria getRam() { return ram; }
    public Disco getDisco() { return disco; }
    public Scheduler getScheduler() { return scheduler; }
    public java.util.List<BCP> getListaProcesos() { return listaProcesos; }
    public MemoriaFija getMemoriaFija() { return memoriaFija; }
    public MemoriaDinamica getMemoriaDinamica() { return memoriaDinamica; }

    public boolean cargarProceso(String rutaAsm) {
        int tamanoReal = loader.cargaArchivoADisco(rutaAsm);

        if (tamanoReal == -1) {
            System.err.println("KERNEL: Error al traducir/cargar el archivo en disco: " + rutaAsm);
            return false;
        }

        File archivo = new File(rutaAsm);

        if (contarProcesosActivos() >= MAX_PROCESOS_ACTIVOS) {
            BCP enEspera = new BCP(contadorProcesos++, archivo.getName(), -1, tamanoReal);
            enEspera.estado = "Nuevo";
            listaProcesos.add(enEspera);
            colaEsperaAdmision.offer(enEspera);

            System.out.println("KERNEL: " + archivo.getName() + " cargado a disco, pero quedó en ESPERA_ADMISION por límite de grado multiprogramación.");
            return true;
        }

        return admitirProcesoDesdeDisco(archivo.getName(), tamanoReal);
    }

    public void finalizarProceso(BCP proceso) {
        if (proceso == null) {
            return;
        }
        liberarMemoriaProceso(proceso);
        intentarPromoverDesdeEspera();
    }

    private boolean admitirProcesoDesdeDisco(String nombreArchivo, int tamanoReal) {
        BCP provisional = new BCP(contadorProcesos, nombreArchivo, -1, tamanoReal);

        boolean exitoAsignacion = asignarMemoriaGestor(provisional);

        if (!exitoAsignacion) {
            System.err.println("KERNEL: No se encontró espacio disponible en el esquema [" + tipoMemoria + "] para admitir " + nombreArchivo + " (Requiere: " + tamanoReal + " celdas)");
            return false;
        }

        int direccionBase = provisional.getDireccionBase();
        contadorProcesos++; 

        int direccionInicioEnDisco = disco.getDireccionInicioArchivo(nombreArchivo);

        if (direccionInicioEnDisco == -1) {
            System.err.println("KERNEL: No se encontró " + nombreArchivo + " en el índice del disco.");
            liberarMemoriaGestor(provisional);
            return false;
        }

        for (int i = 0; i < tamanoReal; i++) {
            String instruccion = disco.leer(direccionInicioEnDisco + i);
            ram.escribirSeguro(direccionBase + i, instruccion, direccionBase, tamanoReal);
        }

        scheduler.agregarProceso(provisional);
        listaProcesos.add(provisional);

        System.out.println("SISTEMA: Proceso " + nombreArchivo + " admitido con base física " + direccionBase);
        return true;
    }

    public void intentarPromoverDesdeEspera() {
        if (colaEsperaAdmision.isEmpty() || contarProcesosActivos() >= MAX_PROCESOS_ACTIVOS) {
            return;
        }

        BCP esperando = colaEsperaAdmision.peek();
        if (esperando == null) {
            return;
        }

        //  Evaluamos espacio usando el Gestor Unificado, no memoriaFija directamente
        boolean exitoAsignacion = asignarMemoriaGestor(esperando);
        if (!exitoAsignacion) {
            System.out.println("KERNEL: Hay procesos en cola de admisión, pero el esquema [" + tipoMemoria + "] no tiene huecos adecuados en este momento.");
            return;
        }

        int direccionBase = esperando.getDireccionBase();
        int direccionInicioEnDisco = disco.getDireccionInicioArchivo(esperando.nombreProceso);
        
        if (direccionInicioEnDisco == -1) {
            System.err.println("KERNEL: No se encontró en disco el proceso en espera " + esperando.nombreProceso);
            colaEsperaAdmision.poll();
            esperando.estado = "ERROR";
            liberarMemoriaGestor(esperando); 
            return;
        }

        for (int i = 0; i < esperando.getAlcance(); i++) {
            String instruccion = disco.leer(direccionInicioEnDisco + i);
            ram.escribirSeguro(direccionBase + i, instruccion, direccionBase, esperando.getAlcance());
        }

        scheduler.agregarProceso(esperando); 
        colaEsperaAdmision.poll(); 

        System.out.println("KERNEL: Proceso " + esperando.nombreProceso + " promovido desde ESPERA_ADMISION con base: " + direccionBase);
    }

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

    private void liberarMemoriaProceso(BCP proceso) {
        if (proceso.getDireccionBase() == -1) {
            return;
        }

        int base = proceso.getDireccionBase();
        int alcance = proceso.getAlcance();

        for (int i = 0; i < alcance; i++) {
            ram.liberarCelda(base + i);
        }

        liberarMemoriaGestor(proceso);

        proceso.setDireccionBase(-1);
        System.out.println("KERNEL: Recursos de hardware y mapa de memoria [" + tipoMemoria + "] actualizados exitosamente para el proceso " + proceso.nombreProceso);
    }
}
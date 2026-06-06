package tarea1.joseandres.kernel;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import tarea1.joseandres.memoria.Memoria;
import tarea1.joseandres.disco.Disco;
import tarea1.joseandres.administrador.Scheduler;
import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.loader.Loader;
import tarea1.joseandres.estrategia.EstrategiaPlanificacion;
import tarea1.joseandres.memoria.MemoriaFija;
import tarea1.joseandres.memoria.MemoriaDinamica;

public class Kernel {

    private Memoria ram;
    private Disco disco;
    private Scheduler scheduler;
    private Loader loader;
    private MemoriaFija memoriaFija;
    private MemoriaDinamica memoriaDinamica;
    private tarea1.joseandres.memoria.MemoriaPaginada memoriaPaginada;
    private String tipoMemoria;
    private String algoritmoAsignacion;

    private int contadorProcesos = 0;
    private static final int MAX_PROCESOS_ACTIVOS = 5;

    private java.util.List<BCP> listaProcesos = new java.util.ArrayList<>(); // lista global de procesos
    private Queue<BCP> colaEsperaAdmision = new LinkedList<>();

    // Cambiá el constructor para recibir el tamanoPagina dinámico desde el JSON
    public Kernel(int tamanoRam, int tamanoDisco, int porcentajeKernel, int porcentajeIndiceDisco,
            String tipoMemoria, int cantParticiones, int[] tamanosParticiones, int tamanoPagina) {

        System.out.println("El tipo de memoria leído es: " + tipoMemoria);
        this.tipoMemoria = tipoMemoria;

        this.ram = new Memoria(tamanoRam, porcentajeKernel);
        this.disco = new Disco(tamanoDisco, porcentajeIndiceDisco);
        this.loader = new Loader(this.disco);
        this.scheduler = new Scheduler();

        this.memoriaFija = new MemoriaFija(this.ram);
        this.memoriaDinamica = new MemoriaDinamica(this.ram);

        //cantidad de las fragmetaciones
        this.memoriaPaginada = new tarea1.joseandres.memoria.MemoriaPaginada(this.ram, tamanoPagina);

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
        } else if (tipoLimpio.contains("PAGIN")) {

            System.out.println("KERNEL: Gestión No Contigua por Paginación Pura (Bitmap) Activa.");
        }
    }
    public void colocarEstrategia(EstrategiaPlanificacion estrategia){
        this.scheduler.setEstrategia(estrategia);
    }

    private boolean asignarMemoriaGestor(BCP proceso) {
        String tipoLimpio = (this.tipoMemoria != null) ? this.tipoMemoria.trim().toUpperCase() : "";

        if (tipoLimpio.contains("DINAM")) {
            return memoriaDinamica.asignarProceso(proceso);
        } else if (tipoLimpio.contains("PAGIN")) {
            return true;
        } else {
            return memoriaFija.asignarProceso(proceso, this.algoritmoAsignacion);
        }
    }

    private void liberarMemoriaGestor(BCP proceso) {
        String tipoLimpio = (this.tipoMemoria != null) ? this.tipoMemoria.trim().toUpperCase() : "";

        if (tipoLimpio.contains("DINAM")) {
            memoriaDinamica.liberarBloquePorProceso(proceso);
        } else if (tipoLimpio.contains("PAGIN")) {
            memoriaPaginada.liberarProcesoPaginado(proceso); // MAPA BITS
        } else {
            memoriaFija.liberarParticionPorProceso(proceso);
        }
    }

    public synchronized BCP solicitarSiguienteProceso() {
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

    public MemoriaFija getMemoriaFija() {
        return memoriaFija;
    }

    public MemoriaDinamica getMemoriaDinamica() {
        return memoriaDinamica;
    }

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

    public synchronized void finalizarProceso(BCP proceso) {
        if (proceso == null) {
            return;
        }
        liberarMemoriaProceso(proceso);
        intentarPromoverDesdeEspera();
    }

    private boolean admitirProcesoDesdeDisco(String nombreArchivo, int tamanoReal) {
        BCP provisional = new BCP(contadorProcesos, nombreArchivo, -1, tamanoReal);
        String tipoLimpio = (this.tipoMemoria != null) ? this.tipoMemoria.trim().toUpperCase() : "";

        int direccionInicioEnDisco = disco.getDireccionInicioArchivo(nombreArchivo);
        if (direccionInicioEnDisco == -1) {
            System.err.println("KERNEL: No se encontró " + nombreArchivo + " en el índice del disco.");
            return false;
        }
        //Ayuda de chatGPT
        // =====================================================================
        // ADMISIÓN POR PAGINACIÓN PURA (NO CONTIGUA)
        // =====================================================================
        if (tipoLimpio.contains("PAGIN")) {
            // Extraemos las líneas de código del disco para pasárselas al cargador segmentado
            java.util.List<String> instrucciones = new java.util.ArrayList<>();
            for (int i = 0; i < tamanoReal; i++) {
                instrucciones.add(disco.leer(direccionInicioEnDisco + i));
            }

            // El gestor de paginación evalúa el Mapa de Bits, si hay campo inyecta y crea la Tabla de Páginas
            boolean exitoPaginacion = memoriaPaginada.asignarProcesoPaginado(provisional, instrucciones);
            if (!exitoPaginacion) {
                System.err.println("KERNEL-PAGINACIÓN: RAM Saturada (Sin marcos libres). " + nombreArchivo + " retenido en espera.");
                return false;
            }

            contadorProcesos++;
            scheduler.agregarProceso(provisional);
            listaProcesos.add(provisional);
            System.out.println("SISTEMA: Proceso " + nombreArchivo + " admitido bajo PAGINACIÓN NO CONTIGUA.");
            return true;
        }

        // =====================================================================
        // RUTA B: ADMISIÓN CONTIGUA TRADICIONAL (FIJA / DINÁMICA)
        // =====================================================================
        boolean exitoAsignacion = asignarMemoriaGestor(provisional);
        if (!exitoAsignacion) {
            System.err.println("KERNEL: No se encontró espacio disponible en el esquema [" + tipoMemoria + "] para admitir " + nombreArchivo + " (Requiere: " + tamanoReal + " celdas)");
            return false;
        }

        int direccionBase = provisional.getDireccionBase();
        contadorProcesos++;

        for (int i = 0; i < tamanoReal; i++) {
            String instruccion = disco.leer(direccionInicioEnDisco + i);
            ram.escribirSeguro(direccionBase + i, instruccion, direccionBase, tamanoReal);
        }

        scheduler.agregarProceso(provisional);
        listaProcesos.add(provisional);

        System.out.println("SISTEMA: Proceso " + nombreArchivo + " admitido con base física " + direccionBase);
        return true;
    }

    //Ayuda de chatGPT
    public void intentarPromoverDesdeEspera() {
        if (colaEsperaAdmision.isEmpty() || contarProcesosActivos() >= MAX_PROCESOS_ACTIVOS) {
            return;
        }

        BCP esperando = colaEsperaAdmision.peek();
        if (esperando == null) {
            return;
        }

        String tipoLimpio = (this.tipoMemoria != null) ? this.tipoMemoria.trim().toUpperCase() : "";
        int direccionInicioEnDisco = disco.getDireccionInicioArchivo(esperando.nombreProceso);

        // =====================================================================
        //  PROMOVEmos POR PAGINACIÓN PURA (NO CONTIGUA)
        // =====================================================================
        if (tipoLimpio.contains("PAGIN")) {
            java.util.List<String> instrucciones = new java.util.ArrayList<>();
            for (int i = 0; i < esperando.getAlcance(); i++) {
                instrucciones.add(disco.leer(direccionInicioEnDisco + i));
            }

            boolean exito = memoriaPaginada.asignarProcesoPaginado(esperando, instrucciones);
            if (!exito) {
                return; // Sigue esperando en disco si no hay marcos libres
            }
            scheduler.agregarProceso(esperando);
            colaEsperaAdmision.poll();
            System.out.println("KERNEL: Proceso " + esperando.nombreProceso + " promovido a PAGINACIÓN desde ESPERA_ADMISION.");
            return;
        }

        // =====================================================================
        // PROMOVEMOS POR RUTA CONTIGUA TRADICIONAL (FIJA / DINÁMICA)
        // =====================================================================
        // Evaluamos espacio usando el Gestor Unificado
        boolean exitoAsignacion = asignarMemoriaGestor(esperando);
        if (!exitoAsignacion) {
            System.out.println("KERNEL: Hay procesos en cola de admisión, pero el esquema [" + tipoMemoria + "] no tiene huecos adecuados en este momento.");
            return;
        }

        if (direccionInicioEnDisco == -1) {
            System.err.println("KERNEL: No se encontró en disco el proceso en espera " + esperando.nombreProceso);
            colaEsperaAdmision.poll();
            esperando.estado = "ERROR";
            liberarMemoriaGestor(esperando);
            return;
        }

        int direccionBase = esperando.getDireccionBase();

        for (int i = 0; i < esperando.getAlcance(); i++) {
            String instruccion = disco.leer(direccionInicioEnDisco + i);
            ram.escribirSeguro(direccionBase + i, instruccion, direccionBase, esperando.getAlcance());
        }

        scheduler.agregarProceso(esperando);
        colaEsperaAdmision.poll();

        System.out.println("KERNEL: Proceso " + esperando.nombreProceso + " promovido desde ESPERA_ADMISION con base: " + direccionBase);
    }

    //Reconoce los procesos corriendo
    private int contarProcesosActivos() {
        int total = 0;
        for (BCP p : listaProcesos) {
            if (p.estado.equals("PREPARADO")
                    || p.estado.startsWith("EJECUCION") // <--- Captura CPU 0, CPU 1, CPU 2, etc.
                    || p.estado.equals("ESPERA")) {
                total++;
            }
        }
        return total;
    }

    private void liberarMemoriaProceso(BCP proceso) {
        String tipoLimpio = (this.tipoMemoria != null) ? this.tipoMemoria.trim().toUpperCase() : "";

        if (tipoLimpio.contains("PAGIN")) {
            liberarMemoriaGestor(proceso); // Limpia mapa de bits y celdas correspondientes
            System.out.println("KERNEL: Recursos de hardware liberados por Paginación para " + proceso.nombreProceso);
            return;
        }

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

    public synchronized void devolverAColaListos(BCP proceso) {
        if (proceso == null) {
            return;
        }
        proceso.estado = "PREPARADO";
        proceso.cpuAsignada = -1; // Soltamos la CPU
        scheduler.agregarProceso(proceso);
    }
    
    public tarea1.joseandres.memoria.MemoriaPaginada getMemoriaPaginada() {
        return this.memoriaPaginada;
    }
}

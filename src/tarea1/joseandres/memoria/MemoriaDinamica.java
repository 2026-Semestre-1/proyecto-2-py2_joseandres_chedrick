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
 * Gestión de Memoria Dinámica utilizando exclusivamente el algoritmo Best-Fit.
 *
 * @author joses
 */
public class MemoriaDinamica {

    private final Memoria memoriaReal;
    private final List<Particion> bloques; //La lista dinamica de los bloqiues (libres/ocupado)
    private int contadorIdParticion = 1;

    public MemoriaDinamica(Memoria memoriaReal) {
        this.memoriaReal = memoriaReal;
        this.bloques = new ArrayList<>();
        inicializarMemoriaDinamica();
    }

    /**
     * Inicializa la memoria de usuario como unico bloque
     */
    public final void inicializarMemoriaDinamica() {
        bloques.clear();
        contadorIdParticion = 1;
        //calculamos el espacio real
        int inicioUsuario = memoriaReal.getInicioUsuario();
        int espacioDisponible = memoriaReal.getTamanoTotal() - inicioUsuario;

        // Creamos el bloque inicial que representa toda la RAM disponible para el usuario
        Particion bloqueInicial = new Particion(contadorIdParticion++, inicioUsuario, espacioDisponible);
        bloques.add(bloqueInicial);

        System.out.println("\n=======================================================");
        System.out.println("   INICIALIZANDO MEMORIA DINÁMICA (BEST-FIT)");
        System.out.println("   Dirección base de usuario (Post-Kernel): " + inicioUsuario);
        System.out.println("   RAM unificada en un único bloque libre de: " + espacioDisponible + " celdas.");
        System.out.println("=======================================================\n");
    }

    /**
     * Asigna un proceso buscando el hueco libre que deje el menor residuo. le
     * aplico el "Splitting" (división del bloque) si sobra espacio.
     */
    public boolean asignarProceso(BCP proceso) {
        Particion bloqueOptimo = null;
        int menorResiduo = Integer.MAX_VALUE;
        int indiceOptimo = -1;

        //  Buscamos el hueco libre que calce más ajustado
        for (int i = 0; i < bloques.size(); i++) {
            Particion b = bloques.get(i);
            if (b.isLibre() && b.getTamano() >= proceso.getAlcance()) {
                int residuoActual = b.getTamano() - proceso.getAlcance();
                if (residuoActual < menorResiduo) {
                    menorResiduo = residuoActual;
                    bloqueOptimo = b;
                    indiceOptimo = i;
                }
            }
        }

        // Se admite y divide el bloque (SPLITTING)
        if (bloqueOptimo != null) {
            int inicioOriginal = bloqueOptimo.getInicio();
            int tamanoProceso = proceso.getAlcance();
            int tamanoOriginalBloque = bloqueOptimo.getTamano();

            // Ocupamos el bloque óptimo encontrado con el proceso actual
            bloqueOptimo.ocupar(proceso);
            proceso.setDireccionBase(inicioOriginal);

            // Ajustamos el tamaño del bloque ocupado al tamaño exacto del proceso.
            bloques.set(indiceOptimo, new Particion(bloqueOptimo.getNumero(), inicioOriginal, tamanoProceso));
            // Como es una partición nueva recién creada, vuelve a estar libre por defecto. 
            // Se debe volver a llamar para ocuparla inmediatamente para que guarde el proceso:
            bloques.get(indiceOptimo).ocupar(proceso);

            // Si el hueco era más grande que el proceso, el residuo se convierte en un nuevo bloque libre
            if (menorResiduo > 0) {
                int inicioResiduo = inicioOriginal + tamanoProceso;
                Particion bloqueResiduo = new Particion(contadorIdParticion++, inicioResiduo, menorResiduo);

                // Lo inyectamos en la lista exactamente después del proceso que se acaba de alojar
                bloques.add(indiceOptimo + 1, bloqueResiduo);
            }

            double porcentajeUtil = ((double) tamanoProceso / tamanoProceso) * 100;

            System.out.println("\n>>> MEMORIA DINÁMICA: PROCESO ADMITIDO (BEST-FIT) <<<");
            System.out.println("MEMORIA [DINAMICA]: '" + proceso.nombreProceso + "' -> Base Física: " + proceso.getDireccionBase());
            System.out.printf(" Bloque Creado: Partición #%d [Tamaño exacto: %d celdas]\n", bloqueOptimo.getNumero(), tamanoProceso);
            System.out.printf(" Espacio Útil:   %d celdas (%.1f%% de aprovechamiento interno)\n", tamanoProceso, porcentajeUtil);
            System.out.printf(" Fragmentación:  0 celdas internas. Residuo remanente externo: %d celdas.\n", menorResiduo);
            System.out.println("-----------------------------------------------------------------------");

            return true;
        }

        //Si no hay hueco suficirnte
        System.out.println("❌ KERNEL DINÁMICO: No se encontró ningún hueco disponible o suficientemente grande para "
                + proceso.nombreProceso + " (Requiere: " + proceso.getAlcance() + " celdas)");
        return false;
    }

    /**
     * Libera el bloque ocupado por un proceso y ejecuta la COALESCENCIA (fusión
     * de vecinos libres).
     */
    public void liberarBloquePorProceso(BCP proceso) {
        if (proceso == null) {
            return;
        }

        int indiceTarget = -1;

        //Localizamos el bloque del proceso
        for (int i = 0; i < bloques.size(); i++) {
            Particion b = bloques.get(i);
            if (!b.isLibre() && b.getProceso() != null && b.getProceso().id == proceso.id) {
                b.liberar();
                indiceTarget = i;
                Errors.logInfo("Memoria Dinamica: Bloque de proceso '" + proceso.nombreProceso + "' liberado con exito.");
                break;
            }
        }

        if (indiceTarget == -1) {
            return;
        }

        // Algo coalencia (Fusión de bloques libres contiguos)
        System.out.println("KERNEL: Ejecutando analisis de coalescencia en memoria...");

        // Fusión con el vecino de ADELANTE (Siguiente en la lista)
        if (indiceTarget + 1 < bloques.size()) {
            Particion vecinoAdelante = bloques.get(indiceTarget + 1);
            if (vecinoAdelante.isLibre()) {
                Particion actual = bloques.get(indiceTarget);
                int nuevoTamano = actual.getTamano() + vecinoAdelante.getTamano();

                // Rediseñamos el bloque actual absorbiendo al de adelante
                bloques.set(indiceTarget, new Particion(actual.getNumero(), actual.getInicio(), nuevoTamano));
                bloques.remove(indiceTarget + 1); // Removemos el duplicado colapsado
                System.out.println("Fusion exitosa con el bloque libre posterior.");
            }
        }

        // Fusión con el vecino de ATRÁS (Anterior en la lista)
        if (indiceTarget - 1 >= 0) {
            Particion vecinoAtras = bloques.get(indiceTarget - 1);
            if (vecinoAtras.isLibre()) {
                Particion actual = bloques.get(indiceTarget);
                int nuevoTamano = vecinoAtras.getTamano() + actual.getTamano();

                // El vecino de atrás absorbe el tamaño del actual
                bloques.set(indiceTarget - 1, new Particion(vecinoAtras.getNumero(), vecinoAtras.getInicio(), nuevoTamano));
                bloques.remove(indiceTarget); // Removemos el nodo actual colapsado
                System.out.println("Fusion exitosa con el bloque libre anterior.");
            }
        }
    }

    public List<Particion> getBloques() {
        return this.bloques;
    }

    public void mostrarEstadoMemoria() {
        System.out.println("\n====== MONITOREO DE MAPA DE MEMORIA DINÁMICA ======");
        for (Particion b : bloques) {
            System.out.println(
                    "Bloque N°: " + b.getNumero()
                    + " | Rangos: [" + b.getInicio() + " - " + b.getFin() + "]"
                    + " | Tamaño: " + b.getTamano()
                    + " | Estado: " + (b.isLibre() ? "LIBRE (Hueco)" : "OCUPADO")
                    + " | Proceso: " + (b.getProceso() != null ? b.getProceso().nombreProceso : "-")
            );
        }
        System.out.println("====================================================\n");
    }
}

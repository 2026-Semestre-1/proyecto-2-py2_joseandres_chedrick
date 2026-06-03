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
 *
 * @author joses
 */
public class MemoriaFija {

    private final Memoria memoriaReal;
    private final List<Particion> particiones;

    public MemoriaFija(Memoria memoriaReal) {
        this.memoriaReal = memoriaReal;
        this.particiones = new ArrayList<>();
    }


    /**
     * Crea particiones de idéntico tamaño (Fragmentación Fija Estática).
     */
 
    public void crearParticionesVariables(int[] tamanosParticiones) {
        particiones.clear();
        int inicioUsuario = memoriaReal.getInicioUsuario();
        int tamanoTotalRAM = memoriaReal.getTamanoTotal();
        int espacioDisponible = tamanoTotalRAM - inicioUsuario;

        System.out.println("\n=======================================================");
        System.out.println("  INICIALIZANDO MAPA DE PARTICIONES VARIABLES");
        System.out.println("   Dirección base de usuario (Post-Kernel): " + inicioUsuario);
        System.out.println("   Espacio físico disponible para usuario: " + espacioDisponible + " celdas.");
        System.out.println("=======================================================");

        int inicioActual = inicioUsuario;
        int numParticion = 1;
        int sumaTamaños = 0;

        for (int tamano : tamanosParticiones) {
            //  Verifica si esta partición desborda el límite físico de la RAM
            if (inicioActual >= tamanoTotalRAM) {
                // lamada a tu utilitario de errores para Logs de consola
                Errors.logError("La particion #" + numParticion + " fue omitida. Motivo: El direccionamiento supero el limite fisico de la RAM (" + tamanoTotalRAM + ").");

                // Lanzamiento del popup visual preventivo directo al usuario
                Errors.mostrarErrorVisual(
                        null,
                        "Error de Desbordamiento de Memoria",
                        "No se pudo crear la Partición #" + numParticion + ".\n"
                        + "Motivo: El direccionamiento físico superó el límite de la RAM (" + tamanoTotalRAM + " celdas).\n"
                        + "Se detuvo la carga de particiones adicionales."
                );
                break; // Detiene la creación de más particiones fuera de rango
            }

            int tamanoAjustado = tamano;
            //  Si la partición actual sobresale parcialmente del límite, se recorta
            if (inicioActual + tamano > tamanoTotalRAM) {
                tamanoAjustado = tamanoTotalRAM - inicioActual;

                Errors.logError("Particion #" + numParticion + " truncada automaticamente de " + tamano + " a " + tamanoAjustado + " celdas.");

                Errors.mostrarAdvertenciaVisual(
                        null,
                        "Ajuste de Configuración Forzado",
                        "¡Atención!\nLa Partición #" + numParticion + " requería " + tamano + " celdas, pero excedía el espacio físico restante.\n"
                        + "Se truncó automáticamente a " + tamanoAjustado + " celdas para no desbordar el simulador."
                );
            }

            int finFisico = inicioActual + tamanoAjustado - 1;

            Particion nueva = new Particion(numParticion, inicioActual, tamanoAjustado);
            particiones.add(nueva);

            System.out.printf(" Partición #%02d | Rango Celdas: [%03d - %03d] | Tamaño Slot: %3d celdas\n",
                    numParticion, inicioActual, finFisico, tamanoAjustado);

            inicioActual += tamanoAjustado;
            sumaTamaños += tamanoAjustado;
            numParticion++;
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("  MEMORIA: Se crearon " + particiones.size() + " particiones variables.");
        System.out.println("  Espacio total asignado a usuarios: " + sumaTamaños + " celdas.");
        System.out.println("  Próxima celda libre teórica: " + inicioActual + " (RAM Total: " + tamanoTotalRAM + ")");
        System.out.println("=======================================================\n");
    }

    /**
     * Crea particiones de idéntico tamaño (Fragmentación Fija Estática
     * Simétrica).
     */
    public void crearParticionesIguales(int cantidadParticiones) {
        particiones.clear();

        int tamanoTotalRAM = memoriaReal.getTamanoTotal();
        int inicioUsuario = memoriaReal.getInicioUsuario();
        int espacioDisponible = tamanoTotalRAM - inicioUsuario;

        System.out.println("\n=== INICIALIZANDO MEMORIA FIJA IGUAL ===");
        System.out.println("SISTEMA: RAM Total: " + tamanoTotalRAM + " celdas.");
        System.out.println("SISTEMA: Espacio reservado Kernel: " + inicioUsuario + " celdas.");
        System.out.println("SISTEMA: Espacio disponible Usuario: " + espacioDisponible + " celdas.");
        System.out.println("----------------------------------------");

        // Cantidad inválida (menor o igual a cero)
        if (cantidadParticiones <= 0) {
            Errors.logError("Cantidad invalida de particiones fijas (" + cantidadParticiones + "). Ajustando a 1.");

            Errors.mostrarErrorVisual(
                    null,
                    "Error de Configuración",
                    "La cantidad de particiones solicitada (" + cantidadParticiones + ") no es válida.\n"
                    + "El simulador creará 1 partición única por defecto utilizando todo el espacio libre."
            );
            cantidadParticiones = 1;
        }

        // Dividimos ese espacio en partes exactamente iguales
        int tamanoEstandar = espacioDisponible / cantidadParticiones;

        // Demasiadas particiones para el tamaño de la RAM
        if (tamanoEstandar == 0) {
            Errors.logError("Demasiadas particiones solicitadas (" + cantidadParticiones + ") para el espacio disponible (" + espacioDisponible + "). Truncando.");

            Errors.mostrarAdvertenciaVisual(
                    null,
                    "Espacio Insuficiente para Particiones",
                    "¡Atención!\nSe solicitaron " + cantidadParticiones + " particiones, pero solo hay " + espacioDisponible + " celdas disponibles.\n"
                    + "El hardware se ha reajustado al límite máximo: se crearán " + espacioDisponible + " particiones de 1 celda cada una."
            );
            tamanoEstandar = 1;
            cantidadParticiones = espacioDisponible; // Reajuste forzado al límite máximo de slots individuales
        }

        int inicioActual = inicioUsuario;

        // Creamos e inyectamos los slots simétricos en la lista
        for (int i = 0; i < cantidadParticiones; i++) {
            // Por la pérdida de decimales en la división entera (/), la última partición podría perder celdas.
            // Forzamos a que la última partición absorba el residuo exacto para cubrir la RAM a la perfección.
            if (i == cantidadParticiones - 1) {
                tamanoEstandar = tamanoTotalRAM - inicioActual;
            }

            Particion nueva = new Particion(i + 1, inicioActual, tamanoEstandar);
            particiones.add(nueva);

            int finActual = inicioActual + tamanoEstandar - 1;
            System.out.println("HARDWARE: Partición #" + nueva.getNumero()
                    + " [Inicio: " + inicioActual
                    + " | Fin: " + finActual
                    + " | Tamaño: " + tamanoEstandar + "] -> ESTADO: LIBRE");

            inicioActual += tamanoEstandar;
        }
        System.out.println("========================================\n");
    }

    public boolean asignarProceso(BCP proceso, String algoritmo) {
        Particion particionSeleccionada = null;

        // --- ESCENARIO 1: FIJA IGUAL ---
        // No requiere evaluar menor desperdicio, agarra la primera libre secuencial
        if (algoritmo != null && algoritmo.equalsIgnoreCase("FIJA_IGUAL")) {
            for (Particion p : particiones) {
                if (p.isLibre() && p.getTamano() >= proceso.getAlcance()) {
                    particionSeleccionada = p;
                    break; // Rompemos inmediatamente para optimizar
                }
            }
        } // --- ESCENARIO 2: BEST-FIT (Para FIJA_VARIABLE) ---
        else {
            int menorDesperdicio = Integer.MAX_VALUE;
            for (Particion p : particiones) {
                if (p.isLibre() && p.getTamano() >= proceso.getAlcance()) {
                    int desperdicioActual = p.getTamano() - proceso.getAlcance();
                    if (desperdicioActual < menorDesperdicio) {
                        menorDesperdicio = desperdicioActual;
                        particionSeleccionada = p;
                    }
                }
            }
        }

        if (particionSeleccionada != null) {
            // Reservamos físicamente el espacio
            particionSeleccionada.ocupar(proceso);
            proceso.setDireccionBase(particionSeleccionada.getInicio());

            // Captura de datos físicos del hardware y del proceso
            int tamanoSlot = particionSeleccionada.getTamano();
            int tamanoProceso = proceso.getAlcance();
            int fragInterna = tamanoSlot - tamanoProceso;

            // (Casteo a double para evitar división entera de Java)
            double porcentajeUtil = ((double) tamanoProceso / tamanoSlot) * 100;
            double porcentajeDesperdicio = ((double) fragInterna / tamanoSlot) * 100;

            String tagModo = (algoritmo != null) ? algoritmo.toUpperCase() : "SISTEMA";

            System.out.println("\n >>> PROCESO ADMITIDO EN HARDWARE <<<");
            System.out.println("MEMORIA [" + tagModo + "]: '" + proceso.nombreProceso + "' -> Base Física: " + proceso.getDireccionBase());
            System.out.printf(" Slot Asignado: Partición #%d [Tamaño total: %d celdas]\n", particionSeleccionada.getNumero(), tamanoSlot);
            System.out.printf(" Espacio Útil:   %d celdas (%.1f%% del bloque aprovechado por el código)\n", tamanoProceso, porcentajeUtil);
            System.out.printf(" DESPERDICIO:    %d celdas (%.1f%% de Fragmentación Interna generada inmediatamente)\n", fragInterna, porcentajeDesperdicio);
            System.out.println("-----------------------------------------------------------------------");

            return true; // Asignación exitosa
        }

        // Si el ciclo no encontró ningún bloque compatible
        System.out.println("KERNEL: No se encontró partición fija disponible o suficientemente grande para admitir "
                + proceso.nombreProceso + " (Requiere: " + proceso.getAlcance() + " celdas)");

        return false;
    }
    // =========================================================
    // 3. LIBERACIÓN Y CONTROL
    // =========================================================

    public void liberarParticionPorProceso(BCP proceso) {
        if (proceso == null) {
            return;
        }

        for (Particion p : particiones) {
            if (!p.isLibre() && p.getProceso() != null && p.getProceso().id == proceso.id) {
                p.liberar();
                Errors.logInfo("Administrador Memoria: Partición " + p.getNumero() + " liberada con éxito.");
                return;
            }
        }
    }

    public List<Particion> getParticiones() {
        return this.particiones;
    }

    public void mostrarEstadoParticiones() {
        System.out.println("\n====== MONITOREO DE PARTICIONES (MEMORIA FIJA) ======");
        for (Particion p : particiones) {
            System.out.println(
                    "Partición N°: " + p.getNumero()
                    + " | Límites: [" + p.getInicio() + " - " + p.getFin() + "]"
                    + " | Tamaño Slot: " + p.getTamano()
                    + " | Estado: " + (p.isLibre() ? "LIBRE" : "OCUPADA")
                    + " | PID asignado: " + (p.getProceso() != null ? p.getProceso().id : "-")
                    + " | Frag. Interna: " + p.getFragmentacionInterna()
            );
        }
    }
}

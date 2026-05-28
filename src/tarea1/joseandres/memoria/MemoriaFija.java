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
 * -> Administrador de memoria fija.
 *  Maneja:
 *  -   Particiones iguales
 *  -   Particiones de distinto tamaño
 *  -   Fragmentación interna
 *
 */
public class MemoriaFija {

    private Memoria memoriaReal;
    private List<Particion> particiones; //Llamo a particiones

    public MemoriaFija(Memoria memoriaReal) {
        this.memoriaReal = memoriaReal;
        this.particiones = new ArrayList<>();
    }

    // =========================================================
    // ================= PARTICIONES IGUALES ===================
    // =========================================================
    public void crearParticionesIguales(int cantidadParticiones) {

        particiones.clear();

        int inicioUsuario = memoriaReal.getInicioUsuario();

        int tamañoDisponible
                = memoriaReal.getTamanoTotal() - inicioUsuario;

        int tamañoParticion
                = tamañoDisponible / cantidadParticiones;

        int inicioActual = inicioUsuario;

        for (int i = 0; i < cantidadParticiones; i++) {

            Particion nueva = new Particion(
                    i,
                    inicioActual,
                    tamañoParticion
            );

            particiones.add(nueva);

            inicioActual += tamañoParticion;
        }

        Errors.logInfo("Particiones fijas iguales creadas correctamente.");
    }

    // =========================================================
    // ============ PARTICIONES DIFERENTE TAMAÑO ===============
    // =========================================================
    public void crearParticionesPersonalizadas(int[] tamaños) {

        particiones.clear();

        int inicioActual = memoriaReal.getInicioUsuario();

        for (int i = 0; i < tamaños.length; i++) {

            Particion nueva = new Particion(
                    i,
                    inicioActual,
                    tamaños[i]
            );

            particiones.add(nueva);

            inicioActual += tamaños[i];
        }

        Errors.logInfo("Particiones personalizadas creadas correctamente.");
    }

    // =========================================================
    // ================= ASIGNAR PROCESO =======================
    // =========================================================
    public boolean asignarProceso(BCP proceso, String[] instrucciones) {

        for (Particion p : particiones) {

            if (p.puedeAsignar(proceso)) {

                // ocupa ls partición
                p.ocupar(proceso);

                // guarda dirección base
                proceso.setDireccionBase(p.getInicio());

                // escribimos las instrucciones
                for (int i = 0; i < instrucciones.length; i++) {

                    memoriaReal.escribirSeguro(
                            p.getInicio() + i,
                            instrucciones[i],
                            p.getInicio(),
                            p.getTamaño()
                    );
                }

                Errors.logInfo(
                        "Proceso "
                        + proceso.nombreProceso
                        + " asignado a partición "
                        + p.getNumero()
                );

                return true;
            }
        }

        Errors.logError(
                "No existe partición libre para el proceso "
                + proceso.nombreProceso
        );

        return false;
}

    // =========================================================
    // ================= LIBERAR PARTICIÓN =====================
    // =========================================================
    public void liberarProceso(BCP proceso) {

        for (Particion p : particiones) {

            if (!p.isLibre()
                    &&p.getProceso() != null
                    && p.getProceso().id == proceso.id) {

                // limpiar memoria visualmente
                for (int i = 0; i < p.getTamaño(); i++) {

                    memoriaReal.liberarCelda(
                            p.getInicio() + i
                    );
                }

                p.liberar();

                Errors.logInfo(
                        "Partición "
                        + p.getNumero()
                        + " liberada."
                );

                return;
            }
        }
    }

    // =========================================================
    // ================= MOSTRAR PARTICIONES ===================
    // =========================================================
    public void mostrarEstadoParticiones() {

        System.out.println("\n====== PARTICIONES ======");

        for (Particion p : particiones) {

            System.out.println(
                    "Partición: " + p.getNumero()
                    + " | Inicio: " + p.getInicio()
                    + " | Tamaño: " + p.getTamaño()
                    + " | Estado: " + (p.isLibre() ? "LIBRE" : "OCUPADA")
                    + " | PID: "
                    + (p.getProceso() != null
                        ? p.getProceso().id
                        : "LIBRE")
                    + " | Frag. Interna: " + p.getFragmentacionInterna()
            );
        }
    }

    // =========================================================
    // ======================= GETTERS =========================
    // =========================================================
    public List<Particion> getParticiones() {
        return particiones;
    }
}
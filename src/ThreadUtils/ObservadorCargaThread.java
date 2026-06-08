/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ThreadUtils;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.SwingUtilities;

/**
 *
 * @author chedr
 */
public class ObservadorCargaThread extends Thread implements RelojObserver {
    // Tupla: { nombre(String), ruta(String), tiempoLlegada(int) }
    private final List<Object[]> procesos;
    private final LinkedBlockingQueue<Integer> cola = new LinkedBlockingQueue<>();
 
    // Callback para interactuar con la UI/kernel desde el hilo correcto
    private final ProcesoCargaCallback callback;
 
    public ObservadorCargaThread(List<Object[]> procesos, ProcesoCargaCallback callback) {
        this.procesos = procesos;
        this.callback = callback;
    }
 
    // Llamado por el cronómetro (hilo del cronómetro)
    @Override
    public void onCambio(int segundoActual) {
        cola.offer(segundoActual);
    }
 
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int segundoActual = cola.take();
 
                System.out.println("  >> El reloj cambió a: "
                        + CronometroThread.formatear(segundoActual));
 
                // Recorrer lista y cargar los que corresponden a este segundo
                for (Object[] tupla : procesos) {
                    int tiempoLlegada = (int) tupla[2];
 
                    if (tiempoLlegada == segundoActual) {
                        String nombre = (String) tupla[0];
                        String ruta   = (String) tupla[1];
 
                        System.out.println("  ✔ Cargando proceso: " + nombre
                                + " (llegada en " + CronometroThread.formatear(tiempoLlegada) + ")");
 
                        // Ejecutar en el Event Dispatch Thread para tocar la UI/kernel de forma segura
                        SwingUtilities.invokeLater(() -> callback.cargar(nombre, ruta));
                    }
                }
 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

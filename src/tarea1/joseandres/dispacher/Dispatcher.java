/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.dispacher;

import tarea1.joseandres.proceso.BCP;
import tarea1.joseandres.memoria.Memoria;

/**
 *
 * @author joses
 * Cambiamos a estado ejecucion, calculamos
 */
public class Dispatcher {

    private Memoria ram;
    private final int TAMANO_BLOQUE_BCP = 14; // Espacio fijo por proceso

    public Dispatcher(Memoria ram) {
        this.ram = ram;
    }

   public void despachar(BCP proceso, int cpuId) {
        if (proceso == null || proceso.PC >= proceso.alcance) return;

        proceso.estado = "EJECUCION (CPU " + cpuId + ")";
        proceso.cpuAsignada = cpuId;

        if (proceso.tiempoInicio == 0) {
            proceso.tiempoInicio = System.currentTimeMillis();
        }

        actualizarBcpEnKernel(proceso);
        System.out.println("DISPATCHER: Proceso " + proceso.nombreProceso + " asignado a CPU " + cpuId);
    }

    public void actualizarBcpEnKernel(BCP p) {
        int offset = p.id * TAMANO_BLOQUE_BCP;

        long duracion;
        if (p.tiempoInicio == 0) {
            duracion = 0;
        } else if (p.tiempoFinal == 0) {
            duracion = (System.currentTimeMillis() - p.tiempoInicio) / 1000;
        } else {
            duracion = (p.tiempoFinal - p.tiempoInicio) / 1000;
        }

        ram.escribirEnKernel(offset + 0, "PID:" + p.id);
        ram.escribirEnKernel(offset + 1, p.nombreProceso);
        ram.escribirEnKernel(offset + 2, p.estado);
        ram.escribirEnKernel(offset + 3, "PC:" + p.PC);
        ram.escribirEnKernel(offset + 4, "IR:" + p.IR);
        ram.escribirEnKernel(offset + 5, "AC:" + p.AC);
        ram.escribirEnKernel(offset + 6, "AX:" + p.AX);
        ram.escribirEnKernel(offset + 7, "BX:" + p.BX);
        ram.escribirEnKernel(offset + 8, "CX:" + p.CX);
        ram.escribirEnKernel(offset + 9, "DX:" + p.DX);
        ram.escribirEnKernel(offset + 10, "TI:" + p.tiempoInicio);
        ram.escribirEnKernel(offset + 11, "TF:" + p.tiempoFinal);
        ram.escribirEnKernel(offset + 12, "TE:" + duracion + "s");
        ram.escribirEnKernel(offset + 13, "CICLOS:" + p.ciclosConsumidos);
    }
}

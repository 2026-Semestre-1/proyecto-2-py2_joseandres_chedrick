/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.proceso;

/**
 *
 * @author chedr
 */
public class EstadisticasProceso {
    // Datos de identidad
    public final String nombre;
    public final int    id;
    public final String estado;
 
    // Registros
    public final int    PC, AC, AX, BX, CX, DX, SP;
    public final String IR;
    public final String stack;
 
    // Memoria
    public final int direccionBase;
    public final int alcance;
 
    // Tiempos absolutos (segundos desde inicio del sistema)
    public final long tLlegada;
    public final long tInicio;
    public final long tFinal;
 
    // Métricas calculadas
    public final long   tr;     // Tiempo de respuesta: tInicio - tLlegada
    public final long   ts;     // Turnaround:          tFinal  - tLlegada
    public final double ratio;  // Tr / Ts
 
    public EstadisticasProceso(
            String nombre, int id, String estado,
            int PC, String IR, int AC,
            int AX, int BX, int CX, int DX,
            int SP, String stack,
            int direccionBase, int alcance,
            long tLlegada, long tInicio, long tFinal,
            long tr, long ts, double ratio) {
 
        this.nombre  = nombre;
        this.id      = id;
        this.estado  = estado;
        this.PC      = PC;
        this.IR      = IR;
        this.AC      = AC;
        this.AX      = AX;
        this.BX      = BX;
        this.CX      = CX;
        this.DX      = DX;
        this.SP      = SP;
        this.stack   = stack;
        this.direccionBase = direccionBase;
        this.alcance       = alcance;
        this.tLlegada = tLlegada;
        this.tInicio  = tInicio;
        this.tFinal   = tFinal;
        this.tr    = tr;
        this.ts    = ts;
        this.ratio = ratio;
    }
}

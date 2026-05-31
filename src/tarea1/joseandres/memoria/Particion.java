/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.memoria;

import tarea1.joseandres.proceso.BCP;

/**
 *
 * @author joses
 * Se encarga de saber cual proceso es cual junto a su direccion
 */
public class Particion {

    private final int numero;      // ID del proceso
    private final int inicio;      // Dirección física inicial en RAM
    private final int tamano;      // Tamaño total de la partición en celdas
    private boolean libre;
    private BCP proceso;
    private int fragmentacionInterna;

    public Particion(int numero, int inicio, int tamano) {
        this.numero = numero; 
        this.inicio = inicio; 
        this.tamano = tamano;
        this.libre = true;
        this.proceso = null;
        this.fragmentacionInterna = 0;
    }

    /**
     * Ocupa la partición con un proceso y calcula la fragmentación interna.
     */
    public void ocupar(BCP proceso) {
        this.libre = false;
        this.proceso = proceso;
        // Fragmentación = Tamaño de la ranura - Lo que realmente requiere el programa (.asm)
        this.fragmentacionInterna = this.tamano - proceso.getAlcance();
    }

    /**
     * Libera la partición limpiando sus referencias de control.
     */
    public void liberar() {
        this.libre = true;
        this.proceso = null;
        this.fragmentacionInterna = 0;
    }

    // =========================================================
    // GETTERS Y SETTERS
    // =========================================================
    public int getNumero() {
        return numero;
    }

    public int getInicio() {
        return inicio;
    }

    public int getFin() {
        return inicio + tamano - 1;
    }

    public int getTamano() {
        return tamano;
    }

    public boolean isLibre() {
        return libre;
    }

    public BCP getProceso() {
        return proceso;
    }

    public int getFragmentacionInterna() {
        return fragmentacionInterna;
    }

    @Override
    public String toString() {
        return "Particion{"
                + "numero=" + numero
                + ", inicio=" + inicio
                + ", fin=" + getFin()
                + ", tamano=" + tamano
                + ", libre=" + libre
                + ", PID=" + (proceso != null ? proceso.id : "Ninguno")
                + ", fragInterna=" + fragmentacionInterna
                + '}';
    }
}

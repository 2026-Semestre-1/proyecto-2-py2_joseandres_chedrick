/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.memoria;
import tarea1.joseandres.proceso.BCP;
/**
 *
 * Representa una partición fija en RAM.
 *
 */
public class Particion {

    private int numero;

    // Dirección inicial en RAM
    private int inicio;

    // Tamaño total de la partición
    private int tamaño;

    // Estado de la partición
    private boolean libre;

    // Proceso asignado
    private BCP proceso;

    // Fragmentación interna
    private int fragmentacionInterna;

    /**
     * Constructor
     */
    public Particion(int numero, int inicio, int tamaño) {

        this.numero = numero;
        this.inicio = inicio;
        this.tamaño = tamaño;

        this.libre = true;

        this.proceso = null;

        this.fragmentacionInterna = 0;
    }

    /**
     * Ocupa la partición con un proceso
     */
    public void ocupar(BCP proceso) {

        this.libre = false;

        this.proceso = proceso;

        // Fragmentación interna
        this.fragmentacionInterna =
                this.tamaño - proceso.getAlcance();
    }

    /**
     * Libera la partición
     */
    public void liberar() {

        this.libre = true;

        this.proceso = null;

        this.fragmentacionInterna = 0;
    }

    /**
     * Verifica si una dirección pertenece a esta partición
     */
    public boolean contieneDireccion(int direccion) {

        return direccion >= inicio
                && direccion < (inicio + tamaño);
    }

    /**
     * Verifica si el proceso cabe en la partición
     */
    public boolean puedeAsignar(BCP proceso) {

        return libre
                && proceso.getAlcance() <= tamaño;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public int getNumero() {
        return numero;
    }

    public int getInicio() {
        return inicio;
    }

    public int getFin() {
        return inicio + tamaño - 1;
    }

    public int getTamaño() {
        return tamaño;
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

    // =========================================================
    // SETTERS
    // =========================================================

    public void setLibre(boolean libre) {
        this.libre = libre;
    }

    public void setProceso(BCP proceso) {
        this.proceso = proceso;
    }

    public void setFragmentacionInterna(int fragmentacionInterna) {
        this.fragmentacionInterna = fragmentacionInterna;
    }

    @Override
    public String toString() {

        return "Particion{"
                + "numero=" + numero
                + ", inicio=" + inicio
                + ", fin=" + getFin()
                + ", tamaño=" + tamaño
                + ", libre=" + libre
                + ", proceso="
                + (proceso != null ? proceso.id : "NONE")
                + ", fragmentacionInterna="
                + fragmentacionInterna
                + '}';
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.uitls;
import javax.swing.JOptionPane;
import java.awt.Component;

/**
 *
 * @author joses
 */
public class Errors {
    
    public static void mostrarErrorVisual(Component parent, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(
                parent,
                mensaje,
                titulo,
                JOptionPane.ERROR_MESSAGE
        );
    }
    
    public static void mostrarAdvertenciaVisual(Component parent, String titulo, String mensaje) {
        JOptionPane.showMessageDialog(
                parent,
                mensaje,
                titulo,
                JOptionPane.WARNING_MESSAGE
        );
    }

    public static void logError(String mensaje) {
        System.err.println("ERROR: " + mensaje);
    }

    public static void logInfo(String mensaje) {
        System.out.println("INFO: " + mensaje);
    }
    
}

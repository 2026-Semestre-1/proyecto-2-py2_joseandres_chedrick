/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ThreadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author chedr
 */
public class CronometroThread extends Thread {
 
    private int segundos = 0;
    private final List<RelojObserver> observers = new ArrayList<>();
 
    public void agregarObserver(RelojObserver observer) {
        observers.add(observer);
    }
 
    private void notificarObservers() {
        for (RelojObserver obs : observers) {
            obs.onCambio(segundos);
        }
    }
 
    public static String formatear(int seg) {
        return String.format("%02d:%02d", seg / 60, seg % 60);
    }
 
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println("⏱ " + formatear(segundos));
            notificarObservers();
            segundos++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
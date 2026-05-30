package tarea1.joseandres;

import javax.swing.SwingUtilities;
import tarea1.joseandres.interfaz.SimuladorGUI;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import tarea1.joseandres.kernel.Kernel;

public class Tarea1JoseAndres {

    public static void main(String[] args) {
        
        // 1. Valores por defecto por si falla la lectura del JSON
        int ramSize = 512;
        int kernelPerc = 20;
        int diskSize = 512;
        int diskIndexPerc = 10;
        
        // Valores por defecto para el Proyecto 2 (Memoria Fija)
        String tipoMemoria = "FIJA_IGUAL";
        int cantidadParticiones = 4;
        String algoritmoAsignacion = "BEST_FIT";
        List<Integer> tamanosParticiones = new ArrayList<>();
        // Inicialización de respaldo por defecto
        tamanosParticiones.add(40);
        tamanosParticiones.add(80);
        tamanosParticiones.add(120);
        tamanosParticiones.add(168);

        try {
            String contenidoJson = Files.readString(Paths.get("src/tarea1/joseandres/config/config.json"));

            // Extracción de hardware clásico
            ramSize = extraerValorEntero(contenidoJson, "ram_size");
            diskSize = extraerValorEntero(contenidoJson, "disk_size");
            kernelPerc = extraerValorEntero(contenidoJson, "kernel_reserve_percentage");
            diskIndexPerc = extraerValorEntero(contenidoJson, "disk_index_percentage");

            // NUEVA EXTRACCIÓN: Datos de memoria fija
            tipoMemoria = extraerValorString(contenidoJson, "tipo");
            cantidadParticiones = extraerValorEntero(contenidoJson, "cantidad_particiones");
            algoritmoAsignacion = extraerValorString(contenidoJson, "algoritmo_asignacion");
            tamanosParticiones = extraerListaEnteros(contenidoJson, "tamanos_particiones");

            System.out.println("Config cargada correctamente desde config.json con soporte de particiones.");

        } catch (Exception e) {
            System.err.println("Error leyendo config.json, se usarán valores por defecto: " + e.getMessage());
        }

        // =====================================================================
        // 🛠️ SOLUCIÓN: CONVERSIÓN Y PASO DE PARÁMETROS DINÁMICOS AL KERNEL
        // =====================================================================
        
        // Convertimos la List<Integer> leída del JSON a un int[] primitivo que espera el Kernel
        int[] arregloTamanos = new int[tamanosParticiones.size()];
        for (int i = 0; i < tamanosParticiones.size(); i++) {
            arregloTamanos[i] = tamanosParticiones.get(i);
        }

        // Instanciamos el Kernel pasándole las variables dinámicas extraídas de tu JSON
        Kernel kernel = new Kernel(
            ramSize, 
            diskSize, 
            kernelPerc, 
            diskIndexPerc, 
            tipoMemoria, 
            cantidadParticiones, 
            arregloTamanos);

        // 3. Lanzamos la interfaz pasándole el Kernel listo y configurado
        SwingUtilities.invokeLater(() -> {
            SimuladorGUI ventana = new SimuladorGUI(kernel);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    /**
     * Ejemplo: "ram_size": 512
     */
    private static int extraerValorEntero(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int inicioClave = json.indexOf(patron);

        if (inicioClave == -1) {
            throw new IllegalArgumentException("No se encontró la clave entera: " + clave);
        }

        int inicioDosPuntos = json.indexOf(":", inicioClave);
        int i = inicioDosPuntos + 1;
        StringBuilder numero = new StringBuilder();

        while (i < json.length()) {
            char c = json.charAt(i);

            if (Character.isDigit(c)) {
                numero.append(c);
            } else if (numero.length() > 0) {
                break;
            }
            i++;
        }

        if (numero.length() == 0) {
            throw new IllegalArgumentException("No se pudo extraer un entero para la clave: " + clave);
        }

        return Integer.parseInt(numero.toString());
    }

    /**
     * Ejemplo: "tipo": "FIJA_VARIABLE"
     */
    private static String extraerValorString(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int inicioClave = json.indexOf(patron);

        if (inicioClave == -1) {
            throw new IllegalArgumentException("No se encontró la clave string: " + clave);
        }

        int inicioDosPuntos = json.indexOf(":", inicioClave);
        
        // Buscamos la primera comilla que abre el valor de texto
        int comillaApertura = json.indexOf("\"", inicioDosPuntos);
        if (comillaApertura == -1) {
            throw new IllegalArgumentException("No se encontró comilla de apertura para: " + clave);
        }

        // Buscamos la comilla que cierra el valor de texto
        int comillaCierre = json.indexOf("\"", comillaApertura + 1);
        if (comillaCierre == -1) {
            throw new IllegalArgumentException("No se encontró comilla de cierre para: " + clave);
        }

        return json.substring(comillaApertura + 1, comillaCierre).trim();
    }

    /**
     * Extrae un arreglo plano de enteros encapsulado en [ ... ]
     * Ejemplo: "tamanos_particiones": [40, 80, 120, 168]
     */
    private static List<Integer> extraerListaEnteros(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int inicioClave = json.indexOf(patron);

        if (inicioClave == -1) {
            throw new IllegalArgumentException("No se encontró la lista: " + clave);
        }

        int inicioCorchete = json.indexOf("[", inicioClave);
        int finCorchete = json.indexOf("]", inicioCorchete);

        if (inicioCorchete == -1 || finCorchete == -1) {
            throw new IllegalArgumentException("Formato de arreglo inválido para: " + clave);
        }

        // Extraemos lo que hay dentro de los corchetes: "40, 80, 120, 168"
        String subContenido = json.substring(inicioCorchete + 1, finCorchete);
        String[] elementos = subContenido.split(",");

        List<Integer> resultado = new ArrayList<>();
        for (String elemento : elementos) {
            String limpio = elemento.trim();
            if (!limpio.isEmpty()) {
                resultado.add(Integer.parseInt(limpio));
            }
        }

        return resultado;
    }
}
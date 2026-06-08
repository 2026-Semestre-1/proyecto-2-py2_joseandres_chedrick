package tarea1.joseandres;

import javax.swing.SwingUtilities;
import tarea1.joseandres.interfaz.SimuladorGUI;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import tarea1.joseandres.kernel.Kernel;

public class Tarea1JoseAndres {

    public static void main(String[] args) {

        // Valores por defecto por si falla la lectura del JSON
        int ramSize           = 512;
        int kernelPerc        = 20;
        int diskSize          = 512;
        int diskIndexPerc     = 10;
        int cantidadCpus      = 2; 
        int tamanoPagina      = 16;

        String tipoMemoria        = "FIJA_IGUAL";
        int cantidadParticiones   = 4;
        String algoritmoAsignacion = "BEST_FIT";
        List<Integer> tamanosParticiones = new ArrayList<>();
        tamanosParticiones.add(40);
        tamanosParticiones.add(80);
        tamanosParticiones.add(120);
        tamanosParticiones.add(168);

        try {
            String contenidoJson = Files.readString(Paths.get("src/tarea1/joseandres/config/config.json"));

            // Hardware
            ramSize       = extraerValorEntero(contenidoJson, "ram_size");
            diskSize      = extraerValorEntero(contenidoJson, "disk_size");
            kernelPerc    = extraerValorEntero(contenidoJson, "kernel_reserve_percentage");
            diskIndexPerc = extraerValorEntero(contenidoJson, "disk_index_percentage");
            tamanoPagina  = extraerValorEntero(contenidoJson, "page_size"); 

            // Sistema
            cantidadCpus = extraerValorEntero(contenidoJson, "cantidad_cpus");

            // Memoria
            tipoMemoria           = extraerValorString(contenidoJson, "tipo");
            cantidadParticiones   = extraerValorEntero(contenidoJson, "cantidad_particiones");
            algoritmoAsignacion   = extraerValorString(contenidoJson, "algoritmo_asignacion");
            tamanosParticiones    = extraerListaEnteros(contenidoJson, "tamanos_particiones");

            System.out.println("Config cargada — cantidad_cpus: " + cantidadCpus + " | page_size: " + tamanoPagina);

        } catch (Exception e) {
            System.err.println("Error leyendo config.json, se usarán valores por defecto: " + e.getMessage());
        }

        // mínimo 1, máximo 4 (límite visual de la GUI)
        cantidadCpus = Math.max(1, Math.min(4, cantidadCpus));

        int[] arregloTamanos = new int[tamanosParticiones.size()];
        for (int i = 0; i < tamanosParticiones.size(); i++) {
            arregloTamanos[i] = tamanosParticiones.get(i);
        }

        //  Ahora pasamos tamanoPagina al constructor del Kernel para que configure la paginación dinámica
        Kernel kernel = new Kernel(
            ramSize,
            diskSize,
            kernelPerc,
            diskIndexPerc,
            tipoMemoria,
            cantidadParticiones,
            arregloTamanos,
            tamanoPagina,
            cantidadCpus
        );

        // Pasamos cantidadCpus a la GUI para que preseleccione el ComboBox
        final int cpusDesdeJson = cantidadCpus;
        SwingUtilities.invokeLater(() -> {
            SimuladorGUI ventana = new SimuladorGUI(kernel, cpusDesdeJson);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    /** Ejemplo: "ram_size": 512 */
    private static int extraerValorEntero(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int inicioClave = json.indexOf(patron);
        if (inicioClave == -1)
            throw new IllegalArgumentException("No se encontró la clave entera: " + clave);

        int inicioDosPuntos = json.indexOf(":", inicioClave);
        int i = inicioDosPuntos + 1;
        StringBuilder numero = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) numero.append(c);
            else if (numero.length() > 0) break;
            i++;
        }
        if (numero.length() == 0)
            throw new IllegalArgumentException("No se pudo extraer un entero para: " + clave);
        return Integer.parseInt(numero.toString());
    }

    /** Ejemplo: "tipo": "FIJA_VARIABLE" */
    private static String extraerValorString(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int inicioClave = json.indexOf(patron);
        if (inicioClave == -1)
            throw new IllegalArgumentException("No se encontró la clave string: " + clave);

        int inicioDosPuntos = json.indexOf(":", inicioClave);
        int comillaApertura = json.indexOf("\"", inicioDosPuntos);
        if (comillaApertura == -1)
            throw new IllegalArgumentException("Sin comilla de apertura para: " + clave);
        int comillaCierre = json.indexOf("\"", comillaApertura + 1);
        if (comillaCierre == -1)
            throw new IllegalArgumentException("Sin comilla de cierre para: " + clave);
        return json.substring(comillaApertura + 1, comillaCierre).trim();
    }

    /** Ejemplo: "tamanos_particiones": [40, 80, 120, 168] */
    private static List<Integer> extraerListaEnteros(String json, String clave) {
        String patron = "\"" + clave + "\"";
        int inicioClave = json.indexOf(patron);
        if (inicioClave == -1)
            throw new IllegalArgumentException("No se encontró la lista: " + clave);

        int inicioCorchete = json.indexOf("[", inicioClave);
        int finCorchete    = json.indexOf("]", inicioCorchete);
        if (inicioCorchete == -1 || finCorchete == -1)
            throw new IllegalArgumentException("Formato de arreglo inválido para: " + clave);

        String subContenido = json.substring(inicioCorchete + 1, finCorchete);
        String[] elementos  = subContenido.split(",");
        List<Integer> resultado = new ArrayList<>();
        for (String elemento : elementos) {
            String limpio = elemento.trim();
            if (!limpio.isEmpty()) resultado.add(Integer.parseInt(limpio));
        }
        return resultado;
    }
}
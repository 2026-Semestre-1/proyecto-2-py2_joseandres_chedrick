package tarea1.joseandres;

import javax.swing.SwingUtilities;
import tarea1.joseandres.interfaz.SimuladorGUI;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;

public class Tarea1JoseAndres {

    public static void main(String[] args) {
        // 1. Generamos escenarios de prueba
        generarArchivoPrueba("exito.asm", getContenidoExito());
        generarArchivoPrueba("error_div.asm", getContenidoErrorDiv());
        generarArchivoPrueba("overflow.asm", getContenidoOverflow());

        // 2. Valores por defecto por si falla la lectura del JSON
        int ramSize = 512;
        int kernelPerc = 20;
        int diskSize = 512;
        int diskIndexPerc = 10;
        

        try { 
            String contenidoJson = Files.readString(Paths.get("src/tarea1/joseandres/config/config.json"));

            ramSize = extraerValorEntero(contenidoJson, "ram_size");
            diskSize = extraerValorEntero(contenidoJson, "disk_size");
            kernelPerc = extraerValorEntero(contenidoJson, "kernel_reserve_percentage");
            diskIndexPerc = extraerValorEntero(contenidoJson, "disk_index_percentage");

            System.out.println("Config cargada correctamente desde config.json");

        } catch (Exception e) {
            System.err.println("Error leyendo config.json, se usarán valores por defecto: " + e.getMessage());
        }

        // 3. Lanzar interfaz con valores dinámicos
        final int finalRamSize = ramSize;
        final int finalKernelPerc = kernelPerc;
        final int finalDiskSize = diskSize;
        final int finalDiskIndexPerc = diskIndexPerc;

        SwingUtilities.invokeLater(() -> {
            SimuladorGUI ventana = new SimuladorGUI(
                finalRamSize,
                finalKernelPerc,
                finalDiskSize,
                finalDiskIndexPerc
            );
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    /**
     * Extrae un entero de un JSON simple buscando una clave.
     * Ejemplo: "ram_size": 512
     */
    private static int extraerValorEntero(String json, String clave) {
    String patron = "\"" + clave + "\"";
    int inicioClave = json.indexOf(patron);

    if (inicioClave == -1) {
        throw new IllegalArgumentException("No se encontró la clave: " + clave);
    }

    int inicioDosPuntos = json.indexOf(":", inicioClave);
    if (inicioDosPuntos == -1) {
        throw new IllegalArgumentException("No se encontró ':' para la clave: " + clave);
    }

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

    public static void generarArchivoPrueba(String nombre, String contenido) {
        try (FileWriter fw = new FileWriter(nombre)) {
            fw.write(contenido);
            fw.flush();
            System.out.println("ARCHIVO GENERADO: " + nombre);
        } catch (IOException e) {
            System.err.println("Error al generar " + nombre + ": " + e.getMessage());
        }
    }

    private static String getContenidoExito() {
        return "MOV AX, 8\n"
             + "MOV BX, 2\n"
             + "LOAD AX\n"
             + "ADD BX\n"
             + "STORE CX\n"
             + "MOV DX, 4\n"
             + "SUB DX\n"
             + "STORE AX";
    }

    private static String getContenidoErrorDiv() {
        return "MOV AX, 10\n"
             + "MOV BX, 0\n"
             + "LOAD AX\n"
             + "DIV BX\n"
             + "STORE CX";
    }

    private static String getContenidoOverflow() {
        StringBuilder sb = new StringBuilder();
        sb.append("MOV AX, 1\n");
        for (int i = 0; i < 400; i++) {
            sb.append("INC AX\n");
        }
        return sb.toString();
    }
}
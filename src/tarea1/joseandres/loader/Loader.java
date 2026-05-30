package tarea1.joseandres.loader;

import tarea1.joseandres.disco.Disco;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author joses
 */
public class Loader {

    private Disco disco;
    private int punteroCarga;

    private static final Map<String, String> OPCODES = new HashMap<>();
    private static final Map<String, String> OPCODES_INVERSO = new HashMap<>();

    // Unificamos la inicialización estática en un solo bloque limpio
    static {
        OPCODES.put("LOAD", "00001");
        OPCODES.put("STORE", "00010");
        OPCODES.put("MOV", "00011");
        OPCODES.put("SUB", "00100");
        OPCODES.put("ADD", "00101");
        OPCODES.put("INC", "00110");
        OPCODES.put("DEC", "00111");
        OPCODES.put("JMP", "01000");
        OPCODES.put("CMP", "01001");
        OPCODES.put("JE", "01010");
        OPCODES.put("JNE", "01011");
        OPCODES.put("PUSH", "01100");
        OPCODES.put("POP", "01101");
        OPCODES.put("PARAM", "01110");
        OPCODES.put("INT", "01111");
        OPCODES.put("SWAP", "10000");

        // Mapeo inverso automático para la GUI de forma inmediata
        for (Map.Entry<String, String> entry : OPCODES.entrySet()) {
            OPCODES_INVERSO.put(entry.getValue(), entry.getKey());
        }
    }

    public Loader(Disco disco) {
        this.disco = disco;
    }

    public int cargaArchivoADisco(String ruta) {
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            System.err.println("ERROR: No se encontró el archivo .asm en " + ruta);
            return -1;
        }

        this.punteroCarga = disco.getSiguienteEspacioDisponible();
        int direccionInicioPrograma = this.punteroCarga;
        int contadorInstrucciones = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith(";")) {
                    continue;
                }

                String[] partes = linea.toUpperCase().split("\\s+");
                String command = partes[0].replace(",", "");

                if (OPCODES.containsKey(command)) {
                    String opcodeTraducido = OPCODES.get(command);
                    String[] partesSucias = linea.toUpperCase().replace(",", " ").split("\\s+");

                    StringBuilder sb = new StringBuilder();
                    sb.append(opcodeTraducido);
                    for (int i = 1; i < partesSucias.length; i++) {
                        sb.append(" ").append(partesSucias[i]);
                    }

                    if (disco.escribir(punteroCarga, sb.toString().trim())) {
                        punteroCarga++;
                        contadorInstrucciones++;
                    } else {
                        return -1;
                    }
                }
            }

            disco.escribir(punteroCarga, "00000");
            punteroCarga++;
            contadorInstrucciones++;

            disco.registrarEnIndice(archivo.getName(), direccionInicioPrograma, contadorInstrucciones);

            return contadorInstrucciones;

        } catch (Exception e) {
            System.err.println("Error crítico al cargar a disco: " + e.getMessage());
            return -1;
        }
    }

    public static String traducirInstruccion(String instruccion) {
        if (instruccion == null || instruccion.equals("00000")) {
            return instruccion;
        }

        String[] partes = instruccion.split("\\s+");
        if (partes.length == 0) {
            return instruccion;
        }

        String opcode = partes[0];
        String nombre = OPCODES_INVERSO.get(opcode);

        if (nombre == null) {
            return instruccion;
        }

        StringBuilder resultado = new StringBuilder(nombre);
        for (int i = 1; i < partes.length; i++) {
            resultado.append(" ").append(partes[i]);
        }

        return resultado.toString();
    }
}

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
    private Disco disco; // Ahora usamos Disco en lugar de Memoria. (.asm -> Disco -> memoria)
    private int punteroCarga;
    
   private static final Map<String, String> OPCODES_INVERSO = new HashMap<>(); //Para la gui
    private static final Map<String, String> OPCODES;
    //K:V -> Actualice el loader para que este sea el encargado a traducir. Usando hashmaping puedo hacerlo directo buscando su valor.
    static {
        OPCODES = new HashMap<>();
        OPCODES.put("LOAD",  "00001");
        OPCODES.put("STORE", "00010");
        OPCODES.put("MOV",   "00011");
        OPCODES.put("SUB",   "00100");
        OPCODES.put("ADD",   "00101");
        //Instrucciones de e/s
        OPCODES.put("INC",   "00110");
        OPCODES.put("DEC",   "00111");
        OPCODES.put("JMP",   "01000");//no depende de GUI ni archivos reales
        OPCODES.put("CMP",   "01001");//no depende de GUI ni archivos reales
        OPCODES.put("JE",    "01010");//no depende de GUI ni archivos reales
        OPCODES.put("JNE",   "01011");//no depende de GUI ni archivos reales
        OPCODES.put("PUSH",  "01100");//no depende de GUI ni archivos reales
        OPCODES.put("POP",   "01101");//no depende de GUI ni archivos reales
        OPCODES.put("PARAM", "01110");//no depende de GUI ni archivos reales
        OPCODES.put("INT",   "01111");
        OPCODES.put("SWAP",  "10000");
        
    }

    public Loader(Disco disco){
        this.disco = disco;
    }
    
    static {
        for (Map.Entry<String, String> entry : OPCODES.entrySet()) {
            OPCODES_INVERSO.put(entry.getValue(), entry.getKey());
        }
    }
    
    //Validamos que si no detecta una llave valida, se detenga y asi el CPU no se come un SintaxisError.
    public int cargaArchivoADisco(String ruta) {
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            System.err.println("ERROR: No se encontró el archivo .asm en " + ruta);
            return -1;
        }

        // Le pedimos al disco el siguiente espacio libre después del índice
        this.punteroCarga = disco.getSiguienteEspacioDisponible();
        int direccionInicioPrograma = this.punteroCarga;
        int contadorInstrucciones = 0; // Para medir el tamaño real

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith(";")) continue;

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
                        contadorInstrucciones++; // Incrementamos el tamaño real
                    } else {
                        return -1;
                    }
                }
            }
            
            // Fin de programa en disco
            disco.escribir(punteroCarga, "00000");
            
            // REGISTRA EN EL ÍNDICE DEL DISCO
            // Ahora pasamos el nombre, la dirección de inicio y el tamaño acumulado
            disco.registrarEnIndice(archivo.getName(), direccionInicioPrograma, contadorInstrucciones);
            
            return contadorInstrucciones; // Devolvemos el tamaño exacto
            
        } catch (Exception e) {
            System.err.println("Error crítico al cargar a disco: " + e.getMessage());
            return -1;
        }
    }
    
 
    
    //Metodod para traducir las instrucciones
    public static String traducirInstruccion(String instruccion) {
        if (instruccion == null || instruccion.equals("00000")) {
            return instruccion;
        }

        String[] partes = instruccion.split("\\s+");

        if (partes.length == 0) return instruccion;

        String opcode = partes[0];

        String nombre = OPCODES_INVERSO.get(opcode);

        if (nombre == null) {
            return instruccion; // si no reconoce, la deja igual
        }

        // reconstruimos la instrucción
        StringBuilder resultado = new StringBuilder(nombre);

        for (int i = 1; i < partes.length; i++) {
            resultado.append(" ").append(partes[i]);
        }

        return resultado.toString();
    }

}
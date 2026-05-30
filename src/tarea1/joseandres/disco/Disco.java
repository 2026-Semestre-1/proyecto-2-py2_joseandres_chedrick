/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarea1.joseandres.disco;

/**
 *
 * @author joses
 */
public class Disco {

    private String[] almacenamiento; //Donde se guardaran todos los datos del disco
    private final int tamanoTotal;
    private int espacioIndice;
    private int punteroIndiceActual = 0; // Para saber donde poner el próximo nombre

    public Disco(int tamano, int porcentajeIndice) {
        this.tamanoTotal = tamano;

        this.espacioIndice = (int) (tamano * (porcentajeIndice / 100.0));
        this.almacenamiento = new String[tamanoTotal];

        // Nuestro indice debe ser par para manejar parejas (Nombre, Posición)
        if (this.espacioIndice % 2 != 0) {
            this.espacioIndice++;
        }

        this.almacenamiento = new String[tamanoTotal];

        // Inicializamos con "00000" (5 bits para Proyecto 1)
        for (int i = 0; i < tamanoTotal; i++) {
            almacenamiento[i] = "00000";
        }
    }

    // Getters para la GUI
    public String leer(int posicion) {
        return (posicion >= 0 && posicion < tamanoTotal) ? almacenamiento[posicion] : "00000";
    }

    public int getEspacioIndice() {
        return espacioIndice;
    }

    public int getTamanoTotal() {
        return tamanoTotal;
    }

    //Registyramos el formato de los archivos
    public boolean registrarEnIndice(String nombre, int direccionInicio, int tamano) {
        // Ya no saltamos de 2 en 2, usamos el espacio disponible secuencialmente
        if (punteroIndiceActual < espacioIndice) {
            // Formato solicitado: "nombre; inicio - fin"
            String entradaIndice = nombre.replace(".asm", "") + "; "
                    + direccionInicio + " - " + (direccionInicio + tamano - 1);

            almacenamiento[punteroIndiceActual] = entradaIndice;
            punteroIndiceActual++; // Solo avanzamos una pos
            return true;
        }
        return false;
    }

    // Ajustamos el método escribir para permitir guardar justo después del índice
    public boolean escribir(int posicion, String instruccion) {
        // Validamos que no intente escribir encima de un índice ya registrado
        if (posicion >= espacioIndice && posicion < tamanoTotal) {
            almacenamiento[posicion] = instruccion;
            return true;
        }
        return false;
    }

    public int getSiguienteEspacioDisponible() {
        // buscamos la primera celda vacia 
        // después de donde termina el índice maximo permitido
        for (int i = espacioIndice; i < tamanoTotal; i++) {
            if (almacenamiento[i].equals("00000")) {
                return i;
            }
        }
        return -1; //Disco lleno
    }

    //Procesamso el string para leer la entrada a disco (f1;i/f)  
    public int getDireccionInicioArchivo(String nombreArchivo) {
        String nombreLimpio = nombreArchivo.replace(".asm", "");
        for (int i = 0; i < espacioIndice; i++) {
            if (almacenamiento[i].contains(nombreLimpio)) {
                // "exito; 4 - 8" -> split(";") -> [1] es " 4 - 8" -> split("-") -> [0] es " 4 "
                String[] partes = almacenamiento[i].split(";");
                String rango = partes[1].trim();
                return Integer.parseInt(rango.split(" - ")[0].trim());
            }
        }
        return -1;
    }

    //==========================================================================================
    //==================================UTILS==========================================
    //==========================================================================================
    public int crearArchivoEnDisco(String nombreArchivo) {
        // Verificamos espacio en el índice
        if (punteroIndiceActual >= espacioIndice) {
            return -1; // esta lleno
        }

        // Buscar siguiente espacio libre en el area del datos
        int direccionInicio = getSiguienteEspacioDisponible();
        if (direccionInicio == -1) {
            return -1; // disco lleno
        }

        // Reservamos al menos 1 celda para el archivo recien creado
        almacenamiento[direccionInicio] = "";

        // Registramos en ind con formato: nombre; inicio - fin
        String entradaIndice = nombreArchivo + "; " + direccionInicio + " - " + direccionInicio;
        almacenamiento[punteroIndiceActual] = entradaIndice;
        punteroIndiceActual++;

        return direccionInicio;
    }

    public int buscarIndiceArchivo(String nombreArchivo) {
        String nombreLimpio = nombreArchivo.replace(".asm", "");

        for (int i = 0; i < espacioIndice; i++) {
            String entrada = almacenamiento[i];

            if (entrada != null
                    && !entrada.equals("00000")
                    && entrada.startsWith(nombreLimpio + ";")) {
                return i;
            }
        }

        return -1;
    }

    public int obtenerInicioDesdeIndice(int indice) {
        String entrada = almacenamiento[indice]; // ejemplo: archivo_1; 52 - 52
        String rango = entrada.split(";")[1].trim();
        String inicio = rango.split(" - ")[0].trim();

        return Integer.parseInt(inicio);
    }

    public int obtenerFinDesdeIndice(int indice) {
        String entrada = almacenamiento[indice];
        String rango = entrada.split(";")[1].trim();
        String fin = rango.split(" - ")[1].trim();

        return Integer.parseInt(fin);
    }

    public boolean actualizarRangoArchivo(int indice, int inicio, int fin) {
        if (indice < 0 || indice >= espacioIndice) {
            return false;
        }

        String entrada = almacenamiento[indice]; // archivo_1; 52 - 52
        String nombre = entrada.split(";")[0].trim();

        almacenamiento[indice] = nombre + "; " + inicio + " - " + fin;
        return true;
    }

    public boolean escribirContenidoArchivo(String nombreArchivo, String contenido) {
        int indice = buscarIndiceArchivo(nombreArchivo);

        if (indice == -1) {
            return false; // archivo no existe
        }

        int inicio = obtenerInicioDesdeIndice(indice);
        int fin = obtenerFinDesdeIndice(indice);

        int nuevaPosicion;

        // Si el archivo esta  vacio, escribimos en su primera celda reservada
        if (almacenamiento[inicio].equals("")) {
            nuevaPosicion = inicio;
        } else {
            nuevaPosicion = fin + 1;
        }

        if (nuevaPosicion >= tamanoTotal) {
            return false; // disco lleno
        }

        if (!almacenamiento[nuevaPosicion].equals("00000") && !almacenamiento[nuevaPosicion].equals("")) {
            return false; // no hay espacio contiguo
        }

        almacenamiento[nuevaPosicion] = contenido;

        actualizarRangoArchivo(indice, inicio, nuevaPosicion);

        return true;
    }

    public String leerContenidoArchivo(String nombreArchivo) {
        int indice = buscarIndiceArchivo(nombreArchivo);

        if (indice == -1) {
            return null;
        }

        int inicio = obtenerInicioDesdeIndice(indice);
        int fin = obtenerFinDesdeIndice(indice);

        StringBuilder contenido = new StringBuilder();

        for (int i = inicio; i <= fin; i++) {
            String dato = almacenamiento[i];

            if (dato != null && !dato.equals("00000") && !dato.equals("")) {
                if (contenido.length() > 0) {
                    contenido.append(" ");
                }

                contenido.append(dato);
            }
        }

        return contenido.toString();
    }
    
    public boolean eliminarArchivo(String nombreArchivo) {
        int indice = buscarIndiceArchivo(nombreArchivo);

        if (indice == -1) {
            return false;
        }

        int inicio = obtenerInicioDesdeIndice(indice);
        int fin = obtenerFinDesdeIndice(indice);

        for (int i = inicio; i <= fin; i++) {
            almacenamiento[i] = "00000";
        }

        almacenamiento[indice] = "00000";

        return true;
    }

    public boolean existeArchivo(String nombreArchivo) {
        return buscarIndiceArchivo(nombreArchivo) != -1;
    }

}

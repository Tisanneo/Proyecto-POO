/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/*
 * GestorArchivos.java
 * Guarda todas las partidas en un archivo centralizado "historial_partidas.txt"
 * pero usando los nombres reales de los jugadores.
 */
package proyecto.poo.servidor;

import java.io.*;
import java.util.Date;

public class GestorArchivos {
    // Archivo único centralizado
    private static final String ARCHIVO = "historial_partidas.txt";

    public static void guardarPartida(String ganador, String perdedor, String dificultad, int turnos) {
        // Usamos try-with-resources para cerrar el archivo automáticamente
        try (FileWriter fw = new FileWriter(ARCHIVO, true); // 'true' activa el modo append (agregar al final)
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            // Escribimos el bloque de información
            out.println("------------------------------------------------");
            out.println("FECHA: " + new Date());
            out.println("DIFICULTAD: " + dificultad);
            // Aquí se guardarán los nombres reales que envía Partida.java
            out.println("GANADOR: " + ganador);
            out.println("PERDEDOR: " + perdedor);
            out.println("MOVIMIENTOS TOTALES (Turnos): " + turnos);
            out.println("------------------------------------------------");
            
            System.out.println("✅ Registro guardado exitosamente en " + ARCHIVO);
            
        } catch (IOException e) {
            System.err.println("Error al escribir en el historial: " + e.getMessage());
        }
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto.poo.servidor;

import java.io.*;
import java.util.Date;

public class GestorArchivos {
    private static final String ARCHIVO = "historial_partidas.txt";

    public static void guardarPartida(String ganador, String perdedor, String dificultad, int turnos) {
        try (FileWriter fw = new FileWriter(ARCHIVO, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.println("------------------------------------------------");
            out.println("FECHA: " + new Date());
            out.println("DIFICULTAD: " + dificultad);
            out.println("GANADOR: " + ganador);
            out.println("PERDEDOR: " + perdedor);
            out.println("MOVIMIENTOS TOTALES (Turnos): " + turnos);
            out.println("------------------------------------------------");
            
            System.out.println("✅ Historial detallado guardado.");
            
        } catch (IOException e) {
            System.err.println("Error guardando historial: " + e.getMessage());
        }
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto.poo.servidor;

import java.io.*;
import java.util.Date;

public class GestorArchivos {
    private static final String ARCHIVO = "historial_partidas.txt";

    public static void guardarPartida(String ganador, String perdedor) {
        try (FileWriter fw = new FileWriter(ARCHIVO, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            // Formato: FECHA | GANADOR | PERDEDOR
            out.println(new Date() + " | Ganador: " + ganador + " | Perdedor: " + perdedor);
            System.out.println("Partida guardada en historial.");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
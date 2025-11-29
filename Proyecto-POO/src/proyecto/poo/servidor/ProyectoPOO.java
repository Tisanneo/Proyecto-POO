package proyecto.poo.servidor;

import java.io.*;
import java.net.*;
import java.util.*;
import proyecto.poo.comunes.*;

public class ProyectoPOO {
    private static final int PUERTO = 4444;
  
    private static List<Socket> espera = new ArrayList<>();
    private static List<ObjectOutputStream> esperaOut = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Servidor Buscaminas Iniciado en puerto " + PUERTO);
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            while(true) {
                Socket s = server.accept();
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                out.flush();
                
                System.out.println("Nuevo cliente conectado: " + s.getInetAddress());
                
                espera.add(s);
                esperaOut.add(out);
                
                out.writeObject(new Mensaje("INFO", "Conectado. Esperando oponente..."));
                
     
                if(espera.size() >= 2) {
                    Socket j1 = espera.remove(0);
                    Socket j2 = espera.remove(0);
                    ObjectOutputStream out1 = esperaOut.remove(0);
                    ObjectOutputStream out2 = esperaOut.remove(0);
                    
                   
                    Partida p = new Partida(j1, j2, out1, out2);
                    new Thread(p).start();
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
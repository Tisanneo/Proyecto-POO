package proyecto.poo.servidor;

import java.io.*;
import java.net.Socket;
import java.awt.Point;
import proyecto.poo.comunes.*;

public class Partida implements Runnable {
    private Socket s1, s2;
    private ObjectOutputStream out1, out2;
    private ObjectInputStream in1, in2;
    private Tablero tableroJ1, tableroJ2;
    private boolean turnoJ1 = true;
    
 
    private String nombreJ1 = "Jugador 1";
    private String nombreJ2 = "Jugador 2";
    
    private boolean activa = true;          
    private boolean juegoTerminado = false; 
    
    private boolean j1ListoParaReiniciar = false;
    private boolean j2ListoParaReiniciar = false;
    
    private String dificultadActual = "Desconocida";
    private int contadorMovimientos = 0;

    public Partida(Socket s1, Socket s2, ObjectOutputStream out1, ObjectOutputStream out2) {
        this.s1 = s1; this.s2 = s2;
        this.out1 = out1; this.out2 = out2;
    }

    @Override
    public void run() {
        try {
            in1 = new ObjectInputStream(s1.getInputStream());
            in2 = new ObjectInputStream(s2.getInputStream());

            Mensaje loginJ1 = (Mensaje) in1.readObject();
            if (loginJ1.getTipo().equals("LOGIN")) {
                nombreJ1 = (String) loginJ1.getContenido();
            }

            Mensaje loginJ2 = (Mensaje) in2.readObject();
            if (loginJ2.getTipo().equals("LOGIN")) {
                nombreJ2 = (String) loginJ2.getContenido();
            }
            
            System.out.println("Partida iniciada: " + nombreJ1 + " vs " + nombreJ2);
            // ---------------------------------------------

            out1.writeObject(new Mensaje("CONFIG_DIFICULTAD", null));
            out2.writeObject(new Mensaje("INFO", "Esperando que el anfitrión (" + nombreJ1 + ") elija la dificultad..."));
            out1.flush(); out2.flush(); 
            
            Mensaje resp = (Mensaje) in1.readObject();
            String dificultad = (String) resp.getContenido();
            
            iniciarJuego(dificultad);

            new Thread(() -> loopJugador(in1, true)).start();
            new Thread(() -> loopJugador(in2, false)).start();

        } catch(Exception e) {
            e.printStackTrace();
            activa = false;
        }
    }
    
    private void iniciarJuego(String dificultad) throws IOException {
        this.dificultadActual = dificultad; 
        this.contadorMovimientos = 0;       
        
        GeneradorTableros gen = new GeneradorTableros();
        Tablero[] tabs = gen.generarParTableros(dificultad);
        tableroJ1 = tabs[0];
        tableroJ2 = tabs[1];
        
        out1.reset();
        out1.writeObject(new Mensaje("INICIO", tableroJ1));
        
        out2.reset();
        out2.writeObject(new Mensaje("INICIO", tableroJ2));
        
        actualizarTurnos();
    }

    private void actualizarTurnos() throws IOException {
        if(!activa) return;
        out1.writeObject(new Mensaje("TURNO", turnoJ1));
        out2.writeObject(new Mensaje("TURNO", !turnoJ1));
        out1.flush(); out2.flush();
    }

    private void loopJugador(ObjectInputStream in, boolean esJ1) {
        try {
            while(activa) {
                Mensaje msj = (Mensaje) in.readObject();
                String tipo = msj.getTipo();
                
                if(tipo.equals("MOVIMIENTO")) {
                    procesarJugada((Point) msj.getContenido(), esJ1);
                }
                else if(tipo.equals("BANDERA")) {
                   procesarBandera((Point) msj.getContenido(), esJ1);
                }
                else if(tipo.equals("REINICIAR")) {
                    procesarReinicio(esJ1);
                }
                else if(tipo.equals("RESPUESTA_DIF")) {
                    String nuevaDificultad = (String) msj.getContenido();
                    iniciarJuego(nuevaDificultad);
                }
            }
        } catch(Exception e) {
            if (activa) { 
                activa = false;
                juegoTerminado = true;
                notificarAbandono(!esJ1); 
            }
        }
    }

    private void notificarAbandono(boolean avisarAJ1) {
        try {
            ObjectOutputStream outDestino = avisarAJ1 ? out1 : out2;
            String mensaje = "Tu oponente ha abandonado la partida.\nLa sesión se cerrará.";
            outDestino.reset();
            outDestino.writeObject(new Mensaje("ABANDONO", mensaje));
            outDestino.flush();
        } catch (IOException ex) {}
    }

    private synchronized void procesarBandera(Point p, boolean esJ1) throws IOException {
        if(!activa || juegoTerminado || esJ1 != turnoJ1) return;
        
        Tablero t = esJ1 ? tableroJ1 : tableroJ2;
        t.toggleBandera(p.x, p.y);
        
        enviarActualizacionTableros();
    }

    private synchronized void procesarJugada(Point p, boolean esJ1) throws IOException {
        if(!activa || juegoTerminado || esJ1 != turnoJ1) return;
        
        Tablero t = esJ1 ? tableroJ1 : tableroJ2;
        Tablero.Celda c = t.getCelda(p.x, p.y);
        if (c.revelada || c.marcada) {
            return; 
        }

        contadorMovimientos++;
        boolean exploto = t.revelarCelda(p.x, p.y);

        if(exploto) {
            t.revelarTodo(); 
            try { enviarActualizacionTableros(); } catch (IOException e) {}
            
            String msgPerdedor = "¡BOOM! Has explotado una mina.\nHas PERDIDO.";
            String msgGanador  = "¡Tu oponente ha explotado una mina!\n¡Has GANADO!";

            
            if (esJ1) {
               
                out1.reset(); out1.writeObject(new Mensaje("GAMEOVER", msgPerdedor)); out1.flush();
                out2.reset(); out2.writeObject(new Mensaje("GAMEOVER", msgGanador)); out2.flush();
             
                GestorArchivos.guardarPartida(nombreJ2, nombreJ1, dificultadActual, contadorMovimientos);
            } else {
              
                out1.reset(); out1.writeObject(new Mensaje("GAMEOVER", msgGanador)); out1.flush();
                out2.reset(); out2.writeObject(new Mensaje("GAMEOVER", msgPerdedor)); out2.flush();
            
                GestorArchivos.guardarPartida(nombreJ1, nombreJ2, dificultadActual, contadorMovimientos);
            }
            juegoTerminado = true; 

        } else {
            if (t.esVictoria()) {
                String ganador = esJ1 ? nombreJ1 : nombreJ2;
                String perdedor = esJ1 ? nombreJ2 : nombreJ1;

                enviarAmbos(new Mensaje("GAMEOVER", "¡FELICIDADES!\n" + ganador + " ha despejado el campo."));
       
                GestorArchivos.guardarPartida(ganador, perdedor, dificultadActual, contadorMovimientos);
                juegoTerminado = true; 
            } else {
                enviarActualizacionTableros();
                turnoJ1 = !turnoJ1;
                actualizarTurnos();
            }
        }
    }

    private void enviarActualizacionTableros() throws IOException {
        out1.reset(); 
        out1.writeObject(new Mensaje("UPDATE", tableroJ1));
        out1.flush();
        out2.reset();
        out2.writeObject(new Mensaje("UPDATE", tableroJ2));
        out2.flush();
    }

    private void enviarAmbos(Mensaje m) throws IOException {
        out1.reset(); out1.writeObject(m); out1.flush();
        out2.reset(); out2.writeObject(m); out2.flush();
    }
    
    private synchronized void procesarReinicio(boolean esJ1) throws IOException {
        if(esJ1) j1ListoParaReiniciar = true;
        else j2ListoParaReiniciar = true;
        
        if(j1ListoParaReiniciar && j2ListoParaReiniciar) {
            j1ListoParaReiniciar = false; j2ListoParaReiniciar = false;
            
            juegoTerminado = false; 
            turnoJ1 = true;
            
            out1.reset();
            out1.writeObject(new Mensaje("CONFIG_DIFICULTAD", null));
            out1.flush();
            
            out2.reset();
            out2.writeObject(new Mensaje("INFO", "El anfitrión está eligiendo dificultad..."));
            out2.flush();
        }
    }
}
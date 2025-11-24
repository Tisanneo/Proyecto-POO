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
    
    // --- CAMBIO 1: Variable para mantener el hilo vivo ---
    private boolean activa = true;          // Mantiene la conexión viva
    private boolean juegoTerminado = false; // Bloquea el juego sin matar la conexión
    
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

            out1.writeObject(new Mensaje("CONFIG_DIFICULTAD", null));
            out2.writeObject(new Mensaje("INFO", "Esperando que el anfitrión elija la dificultad..."));
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
        this.dificultadActual = dificultad; // <--- GUARDAR ESTO
        this.contadorMovimientos = 0;       // <--- RESETEAR ESTO
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
            }
        } catch(Exception e) {
            activa = false; 
        }
    }

    private synchronized void procesarBandera(Point p, boolean esJ1) throws IOException {
        // --- CAMBIO 2: Bloqueamos si el juego terminó, pero seguimos escuchando ---
        if(!activa || juegoTerminado || esJ1 != turnoJ1) return;
        
        Tablero t = esJ1 ? tableroJ1 : tableroJ2;
        t.toggleBandera(p.x, p.y);
        
        enviarActualizacionTableros();
    }

    private synchronized void procesarJugada(Point p, boolean esJ1) throws IOException {
        // --- CAMBIO 3: Bloqueo por juego terminado ---
        if(!activa || juegoTerminado || esJ1 != turnoJ1) return;
        contadorMovimientos++;

        Tablero t = esJ1 ? tableroJ1 : tableroJ2;
        
        boolean exploto = t.revelarCelda(p.x, p.y);

        if(exploto) {
            // Mostrar la mina que explotó
            try { enviarActualizacionTableros(); } catch (IOException e) {}
            t.revelarTodo(); 
            
            String msgPerdedor = "¡BOOM! Has explotado una mina.\nHas PERDIDO.";
            String msgGanador  = "¡Tu oponente ha explotado una mina!\n¡Has GANADO!";

            if (esJ1) {
                // SI J1 EXPLOTA: J1 Pierde, J2 Gana
                out1.reset(); out1.writeObject(new Mensaje("GAMEOVER", msgPerdedor)); out1.flush();
                out2.reset(); out2.writeObject(new Mensaje("GAMEOVER", msgGanador)); out2.flush();
                
                // CORRECCIÓN AQUÍ: Poner los nombres directos
                GestorArchivos.guardarPartida("Jugador 2", "Jugador 1", dificultadActual, contadorMovimientos);
            } else {
                // SI J2 EXPLOTA: J2 Pierde, J1 Gana
                out1.reset(); out1.writeObject(new Mensaje("GAMEOVER", msgGanador)); out1.flush();
                out2.reset(); out2.writeObject(new Mensaje("GAMEOVER", msgPerdedor)); out2.flush();
                
                // CORRECCIÓN AQUÍ: Poner los nombres directos
                GestorArchivos.guardarPartida("Jugador 1", "Jugador 2", dificultadActual, contadorMovimientos);
            }
            
            juegoTerminado = true; 

        } else {
            if (t.esVictoria()) {
                String ganador = esJ1 ? "Jugador 1" : "Jugador 2";
                String perdedor = esJ1 ? "Jugador 2" : "Jugador 1";

                enviarAmbos(new Mensaje("GAMEOVER", "¡FELICIDADES!\n" + ganador + " ha despejado el campo."));
                GestorArchivos.guardarPartida(ganador, perdedor, dificultadActual, contadorMovimientos);
                
                // --- CAMBIO 5: PAUSAR, NO MATAR ---
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
            
            // --- CAMBIO 6: REVIVIR EL JUEGO ---
            juegoTerminado = false; 
            turnoJ1 = true;
            
            // Reiniciar con la misma dificultad o preguntar de nuevo (aquí por simplicidad reinicia en Principiante)
            // Si quisieras preguntar dificultad de nuevo, habría que hacer más cambios en el protocolo
            iniciarJuego("Principiante"); 
        }
    }
}
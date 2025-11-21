package proyecto.poo.servidor;
/*HOOOOOOOOOOOLAAAAAAAAAAAAAAAA*/
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
    private boolean activa = true;
    
    private boolean j1ListoParaReiniciar = false;
    private boolean j2ListoParaReiniciar = false;

    public Partida(Socket s1, Socket s2, ObjectOutputStream out1, ObjectOutputStream out2) {
        this.s1 = s1; this.s2 = s2;
        this.out1 = out1; this.out2 = out2;
    }

    @Override
    public void run() {
        try {
            in1 = new ObjectInputStream(s1.getInputStream());
            in2 = new ObjectInputStream(s2.getInputStream());

            // Preguntar dificultad
            out1.writeObject(new Mensaje("CONFIG_DIFICULTAD", null));
            out2.writeObject(new Mensaje("INFO", "Esperando que el anfitrión elija la dificultad..."));
            
            // IMPORTANTE: Flush para asegurar que el mensaje llega
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
        GeneradorTableros gen = new GeneradorTableros();
        Tablero[] tabs = gen.generarParTableros(dificultad);
        tableroJ1 = tabs[0];
        tableroJ2 = tabs[1];
        
        // RESETEAMOS antes de enviar para evitar caché
        out1.reset();
        out1.writeObject(new Mensaje("INICIO", tableroJ1));
        
        out2.reset();
        out2.writeObject(new Mensaje("INICIO", tableroJ2));
        
        actualizarTurnos();
    }

    private void actualizarTurnos() throws IOException {
        if(!activa) return;
        // No necesitamos reset para booleanos simples, pero no hace daño
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
        if(!activa || esJ1 != turnoJ1) return;
        
        Tablero t = esJ1 ? tableroJ1 : tableroJ2;
        t.toggleBandera(p.x, p.y);
        
        enviarActualizacionTableros();
    }

    private synchronized void procesarJugada(Point p, boolean esJ1) throws IOException {
        if(!activa || esJ1 != turnoJ1) return;

        Tablero t = esJ1 ? tableroJ1 : tableroJ2;
        
        boolean exploto = t.revelarCelda(p.x, p.y);

        if(exploto) {
            String ganador = esJ1 ? "Jugador 2" : "Jugador 1";
            //envia la actualizacion primero
            try {
                 enviarActualizacionTableros();
            } catch (IOException e) {
                  e.printStackTrace();
              }
            t.revelarTodo(); 
            enviarAmbos(new Mensaje("GAMEOVER", "¡BOOM! Has explotado una mina.\nGanador: " + ganador));
        }  else {
            // VERIFICAR VICTORIA
            if (t.esVictoria()) {
                String ganador = esJ1 ? "Jugador 1" : "Jugador 2";
                String perdedor = esJ1 ? "Jugador 2" : "Jugador 1";

                // Enviamos mensaje de Fin de juego
                enviarAmbos(new Mensaje("GAMEOVER", "¡FELICIDADES!\n" + ganador + " ha despejado el campo."));

                // GUARDAR EN HISTORIAL (Ver Paso 3)
                GestorArchivos.guardarPartida(ganador, perdedor); 

                activa = false; // Detener juego
            } else {
                // Si nadie gana ni pierde, seguimos jugando
                enviarActualizacionTableros();
                turnoJ1 = !turnoJ1;
                actualizarTurnos();
            }
        }
    }

    // --- MÉTODO NUEVO PARA ENVIAR TABLEROS CON RESET ---
    private void enviarActualizacionTableros() throws IOException {
        // ¡ESTA ES LA SOLUCIÓN AL PROBLEMA!
        // reset() borra la caché de objetos enviados, obligando a enviar los datos nuevos
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
            turnoJ1 = true;
            iniciarJuego("Principiante"); 
        }
    }
}
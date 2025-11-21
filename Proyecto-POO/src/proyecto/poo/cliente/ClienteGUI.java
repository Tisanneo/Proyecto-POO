package proyecto.poo.cliente;

import proyecto.poo.comunes.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ClienteGUI extends JFrame {
    private JLabel lblInfo;
    private JPanel panelJuego;
    private JButton[][] botones;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Tablero tablero;
    private boolean miTurno = false;
    
    private JLabel lblTiempo;           // Para mostrar el tiempo (Unidad 1)
    private javax.swing.Timer timer;    // El objeto cronómetro
    private int segundos = 0;           // Contador de segundos
    private String nombreJugador;       // Para identificación (Persistencia)
    
    // Colores estilo Buscaminas clásico
    private static final Color[] COLORES_NUMEROS = {
        null, Color.BLUE, new Color(0, 128, 0), Color.RED, 
        new Color(0, 0, 128), new Color(128, 0, 0), new Color(0, 128, 128),
        Color.BLACK, Color.GRAY
    };

    // --- CONSTRUCTOR ACTUALIZADO ---
    public ClienteGUI() {
        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch(Exception e){}

        // 1. LOGIN: Pedir nombre ANTES de mostrar la ventana (Crucial para Persistencia)
        nombreJugador = JOptionPane.showInputDialog(null, "Ingresa tu Nickname:", "Buscaminas UABC", JOptionPane.QUESTION_MESSAGE);
        if (nombreJugador == null || nombreJugador.trim().isEmpty()) {
            nombreJugador = "Jugador_" + (int)(Math.random() * 1000);
        }
        
        // Configuración básica de la ventana
        setTitle("Buscaminas Multijugador - Usuario: " + nombreJugador);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); // Usamos BorderLayout para organizar arriba/centro

        // 2. PANEL SUPERIOR (ESTADO + TIEMPO)
        // Creamos un panel especial para la info de arriba
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelSuperior.setBackground(new Color(240, 240, 240));

        // Etiqueta de Estado (Turno)
        lblInfo = new JLabel("Conectando al servidor...", SwingConstants.LEFT);
        lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        // Etiqueta de Tiempo (Nueva)
        lblTiempo = new JLabel("⏱ Tiempo: 00:00", SwingConstants.RIGHT);
        lblTiempo.setFont(new Font("Monospaced", Font.BOLD, 15));
        lblTiempo.setForeground(Color.DARK_GRAY);

        // Agregamos las etiquetas al panel superior
        panelSuperior.add(lblInfo, BorderLayout.WEST);   // Izquierda
        panelSuperior.add(lblTiempo, BorderLayout.EAST); // Derecha
        
        // Agregamos el panel superior a la ventana
        add(panelSuperior, BorderLayout.NORTH);

        // 3. PANEL DE JUEGO (CENTRO)
        panelJuego = new JPanel();
        panelJuego.setBackground(Color.LIGHT_GRAY);
        // Se usará GridLayout dinámico más tarde, al recibir el tablero
        add(panelJuego, BorderLayout.CENTER);

        // 4. CONFIGURACIÓN DEL TIMER (Reloj)
        // Se configura aquí, pero se inicia (start) cuando llega el mensaje "INICIO"
        timer = new javax.swing.Timer(1000, e -> {
            segundos++;
            long min = segundos / 60;
            long seg = segundos % 60;
            lblTiempo.setText(String.format("⏱ Tiempo: %02d:%02d", min, seg));
        });
        
        // Tamaño inicial (se ajustará solo con pack() después)
        setSize(600, 700);
        setLocationRelativeTo(null); // Centrar en pantalla
    }

    public void iniciarConexion() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 4444);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while(true) {
                    Mensaje msj = (Mensaje) in.readObject();
                    procesarMensaje(msj);
                }
            } catch(Exception e) {
                lblInfo.setText("Desconectado: " + e.getMessage());
            }
        }).start();
    }

    private void procesarMensaje(Mensaje msj) {
        SwingUtilities.invokeLater(() -> {
            String tipo = msj.getTipo();
            
            switch (tipo) {
                case "CONFIG_DIFICULTAD":
                    mostrarMenuDificultad();
                    break;
                case "INFO":
                    lblInfo.setText(msj.getContenido().toString());
                    break;
                case "INICIO":
                    // 1. Configurar reloj
                    segundos = 0; 
                    timer.start(); 
                    // 2. IMPORTANTE: ¡No hacer break aquí! Dejar que pase al UPDATE
                    //    o copiar la lógica de pintar abajo.
                    //    Lo más seguro es cargar el tablero aquí mismo:
                    this.tablero = (Tablero) msj.getContenido();
                    pintarTableroMejorado();
                    break;
                case "UPDATE":
                    this.tablero = (Tablero) msj.getContenido();
                    pintarTableroMejorado();
                    break;
                case "TURNO":
                    this.miTurno = (boolean) msj.getContenido();
                    lblInfo.setText(miTurno ? "🟢 TU TURNO" : "🔴 TURNO DEL OPONENTE");
                    lblInfo.setForeground(miTurno ? new Color(0, 100, 0) : Color.RED);
                    break;
                case "GAMEOVER":
                    timer.stop();
                    JOptionPane.showMessageDialog(this, msj.getContenido());
                    preguntarReinicio();
                    break;
            }
        });
    }

    private void mostrarMenuDificultad() {
        String[] opciones = {"Principiante", "Intermedio", "Avanzado"};
        int seleccion = JOptionPane.showOptionDialog(
            this, 
            "Eres el anfitrión. Selecciona la dificultad:", 
            "Configuración de Partida",
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, opciones, opciones[0]
        );
        
        String dif = (seleccion >= 0) ? opciones[seleccion] : "Principiante";
        try {
            out.writeObject(new Mensaje("RESPUESTA_DIF", dif));
            out.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    private void preguntarReinicio() {
         int resp = JOptionPane.showConfirmDialog(this, "¿Jugar otra vez?", "Fin", JOptionPane.YES_NO_OPTION);
         if(resp == JOptionPane.YES_OPTION) {
             try { out.writeObject(new Mensaje("REINICIAR", null)); } catch(Exception e){}
             lblInfo.setText("Esperando al otro jugador...");
         } else {
             System.exit(0);
         }
    }

    private void pintarTableroMejorado() {
        int f = tablero.getFilas();
        int c = tablero.getColumnas();
        
        if (botones == null || botones.length != f) {
            panelJuego.removeAll();
            panelJuego.setLayout(new GridLayout(f, c, 2, 2)); // Espacio entre celdas
            botones = new JButton[f][c];
            
            for(int i=0; i<f; i++) {
                for(int j=0; j<c; j++) {
                    JButton btn = crearBotonEstilizado(i, j);
                    botones[i][j] = btn;
                    panelJuego.add(btn);
                }
            }
            panelJuego.revalidate();
            this.pack(); // Ajustar ventana al tamaño del tablero
            this.setLocationRelativeTo(null);
        }

        // Actualizar estados
        for(int i=0; i<f; i++) {
            for(int j=0; j<c; j++) {
                actualizarBoton(botones[i][j], tablero.getCelda(i, j));
            }
        }
    }
    
    private JButton crearBotonEstilizado(int r, int c) {
        JButton btn = new JButton();
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(40, 40));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(!miTurno) return;
                try {
                    if(SwingUtilities.isRightMouseButton(e)) {
                        out.writeObject(new Mensaje("BANDERA", new Point(r, c)));
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        out.writeObject(new Mensaje("MOVIMIENTO", new Point(r, c)));
                    }
                    out.flush();
                } catch(Exception ex) { ex.printStackTrace(); }
            }
        });
        return btn;
    }
    
    private void actualizarBoton(JButton btn, Tablero.Celda celda) {
        if (celda.revelada) {
            btn.setEnabled(false);
            btn.setBackground(new Color(230, 230, 230)); // Gris muy claro
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            
            if (celda.esMina) {
                btn.setBackground(Color.RED);
                btn.setText("💣");
            } else if (celda.minasAlrededor > 0) {
                btn.setText(String.valueOf(celda.minasAlrededor));
                btn.setForeground(COLORES_NUMEROS[Math.min(celda.minasAlrededor, 8)]);
            } else {
                btn.setText(""); // Vacío (0)
            }
        } else {
            btn.setEnabled(true);
            btn.setBackground(new Color(180, 180, 180)); // Gris "sin pulsar"
            btn.setText(celda.marcada ? "🚩" : "");
            btn.setForeground(Color.RED); // Color de la bandera
        }
    }
}
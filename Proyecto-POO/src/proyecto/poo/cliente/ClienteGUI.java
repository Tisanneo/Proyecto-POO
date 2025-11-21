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
    
    // Colores estilo Buscaminas clásico
    private static final Color[] COLORES_NUMEROS = {
        null, Color.BLUE, new Color(0, 128, 0), Color.RED, 
        new Color(0, 0, 128), new Color(128, 0, 0), new Color(0, 128, 128),
        Color.BLACK, Color.GRAY
    };

    public ClienteGUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e){}
        setTitle("Buscaminas Multijugador Pro");
        setSize(700, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        lblInfo = new JLabel("Conectando al servidor...", SwingConstants.CENTER);
        lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblInfo.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(lblInfo, BorderLayout.NORTH);

        panelJuego = new JPanel();
        panelJuego.setBackground(Color.LIGHT_GRAY);
        add(panelJuego, BorderLayout.CENTER);
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
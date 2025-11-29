package proyecto.poo.cliente;

import proyecto.poo.comunes.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.sound.sampled.*; 
import java.net.Socket;

public class ClienteGUI extends JFrame {
    private JLabel lblInfo;
    private JPanel panelJuego;
    private JButton[][] botones;
    
    private static Clip clipMusica = null; 
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Tablero tablero;
    private boolean miTurno = false;
    
    private JLabel lblTiempo;           
    private javax.swing.Timer timer;    
    private javax.swing.Timer timerEspera; 
    private int segundos = 0;           
    private String nombreJugador;       
    

    private ImageIcon iconFondoBeat; 
    private Image gifFondoBeat;      
    
   
    private static final Color COLOR_FONDO = new Color(10, 5, 35); 
    private static final Color COLOR_NEON_PRIMARIO = new Color(180, 0, 255); 
    private static final Color COLOR_NEON_SECUNDARIO = new Color(0, 255, 255); 
    private static final Color COLOR_CELDA_OCULTA = new Color(30, 30, 70); 
    private static final Color COLOR_CELDA_REVELADA = new Color(5, 5, 20); 
    
    private static final Font FUENTE_RETRO = new Font("Monospaced", Font.BOLD, 16);
    private static final Font FUENTE_EMOJI = new Font("Segoe UI Emoji", Font.PLAIN, 20);

    private static final Color[] COLORES_NUMEROS = {
        null, 
        new Color(0, 255, 255), new Color(50, 255, 50), new Color(255, 50, 50), 
        new Color(255, 255, 0), new Color(255, 0, 255), new Color(0, 128, 128), 
        Color.WHITE, Color.GRAY
    };

    public ClienteGUI() {
      
        reproducirMusica(); 
        

        try {
            java.net.URL url = getClass().getResource("/proyecto/poo/recursos/beat.gif");
            if (url != null) {
                iconFondoBeat = new ImageIcon(url);
                gifFondoBeat = iconFondoBeat.getImage();
            }
        } catch(Exception e){
            System.err.println("Error al cargar beat.gif: " + e.getMessage());
        }
        

        try {
            UIManager.put("OptionPane.background", COLOR_FONDO);
            UIManager.put("Panel.background", COLOR_FONDO);
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
            UIManager.put("Button.background", COLOR_CELDA_OCULTA);
            UIManager.put("Button.foreground", Color.WHITE);
        } catch(Exception e){}


        nombreJugador = pedirNombrePersonalizado();

        if (nombreJugador.isEmpty()) {
            nombreJugador = "PLAYER_" + (int)(Math.random() * 1000);
        }
        

        setTitle("BUSCAMINAS MULTIJUGADOR - " + nombreJugador);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(COLOR_FONDO);
        setLayout(new BorderLayout()); 
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cerrarAplicacion();
            }
        });
        
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(COLOR_FONDO);
        panelSuperior.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createMatteBorder(0, 0, 4, 0, COLOR_NEON_PRIMARIO)
        ));

        lblInfo = new JLabel(" CONECTANDO SERVIDOR...", SwingConstants.LEFT);
        lblInfo.setFont(FUENTE_RETRO);
        lblInfo.setForeground(COLOR_NEON_SECUNDARIO);
        lblInfo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        
 
        JPanel panelDerecho = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panelDerecho.setOpaque(false); 

        lblTiempo = new JLabel("TIEMPO: 00:00", SwingConstants.RIGHT);
        lblTiempo.setFont(FUENTE_RETRO);
        lblTiempo.setForeground(Color.WHITE);
        

        JButton btnVerMinas = new JButton("👁");
        btnVerMinas.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btnVerMinas.setBackground(COLOR_FONDO);
        btnVerMinas.setForeground(Color.YELLOW);
        btnVerMinas.setFocusPainted(false);
        btnVerMinas.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 1));
        btnVerMinas.setPreferredSize(new Dimension(40, 25));
        btnVerMinas.setToolTipText("Ver/Ocultar Minas (Trampa)");
        
    
        final boolean[] trampaActiva = {false};

        btnVerMinas.addActionListener(e -> {
            if (tablero == null || botones == null) return;
            
            trampaActiva[0] = !trampaActiva[0];
            boolean mostrar = trampaActiva[0];

            if (mostrar) {
                btnVerMinas.setBackground(Color.YELLOW);
                btnVerMinas.setForeground(Color.BLACK); 
            } else {
                btnVerMinas.setBackground(COLOR_FONDO);
                btnVerMinas.setForeground(Color.YELLOW); 
            }

            int f = tablero.getFilas();
            int c = tablero.getColumnas();
            
            for(int i=0; i<f; i++) {
                for(int j=0; j<c; j++) {
                    Tablero.Celda celda = tablero.getCelda(i, j);
                    JButton btn = botones[i][j];
                    
                    if (!celda.revelada) {
                        if (mostrar && celda.esMina) {
                            btn.setText("💣");
                            btn.setForeground(Color.MAGENTA);
                            btn.setBorder(new LineBorder(Color.MAGENTA, 1));
                        } else {
                            btn.setBackground(COLOR_CELDA_OCULTA);
                            btn.setBorder(BorderFactory.createBevelBorder(
                                    javax.swing.border.BevelBorder.RAISED, 
                                    new Color(60, 60, 100), Color.BLACK));
                            
                            if (celda.marcada) {
                                btn.setText("🚩");
                                btn.setForeground(COLOR_NEON_PRIMARIO);
                            } else {
                                btn.setText("");
                            }
                        }
                    }
                }
            }
        });

    
        JToggleButton btnMute = new JToggleButton("🔊");
        btnMute.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btnMute.setBackground(COLOR_FONDO);
        btnMute.setForeground(Color.WHITE);
        btnMute.setFocusPainted(false);
        btnMute.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        btnMute.setPreferredSize(new Dimension(40, 25));
        
        btnMute.addActionListener(e -> {
            if (btnMute.isSelected()) {
                btnMute.setText("🔇");
                btnMute.setBackground(Color.RED);
                detenerMusica(); 
            } else {
                btnMute.setText("🔊");
                btnMute.setBackground(COLOR_FONDO);
                reproducirMusica(); 
            }
        });

        panelDerecho.add(btnVerMinas);
        panelDerecho.add(lblTiempo);
        panelDerecho.add(btnMute);

        panelSuperior.add(lblInfo, BorderLayout.WEST);
        panelSuperior.add(panelDerecho, BorderLayout.EAST);
        
        add(panelSuperior, BorderLayout.NORTH);


        panelJuego = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); 
                if (gifFondoBeat != null && botones == null) { 
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    g2d.drawImage(gifFondoBeat, 0, 0, getWidth(), getHeight(), this);
                    g2d.dispose();
                }
            }
        };
        
        panelJuego.setBackground(COLOR_FONDO); 
        

        panelJuego.setLayout(new GridBagLayout());
        
        JPanel panelEsperaContenido = new JPanel();
        panelEsperaContenido.setLayout(new BoxLayout(panelEsperaContenido, BoxLayout.Y_AXIS));
        panelEsperaContenido.setOpaque(false); 

        try {
            java.net.URL urlGif = getClass().getResource("/proyecto/poo/recursos/penguin.gif");
            if (urlGif != null) {
                JLabel lblGif = new JLabel(new ImageIcon(urlGif));
                lblGif.setAlignmentX(Component.CENTER_ALIGNMENT); 
                panelEsperaContenido.add(lblGif);
            }
        } catch (Exception e) {}

        panelEsperaContenido.add(Box.createVerticalStrut(20));

        JLabel lblWait1 = new JLabel("ESCANEANDO RED...");
        lblWait1.setFont(new Font("Monospaced", Font.BOLD, 24));
        lblWait1.setForeground(COLOR_NEON_SECUNDARIO);
        lblWait1.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblWait2 = new JLabel("PREPARANDO MINAS VIRTUALES");
        lblWait2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lblWait2.setForeground(Color.GRAY);
        lblWait2.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelEsperaContenido.add(lblWait1);
        panelEsperaContenido.add(Box.createVerticalStrut(10));
        panelEsperaContenido.add(lblWait2);
        
        panelJuego.add(panelEsperaContenido); 

        add(panelJuego, BorderLayout.CENTER);

        
        timer = new javax.swing.Timer(1000, e -> {
            segundos++;
            long min = segundos / 60;
            long seg = segundos % 60;
            lblTiempo.setText(String.format("TIEMPO: %02d:%02d ", min, seg));
        });

    
        timerEspera = new javax.swing.Timer(700, e -> {
              Color actual = lblInfo.getForeground();
              if (actual.equals(COLOR_NEON_SECUNDARIO)) lblInfo.setForeground(Color.WHITE);
              else lblInfo.setForeground(COLOR_NEON_SECUNDARIO);
        });
        timerEspera.start(); 
        
        this.setUndecorated(false); 
        this.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        this.setVisible(true);
    }

    public void iniciarConexion() {
            new Thread(() -> {
                try {
                  
                    socket = new Socket("localhost", 4444);
                    out = new ObjectOutputStream(socket.getOutputStream());

                  
                    
                    out.writeObject(new Mensaje("LOGIN", nombreJugador));
                    out.flush();
              

                    in = new ObjectInputStream(socket.getInputStream());

                    while(true) {
                        Mensaje msj = (Mensaje) in.readObject();
                        procesarMensaje(msj);
                    }
                } catch(Exception e) {
                    lblInfo.setText(" ERROR DE CONEXIÓN");
                    lblInfo.setForeground(Color.RED);
                    if(timerEspera != null) timerEspera.stop(); 
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
                    lblInfo.setText(" " + msj.getContenido().toString().toUpperCase());
                    break;
                case "INICIO":
                    if (timerEspera != null) timerEspera.stop();
                    lblInfo.setForeground(COLOR_NEON_SECUNDARIO);

                    segundos = 0; 
                    timer.start(); 
                    this.tablero = (Tablero) msj.getContenido();
                    pintarTableroEstilizado();
                    panelJuego.repaint();
                    break;
                case "UPDATE":
                    this.tablero = (Tablero) msj.getContenido();
                    pintarTableroEstilizado(); 
                    break;
                case "TURNO":
                    this.miTurno = (boolean) msj.getContenido();
                    if(miTurno) {
                        lblInfo.setText(" > TU TURNO <");
                        lblInfo.setForeground(Color.GREEN);
                        ((JPanel)lblInfo.getParent()).setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(10, 10, 10, 10),
                            BorderFactory.createLineBorder(Color.GREEN, 2)
                        ));
                    } else {
                        lblInfo.setText(" TURNO OPONENTE...");
                        lblInfo.setForeground(Color.RED);
                        ((JPanel)lblInfo.getParent()).setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(10, 10, 10, 10),
                            BorderFactory.createLineBorder(COLOR_NEON_PRIMARIO, 2)
                        ));
                    }
                    break;
                case "GAMEOVER":
                    timer.stop();
                    JOptionPane.showMessageDialog(this, msj.getContenido());
                    preguntarReinicio();
                    break;
                    
                case "ABANDONO":
                    timer.stop();
                    JOptionPane.showMessageDialog(this, 
                        msj.getContenido(), 
                        "Fin de la conexión", 
                        JOptionPane.ERROR_MESSAGE);
                    System.exit(0); 
                    break;
            }
        });
    }
    
   
    private void pintarTableroEstilizado() {
        int f = tablero.getFilas();
        int c = tablero.getColumnas();
        
        panelJuego.removeAll();
        
        panelJuego.setLayout(new GridBagLayout()); 
        panelJuego.setBackground(COLOR_FONDO);
        panelJuego.setBorder(null); 

        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        int alturaDisponible = screenSize.height - 200; 
        
        int ladoBoton = alturaDisponible / f;
        
        
        if (ladoBoton > 60) ladoBoton = 60;
        if (ladoBoton < 30) ladoBoton = 30; 
        
        Dimension tamanoCuadrado = new Dimension(ladoBoton, ladoBoton);

        
        JPanel contenedorTablero = new JPanel();
        contenedorTablero.setLayout(new GridLayout(f, c, 2, 2)); 
        contenedorTablero.setBackground(COLOR_NEON_PRIMARIO); 
        
        contenedorTablero.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10), 
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_NEON_PRIMARIO, 3), 
                BorderFactory.createLineBorder(COLOR_FONDO, 5) 
            )
        ));

        botones = new JButton[f][c];
        
        for(int i=0; i<f; i++) {
            for(int j=0; j<c; j++) {
                JButton btn = crearBotonRetro(i, j);
                btn.setPreferredSize(tamanoCuadrado); 
                
                if (ladoBoton < 40) {
                    btn.setFont(btn.getFont().deriveFont(12f));
                }
                
                botones[i][j] = btn;
                contenedorTablero.add(btn);
            }
        }
        
        panelJuego.add(contenedorTablero);

        panelJuego.revalidate();
        panelJuego.repaint();
        
        for(int i=0; i<f; i++) {
            for(int j=0; j<c; j++) {
                actualizarEstadoBoton(botones[i][j], tablero.getCelda(i, j));
            }
        }
    }

    private JButton crearBotonRetro(int r, int c) {
        JButton btn = new JButton();
        btn.setFocusPainted(false);
        btn.setFont(FUENTE_RETRO);
        
        btn.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, 
                new Color(50, 50, 90), Color.BLACK));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                 if(btn.isEnabled()) btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
                 btn.setBackground(new Color(50, 50, 90));
            }
            public void mouseExited(MouseEvent e) {
                 if(btn.isEnabled()) btn.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, 
                         new Color(50, 50, 90), Color.BLACK));
                 btn.setBackground(COLOR_CELDA_OCULTA);
            }
            public void mouseClicked(MouseEvent e) {
                if(!miTurno) return;
                try {
                    if(SwingUtilities.isRightMouseButton(e)) {
                        out.writeObject(new Mensaje("BANDERA", new Point(r, c)));
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        out.writeObject(new Mensaje("MOVIMIENTO", new Point(r, c)));
                    }
                    out.flush();
                } catch(Exception ex) {}
            }
        });
        return btn;
    }

    private void actualizarEstadoBoton(JButton btn, Tablero.Celda celda) {
        if (celda.revelada) {
            if (celda.esMina) {
                btn.setEnabled(true); 
                btn.setBackground(Color.BLACK);
                btn.setFont(FUENTE_EMOJI);
                btn.setText("💣"); 
                btn.setForeground(Color.RED);
                btn.setBorder(new LineBorder(Color.RED, 1));
            } else {
                btn.setEnabled(false); 
                btn.setBackground(COLOR_CELDA_REVELADA);
                btn.setBorder(new LineBorder(new Color(20,20,40))); 
                btn.setFont(FUENTE_RETRO);
                
                if (celda.minasAlrededor > 0) {
                    btn.setText(String.valueOf(celda.minasAlrededor));
                    btn.setForeground(COLORES_NUMEROS[Math.min(celda.minasAlrededor, 8)]);
                } else {
                    btn.setText("");
                }
            }
        } else {
            btn.setEnabled(true);
            btn.setBackground(COLOR_CELDA_OCULTA);
            btn.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, 
                                 new Color(60, 60, 100), Color.BLACK));
            
            if (celda.marcada) {
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                btn.setText("🚩");
                btn.setForeground(COLOR_NEON_PRIMARIO);
            } else {
                if (!btn.getText().equals("💣")) {
                    btn.setFont(FUENTE_EMOJI);
                    btn.setText("");
                }
            }
        }
    }

    private void mostrarMenuDificultad() {
        String[] opciones = {"PRINCIPIANTE", "INTERMEDIO", "AVANZADO"};
        int seleccion = JOptionPane.showOptionDialog(
            this, 
            "SELECCIONA EL NIVEL DE DIFICULTAD:", 
            "CONFIGURACIÓN DE PARTIDA",
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, opciones, opciones[0]
        );
        
        String dif = (seleccion >= 0) ? opciones[seleccion] : "Principiante";
        if(dif.equals("PRINCIPIANTE")) dif = "Principiante";
        if(dif.equals("INTERMEDIO")) dif = "Intermedio";
        if(dif.equals("AVANZADO")) dif = "Avanzado";

        try {
            out.writeObject(new Mensaje("RESPUESTA_DIF", dif));
            out.flush();
        } catch (IOException e) {}
    }
    
    private void preguntarReinicio() {
          int resp = JOptionPane.showConfirmDialog(this, "¿INSERTAR COIN PARA REINICIAR?", "GAME OVER", JOptionPane.YES_NO_OPTION);
          if(resp == JOptionPane.YES_OPTION) {
             try { out.writeObject(new Mensaje("REINICIAR", null)); } catch(Exception e){}
             lblInfo.setText(" ESPERANDO JUGADOR 2...");
             lblInfo.setForeground(Color.YELLOW);
          } else {
             cerrarAplicacion();
          }
    }

    private void cerrarAplicacion() {
        try {
            if (clipMusica != null) {
                clipMusica.stop();
                clipMusica.close();
                clipMusica = null; 
            }
            if (socket != null) socket.close(); 
            if (timer != null) timer.stop();   
        } catch (Exception e) {}
        System.exit(0); 
    }
    
    private String pedirNombrePersonalizado() {
        JDialog dialog = new JDialog((Frame)null, "Login", true);
        dialog.setUndecorated(true); 
        dialog.setSize(400, 280); 
        dialog.setLocationRelativeTo(null);
        
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBorder(new LineBorder(COLOR_NEON_PRIMARIO, 2)); 

        JPanel barraTitulo = new JPanel(new BorderLayout());
        barraTitulo.setBackground(new Color(20, 10, 50)); 
        barraTitulo.setPreferredSize(new Dimension(400, 30));
        barraTitulo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_NEON_PRIMARIO));

        JLabel titleLabel = new JLabel("  LOGIN DE USUARIO");
        titleLabel.setForeground(Color.CYAN);
        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelBotones.setOpaque(false);

        JButton btnMuteBarra = new JButton("🔊");
        btnMuteBarra.setForeground(Color.WHITE);
        btnMuteBarra.setBackground(new Color(20, 10, 50));
        btnMuteBarra.setBorderPainted(false);
        btnMuteBarra.setFocusPainted(false);
        btnMuteBarra.setPreferredSize(new Dimension(40, 30));
        btnMuteBarra.addActionListener(e -> {
            if (btnMuteBarra.getText().equals("🔊")) {
                btnMuteBarra.setText("🔇");
                btnMuteBarra.setForeground(Color.RED);
                detenerMusica(); 
            } else {
                btnMuteBarra.setText("🔊");
                btnMuteBarra.setForeground(Color.WHITE);
                reproducirMusica();
            }
        });

        JButton btnCerrar = new JButton("X");
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setBackground(new Color(200, 50, 50)); 
        btnCerrar.setBorderPainted(false);
        btnCerrar.setFocusPainted(false);
        btnCerrar.setPreferredSize(new Dimension(45, 30));
        btnCerrar.addActionListener(e -> System.exit(0)); 

        btnCerrar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnCerrar.setBackground(Color.RED); }
            public void mouseExited(MouseEvent e) { btnCerrar.setBackground(new Color(200, 50, 50)); }
        });

        panelBotones.add(btnMuteBarra);
        panelBotones.add(btnCerrar);

        barraTitulo.add(titleLabel, BorderLayout.WEST);
        barraTitulo.add(panelBotones, BorderLayout.EAST);

        MouseAdapter dragListener = new MouseAdapter() {
            private int pX, pY;
            public void mousePressed(MouseEvent e) {
                pX = e.getX();
                pY = e.getY();
            }
            public void mouseDragged(MouseEvent e) {
                dialog.setLocation(dialog.getLocation().x + e.getX() - pX,
                                   dialog.getLocation().y + e.getY() - pY);
            }
        };
        barraTitulo.addMouseListener(dragListener);
        barraTitulo.addMouseMotionListener(dragListener);
        
        mainContainer.add(barraTitulo, BorderLayout.NORTH);

        final StringBuilder nombreIngresado = new StringBuilder("");

        ImageIcon iconFondo = null;
        try {
            java.net.URL url = getClass().getResource("/proyecto/poo/recursos/beat.gif");
            if (url != null) iconFondo = new ImageIcon(url);
        } catch(Exception e){}
        
        final Image gifFondo = (iconFondo != null) ? iconFondo.getImage() : null;

        JPanel panelFormulario = new JPanel(new GridLayout(4, 1, 10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); 
                if (gifFondo != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                    g2d.drawImage(gifFondo, 0, 0, getWidth(), getHeight(), this);
                    g2d.dispose();
                }
            }
        };
        
        panelFormulario.setBackground(COLOR_FONDO);
        panelFormulario.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JLabel lblTitulo = new JLabel("INGRESA TU NOMBRE", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Monospaced", Font.BOLD, 20));
        lblTitulo.setForeground(COLOR_NEON_SECUNDARIO);
        
        JTextField txtNombre = new JTextField();
        txtNombre.setFont(new Font("Monospaced", Font.BOLD, 18));
        txtNombre.setBackground(new Color(30, 30, 60, 200)); 
        txtNombre.setForeground(Color.WHITE);
        txtNombre.setCaretColor(Color.WHITE);
        txtNombre.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        txtNombre.setHorizontalAlignment(JTextField.CENTER);

        JButton btnJugar = new JButton("INSERT COIN (JUGAR)");
        btnJugar.setFont(new Font("Monospaced", Font.BOLD, 16));
        btnJugar.setBackground(new Color(0, 100, 0));
        btnJugar.setForeground(Color.WHITE);
        btnJugar.setFocusPainted(false);
        btnJugar.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        
        btnJugar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btnJugar.setBackground(new Color(0, 200, 0));
                btnJugar.setForeground(Color.BLACK);
                btnJugar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(MouseEvent e) {
                btnJugar.setBackground(new Color(0, 100, 0));
                btnJugar.setForeground(Color.WHITE);
            }
        });

        btnJugar.addActionListener(e -> {
            if (!txtNombre.getText().trim().isEmpty()) {
                nombreIngresado.append(txtNombre.getText().trim());
                dialog.dispose();
            } else {
                txtNombre.setBorder(new LineBorder(Color.RED, 2));
            }
        });
        
        txtNombre.addActionListener(e -> btnJugar.doClick());

        panelFormulario.add(lblTitulo);
        panelFormulario.add(new JLabel(""));
        panelFormulario.add(txtNombre);
        panelFormulario.add(btnJugar);
        
        mainContainer.add(panelFormulario, BorderLayout.CENTER);
        
        dialog.add(mainContainer);
        dialog.setVisible(true);

        return nombreIngresado.toString();
    }
        
    public static void reproducirMusica() {
        try {
            if (clipMusica != null) {
                if (!clipMusica.isRunning()) {
                    clipMusica.loop(Clip.LOOP_CONTINUOUSLY);
                    clipMusica.start();
                }
                return; 
            }
            
            java.net.URL urlMusica = ClienteGUI.class.getResource("/proyecto/poo/recursos/Blippblipp.wav");
            
            if (urlMusica != null) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(urlMusica);
                clipMusica = AudioSystem.getClip(); 
                clipMusica.open(audioInput);
                try {
                    FloatControl gainControl = (FloatControl) clipMusica.getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(-10.0f); 
                } catch (Exception e) {}
                clipMusica.loop(Clip.LOOP_CONTINUOUSLY);
                clipMusica.start();
            } else {
                  System.out.println("No se encontró la canción Blippblipp.wav en la ruta de recursos.");
            }
        } catch (Exception e) {}
    }

    public static void detenerMusica() {
        if (clipMusica != null && clipMusica.isRunning()) {
            clipMusica.stop();
        }
    }
}
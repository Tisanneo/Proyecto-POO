package proyecto.poo.cliente;

import javax.swing.SwingUtilities;
 
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteGUI gui = new ClienteGUI();
            gui.setVisible(true);
            gui.iniciarConexion();
        });
    }
}
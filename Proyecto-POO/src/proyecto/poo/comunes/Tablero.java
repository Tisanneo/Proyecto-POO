package proyecto.poo.comunes;

import java.io.Serializable;
import java.util.Random;

public class Tablero implements Serializable {
    private int filas;
    private int columnas;
    private int minas;
    private Celda[][] celdas;

    public static class Celda implements Serializable {
        public boolean esMina;
        public boolean revelada;
        public int minasAlrededor;
        public boolean marcada; // Bandera
    }

    public Tablero(int filas, int columnas, int minas) {
        this.filas = filas;
        this.columnas = columnas;
        this.minas = minas;
        this.celdas = new Celda[filas][columnas];
        inicializarCeldas();
        colocarMinas();
        calcularNumeros();
    }

    // Constructor de copia
    public Tablero(Tablero t) {
        this.filas = t.filas;
        this.columnas = t.columnas;
        this.minas = t.minas;
        this.celdas = new Celda[filas][columnas];
        for(int i=0; i<filas; i++) {
            for(int j=0; j<columnas; j++) {
                this.celdas[i][j] = new Celda();
                this.celdas[i][j].esMina = t.celdas[i][j].esMina;
                this.celdas[i][j].minasAlrededor = t.celdas[i][j].minasAlrededor;
                this.celdas[i][j].revelada = t.celdas[i][j].revelada;
                this.celdas[i][j].marcada = t.celdas[i][j].marcada;
            }
        }
    }

    /**
     * Lógica principal del Buscaminas "Real".
     * Si es vacío (0), revela recursivamente a los vecinos.
     * Retorna true si explotó una mina.
     */
    public boolean revelarCelda(int r, int c) {
        if (r < 0 || r >= filas || c < 0 || c >= columnas) return false;
        Celda celda = celdas[r][c];
        
        if (celda.revelada || celda.marcada) return false;
        
        celda.revelada = true;

        if (celda.esMina) {
            return true; // ¡Boom!
        }

        // Si es un 0 (vacío), expandir automáticamente (Flood Fill)
        if (celda.minasAlrededor == 0) {
            for (int i = r - 1; i <= r + 1; i++) {
                for (int j = c - 1; j <= c + 1; j++) {
                    if (i != r || j != c) {
                        revelarCelda(i, j);
                    }
                }
            }
        }
        return false;
    }
    
    public void toggleBandera(int r, int c) {
        if (!celdas[r][c].revelada) {
            celdas[r][c].marcada = !celdas[r][c].marcada;
        }
    }

    private void inicializarCeldas() {
        for(int i=0; i<filas; i++) 
            for(int j=0; j<columnas; j++) celdas[i][j] = new Celda();
    }

    private void colocarMinas() {
        Random rnd = new Random();
        int puestas = 0;
        while(puestas < minas) {
            int r = rnd.nextInt(filas);
            int c = rnd.nextInt(columnas);
            if(!celdas[r][c].esMina) {
                celdas[r][c].esMina = true;
                puestas++;
            }
        }
    }

    private void calcularNumeros() {
        for(int i=0; i<filas; i++) {
            for(int j=0; j<columnas; j++) {
                if(!celdas[i][j].esMina) {
                    celdas[i][j].minasAlrededor = contarVecinos(i, j);
                }
            }
        }
    }

    private int contarVecinos(int r, int c) {
        int count = 0;
        for(int i=r-1; i<=r+1; i++) {
            for(int j=c-1; j<=c+1; j++) {
                if(i>=0 && i<filas && j>=0 && j<columnas && celdas[i][j].esMina) count++;
            }
        }
        return count;
    }

    public int getFilas() { return filas; }
    public int getColumnas() { return columnas; }
    public Celda getCelda(int r, int c) { return celdas[r][c]; }
}
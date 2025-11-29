package proyecto.poo.comunes;

import java.util.Random;

public class GeneradorTableros {
    
    public Tablero[] generarParTableros(String dificultad) {
        int f, c, m;
        
        switch(dificultad) {
            case "Intermedio":
                f = 10; c = 10; m = 15;
                break;
            case "Avanzado":
                f = 16; c = 16; m = 40;
                break;
            case "Principiante":
            default:
                f = 8; c = 8; m = 10;
                break;
        }
        
    
        return new Tablero[] {
            new Tablero(f, c, m),
            new Tablero(f, c, m)
        };
    }
}
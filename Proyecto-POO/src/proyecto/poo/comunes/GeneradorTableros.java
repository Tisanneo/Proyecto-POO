package proyecto.poo.comunes;

import java.util.*;

public class GeneradorTableros {
    private List<Tablero> plantillas;
    private Random random;

    public GeneradorTableros() {
        plantillas = new ArrayList<>();
        random = new Random();
        crearPlantillasPorDefecto();
        System.out.println("Generador de tableros inicializado con " + plantillas.size() + " plantillas");
    }

    private void crearPlantillasPorDefecto() {
        try {
            // Crear plantillas básicas de diferentes tamaños
            System.out.println("Creando plantillas por defecto...");

            Tablero tablero1 = crearTableroAleatorio(8, 8, 10); // Principiante
            if (tablero1 != null)
                plantillas.add(tablero1);

            Tablero tablero2 = crearTableroAleatorio(10, 10, 15); // Intermedio
            if (tablero2 != null)
                plantillas.add(tablero2);

            Tablero tablero3 = crearTableroAleatorio(12, 12, 20); // Avanzado
            if (tablero3 != null)
                plantillas.add(tablero3);

            Tablero tablero4 = crearTableroAleatorio(8, 8, 12);
            if (tablero4 != null)
                plantillas.add(tablero4);

            Tablero tablero5 = crearTableroAleatorio(10, 10, 18);
            if (tablero5 != null)
                plantillas.add(tablero5);

            System.out.println("Plantillas creadas exitosamente: " + plantillas.size());

        } catch (Exception e) {
            System.err.println("Error creando plantillas por defecto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Tablero[] generarTableros() {
        // Verificar que tenemos suficientes plantillas
        if (plantillas.size() < 2) {
            System.err.println("No hay suficientes plantillas. Creando tableros básicos...");
            return new Tablero[] {
                    crearTableroAleatorio(8, 8, 10),
                    crearTableroAleatorio(8, 8, 10)
            };
        }

        // Seleccionar dos plantillas diferentes
        int idx1 = random.nextInt(plantillas.size());
        int idx2;
        do {
            idx2 = random.nextInt(plantillas.size());
        } while (idx2 == idx1 && plantillas.size() > 1);

        System.out.println("Generando tableros desde plantillas " + idx1 + " y " + idx2);

        return new Tablero[] {
                new Tablero(plantillas.get(idx1)),
                new Tablero(plantillas.get(idx2))
        };
    }

    private Tablero crearTableroAleatorio(int filas, int columnas, int minas) {
        try {
            Tablero tablero = new Tablero(filas, columnas, minas);
            System.out.println("Tablero creado: " + filas + "x" + columnas + " con " + minas + " minas");
            return tablero;
        } catch (Exception e) {
            System.err.println("Error creando tablero " + filas + "x" + columnas + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
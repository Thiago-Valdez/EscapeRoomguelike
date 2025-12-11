package fisica;

import mapa.*;
import com.badlogic.gdx.physics.box2d.Body;

import java.util.ArrayList;
import java.util.List;

public class SistemaColisiones {

    /** Ancho/alto de la “boca” de la puerta, en píxeles (ajústalo a tu arte). */
    private static final int PUERTA_ANCHO = 64;
    private static final int PUERTA_ALTO  = 64;

    private final FabricaCuerpos fabrica;

    public SistemaColisiones(FabricaCuerpos fabrica) {
        this.fabrica = fabrica;
    }

    /**
     * Construye colisiones para una sala:
     * - Pared perimetral (4 rects finos).
     * - Sensores de puerta en las posiciones declaradas por la Habitación.
     *
     * Trabaja directamente con la Habitacion (usa h.gridX / h.gridY).
     * Devuelve los bodies creados por si querés destruirlos al salir de la sala.
     */
    public List<Body> construirParaSala(Habitacion h) {
        List<Body> bodies = new ArrayList<>();

        // Posición base de la sala en el mundo (en píxeles)
        float baseX = h.gridX * h.ancho;
        float baseY = h.gridY * h.alto;

        // === PAREDES: 4 rectángulos delgados en los bordes ===
        int grosor = 16; // píxeles; ajustá al colisionador que prefieras

        // abajo
        bodies.add(fabrica.crearPared(baseX,
            baseY,
            h.ancho,
            grosor));
        // arriba
        bodies.add(fabrica.crearPared(baseX,
            baseY + h.alto - grosor,
            h.ancho,
            grosor));
        // izquierda
        bodies.add(fabrica.crearPared(baseX,
            baseY,
            grosor,
            h.alto));
        // derecha
        bodies.add(fabrica.crearPared(baseX + h.ancho - grosor,
            baseY,
            grosor,
            h.alto));

        // === PUERTAS: sensores en posiciones locales declaradas por la Habitación ===
        for (var e : h.puertas.entrySet()) {
            Direccion dir = e.getKey();
            EspecificacionPuerta p = e.getValue();

            // Convertir coord local (de la habitación) a mundo
            float px = baseX + p.localX - PUERTA_ANCHO / 2f;
            float py = baseY + p.localY - PUERTA_ALTO / 2f;

            // Por ahora solo usamos la habitación de origen y la dirección;
            // el destino se puede resolver en otro sistema (lo dejamos null).
            DatosPuerta datos = new DatosPuerta(h, null, dir);

            bodies.add(
                fabrica.crearPuertaSensor(
                    px,
                    py,
                    PUERTA_ANCHO,
                    PUERTA_ALTO,
                    datos
                )
            );
        }

        return bodies;
    }
}

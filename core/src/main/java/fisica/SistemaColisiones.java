package fisica;

import mapa.*;
import com.badlogic.gdx.physics.box2d.Body;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema LEGACY de colisiones por sala.
 *
 * IMPORTANTE:
 * - Con el sistema nuevo, las puertas/sensores del piso se generan en GeneradorParedesSalas
 *   según DisposicionMapa + conexiones del piso.
 * - Por eso, este sistema ya NO debe crear sensores de puerta, o te duplica / rompe coherencia.
 */
public class SistemaColisiones {

    private final FabricaCuerpos fabrica;

    public SistemaColisiones(FabricaCuerpos fabrica) {
        this.fabrica = fabrica;
    }

    /**
     * Construye colisiones para una sala:
     * - SOLO paredes perimetrales (4 rects finos).
     *
     * Si aún querés puertas, deben generarse con GeneradorParedesSalas,
     * no acá.
     */
    public List<Body> construirParaSala(Habitacion h) {
        List<Body> bodies = new ArrayList<>();

        float baseX = h.gridX * h.ancho;
        float baseY = h.gridY * h.alto;

        int grosor = 16;

        // abajo
        bodies.add(fabrica.crearPared(baseX, baseY, h.ancho, grosor));
        // arriba
        bodies.add(fabrica.crearPared(baseX, baseY + h.alto - grosor, h.ancho, grosor));
        // izquierda
        bodies.add(fabrica.crearPared(baseX, baseY, grosor, h.alto));
        // derecha
        bodies.add(fabrica.crearPared(baseX + h.ancho - grosor, baseY, grosor, h.alto));

        return bodies;
    }
}

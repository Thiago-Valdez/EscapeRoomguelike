package fisica;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.*;
import mapa.Direccion;
import mapa.EspecificacionPuerta;
import mapa.DisposicionMapa;
import mapa.GrafoPuertas;
import mapa.Habitacion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Genera paredes y sensores de puertas para las habitaciones del CAMINO actual.
 *
 * Todo está en PIXELES (sin PPM).
 *
 * - Crea 4 paredes "dentro" del rectángulo de la sala.
 * - Para cada puerta de la sala, crea un sensor.
 * - SOLO crea el sensor y llama al listener si el destino pertenece al camino generado.
 */
public class GeneradorParedesSalas {

    public interface ListenerPuerta {
        void onPuertaCreada(Fixture fixture,
                            Habitacion origen,
                            Habitacion destino,
                            Direccion direccion);
    }

    private final World world;
    private final List<Habitacion> camino;
    private final Set<Habitacion> caminoSet;
    private final GrafoPuertas grafo;

    // Grosor de la pared hacia adentro de la sala
    private static final float GROSOR_MURO = 16f;

    // Tamaño del sensor de puerta
    private static final float ANCHO_PUERTA = 96f;
    private static final float ALTO_PUERTA  = 96f;

    public GeneradorParedesSalas(FisicaMundo fisica,
                                 DisposicionMapa disposicion,
                                 GrafoPuertas grafo) {
        this.world = fisica.world();
        this.camino = disposicion.getCamino();
        this.caminoSet = new HashSet<>(camino);
        this.grafo = grafo;
    }

    /** Genera paredes + sensores de puertas SOLO para las salas del camino. */
    public void generar(ListenerPuerta listener) {
        Gdx.app.log("GEN_PUERTAS", "Camino tiene " + camino.size() + " salas");

        for (Habitacion h : camino) {
            crearParedesRectangulares(h);
            crearSensoresPuertas(h, listener);
        }
    }

    /**
     * Crea 4 paredes dentro del rectángulo de la habitación:
     * norte, sur, este, oeste.
     */
    private void crearParedesRectangulares(Habitacion h) {
        float baseX = h.gridX * h.ancho;
        float baseY = h.gridY * h.alto;

        // NORTE (arriba)
        crearMuro(
            baseX + h.ancho / 2f,
            baseY + h.alto - GROSOR_MURO / 2f,
            h.ancho / 2f,
            GROSOR_MURO / 2f
        );

        // SUR (abajo)
        crearMuro(
            baseX + h.ancho / 2f,
            baseY + GROSOR_MURO / 2f,
            h.ancho / 2f,
            GROSOR_MURO / 2f
        );

        // OESTE (izquierda)
        crearMuro(
            baseX + GROSOR_MURO / 2f,
            baseY + h.alto / 2f,
            GROSOR_MURO / 2f,
            h.alto / 2f
        );

        // ESTE (derecha)
        crearMuro(
            baseX + h.ancho - GROSOR_MURO / 2f,
            baseY + h.alto / 2f,
            GROSOR_MURO / 2f,
            h.alto / 2f
        );
    }

    private void crearMuro(float cx, float cy, float halfW, float halfH) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(cx, cy);
        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfW, halfH);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.friction = 0f;
        fd.restitution = 0f;
        fd.density = 0f;

        body.createFixture(fd);
        shape.dispose();
    }

    /**
     * Crea sensores de puerta en el borde correspondiente.
     * SOLO crea el sensor y llama al listener si el destino está en el camino.
     */
    private void crearSensoresPuertas(Habitacion origen,
                                      ListenerPuerta listener) {

        float baseX = origen.gridX * origen.ancho;
        float baseY = origen.gridY * origen.alto;

        for (var entry : origen.puertas.entrySet()) {
            Direccion dir = entry.getKey();
            EspecificacionPuerta spec = entry.getValue();

            // 🔹 Primero vemos a dónde debería llevar esta puerta
            Habitacion destino = grafo.destinoDe(origen, dir);

            // Si no hay conexión lógica o el destino no pertenece al camino, ni siquiera creamos el sensor
            if (destino == null || !caminoSet.contains(destino)) {
                Gdx.app.log("GEN_PUERTAS",
                    "Puerta SIN destino desde " + origen.nombreVisible +
                        " por " + dir + " (destino fuera del camino)");
                continue;
            }

            // Posición centro de la puerta en mundo (píxeles)
            float px = baseX + spec.localX;
            float py = baseY + spec.localY;

            float halfW, halfH;

            // Ajustamos el rectángulo del sensor según el lado
            switch (dir) {
                case NORTE, SUR -> {
                    halfW = ANCHO_PUERTA / 2f;
                    halfH = GROSOR_MURO; // un poco alto
                }
                case ESTE, OESTE -> {
                    halfW = GROSOR_MURO; // un poco ancho
                    halfH = ALTO_PUERTA / 2f;
                }
                default -> {
                    halfW = ANCHO_PUERTA / 2f;
                    halfH = ALTO_PUERTA / 2f;
                }
            }

            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.StaticBody;
            bd.position.set(px, py);
            Body body = world.createBody(bd);

            PolygonShape shape = new PolygonShape();
            shape.setAsBox(halfW, halfH);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.isSensor = true;

            Fixture fixture = body.createFixture(fd);
            shape.dispose();

            // Notificamos al listener para que coloque DatosPuerta en el fixture
            if (listener != null) {
                listener.onPuertaCreada(fixture, origen, destino, dir);
            }

            Gdx.app.log("GEN_PUERTAS",
                "Puerta creada: " + origen.nombreVisible +
                    " --" + dir + "--> " + destino.nombreVisible);
        }
    }
}

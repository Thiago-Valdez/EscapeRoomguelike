package fisica;

import com.badlogic.gdx.physics.box2d.*;
import mapa.*;

import java.util.Map;

/**
 * Se encarga de:
 *  - Crear las paredes de cada habitación (con huecos donde haya puertas).
 *  - Crear los sensores de puerta (fixtures sensor).
 *
 * No guarda estado de las puertas; eso lo resuelve el caller mediante el ListenerPuertas.
 */
public class GeneradorParedesSalas {

    public static final float WALL_THICKNESS = 16f;
    public static final float DOOR_WIDTH = 64f;

    private final FisicaMundo fisica;
    private final DisposicionMapa disposicion;

    /** Callback para que el juego registre qué fixture corresponde a qué puerta. */
    public interface ListenerPuertas {
        void onCrearPuerta(Fixture fixture,
                           DisposicionMapa.Colocacion origen,
                           DisposicionMapa.Colocacion destino,
                           Direccion direccion);
    }

    public GeneradorParedesSalas(FisicaMundo fisica, DisposicionMapa disposicion) {
        this.fisica = fisica;
        this.disposicion = disposicion;
    }

    /** Genera todas las paredes y sensores de puertas del mapa. */
    public void generar(ListenerPuertas listener) {
        World world = fisica.world();

        for (DisposicionMapa.Colocacion col : disposicion.todas()) {
            Habitacion h = col.habitacion;
            float baseX = col.gx * h.ancho;
            float baseY = col.gy * h.alto;

            // Puertas solo si hay sala vecina
            EspecificacionPuerta puertaNorte = puertaConVecino(col, Direccion.NORTE);
            EspecificacionPuerta puertaSur   = puertaConVecino(col, Direccion.SUR);
            EspecificacionPuerta puertaEste  = puertaConVecino(col, Direccion.ESTE);
            EspecificacionPuerta puertaOeste = puertaConVecino(col, Direccion.OESTE);

            // ---------- MURO NORTE ----------
            {
                float y = baseY + h.alto - WALL_THICKNESS / 2f;

                if (puertaNorte == null) {
                    crearParedRectangular(world,
                        baseX + h.ancho / 2f, y,
                        h.ancho, WALL_THICKNESS);
                } else {
                    float doorLocalX = puertaNorte.localX;
                    float leftWidth  = doorLocalX - DOOR_WIDTH / 2f;
                    float rightWidth = h.ancho - (doorLocalX + DOOR_WIDTH / 2f);

                    if (leftWidth > 2f) {
                        crearParedRectangular(world,
                            baseX + leftWidth / 2f, y,
                            leftWidth, WALL_THICKNESS);
                    }
                    if (rightWidth > 2f) {
                        crearParedRectangular(world,
                            baseX + doorLocalX + DOOR_WIDTH / 2f + rightWidth / 2f, y,
                            rightWidth, WALL_THICKNESS);
                    }
                }
            }

            // ---------- MURO SUR ----------
            {
                float y = baseY + WALL_THICKNESS / 2f;

                if (puertaSur == null) {
                    crearParedRectangular(world,
                        baseX + h.ancho / 2f, y,
                        h.ancho, WALL_THICKNESS);
                } else {
                    float doorLocalX = puertaSur.localX;
                    float leftWidth  = doorLocalX - DOOR_WIDTH / 2f;
                    float rightWidth = h.ancho - (doorLocalX + DOOR_WIDTH / 2f);

                    if (leftWidth > 2f) {
                        crearParedRectangular(world,
                            baseX + leftWidth / 2f, y,
                            leftWidth, WALL_THICKNESS);
                    }
                    if (rightWidth > 2f) {
                        crearParedRectangular(world,
                            baseX + doorLocalX + DOOR_WIDTH / 2f + rightWidth / 2f, y,
                            rightWidth, WALL_THICKNESS);
                    }
                }
            }

            // ---------- MURO OESTE ----------
            {
                float x = baseX + WALL_THICKNESS / 2f;

                if (puertaOeste == null) {
                    crearParedRectangular(world,
                        x, baseY + h.alto / 2f,
                        WALL_THICKNESS, h.alto);
                } else {
                    float doorLocalY = puertaOeste.localY;
                    float bottomHeight = doorLocalY - DOOR_WIDTH / 2f;
                    float topHeight    = h.alto - (doorLocalY + DOOR_WIDTH / 2f);

                    if (bottomHeight > 2f) {
                        crearParedRectangular(world,
                            x, baseY + bottomHeight / 2f,
                            WALL_THICKNESS, bottomHeight);
                    }
                    if (topHeight > 2f) {
                        crearParedRectangular(world,
                            x, baseY + doorLocalY + DOOR_WIDTH / 2f + topHeight / 2f,
                            WALL_THICKNESS, topHeight);
                    }
                }
            }

            // ---------- MURO ESTE ----------
            {
                float x = baseX + h.ancho - WALL_THICKNESS / 2f;

                if (puertaEste == null) {
                    crearParedRectangular(world,
                        x, baseY + h.alto / 2f,
                        WALL_THICKNESS, h.alto);
                } else {
                    float doorLocalY = puertaEste.localY;
                    float bottomHeight = doorLocalY - DOOR_WIDTH / 2f;
                    float topHeight    = h.alto - (doorLocalY + DOOR_WIDTH / 2f);

                    if (bottomHeight > 2f) {
                        crearParedRectangular(world,
                            x, baseY + bottomHeight / 2f,
                            WALL_THICKNESS, bottomHeight);
                    }
                    if (topHeight > 2f) {
                        crearParedRectangular(world,
                            x, baseY + doorLocalY + DOOR_WIDTH / 2f + topHeight / 2f,
                            WALL_THICKNESS, topHeight);
                    }
                }
            }

            // ---------- SENSORES DE PUERTA ----------
            for (Map.Entry<Direccion, EspecificacionPuerta> entry : h.puertas.entrySet()) {
                Direccion dir = entry.getKey();
                EspecificacionPuerta p = entry.getValue();

                int nx = col.gx + dx(dir);
                int ny = col.gy + dy(dir);
                DisposicionMapa.Colocacion destino = disposicion.buscar(nx, ny);
                if (destino == null) continue;

                float px, py;

                switch (dir) {
                    case NORTE -> {
                        px = baseX + p.localX;
                        py = baseY + h.alto - WALL_THICKNESS - 16f;
                    }
                    case SUR -> {
                        px = baseX + p.localX;
                        py = baseY + WALL_THICKNESS + 16f;
                    }
                    case ESTE -> {
                        px = baseX + h.ancho - WALL_THICKNESS - 16f;
                        py = baseY + p.localY;
                    }
                    case OESTE -> {
                        px = baseX + WALL_THICKNESS + 16f;
                        py = baseY + p.localY;
                    }
                    default -> {
                        px = baseX + h.ancho / 2f;
                        py = baseY + h.alto / 2f;
                    }
                }

                BodyDef bd = new BodyDef();
                bd.type = BodyDef.BodyType.StaticBody;
                bd.position.set(px, py);

                Body body = world.createBody(bd);

                PolygonShape sensorShape = new PolygonShape();
                sensorShape.setAsBox(12f, 12f);

                FixtureDef fdef = new FixtureDef();
                fdef.isSensor = true;
                fdef.shape = sensorShape;

                Fixture fx = body.createFixture(fdef);
                sensorShape.dispose();

                // Le delegamos al listener registrar la puerta
                if (listener != null) {
                    listener.onCrearPuerta(fx, col, destino, dir);
                }
            }
        }
    }

    // ---------- Helpers privados ----------

    /** Devuelve una puerta solo si existe sala vecina en esa dirección. */
    private EspecificacionPuerta puertaConVecino(DisposicionMapa.Colocacion col, Direccion dir) {
        Habitacion h = col.habitacion;
        EspecificacionPuerta p = h.puertas.get(dir);
        if (p == null) return null;

        int nx = col.gx + dx(dir);
        int ny = col.gy + dy(dir);
        DisposicionMapa.Colocacion destino = disposicion.buscar(nx, ny);
        if (destino == null) return null;

        return p;
    }

    private void crearParedRectangular(World world, float cx, float cy, float w, float h) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(cx, cy);

        Body b = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(w / 2f, h / 2f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.friction = 0.5f;
        fd.restitution = 0f;

        b.createFixture(fd);
        shape.dispose();
    }

    private static int dx(Direccion d) {
        return switch (d) {
            case ESTE -> 1;
            case OESTE -> -1;
            default -> 0;
        };
    }

    private static int dy(Direccion d) {
        return switch (d) {
            case NORTE -> 1;
            case SUR   -> -1;
            default -> 0;
        };
    }
}

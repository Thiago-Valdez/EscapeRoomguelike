package entidades;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.*;

import fisica.FisicaMundo;
import mapa.Habitacion;
import mapa.PuertaVisual;
import mapa.TipoSala;

import java.util.*;

/**
 * Administra entidades del juego:
 *  - Jugador (cuerpo de Box2D)
 *  - Ítems en el mundo (pickups)
 *
 *  TODO se maneja en PIXELES.
 */
public class GestorDeEntidades {

    private final World world;
    private final Jugador jugador;

    private Body cuerpoJugador;

    private final Map<Habitacion, List<PuertaVisual>> puertasPorSala = new HashMap<>();

    // Ítems tirados en el mundo
    private final List<Item> itemsMundo = new ArrayList<>();
    private final Map<Item, Body> cuerposItems = new HashMap<>();

    // Para no respawnear infinitamente ítems de BOTIN
    private final Set<Habitacion> botinesConItem = new HashSet<>();

    public GestorDeEntidades(World world, Jugador jugador) {
        this.world = world; // ✅ USA el world que le pasan
        this.jugador = jugador;
    }

    // ================= JUGADOR ===================

    /**
     * Crea (si no existe) o reposiciona el cuerpo del jugador en la sala indicada.
     * px,py están en PIXELES y se usan tal cual.
     */
    public Body crearJugadorEnSalaInicial(Habitacion sala, float px, float py) {
        if (cuerpoJugador == null) {
            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.DynamicBody;
            bd.position.set(px, py);
            bd.fixedRotation = true;
            cuerpoJugador = world.createBody(bd);

            CircleShape shape = new CircleShape();
            shape.setRadius(24f); // un poco más grande para verlo bien

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1f;
            fd.friction = 0f;
            fd.restitution = 0f;

            Fixture f = cuerpoJugador.createFixture(fd);
            f.setUserData("jugador");

            shape.dispose();

            System.out.println("[GestorEntidades] Jugador creado en (" + px + "," + py + ")");
        } else {
            cuerpoJugador.setTransform(px, py, cuerpoJugador.getAngle());
            cuerpoJugador.setLinearVelocity(0f, 0f);
            System.out.println("[GestorEntidades] Jugador movido a (" + px + "," + py + ")");
        }

        return cuerpoJugador;
    }

    public Body getCuerpoJugador() {
        return cuerpoJugador;
    }

    // ================= ÍTEMS / ACTUALIZACIÓN ===================

    /**
     * Lógica que se ejecuta cada frame.
     * Por ahora solo usamos salaActual para spawnear ítems en salas BOTIN.
     */
    public void actualizar(float delta, Habitacion salaActual) {
        if (salaActual != null && salaActual.tipo == TipoSala.BOTIN) {
            intentarSpawnearItemEnBotin(salaActual);
        }
    }

    public void registrarPuertaVisual(Habitacion sala, PuertaVisual pv) {
        puertasPorSala
            .computeIfAbsent(sala, k -> new ArrayList<>())
            .add(pv);
    }

    public void renderPuertas(ShapeRenderer sr, Habitacion salaActual) {
        if (sr == null || salaActual == null) return;

        List<PuertaVisual> puertas = puertasPorSala.get(salaActual);
        if (puertas == null) return;

        for (PuertaVisual p : puertas) {
            p.render(sr);
        }
    }



    private void intentarSpawnearItemEnBotin(Habitacion salaBotin) {
        if (botinesConItem.contains(salaBotin)) {
            return; // ya tiene ítem
        }

        Item item = ItemTipo.generarAleatorioPorRareza();
        if (item == null) return;

        float baseX = salaBotin.gridX * salaBotin.ancho;
        float baseY = salaBotin.gridY * salaBotin.alto;

        float px = baseX + salaBotin.ancho / 2f;
        float py = baseY + salaBotin.alto / 2f;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(px, py); // PIXELES
        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(12f); // PIXELES

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true;

        Fixture fixture = body.createFixture(fd);
        shape.dispose();

        // Ponemos el propio Item como userData para reconocerlo en ContactListener
        fixture.setUserData(item);

        itemsMundo.add(item);
        cuerposItems.put(item, body);
        botinesConItem.add(salaBotin);
    }

    /** Llamado desde el ContactListener cuando se recoge un item. */
    public void recogerItem(Item item) {
        if (item == null) return;

        jugador.agregarObjeto(item);

        // 🔥 recalculamos TODOS los stats
        jugador.reaplicarEfectosDeItems();

        Body body = cuerposItems.remove(item);
        if (body != null) {
            world.destroyBody(body);
        }
        itemsMundo.remove(item);
    }



    public World getWorld() {
        return world;
    }

    // ================= RENDER ===================

    public void render(SpriteBatch batch) {
        // Por ahora el jugador y los ítems solo se ven por Box2DDebugRenderer.
        // Más adelante, cuando tengas sprites, los dibujamos acá.
    }
}

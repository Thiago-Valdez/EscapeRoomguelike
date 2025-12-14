package entidades;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.*;
import mapa.Habitacion;
import mapa.PuertaVisual;
import mapa.TipoSala;

import java.util.*;

public class GestorDeEntidades {

    private final World world;

    // ✅ ahora soporta N jugadores
    private final Map<Integer, Jugador> jugadores = new HashMap<>();

    private final Map<Habitacion, List<PuertaVisual>> puertasPorSala = new HashMap<>();

    // Ítems tirados en el mundo
    private final List<Item> itemsMundo = new ArrayList<>();
    private final Map<Item, Body> cuerposItems = new HashMap<>();

    // Para no respawnear infinitamente ítems de BOTIN
    private final Set<Habitacion> botinesConItem = new HashSet<>();

    public GestorDeEntidades(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

    // ===================== JUGADORES =====================

    public void registrarJugador(Jugador jugador) {
        if (jugador == null) return;
        jugadores.put(jugador.getId(), jugador);
    }

    public Jugador getJugador(int id) {
        return jugadores.get(id);
    }

    public Collection<Jugador> getJugadores() {
        return Collections.unmodifiableCollection(jugadores.values());
    }

    public Body getCuerpoJugador(int id) {
        Jugador j = jugadores.get(id);
        return (j != null) ? j.getCuerpoFisico() : null;
    }

    /**
     * Crea (si no existe) o reposiciona el cuerpo del jugador en la sala indicada.
     * px,py están en PIXELES.
     *
     * ✅ Fuente de verdad de identidad:
     * - body.userData = Jugador (en Jugador.setCuerpoFisico)
     *
     * Fixture userData NO se usa para id. (Podés dejarlo como tag debug opcional)
     */
    public Body crearOReposicionarJugador(int id, Habitacion sala, float px, float py) {
        Jugador jugador = jugadores.get(id);
        if (jugador == null) return null;

        Body body = jugador.getCuerpoFisico();

        if (body == null) {
            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.DynamicBody;
            bd.position.set(px, py);
            bd.fixedRotation = true;

            body = world.createBody(bd);

            CircleShape shape = new CircleShape();
            shape.setRadius(12f);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1f;
            fd.friction = 0f;

            Fixture f = body.createFixture(fd);

            // ✅ tag opcional solo para debug (NO es fuente de verdad)
            f.setUserData("jugador");

            shape.dispose();

            // ✅ esto pone body.userData = jugador
            jugador.setCuerpoFisico(body);

            System.out.println("[GestorEntidades] Jugador" + id + " creado en (" + px + "," + py + ")");
        } else {
            body.setTransform(px, py, body.getAngle());
            body.setLinearVelocity(0f, 0f);

            System.out.println("[GestorEntidades] Jugador" + id + " movido a (" + px + "," + py + ")");
        }

        return body;
    }

    // ===================== PUERTAS VISUALES =====================

    public void registrarPuertaVisual(Habitacion sala, PuertaVisual pv) {
        puertasPorSala.computeIfAbsent(sala, k -> new ArrayList<>()).add(pv);
    }

    public void renderPuertas(ShapeRenderer sr, Habitacion salaActual) {
        if (sr == null || salaActual == null) return;

        List<PuertaVisual> puertas = puertasPorSala.get(salaActual);
        if (puertas == null) return;

        for (PuertaVisual p : puertas) p.render(sr);
    }

    // ===================== ITEMS / UPDATE =====================

    public void actualizar(float delta, Habitacion salaActual) {
        if (salaActual != null && salaActual.tipo == TipoSala.BOTIN) {
            intentarSpawnearItemEnBotin(salaActual);
        }
    }

    private void intentarSpawnearItemEnBotin(Habitacion salaBotin) {
        if (botinesConItem.contains(salaBotin)) return;

        Item item = ItemTipo.generarAleatorioPorRareza();
        if (item == null) return;

        float baseX = salaBotin.gridX * salaBotin.ancho;
        float baseY = salaBotin.gridY * salaBotin.alto;

        float px = baseX + salaBotin.ancho / 2f;
        float py = baseY + salaBotin.alto / 2f;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(px, py);
        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(12f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true;

        Fixture fixture = body.createFixture(fd);
        shape.dispose();

        fixture.setUserData(item);

        itemsMundo.add(item);
        cuerposItems.put(item, body);
        botinesConItem.add(salaBotin);
    }

    /** ✅ Coop real: el item se aplica al jugador que lo recogió */
    public void recogerItem(int jugadorId, Item item) {
        if (item == null) return;

        Jugador jugador = jugadores.get(jugadorId);
        if (jugador == null) return;

        jugador.agregarObjeto(item);
        jugador.reaplicarEfectosDeItems();

        Body body = cuerposItems.remove(item);
        if (body != null) world.destroyBody(body);
        itemsMundo.remove(item);
    }

    // ===================== RENDER =====================

    public void render(SpriteBatch batch) {
        // futuro: dibujar sprites de jugadores/items
    }
}

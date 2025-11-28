package entidades;

import com.badlogic.gdx.physics.box2d.*;
import entidades.items.ItemTipo;
import fisica.FisicaMundo;
import mapa.DisposicionMapa;
import mapa.Habitacion;
import mapa.TipoSala;

import java.util.*;

/**
 * Se encarga de manejar todas las entidades del juego:
 *  - Jugador (lógica + cuerpo físico + control de movimiento)
 *  - Pickups (Items) en salas BOTIN
 *
 * Deja a JuegoPrincipal más limpio.
 */
public class GestorDeEntidades {

    // --- Dependencias principales ---
    private final FisicaMundo fisica;
    private final DisposicionMapa disposicion;

    // --- Jugador ---
    private Jugador jugador;
    private Body jugadorBody;
    private ControlJugador controlJugador;

    // Radio del jugador (debe coincidir con el circleShape que uses)
    private static final float PLAYER_RADIUS = 16f;

    // --- Pickups ---
    public static class PickupItem {
        public final DisposicionMapa.Colocacion sala;
        public final Item item;
        public final Body cuerpo;

        public PickupItem(DisposicionMapa.Colocacion sala, Item item, Body cuerpo) {
            this.sala = sala;
            this.item = item;
            this.cuerpo = cuerpo;
        }
    }

    private final List<PickupItem> pickups = new ArrayList<>();
    private final List<PickupItem> pickupsPendientes = new ArrayList<>();
    private final Map<Fixture, PickupItem> pickupPorFixture = new HashMap<>();

    private final Random rngItems = new Random();

    public GestorDeEntidades(FisicaMundo fisica, DisposicionMapa disposicion) {
        this.fisica = fisica;
        this.disposicion = disposicion;
    }

    // =========================================================
    // JUGADOR
    // =========================================================

    /**
     * Crea el jugador lógico y su cuerpo físico en el centro de la sala inicial.
     */
    public void crearJugadorEnSalaInicial(DisposicionMapa.Colocacion salaInicial) {
        if (salaInicial == null) {
            throw new IllegalArgumentException("La sala inicial no puede ser null");
        }

        // --- Lógica del jugador ---
        // Podés cambiar estos valores más adelante o parametrizarlo.
        this.jugador = new Jugador("Thiago", Genero.HOMBRE, Estilo.CAZADOR);
        jugador.setVelocidad(200f);
        jugador.setVidaMaxima(3);
        jugador.setVida(3);

        // --- Cuerpo físico ---
        crearCuerpoJugadorEnSala(salaInicial);

        // --- Control de movimiento ---
        this.controlJugador = new ControlJugador(jugador, jugadorBody);
    }

    /**
     * Crea o recrea el cuerpo físico del jugador en el centro de la sala indicada.
     * Mantiene la instancia de Jugador (solo cambia su cuerpo físico).
     */
    public void crearCuerpoJugadorEnSala(DisposicionMapa.Colocacion sala) {
        if (sala == null) return;

        // Si ya hay un cuerpo anterior, lo destruimos
        if (jugadorBody != null) {
            fisica.world().destroyBody(jugadorBody);
            jugadorBody = null;
        }

        World world = fisica.world();
        Habitacion h = sala.habitacion;

        float x = sala.gx * h.ancho + h.ancho / 2f;
        float y = sala.gy * h.alto + h.alto / 2f;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);

        jugadorBody = world.createBody(bd);

        CircleShape circle = new CircleShape();
        circle.setRadius(PLAYER_RADIUS);

        FixtureDef fd = new FixtureDef();
        fd.shape = circle;
        fd.density = 1f;
        fd.friction = 0.2f;
        fd.restitution = 0f;

        Fixture fx = jugadorBody.createFixture(fd);
        fx.setUserData(jugador);
        circle.dispose();

        jugador.setCuerpoFisico(jugadorBody);

        if (controlJugador != null) {
            controlJugador.setCuerpo(jugadorBody);
        }
    }

    /**
     * Mueve al jugador a una posición absoluta (x,y) en el mundo, reseteando su velocidad.
     * Útil cuando cambiás de sala (teleport al entrar por una puerta).
     */
    public void moverJugadorA(float x, float y) {
        if (jugadorBody == null) return;
        jugadorBody.setTransform(x, y, 0f);
        jugadorBody.setLinearVelocity(0f, 0f);
    }

    /**
     * Llama en el render para procesar input de movimiento.
     */
    public void actualizarMovimientoJugador() {
        if (controlJugador != null) {
            controlJugador.actualizarMovimiento();
        }
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Body getJugadorBody() {
        return jugadorBody;
    }

    public ControlJugador getControlJugador() {
        return controlJugador;
    }

    public float getPlayerRadius() {
        return PLAYER_RADIUS;
    }

    // =========================================================
    // PICKUPS (Items en salas BOTIN)
    // =========================================================

    /**
     * Crea un pickup (item en el mundo) en todas las salas de tipo BOTIN.
     */
    public void crearPickupsEnSalasBotin() {
        World world = fisica.world();

        for (DisposicionMapa.Colocacion col : disposicion.todas()) {
            if (col.habitacion.tipo != TipoSala.BOTIN) continue;

            Habitacion h = col.habitacion;
            float baseX = col.gx * h.ancho;
            float baseY = col.gy * h.alto;
            float centroX = baseX + h.ancho / 2f;
            float centroY = baseY + h.alto / 2f;

            // Elegir tipo de item según rareza
            ItemTipo tipoRandom = ItemTipo.aleatorioSegunRareza(rngItems);
            Item item = tipoRandom.crear();

            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.StaticBody;
            bd.position.set(centroX, centroY);

            Body body = world.createBody(bd);

            CircleShape shape = new CircleShape();
            shape.setRadius(18f);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.isSensor = true;

            Fixture fx = body.createFixture(fd);
            shape.dispose();

            PickupItem pi = new PickupItem(col, item, body);
            fx.setUserData(pi);
            pickupPorFixture.put(fx, pi);
            pickups.add(pi);

            System.out.println("[GestorDeEntidades] Spawn pickup en BOTIN (" +
                col.gx + "," + col.gy + "): " + item.getNombre());
        }
    }

    /**
     * Devuelve true si el userData de un fixture corresponde a un PickupItem manejado por este gestor.
     */
    public boolean esPickup(Object userData) {
        return userData instanceof PickupItem;
    }

    /**
     * Marca un pickup para ser recogido; se procesa luego de world.step()
     * para evitar modificar el mundo durante el callback de colisión.
     */
    public void marcarPickupRecogido(Object userData) {
        if (!(userData instanceof PickupItem pi)) return;
        pickupsPendientes.add(pi);
    }

    /**
     * Procesa todos los pickups marcados: aplica el item al jugador, destruye el cuerpo
     * y limpia las referencias. Llamar una vez por frame después de fisica.step().
     */
    public void procesarPickupsPendientes() {
        if (pickupsPendientes.isEmpty()) return;
        if (jugador == null) {
            pickupsPendientes.clear();
            return;
        }

        World world = fisica.world();

        for (PickupItem pi : pickupsPendientes) {
            if (pi.item != null) {
                jugador.agregarObjeto(pi.item);
                pi.item.aplicar(jugador);
                System.out.println("[GestorDeEntidades] Jugador recogió: " + pi.item.getNombre());
            }
            world.destroyBody(pi.cuerpo);
            pickups.remove(pi);
            pickupPorFixture.values().removeIf(p -> p == pi);
        }

        pickupsPendientes.clear();
    }

    public List<PickupItem> getPickups() {
        return Collections.unmodifiableList(pickups);
    }
}

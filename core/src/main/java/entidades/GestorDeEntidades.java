package entidades;

import java.util.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.*;

import entidades.enemigos.Enemigo;
import entidades.items.Item;
import entidades.items.ItemTipo;
import entidades.personajes.Jugador;
import mapa.model.Habitacion;
import mapa.model.TipoSala;
import mapa.puertas.PuertaVisual;

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

    // ===================== ENEMIGOS =====================
    // Enemigos vivos en el mundo (no tienen vida, se limpian por evento/puzzle o cambio de sala)
    private final List<Enemigo> enemigosMundo = new ArrayList<>();

    // Para poder destruir bodies de enemigos sin buscar en el World
    private final Map<Enemigo, Body> cuerposEnemigos = new HashMap<>();

    // Para limpiar por sala (cuando salís o se resuelve puzzle)
    private final Map<Habitacion, List<Enemigo>> enemigosPorSala = new HashMap<>();

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

    // ===================== ENEMIGOS =====================

    /**
     * Registra un enemigo (ya creado) y lo asocia a una sala.
     * Recomendado: llamar desde EnemigosDesdeTiled luego de crear body+enemigo.
     */
    public void registrarEnemigo(Habitacion sala, Enemigo enemigo) {
        if (enemigo == null) return;

        enemigosMundo.add(enemigo);

        if (enemigo.getCuerpoFisico() != null) {
            cuerposEnemigos.put(enemigo, enemigo.getCuerpoFisico());
        }

        if (sala != null) {
            enemigosPorSala.computeIfAbsent(sala, k -> new ArrayList<>()).add(enemigo);
        }
    }

    public List<Enemigo> getEnemigosMundo() {
        return Collections.unmodifiableList(enemigosMundo);
    }

    /**
     * ✅ NUEVO: devuelve enemigos asociados a una sala (copia inmutable).
     * Útil para iniciar animación de muerte SOLO a los de salaActual.
     */
    public List<Enemigo> getEnemigosDeSala(Habitacion sala) {
        if (sala == null) return Collections.emptyList();
        List<Enemigo> lista = enemigosPorSala.get(sala);
        if (lista == null || lista.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(lista);
    }

    /**
     * Elimina todos los enemigos (por ejemplo, al resolver puzzle global o reset).
     */
    public void eliminarTodosLosEnemigos() {
        for (Enemigo e : new ArrayList<>(enemigosMundo)) {
            eliminarEnemigo(e);
        }
        enemigosPorSala.clear();
    }

    /**
     * Elimina los enemigos de una sala específica (útil al salir de sala o al resolver puzzle de esa sala).
     * ⚠️ Esto destruye instantáneo. Para animación de muerte, NO lo uses directamente:
     * usá: getEnemigosDeSala + sprites.iniciarMuerte + eliminarEnemigo cuando termine.
     */
    public void eliminarEnemigosDeSala(Habitacion sala) {
        if (sala == null) return;

        List<Enemigo> lista = enemigosPorSala.get(sala);
        if (lista == null || lista.isEmpty()) return;

        List<Enemigo> copia = new ArrayList<>(lista);
        for (Enemigo e : copia) {
            eliminarEnemigo(e);
        }

        enemigosPorSala.remove(sala);
    }

    /**
     * ✅ NUEVO: público para poder eliminar 1 enemigo cuando termina su anim de muerte.
     */
    public void eliminarEnemigo(Enemigo enemigo) {
        if (enemigo == null) return;

        Body b = cuerposEnemigos.remove(enemigo);
        if (b == null) b = enemigo.getCuerpoFisico();

        if (b != null) {
            world.destroyBody(b);
        }

        enemigosMundo.remove(enemigo);

        // quitarlo de cualquier lista de sala (costo pequeño, cantidad baja)
        for (List<Enemigo> lista : enemigosPorSala.values()) {
            lista.remove(enemigo);
        }

        // si alguna sala quedó con lista vacía, la limpiamos
        enemigosPorSala.values().removeIf(List::isEmpty);
    }

    /**
     * Update de enemigos (IA simple). Lo llamás desde Partida.
     * Si no tenés j2 en alguna run, pasás null y el enemigo elige lo que corresponda.
     */
    public void actualizarEnemigos(float delta, Jugador j1, Jugador j2) {
        for (Enemigo e : enemigosMundo) {
            e.actualizar(delta, j1, j2);
        }
    }

    // ===================== PUERTAS VISUALES =====================

    public void registrarPuertaVisual(Habitacion sala, PuertaVisual pv) {
        puertasPorSala.computeIfAbsent(sala, k -> new ArrayList<>()).add(pv);
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
        // futuro: dibujar sprites de jugadores/items/enemigos
    }

    public List<PuertaVisual> getPuertasVisuales(Habitacion sala) {
        List<PuertaVisual> l = puertasPorSala.get(sala);
        return l != null ? l : java.util.Collections.emptyList();
    }

}

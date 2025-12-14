package juego;

import camara.CamaraDeSala;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import control.ControlPuzzlePorSala;
import control.GestorSalas;
import entidades.*;
import fisica.BotonesDesdeTiled;
import fisica.ColisionesDesdeTiled;
import fisica.FisicaMundo;
import fisica.GeneradorSensoresPuertas;
import interfaces.HudJuego;
import interfaces.ListenerCambioSala;
import io.github.principal.Principal;
import mapa.*;

import java.util.*;

/**
 * Contenedor del gameplay.
 *
 * Objetivo: que JuegoPrincipal quede mínimo y que acá esté TODA la lógica del loop.
 */
public class Partida {

    private final Principal game;

    // --- Mundo físico (único) ---
    private World world;
    private FisicaMundo fisica;

    // --- Render ---
    private SpriteBatch batch;
    private ShapeRenderer shapeRendererMundo;

    // --- Mapa Tiled ---
    private TiledMap mapaTiled;
    private OrthogonalTiledMapRenderer mapaRenderer;

    // --- Cámara ---
    private CamaraDeSala camaraSala;

    // --- Mapa lógico ---
    private DisposicionMapa disposicion;
    private Habitacion salaActual;
    private ControlPuzzlePorSala controlPuzzle;

    // --- Gestión ---
    private GestorSalas gestorSalas;
    private GestorDeEntidades gestorEntidades;

    // --- Jugadores ---
    private Jugador jugador1;
    private Jugador jugador2;
    private ControlJugador controlJugador1;
    private ControlJugador controlJugador2;

    // ✅ Sprites unificados (jugadores + enemigos)
    private final Map<Entidad, SpritesEntidad> spritesPorEntidad = new HashMap<>();

    // ✅ NUEVO: enemigos que están reproduciendo animación de muerte
    private final Set<Enemigo> enemigosEnMuerte = new HashSet<>();

    // --- HUD ---
    private HudJuego hud;
    public List<Habitacion> salasDelPiso;

    // --- Listeners de cambio de sala (HUD, logs, etc.) ---
    private final List<ListenerCambioSala> listenersCambioSala = new ArrayList<>();

    // --- Colas de eventos (evita modificar Box2D dentro del callback) ---
    private final List<EventoPuerta> puertasPendientes = new ArrayList<>();
    private final List<EventoPickup> itemsPendientes = new ArrayList<>();
    private final List<EventoBoton> eventosBoton = new ArrayList<>();
    private final Set<entidades.Item> itemsYaProcesados = new HashSet<>();
    private final List<EventoDanio> daniosPendientes = new ArrayList<>();
    private final Set<Integer> jugadoresDanioFrame = new HashSet<>();


    // --- Flags ---
    private boolean debugFisica = true;
    private int framesBloqueoPuertas = 0; // cooldown para evitar ping-pong de puertas

    public Partida(Principal game) {
        this.game = game;
    }

    // ==========================
    // EVENTOS: CAMBIO DE SALA
    // ==========================

    public void agregarListenerCambioSala(ListenerCambioSala listener) {
        if (listener != null && !listenersCambioSala.contains(listener)) {
            listenersCambioSala.add(listener);
        }
    }

    private void notificarCambioSala(Habitacion salaAnterior, Habitacion salaNueva) {
        for (ListenerCambioSala listener : listenersCambioSala) {
            listener.salaCambiada(salaAnterior, salaNueva);
        }
    }

    // ==========================
    // INIT
    // ==========================

    public void init() {
        batch = new SpriteBatch();
        shapeRendererMundo = new ShapeRenderer();

        // 2) Generar mapa lógico (camino de habitaciones)
        GeneradorMapa.Configuracion cfg = new GeneradorMapa.Configuracion();
        cfg.nivel = 1;
        cfg.semilla = System.currentTimeMillis();

        List<Habitacion> todasLasHabitaciones = Arrays.asList(Habitacion.values());
        GrafoPuertas grafo = new GrafoPuertas(todasLasHabitaciones, new Random(cfg.semilla));

        GeneradorMapa generador = new GeneradorMapa(cfg, grafo);
        disposicion = generador.generar();
        this.salasDelPiso = generador.salasDelPiso;

        salaActual = disposicion.salaInicio();
        disposicion.descubrir(salaActual);

        controlPuzzle = new ControlPuzzlePorSala();
        controlPuzzle.alEntrarASala(salaActual);

        camaraSala = new CamaraDeSala(512f, 512f);
        camaraSala.setFactorLerp(0f);
        camaraSala.centrarEn(salaActual);

        world = new World(new Vector2(0, 0), true);
        fisica = new FisicaMundo(world);

        mapaTiled = new TmxMapLoader().load("TMX/mapa.tmx");
        mapaRenderer = new OrthogonalTiledMapRenderer(mapaTiled, 1f);
        ColisionesDesdeTiled.crearColisiones(mapaTiled, world);
        BotonesDesdeTiled.crearBotones(mapaTiled, world);

        float baseX = salaActual.gridX * salaActual.ancho;
        float baseY = salaActual.gridY * salaActual.alto;
        float px = baseX + salaActual.ancho / 2f;
        float py = baseY + salaActual.alto / 2f;

        jugador1 = new Jugador(1, "Jugador 1", Genero.MASCULINO, Estilo.CLASICO);
        jugador2 = new Jugador(2, "Jugador 2", Genero.FEMENINO, Estilo.CLASICO);

        gestorEntidades = new GestorDeEntidades(world);
        gestorEntidades.registrarJugador(jugador1);
        gestorEntidades.registrarJugador(jugador2);

        gestorEntidades.crearOReposicionarJugador(1, salaActual, px - 32f, py);
        gestorEntidades.crearOReposicionarJugador(2, salaActual, px + 32f, py);

        // ✅ Sprites de jugadores (48x48 confirmado)
        registrarSpriteDeEntidad(jugador1, new SpritesJugador(jugador1, 48, 48), +6f, -2f);
        registrarSpriteDeEntidad(jugador2, new SpritesJugador(jugador2, 48, 48), +6f, -2f);

        // ✅ Spawn enemigos desde Tiled (solo para la sala actual)
        EnemigosDesdeTiled.crearEnemigosDesdeMapa(mapaTiled, salaActual, world, gestorEntidades);
        registrarSpritesDeEnemigosVivos();

        GeneradorSensoresPuertas genPuertas = new GeneradorSensoresPuertas(fisica, disposicion);
        genPuertas.generar((fixture, origen, destino, dir) -> {
            Box2dUtils.Aabb bb = Box2dUtils.aabb(fixture);

            PuertaVisual visual = new PuertaVisual(bb.minX(), bb.minY(), bb.width(), bb.height());
            DatosPuerta datos = new DatosPuerta(origen, destino, dir, visual);
            fixture.setUserData(datos);
            gestorEntidades.registrarPuertaVisual(origen, visual);
        });

        Vector2 p1 = jugador1.getCuerpoFisico().getPosition();
        Vector2 p2 = jugador2.getCuerpoFisico().getPosition();
        camaraSala.centrarEn((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f);

        controlJugador1 = new ControlJugador(jugador1, Input.Keys.W, Input.Keys.S, Input.Keys.A, Input.Keys.D);
        controlJugador2 = new ControlJugador(jugador2, Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT);

        gestorSalas = new GestorSalas(disposicion, fisica, camaraSala, gestorEntidades);

        hud = new HudJuego(disposicion, jugador1);
        hud.actualizarSalaActual(salaActual);
        agregarListenerCambioSala(hud);

        fisica.setContactListener(new ContactosPartida(this));
    }

    private void registrarSpriteDeEntidad(Entidad e, SpritesEntidad sprite, float offX, float offY) {
        if (e == null || sprite == null) return;
        sprite.setOffset(offX, offY);
        spritesPorEntidad.put(e, sprite);
    }

    private void registrarSpritesDeEnemigosVivos() {
        for (Enemigo e : gestorEntidades.getEnemigosMundo()) {
            if (e == null) continue;
            if (spritesPorEntidad.containsKey(e)) continue;

            SpritesEnemigo se = new SpritesEnemigo(e, 48, 48);
            se.setOffset(0f, -2f);
            spritesPorEntidad.put(e, se);
        }
    }

    private void limpiarSpritesDeEntidadesMuertas() {
        Iterator<Map.Entry<Entidad, SpritesEntidad>> it = spritesPorEntidad.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entidad, SpritesEntidad> entry = it.next();
            Entidad ent = entry.getKey();

            if (ent == null) {
                entry.getValue().dispose();
                it.remove();
                continue;
            }

            if (ent instanceof Jugador) continue;

            if (ent instanceof Enemigo enemigo) {
                boolean sigueVivo = gestorEntidades.getEnemigosMundo().contains(enemigo);
                if (!sigueVivo) {
                    entry.getValue().dispose();
                    it.remove();
                }
            }
        }
    }

    // ✅ NUEVO: iniciar animación de muerte para enemigos de la sala actual
    private void matarEnemigosDeSalaConAnim(Habitacion sala) {
        if (sala == null) return;

        for (Enemigo e : gestorEntidades.getEnemigosDeSala(sala)) {
            SpritesEntidad sp = spritesPorEntidad.get(e);
            if (sp != null) {
                sp.iniciarMuerte();
                enemigosEnMuerte.add(e);
            } else {
                // si no tiene sprite, lo eliminamos directo
                gestorEntidades.eliminarEnemigo(e);
            }
        }
    }



    // ✅ NUEVO: eliminar realmente enemigos cuya animación ya terminó
    private void procesarEnemigosEnMuerte() {
        if (enemigosEnMuerte.isEmpty()) return;

        Iterator<Enemigo> it = enemigosEnMuerte.iterator();
        while (it.hasNext()) {
            Enemigo e = it.next();
            SpritesEntidad sp = spritesPorEntidad.get(e);

            // si por algún motivo ya no existe, sacarlo
            if (sp == null) {
                it.remove();
                continue;
            }

            if (sp.muerteTerminada()) {
                gestorEntidades.eliminarEnemigo(e);
                sp.dispose();
                spritesPorEntidad.remove(e);
                it.remove();
            }
        }
    }

    // ==========================
    // CONTACTOS -> ENCOLAR
    // ==========================

    void encolarContactoPuerta(Fixture puertaFx, Fixture otroFx) {
        if (puertaFx == null || otroFx == null) return;
        if (framesBloqueoPuertas > 0) return;

        Object ud = puertaFx.getUserData();
        if (!(ud instanceof DatosPuerta puerta)) return;

        int jugadorId = getJugadorId(otroFx);
        if (jugadorId == -1) return;

        if (salaActual == puerta.origen() || salaActual == puerta.destino()) {
            puertasPendientes.add(new EventoPuerta(puerta, jugadorId));
        }
    }

    void encolarPickup(Fixture jugadorFx, Fixture otroFx) {
        if (jugadorFx == null || otroFx == null) return;
        int jugadorId = getJugadorId(jugadorFx);
        if (jugadorId == -1) return;
        Object ud = otroFx.getUserData();
        if (ud instanceof entidades.Item item) {
            itemsPendientes.add(new EventoPickup(item, jugadorId));
        }
    }

    void encolarBoton(Fixture jugadorFx, Fixture otroFx, boolean down) {
        if (jugadorFx == null || otroFx == null) return;
        int jugadorId = getJugadorId(jugadorFx);
        if (jugadorId == -1) return;
        Object ud = otroFx.getUserData();
        if (ud instanceof DatosBoton db) {
            eventosBoton.add(new EventoBoton(db, jugadorId, down));
        }
    }

    private int getJugadorId(Fixture fx) {
        if (fx == null) return -1;
        Body b = fx.getBody();
        if (b == null) return -1;
        Object ud = b.getUserData();
        if (ud instanceof Jugador j) return j.getId();
        return -1;
    }

    public void aplicarDanioPorEnemigo(Jugador jugador, Enemigo enemigo) {
        if (jugador == null) return;

        // Evitar repetir daño si ya está en muerte o inmune
        if (jugador.estaEnMuerte() || jugador.esInmune() || !jugador.estaViva()) return;

        jugador.recibirDanio();

        // iniciar animación de muerte del sprite
        SpritesEntidad sp = spritesPorEntidad.get(jugador);
        if (sp != null) sp.iniciarMuerte();

        // opcional: frenar el cuerpo YA
        if (jugador.getCuerpoFisico() != null) {
            jugador.getCuerpoFisico().setLinearVelocity(0f, 0f);
        }
    }

    public void encolarDanioJugador(int jugadorId, float ex, float ey) {
        if (jugadorId <= 0) return;
        daniosPendientes.add(new EventoDanio(jugadorId, ex, ey));
    }



    // ==========================
    // LOOP
    // ==========================

    public void render(float delta) {
        if (world == null) return;

        gestorEntidades.actualizar(delta, salaActual);

        if (controlJugador1 != null) controlJugador1.actualizar(delta);
        if (controlJugador2 != null) controlJugador2.actualizar(delta);

        gestorEntidades.actualizarEnemigos(delta, jugador1, jugador2);

        fisica.step(delta);

        procesarPuertasPendientes();
        procesarItemsPendientes();
        procesarBotonesPendientes();
        procesarDaniosPendientes();

        jugador1.updateEstado(delta);
        jugador2.updateEstado(delta);

        tickJugador(delta, jugador1);
        tickJugador(delta, jugador2);

        // ✅ sprites / spawns
        registrarSpritesDeEnemigosVivos();

        // ✅ NUEVO: procesar muertes (destruir bodies al terminar animación)
        procesarEnemigosEnMuerte();

        limpiarSpritesDeEntidadesMuertas();

        if (framesBloqueoPuertas > 0) framesBloqueoPuertas--;

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camaraSala.update(delta);

        if (mapaRenderer != null) {
            mapaRenderer.setView(camaraSala.getCamara());
            mapaRenderer.render();
        }

        shapeRendererMundo.setProjectionMatrix(camaraSala.getCamara().combined);
        shapeRendererMundo.begin(ShapeRenderer.ShapeType.Filled);
        gestorEntidades.renderPuertas(shapeRendererMundo, salaActual);
        shapeRendererMundo.end();

        batch.setProjectionMatrix(camaraSala.getCamara().combined);
        batch.begin();

        // Enemigos primero (atrás)
        for (Enemigo e : gestorEntidades.getEnemigosMundo()) {
            SpritesEntidad s = spritesPorEntidad.get(e);
            if (s != null) {
                s.update(delta);
                s.render(batch);
            }
        }

        // Jugadores después (adelante)
        SpritesEntidad s1 = spritesPorEntidad.get(jugador1);
        if (s1 != null) { s1.update(delta); s1.render(batch); }
        SpritesEntidad s2 = spritesPorEntidad.get(jugador2);
        if (s2 != null) { s2.update(delta); s2.render(batch); }

        batch.end();

        if (debugFisica) {
            fisica.debugDraw(camaraSala.getCamara());
        }

        if (hud != null) {
            hud.render();
        }
    }

    private void procesarPuertasPendientes() {
        if (controlPuzzle != null && controlPuzzle.estaBloqueada(salaActual)) {
            puertasPendientes.clear();
            return;
        }

        if (puertasPendientes.isEmpty()) return;
        if (framesBloqueoPuertas > 0) {
            puertasPendientes.clear();
            return;
        }

        EventoPuerta ev = puertasPendientes.get(0);
        Habitacion nueva = gestorSalas.irASalaVecinaPorPuerta(salaActual, ev.puerta(), ev.jugadorId());

        if (nueva != null) {
            Habitacion anterior = salaActual;

            // ✅ Cambio de sala: limpiamos instantáneo (no anim) para evitar cuerpos vivos fuera de la sala
            gestorEntidades.eliminarEnemigosDeSala(anterior);

            // también limpiamos cualquier tracking de muerte de esa sala
            enemigosEnMuerte.removeIf(e -> !gestorEntidades.getEnemigosMundo().contains(e));

            salaActual = nueva;
            disposicion.descubrir(salaActual);
            notificarCambioSala(anterior, salaActual);
            if (controlPuzzle != null) controlPuzzle.alEntrarASala(salaActual);

            EnemigosDesdeTiled.crearEnemigosDesdeMapa(mapaTiled, salaActual, world, gestorEntidades);
            registrarSpritesDeEnemigosVivos();
            limpiarSpritesDeEntidadesMuertas();
        }

        framesBloqueoPuertas = 15;
        puertasPendientes.clear();
    }

    private void procesarItemsPendientes() {
        if (itemsPendientes.isEmpty()) return;
        itemsYaProcesados.clear();
        for (EventoPickup ev : itemsPendientes) {
            if (!itemsYaProcesados.add(ev.item())) continue;
            gestorEntidades.recogerItem(ev.jugadorId(), ev.item());
        }
        itemsPendientes.clear();
    }

    private void procesarBotonesPendientes() {
        if (eventosBoton.isEmpty()) return;

        for (EventoBoton ev : eventosBoton) {
            DatosBoton boton = ev.boton();
            int jugadorId = ev.jugadorId();

            if (boton.sala() != salaActual) continue;

            boolean valido = (jugadorId == boton.jugadorId());
            if (!valido) continue;

            if (ev.down()) {
                boolean desbloqueo = controlPuzzle.botonDown(salaActual, boton.jugadorId());
                if (desbloqueo) {
                    Gdx.app.log("PUZZLE", "Sala desbloqueada: " + salaActual.nombreVisible);

                    // ✅ NUEVO: enemigos “mueren” con animación
                    matarEnemigosDeSalaConAnim(salaActual);
                }
            } else {
                controlPuzzle.botonUp(salaActual, boton.jugadorId());
            }
        }

        eventosBoton.clear();
    }

    private void procesarDaniosPendientes() {
        if (daniosPendientes.isEmpty()) return;

        jugadoresDanioFrame.clear();

        for (EventoDanio ev : daniosPendientes) {
            int id = ev.jugadorId();
            if (!jugadoresDanioFrame.add(id)) continue; // evita daño duplicado en el mismo frame

            Jugador j = gestorEntidades.getJugador(id);
            if (j == null) continue;

            // respetar inmune / enMuerte / muerto
            if (!j.estaViva() || j.estaEnMuerte() || j.esInmune()) continue;

            Body body = j.getCuerpoFisico();
            if (body == null) continue;

            // =========================
            // 1) Separación anti-loop (antes de congelar)
            // =========================
            float px = body.getPosition().x;
            float py = body.getPosition().y;

            float dx = px - ev.ex();
            float dy = py - ev.ey();

            // si están exactamente encima, elegimos una dirección fija
            float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) {
                dx = 1f;
                dy = 0f;
                len2 = 1f;
            }

            float invLen = (float)(1.0 / Math.sqrt(len2));
            dx *= invLen;
            dy *= invLen;

            float separacion = 40f; // px (ajustá a gusto: 30-80)
            body.setTransform(px + dx * separacion, py + dy * separacion, body.getAngle());
            body.setLinearVelocity(0f, 0f);
            body.setAngularVelocity(0f);

            // =========================
            // 2) Aplicar daño + cooldown anti re-hit
            // =========================
            j.recibirDanio();
            j.marcarHitCooldown(1.0f); // extra anti-loop (podés subir a 1.5 si sigue jodido)

            // =========================
            // 3) Animación + feedback
            // =========================
            SpritesEntidad sp = spritesPorEntidad.get(j);
            if (sp != null) {
                sp.iniciarMuerte();
                // sp.iniciarParpadeo(); // si ya lo implementaste
            }
        }

        daniosPendientes.clear();
    }


    private void tickJugador(float delta, Jugador j){
        boolean estabaEnMuerte = j.estaEnMuerte();
        j.tick(delta);

        if (estabaEnMuerte && !j.estaEnMuerte()) {
            SpritesEntidad sp = spritesPorEntidad.get(j);
            if (sp != null) sp.detenerMuerte();
        }
    }


    // ==========================
    // LIFECYCLE
    // ==========================

    public void resize(int width, int height) {
        if (camaraSala != null) camaraSala.getViewport().update(width, height, true);
        if (hud != null) hud.resize(width, height);
    }

    public void dispose() {
        if (mapaRenderer != null) mapaRenderer.dispose();
        if (mapaTiled != null) mapaTiled.dispose();
        if (batch != null) batch.dispose();
        if (shapeRendererMundo != null) shapeRendererMundo.dispose();
        if (fisica != null) fisica.dispose();
        if (hud != null) hud.dispose();

        for (SpritesEntidad s : spritesPorEntidad.values()) {
            if (s != null) s.dispose();
        }
        spritesPorEntidad.clear();
        enemigosEnMuerte.clear();

        world = null;
    }
}

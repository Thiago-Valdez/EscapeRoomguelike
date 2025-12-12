package pantallas;

import camara.CamaraDeSala;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import control.GestorSalas;
import entidades.ControlJugador;
import entidades.Genero;
import entidades.Jugador;
import entidades.GestorDeEntidades;
import entidades.Estilo;
import mapa.PuertaVisual;
import fisica.ColisionesDesdeTiled;
import fisica.FisicaMundo;
import fisica.GeneradorParedesSalas;
import interfaces.HudJuego;
import io.github.principal.Principal;
import mapa.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import interfaces.ListenerCambioSala;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class JuegoPrincipal implements Screen {

    private final Principal game;

    // --- Mundo físico (único) ---
    private World world;
    private FisicaMundo fisica;

    // --- Render básico ---
    private SpriteBatch batch;

    // 🔥 ShapeRenderer del MUNDO (no HUD, no fisica)
    private ShapeRenderer shapeRendererMundo;

    // --- Mapa Tiled ---
    private TiledMap mapaTiled;
    private OrthogonalTiledMapRenderer mapaRenderer;

    // --- Cámara de sala ---
    private CamaraDeSala camaraSala;

    // --- Mapa lógico / camino ---
    private DisposicionMapa disposicion;
    private Habitacion salaActual;

    // --- Gestión de salas (cambio de habitación + cámara) ---
    private GestorSalas gestorSalas;

    // --- Jugador + control ---
    private Jugador jugador;
    private ControlJugador controlJugador;
    private GestorDeEntidades gestorEntidades;

    // --- HUD ---
    private HudJuego hud;
    public List<Habitacion> salasDelPiso;

    // --- Listeners de cambio de sala (HUD, logs, etc.) ---
    private final List<ListenerCambioSala> listenersCambioSala = new ArrayList<>();

    // --- Cola de puertas tocadas (para evitar modificar Box2D dentro del callback) ---
    private final List<DatosPuerta> puertasPendientes = new ArrayList<>();

    // --- Flags ---
    private boolean debugFisica = true;

    // Cooldown para evitar ping-pong de puertas
    private int framesBloqueoPuertas = 0;

    public JuegoPrincipal(Principal game) {
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

    @Override
    public void show() {
        // 1) Render básico
        batch = new SpriteBatch();

        // 🔥 ShapeRenderer del mundo
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

        // 3) Cámara
        camaraSala = new CamaraDeSala(512f, 512f);
        camaraSala.setFactorLerp(0f);
        camaraSala.centrarEn(salaActual);

        // 4) Mundo físico
        world = new World(new Vector2(0, 0), true);
        Gdx.app.log("JuegoPrincipal", "World principal hash=" + System.identityHashCode(world));
        fisica = new FisicaMundo(world);

        // 5) Mapa Tiled
        mapaTiled = new TmxMapLoader().load("TMX/mapa.tmx");
        mapaRenderer = new OrthogonalTiledMapRenderer(mapaTiled, 1f);
        ColisionesDesdeTiled.crearColisiones(mapaTiled, world);

        // 6) Calcular spawn jugador (centro sala inicial)
        float baseX = salaActual.gridX * salaActual.ancho;
        float baseY = salaActual.gridY * salaActual.alto;
        float px = baseX + salaActual.ancho / 2f;
        float py = baseY + salaActual.alto / 2f;

        // 7) Crear jugador
        jugador = new Jugador(
            "Jugador 1",
            Genero.MASCULINO,
            Estilo.CLASICO,
            world,
            px,
            py
        );

        Vector2 posJugador = jugador.getCuerpoFisico().getPosition();
        camaraSala.centrarEn(posJugador.x, posJugador.y);

        // 8) Gestor de entidades (🔥 ANTES de crear puertas visuales)
        gestorEntidades = new GestorDeEntidades(world, jugador);

        // 9) Generar paredes + sensores (y registrar puertas visuales)
        GeneradorParedesSalas genParedes = new GeneradorParedesSalas(fisica, disposicion);

        genParedes.generar((fixture, origen, destino, dir) -> {

            // tamaño visual coherente con el sensor
            float w = (dir == Direccion.ESTE || dir == Direccion.OESTE) ? 16f : 96f;
            float h = (dir == Direccion.ESTE || dir == Direccion.OESTE) ? 96f : 16f;

            Vector2 p = fixture.getBody().getPosition();

            // ⚠️ PuertaVisual espera x,y,w,h como rect (abajo-izq + tamaño)
            // el body está en el centro, así que convertimos:
            PuertaVisual visual = new PuertaVisual(
                p.x - w / 2f,
                p.y - h / 2f,
                w,
                h
            );

            DatosPuerta datos = new DatosPuerta(origen, destino, dir, visual);
            fixture.setUserData(datos);

            // Registrar puerta visual en el gestor
            gestorEntidades.registrarPuertaVisual(origen, visual);
        });

        // 10) Control de jugador
        controlJugador = new ControlJugador(jugador, jugador.getCuerpoFisico());

        // 11) Gestor de salas
        gestorSalas = new GestorSalas(disposicion, fisica, camaraSala, salaActual, jugador);

        // 12) HUD
        hud = new HudJuego(disposicion, jugador);
        hud.actualizarSalaActual(salaActual);
        agregarListenerCambioSala(hud);

        // 13) ContactListener (puertas)
        fisica.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                manejarContacto(contact.getFixtureA());
                manejarContacto(contact.getFixtureB());
            }

            @Override public void endContact(Contact contact) {}
            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }

    /** Encola contactos; no modificamos Box2D dentro del callback. */
    private void manejarContacto(Fixture fixture) {
        if (fixture == null) return;
        if (framesBloqueoPuertas > 0) return;

        Object ud = fixture.getUserData();
        if (ud instanceof DatosPuerta puerta) {
            if (salaActual == puerta.origen() || salaActual == puerta.destino()) {
                puertasPendientes.add(puerta);
            }
        }
    }

    private void procesarPuertasPendientes() {
        if (puertasPendientes.isEmpty()) return;

        if (framesBloqueoPuertas > 0) {
            puertasPendientes.clear();
            return;
        }

        Body cuerpoJugador = jugador.getCuerpoFisico();
        if (cuerpoJugador == null) {
            puertasPendientes.clear();
            return;
        }

        DatosPuerta puerta = puertasPendientes.get(0);

        System.out.println("[JuegoPrincipal] Cambio de sala: "
            + puerta.origen().nombreVisible + " por " + puerta.direccion());

        gestorSalas.irASalaVecinaPorPuerta(puerta, cuerpoJugador);

        Habitacion nueva = gestorSalas.getSalaActual();
        if (nueva != null) {
            Habitacion anterior = salaActual;
            salaActual = nueva;
            disposicion.descubrir(salaActual);
            notificarCambioSala(anterior, salaActual);
        }

        framesBloqueoPuertas = 15;
        puertasPendientes.clear();
    }

    @Override
    public void render(float delta) {
        if (world == null) return;

        gestorEntidades.actualizar(delta, salaActual);

        if (controlJugador != null) {
            controlJugador.actualizar(delta);
        }

        fisica.step();
        procesarPuertasPendientes();

        if (framesBloqueoPuertas > 0) framesBloqueoPuertas--;

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camaraSala.update(delta);

        if (mapaRenderer != null) {
            mapaRenderer.setView(camaraSala.getCamara());
            mapaRenderer.render();
        }

        // 🔥 Render puertas del mundo con ShapeRenderer
        shapeRendererMundo.setProjectionMatrix(camaraSala.getCamara().combined);
        shapeRendererMundo.begin(ShapeRenderer.ShapeType.Filled);


        // 7.5) Dibujar puertas visuales (mundo)
        gestorEntidades.renderPuertas(shapeRendererMundo, salaActual);
        shapeRendererMundo.end();


        // Render entidades
        batch.setProjectionMatrix(camaraSala.getCamara().combined);
        batch.begin();
        gestorEntidades.render(batch);
        batch.end();

        if (debugFisica) {
            fisica.debugDraw(camaraSala.getCamara());
        }

        if (hud != null) {
            hud.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (camaraSala != null) camaraSala.getViewport().update(width, height, true);
        if (hud != null) hud.resize(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (mapaRenderer != null) mapaRenderer.dispose();
        if (mapaTiled != null) mapaTiled.dispose();
        if (batch != null) batch.dispose();
        if (shapeRendererMundo != null) shapeRendererMundo.dispose();
        if (fisica != null) fisica.dispose();
        if (hud != null) hud.dispose();
    }
}

package pantallas;

import camara.CamaraDeSala;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import control.GestorSalas;
import control.ControlPuzzlePorSala;
import entidades.ControlJugador;
import entidades.Genero;
import entidades.Jugador;
import entidades.GestorDeEntidades;
import entidades.Estilo;
import fisica.BotonesDesdeTiled;
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
    private ControlPuzzlePorSala controlPuzzle;

    // --- Gestión de salas (cambio de habitación + cámara) ---
    private GestorSalas gestorSalas;

    // --- Jugador + control ---
    private Jugador jugador1;
    private Jugador jugador2;
    private ControlJugador controlJugador;
    private GestorDeEntidades gestorEntidades;


    private PuzzleSala puzzleSala;


    // --- HUD ---
    private HudJuego hud;
    public List<Habitacion> salasDelPiso;

    // --- Listeners de cambio de sala (HUD, logs, etc.) ---
    private final List<ListenerCambioSala> listenersCambioSala = new ArrayList<>();

    // --- Cola de puertas tocadas (para evitar modificar Box2D dentro del callback) ---
    private final List<DatosPuerta> puertasPendientes = new ArrayList<>();

    // arriba con las otras colas
    private final List<entidades.Item> itemsPendientes = new ArrayList<>();

    private final List<EventoBoton> eventosBoton = new ArrayList<>();


    // --- Flags ---
    private boolean debugFisica = true;

    // Cooldown para evitar ping-pong de puertas
    private int framesBloqueoPuertas = 0;

    // Modo temporal para testear con 1 solo jugador
    private static final boolean MODO_UN_JUGADOR = true;

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

        controlPuzzle = new ControlPuzzlePorSala();
        controlPuzzle.alEntrarASala(salaActual);


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
        BotonesDesdeTiled.crearBotones(mapaTiled, world);

        // 6) Calcular spawn jugador (centro sala inicial)
        float baseX = salaActual.gridX * salaActual.ancho;
        float baseY = salaActual.gridY * salaActual.alto;
        float px = baseX + salaActual.ancho / 2f;
        float py = baseY + salaActual.alto / 2f;

        // 7) Crear jugador
        jugador1 = new Jugador("Jugador 1", Genero.MASCULINO, Estilo.CLASICO);
        jugador2 = new Jugador("Jugador 2", Genero.FEMENINO, Estilo.CLASICO);

        puzzleSala = new PuzzleSala();


        // 8) Gestor de entidades (🔥 ANTES de crear puertas visuales)
        gestorEntidades = new GestorDeEntidades(world, jugador1);

        gestorEntidades.crearJugadorEnSalaInicial(salaActual, px, py);

        // 9) Generar paredes + sensores (y registrar puertas visuales)
        GeneradorParedesSalas genParedes = new GeneradorParedesSalas(fisica, disposicion);

        genParedes.generar((fixture, origen, destino, dir) -> {

            Shape shape = fixture.getShape();
            Body body = fixture.getBody();

            if (shape instanceof PolygonShape poly) {

                // Vértices del poly en coordenadas LOCALES del body
                Vector2 v = new Vector2();
                float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
                float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;

                for (int i = 0; i < poly.getVertexCount(); i++) {
                    poly.getVertex(i, v);

                    // Pasar a coordenadas mundo
                    Vector2 w = body.getWorldPoint(v);

                    minX = Math.min(minX, w.x);
                    minY = Math.min(minY, w.y);
                    maxX = Math.max(maxX, w.x);
                    maxY = Math.max(maxY, w.y);
                }

                PuertaVisual visual = new PuertaVisual(
                    minX,
                    minY,
                    (maxX - minX),
                    (maxY - minY)
                );

                DatosPuerta datos = new DatosPuerta(origen, destino, dir, visual);
                fixture.setUserData(datos);

                gestorEntidades.registrarPuertaVisual(origen, visual);
            }

        });

        Vector2 posJugador = jugador1.getCuerpoFisico().getPosition();
        camaraSala.centrarEn(posJugador.x, posJugador.y);

        // 10) Control de jugador
        controlJugador = new ControlJugador(jugador1);

        // 11) Gestor de salas
        gestorSalas = new GestorSalas(disposicion, fisica, camaraSala, salaActual, jugador1);

        // 12) HUD
        hud = new HudJuego(disposicion, jugador1);
        hud.actualizarSalaActual(salaActual);
        agregarListenerCambioSala(hud);

        // 13) ContactListener (puertas)
        fisica.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {

                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();

                manejarContacto(contact.getFixtureA());
                manejarContacto(contact.getFixtureB());

                // 🔥 nuevo: intentar detectar pickup
                intentarEncolarPickup(a, b);
                intentarEncolarPickup(b, a);

                intentarEncolarBoton(a, b, true);  // DOWN
                intentarEncolarBoton(b, a, true);  // DOWN

            }

            @Override
            public void endContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();

                intentarEncolarBoton(a, b, false); // UP
                intentarEncolarBoton(b, a, false); // UP
            }
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
        if (controlPuzzle != null && controlPuzzle.estaBloqueada(salaActual)) {
            puertasPendientes.clear();
            return;
        }

        if (puertasPendientes.isEmpty()) return;

        if (framesBloqueoPuertas > 0) {
            puertasPendientes.clear();
            return;
        }

        DatosPuerta puerta = puertasPendientes.get(0);

        gestorSalas.irASalaVecinaPorPuerta(puerta);

        Habitacion nueva = gestorSalas.getSalaActual();
        if (nueva != null) {
            Habitacion anterior = salaActual;
            salaActual = nueva;
            disposicion.descubrir(salaActual);
            notificarCambioSala(anterior, salaActual);

            if (controlPuzzle != null) controlPuzzle.alEntrarASala(salaActual);
        }

        framesBloqueoPuertas = 15;
        puertasPendientes.clear();
    }


    private void intentarEncolarPickup(Fixture jugadorFx, Fixture otroFx) {
        if (jugadorFx == null || otroFx == null) return;

        if (!esJugador(jugadorFx)) return;

        Object ud = otroFx.getUserData();
        if (ud instanceof entidades.Item item) {
            itemsPendientes.add(item);
        }
    }

    private void intentarEncolarBoton(Fixture jugadorFx, Fixture otroFx, boolean down) {
        if (jugadorFx == null || otroFx == null) return;

        int jugadorId = getJugadorId(jugadorFx);
        if (jugadorId == -1) return;

        Object ud = otroFx.getUserData();
        if (ud instanceof mapa.DatosBoton db) {
            eventosBoton.add(new EventoBoton(db, jugadorId, down));
        }
    }




    private boolean esJugador(Fixture fx) {
        Object ud = fx.getUserData();
        if ("jugador".equals(ud)) return true;

        // fallback: si tu Jugador guarda el userData en el Body
        Body b = fx.getBody();
        if (b != null && b.getUserData() instanceof entidades.Jugador) return true;

        return false;
    }

    private int getJugadorId(Fixture fx) {
        Object ud = fx.getUserData();
        if ("jugador".equals(ud)) return 1; // por ahora
        if (fx.getBody() != null && fx.getBody().getUserData() instanceof entidades.Jugador) return 1;
        return -1;
    }



    @Override
    public void render(float delta) {
        if (world == null) return;

        gestorEntidades.actualizar(delta, salaActual);

        if (controlJugador != null) {
            controlJugador.actualizar(delta);
        }

        fisica.step(delta);
        procesarPuertasPendientes();
        procesarItemsPendientes();
        procesarBotonesPendientes();



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

    private void procesarItemsPendientes() {
        if (itemsPendientes.isEmpty()) return;

        // procesá 1 por frame o todos; yo haría todos
        for (entidades.Item it : itemsPendientes) {
            gestorEntidades.recogerItem(it);
        }
        itemsPendientes.clear();
    }

    private void procesarBotonesPendientes() {
        if (eventosBoton.isEmpty()) return;

        for (EventoBoton ev : eventosBoton) {
            mapa.DatosBoton boton = ev.boton();
            int jugadorId = ev.jugadorId();

            // Solo botones de la sala actual
            if (boton.sala() != salaActual) continue;

            // Validación: quién puede presionar qué botón
            boolean valido = (jugadorId == boton.jugadorId());

            // TEMP: modo 1 jugador -> dejamos "latched" (no mantenido) para testear
            if (MODO_UN_JUGADOR && jugadorId == 1) {
                // En modo 1 jugador, ignoramos UP y tratamos DOWN como “presionado permanente”
                if (!ev.down()) continue;

                boolean desbloqueo = controlPuzzle.botonDown(salaActual, boton.jugadorId());
                if (desbloqueo) Gdx.app.log("PUZZLE", "Sala desbloqueada: " + salaActual.nombreVisible);
                continue;
            }

            if (!valido) continue;

            if (ev.down()) {
                boolean desbloqueo = controlPuzzle.botonDown(salaActual, boton.jugadorId());
                if (desbloqueo) {
                    Gdx.app.log("PUZZLE", "Sala desbloqueada: " + salaActual.nombreVisible);
                }
            } else {
                controlPuzzle.botonUp(salaActual, boton.jugadorId());
            }
        }

        eventosBoton.clear();
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

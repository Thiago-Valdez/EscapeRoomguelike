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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

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

    // --- Cola de puertas tocadas (para evitar modificar Box2D dentro del callback) ---
    private final List<DatosPuerta> puertasPendientes = new ArrayList<>();

    // --- Flags ---
    private boolean debugFisica = true;

    // Cooldown para evitar ping-pong de puertas
    private int framesBloqueoPuertas = 0;

    public JuegoPrincipal(Principal game) {
        this.game = game;
        // No creamos World ni cosas pesadas acá; todo va en show().
    }

    @Override
    public void show() {
        // 1) Render básico
        batch = new SpriteBatch();

        // 2) Generar mapa lógico (camino de habitaciones)
        GeneradorMapa.Configuracion cfg = new GeneradorMapa.Configuracion();
        cfg.nivel = 1;
        cfg.semilla = System.currentTimeMillis(); // o una fija si querés repetir niveles

        // 2.1) Lista de TODAS las habitaciones definidas en el enum
        List<Habitacion> todasLasHabitaciones = Arrays.asList(Habitacion.values());

        // 2.2) Grafo de puertas (todas las conexiones lógicas posibles)
        GrafoPuertas grafo = new GrafoPuertas(todasLasHabitaciones, new Random(cfg.semilla));

        // 2.3) Generador de mapa que usa ese grafo
        GeneradorMapa generador = new GeneradorMapa(cfg, grafo);
        disposicion = generador.generar();

        // Sala inicial = INICIO_1 y la marcamos como descubierta
        salaActual = disposicion.salaInicio();
        disposicion.descubrir(salaActual);

        // 3) Cámara de sala (una habitación = 512x512 px)
        camaraSala = new CamaraDeSala(512f, 512f);
        camaraSala.setFactorLerp(0f); // snap instantáneo
        camaraSala.centrarEn(salaActual);

        // 4) Mundo físico (Box2D, un solo World para todo)
        world = new World(new Vector2(0, 0), true);
        Gdx.app.log("JuegoPrincipal", "World principal hash=" + System.identityHashCode(world));

        // ⚠️ Asegurate de que FisicaMundo use *este* world, y NO cree uno nuevo adentro.
        fisica = new FisicaMundo(world);

        // 5) Mapa Tiled (arte)
        mapaTiled = new TmxMapLoader().load("TMX/mapa.tmx");
        mapaRenderer = new OrthogonalTiledMapRenderer(mapaTiled, 1f);

        // Colisiones dibujadas en Tiled (capa "colision")
        ColisionesDesdeTiled.crearColisiones(mapaTiled, world);

        // 6) Generar paredes + sensores de puertas SOLO para las salas del camino
        GeneradorParedesSalas genParedes =
            new GeneradorParedesSalas(fisica, disposicion, grafo);

        // ListenerPuerta -> acá es donde asociamos DatosPuerta a cada fixture de sensor
        genParedes.generar((fixture, origen, destino, dir) -> {
            DatosPuerta datos = new DatosPuerta(origen, destino, dir);
            fixture.setUserData(datos);
        });

        // 7) Calcular posición inicial del jugador (centro de salaActual)
        float baseX = salaActual.gridX * salaActual.ancho;
        float baseY = salaActual.gridY * salaActual.alto;
        float px = baseX + salaActual.ancho / 2f;
        float py = baseY + salaActual.alto / 2f;

        // 8) Crear jugador (stats + estética + cuerpo físico en este World)
        jugador = new Jugador(
            "Jugador 1",
            Genero.MASCULINO,
            Estilo.CLASICO,
            world,
            px,
            py
        );

        Vector2 pos = jugador.getCuerpoFisico().getPosition();
        camaraSala.centrarEn(pos.x, pos.y);

        // 9) Gestor de entidades
        gestorEntidades = new GestorDeEntidades(world, jugador);

        // 10) Control de jugador (input)
        controlJugador = new ControlJugador(jugador, jugador.getCuerpoFisico());

        // 11) Gestor de salas (lógica de cambio de sala + cámara)
        gestorSalas = new GestorSalas(disposicion, fisica, camaraSala, salaActual, jugador);

        // 12) HUD
        hud = new HudJuego(disposicion, jugador);
        hud.actualizarSalaActual(salaActual);

        // 13) ContactListener (puertas + items)
        fisica.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();
                manejarContacto(a);
                manejarContacto(b);
            }

            @Override public void endContact(Contact contact) {}
            @Override public void preSolve(Contact contact, Manifold oldManifold) {}
            @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
        });
    }


    /** Encola contactos; no modificamos Box2D dentro del callback. */
    private void manejarContacto(Fixture fixture) {
        if (fixture == null) return;

        // Si estamos en cooldown, ignoramos contactos de puertas
        if (framesBloqueoPuertas > 0) return;

        Object ud = fixture.getUserData();
        if (ud instanceof DatosPuerta puerta) {
            puertasPendientes.add(puerta);
        }

        // Aquí podrías manejar también ítems, encolando su recogida.
    }


    /**
     * Procesa la cola de puertas *después* del world.step().
     * Solo procesa UNA puerta por frame y activa un cooldown
     * para evitar ping-pong inmediato.
     */
    private void procesarPuertasPendientes() {
        if (puertasPendientes.isEmpty()) return;

        // Si todavía estamos bloqueando puertas, tiramos la cola y salimos
        if (framesBloqueoPuertas > 0) {
            puertasPendientes.clear();
            return;
        }

        Body cuerpoJugador = jugador.getCuerpoFisico();
        if (cuerpoJugador == null) {
            puertasPendientes.clear();
            return;
        }

        // Procesamos SOLO la primera puerta detectada
        DatosPuerta puerta = puertasPendientes.get(0);

        System.out.println("[JuegoPrincipal] Cambio de sala: "
            + puerta.origen().nombreVisible + " por " + puerta.direccion());

        // GestorSalas se encarga de cambiar sala + cámara + reposicionar jugador
        gestorSalas.irASalaVecinaPorPuerta(puerta, cuerpoJugador);

        // Actualizamos referencia de sala actual desde GestorSalas
        Habitacion nueva = gestorSalas.getSalaActual();
        if (nueva != null) {
            salaActual = nueva;
            disposicion.descubrir(salaActual);
            hud.actualizarSalaActual(salaActual);
        }

        // Activamos cooldown (~0.25s si vas a 60 FPS)
        framesBloqueoPuertas = 15;

        // Limpiamos la cola
        puertasPendientes.clear();
    }


    @Override
    public void render(float delta) {
        if (world == null) return; // por seguridad

        // 1) Actualizar lógica de entidades (IA, timers, ítems en BOTIN, etc.)
        gestorEntidades.actualizar(delta, salaActual);

        // 2) Actualizar jugador (input -> velocidad del body)
        if (controlJugador != null) {
            controlJugador.actualizar(delta);
        }

        // 3) Paso de física (world.step por dentro de FisicaMundo)
        fisica.step();

        // 4) Procesar cambios de sala fuera del callback de Box2D
        procesarPuertasPendientes();


        // Reducir cooldown de puertas
        if (framesBloqueoPuertas > 0) {
            framesBloqueoPuertas--;
        }

        // 5) Limpiar pantalla
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 6) Actualizar cámara (lerp si está activado)
        camaraSala.update(delta);

        // 7) Dibujar mapa Tiled
        if (mapaRenderer != null) {
            mapaRenderer.setView(camaraSala.getCamara());
            mapaRenderer.render();
        }

        // 8) Dibujar entidades (jugador, ítems, etc.)
        batch.setProjectionMatrix(camaraSala.getCamara().combined);
        batch.begin();
        gestorEntidades.render(batch);
        batch.end();

        // 9) Debug de Box2D
        if (debugFisica) {
            fisica.debugDraw(camaraSala.getCamara());
            //fisica.debugLogJugador(); // dejalo mientras estés debuggeando
        }

        // 10) HUD
        if (hud != null) {
            hud.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (camaraSala != null) {
            camaraSala.getViewport().update(width, height, true);
        }
        if (hud != null) {
            hud.resize(width, height);
        }
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
        if (fisica != null) fisica.dispose();
        // Si FisicaMundo NO se encarga de dispose del World, entonces:
        // if (world != null) world.dispose();
        if (hud != null) hud.dispose();
    }
}

package juego;

import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;

import camara.CamaraDeSala;
import control.input.ControlJugador;
import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;
import entidades.Entidad;
import entidades.GestorDeEntidades;
import entidades.datos.*;
import entidades.enemigos.*;
import entidades.eventos.*;
import entidades.items.*;
import entidades.personajes.*;
import entidades.sprites.*;
import fisica.BotonesDesdeTiled;
import fisica.ColisionesDesdeTiled;
import fisica.FisicaMundo;
import fisica.GeneradorSensoresPuertas;
import interfaces.hud.HudJuego;
import interfaces.listeners.ListenerCambioSala;
import io.github.principal.Principal;
import juego.contactos.EnrutadorContactosPartida;
import juego.inicializacion.ContextoPartida;
import juego.inicializacion.InicializadorPartida;
import juego.inicializacion.InicializadorSensoresPuertas;
import juego.inicializacion.InicializadorSpritesPartida;
import juego.sistemas.CanalRenderizadoPartida;
import juego.sistemas.ProcesadorColasEventos;
import juego.sistemas.SistemaActualizacionPartida;
import juego.sistemas.SistemaSpritesEntidades;
import juego.sistemas.SistemaTransicionSala;
import mapa.botones.*;
import mapa.generacion.*;
import mapa.minimapa.*;
import mapa.model.*;
import mapa.puertas.*;

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

    // --- HUD ---
    private HudJuego hud;
    public List<Habitacion> salasDelPiso;

    // --- Listeners de cambio de sala (HUD, logs, etc.) ---
    private final List<ListenerCambioSala> listenersCambioSala = new ArrayList<>();

    // --- Colas de eventos (evita modificar Box2D dentro del callback) ---
    private final List<EventoPuerta> puertasPendientes = new ArrayList<>();
    private final List<EventoPickup> itemsPendientes = new ArrayList<>();
    private final List<EventoBoton> eventosBoton = new ArrayList<>();
    private final Set<entidades.items.Item> itemsYaProcesados = new HashSet<>();
    private final List<EventoDanio> daniosPendientes = new ArrayList<>();
    private final Set<Integer> jugadoresDanioFrame = new HashSet<>();


    private final List<BotonVisual> botonesVisuales = new ArrayList<>();
    private Texture texBotonRojo;
    private Texture texBotonAzul;
    private TextureRegion[][] framesBotonRojo;
    private TextureRegion[][] framesBotonAzul;

    private final java.util.List<PuertaVisual> puertasVisuales = new java.util.ArrayList<>();
    private Texture texPuertaAbierta;
    private Texture texPuertaCerrada;
    private TextureRegion regPuertaAbierta;
    private TextureRegion regPuertaCerrada;




    // --- Sistemas (extraídos) ---
    private final SistemaTransicionSala sistemaTransicionSala = new SistemaTransicionSala();
    private final ProcesadorColasEventos procesadorColasEventos = new ProcesadorColasEventos();

    // Sistemas extraídos
    private SistemaSpritesEntidades sistemaSprites;
    private SistemaActualizacionPartida sistemaActualizacion;
    private CanalRenderizadoPartida canalRenderizado;

    // --- Flags ---
    private boolean debugFisica = true;

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
        // 1) Construcción inicial (render + mapa lógico + fisica + tiled + jugadores + HUD)
        ContextoPartida ctx = InicializadorPartida.crearContextoInicial();
        aplicarContexto(ctx);

        // 1.1) Sprites de botones + botones desde Tiled (sensores + visuales)
        cargarSpritesBotones();

        // pixel art sin blur cuando se escalan
        if (texBotonRojo != null) texBotonRojo.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        if (texBotonAzul != null) texBotonAzul.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // crear sensores + visuales (una sola vez)
        BotonesDesdeTiled.crearBotones(mapaTiled, world, framesBotonRojo, framesBotonAzul, botonesVisuales);

        // 2) Sprites
        sistemaSprites = InicializadorSpritesPartida.crearSistemaSprites(gestorEntidades, jugador1, jugador2);

        // 3) Sensores de puertas (generador)
        InicializadorSensoresPuertas.generarSensoresPuertas(fisica, disposicion, reg -> {

            PuertaVisual pv = reg.visual();
            pv.setFrames(regPuertaAbierta, regPuertaCerrada);
            puertasVisuales.add(pv);

            gestorEntidades.registrarPuertaVisual(reg.origen(), reg.visual());
        });

        // 4) Listeners HUD
        agregarListenerCambioSala(hud);

        // 5) Sistemas
        sistemaActualizacion = new SistemaActualizacionPartida(
                gestorEntidades,
                fisica,
                camaraSala,
                sistemaTransicionSala,
                procesadorColasEventos,
                sistemaSprites
        );

        canalRenderizado = new CanalRenderizadoPartida(
                camaraSala,
                mapaRenderer,
                shapeRendererMundo,
                batch,
                fisica,
                hud,
                gestorEntidades,
                sistemaSprites
        );

        // 6) Contactos
        fisica.setContactListener(new EnrutadorContactosPartida(this));


    }

    private void aplicarContexto(ContextoPartida ctx) {
        this.world = ctx.world;
        this.fisica = ctx.fisica;

        this.batch = ctx.batch;
        this.shapeRendererMundo = ctx.shapeRendererMundo;

        this.mapaTiled = ctx.mapaTiled;
        this.mapaRenderer = ctx.mapaRenderer;

        this.camaraSala = ctx.camaraSala;

        this.disposicion = ctx.disposicion;
        this.salaActual = ctx.salaActual;
        this.controlPuzzle = ctx.controlPuzzle;
        this.salasDelPiso = ctx.salasDelPiso;

        this.gestorEntidades = ctx.gestorEntidades;
        this.gestorSalas = ctx.gestorSalas;

        this.jugador1 = ctx.jugador1;
        this.jugador2 = ctx.jugador2;
        this.controlJugador1 = ctx.controlJugador1;
        this.controlJugador2 = ctx.controlJugador2;

        this.hud = ctx.hud;
    }

    private void cargarSpritesBotones() {
        // Carga texturas
        texBotonRojo = new Texture(Gdx.files.internal("Botones/boton_rojo.png"));
        texBotonAzul = new Texture(Gdx.files.internal("Botones/boton_azul.png"));

        // Split: 1 fila x 2 columnas (UP/DOWN)
        framesBotonRojo = TextureRegion.split(
            texBotonRojo,
            texBotonRojo.getWidth() / 2,
            texBotonRojo.getHeight()
        );

        framesBotonAzul = TextureRegion.split(
            texBotonAzul,
            texBotonAzul.getWidth() / 2,
            texBotonAzul.getHeight()
        );

        // Validación rápida (para detectar spritesheet mal cortado)
        if (framesBotonRojo.length < 1 || framesBotonRojo[0].length < 2) {
            throw new IllegalStateException("Spritesheet rojo inválido. Se esperaba 1x2 (UP/DOWN).");
        }
        if (framesBotonAzul.length < 1 || framesBotonAzul[0].length < 2) {
            throw new IllegalStateException("Spritesheet azul inválido. Se esperaba 1x2 (UP/DOWN).");
        }
    }

    private void cargarSpritesPuertas() {
        texPuertaAbierta = new Texture(Gdx.files.internal("Puertas/puerta_abierta.png"));
        texPuertaCerrada = new Texture(Gdx.files.internal("Puertas/puerta_cerrada.png"));

        texPuertaAbierta.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texPuertaCerrada.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        regPuertaAbierta = new TextureRegion(texPuertaAbierta);
        regPuertaCerrada = new TextureRegion(texPuertaCerrada);
    }



    // ==========================
    // GETTERS mínimos (para sistemas / enrutador de contactos)
    // ==========================

    public Habitacion getSalaActual() { return salaActual; }
    public SistemaTransicionSala getSistemaTransicionSala() { return sistemaTransicionSala; }
    public List<EventoPuerta> getPuertasPendientes() { return puertasPendientes; }
    public List<EventoPickup> getItemsPendientes() { return itemsPendientes; }
    public List<EventoBoton> getEventosBoton() { return eventosBoton; }

    public void aplicarDanioPorEnemigo(Jugador jugador, Enemigo enemigo) {
        if (jugador == null) return;

        // Evitar repetir daño si ya está en muerte o inmune
        if (jugador.estaEnMuerte() || jugador.esInmune() || !jugador.estaViva()) return;

        jugador.recibirDanio();

        // iniciar animación de muerte del sprite
        if (sistemaSprites != null) sistemaSprites.iniciarMuerte(jugador);

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

        if (sistemaActualizacion != null) {
            salaActual = sistemaActualizacion.actualizar(
                    delta,
                    salaActual,
                    jugador1,
                    jugador2,
                    controlJugador1,
                    controlJugador2,
                    puertasPendientes,
                    itemsPendientes,
                    itemsYaProcesados,
                    eventosBoton,
                    daniosPendientes,
                    jugadoresDanioFrame,
                    botonesVisuales,
                    controlPuzzle,
                    gestorSalas,
                    disposicion,
                    this::notificarCambioSala,
                    mapaTiled,
                    world
            );
        }

        if (canalRenderizado != null) {
            canalRenderizado.render(delta, salaActual, debugFisica, jugador1, jugador2, botonesVisuales);
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

        if (sistemaSprites != null) {
            sistemaSprites.dispose();
        }

        if (texBotonRojo != null) texBotonRojo.dispose();
        if (texBotonAzul != null) texBotonAzul.dispose();

        if (texPuertaAbierta != null) texPuertaAbierta.dispose();
        if (texPuertaCerrada != null) texPuertaCerrada.dispose();

        world = null;
    }
}

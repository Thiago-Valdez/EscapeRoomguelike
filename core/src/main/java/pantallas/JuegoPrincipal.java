package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.*;
import entidades.GestorDeEntidades;
import entidades.Jugador;
import fisica.FisicaMundo;
import fisica.GeneradorParedesSalas;
import io.github.principal.Principal;
import mapa.*;
import interfaces.HudJuego;

import java.util.*;

public class JuegoPrincipal implements Screen {

    private final Principal game;

    // Cámara del mundo
    private OrthographicCamera camara;

    // Física
    private FisicaMundo fisica;
    private ShapeRenderer shapeMundo; // debug visual del jugador

    // Mapa
    private GeneradorMapa generador;
    private DisposicionMapa disposicion;
    private DisposicionMapa.Colocacion salaActual;
    private final Set<DisposicionMapa.Colocacion> salasDescubiertas = new HashSet<>();

    // Gestor de entidades (jugador + pickups)
    private GestorDeEntidades gestorEntidades;

    // HUD
    private HudJuego hud;

    // Offset seguro desde la pared para spawnear al jugador al cambiar de sala
    private float safeOffsetFromWall;

    // ----- Datos de puertas -----
    public static class DatosPuerta {
        public final DisposicionMapa.Colocacion origen;
        public final DisposicionMapa.Colocacion destino;
        public final Direccion direccion;

        public DatosPuerta(DisposicionMapa.Colocacion origen,
                           DisposicionMapa.Colocacion destino,
                           Direccion direccion) {
            this.origen = origen;
            this.destino = destino;
            this.direccion = direccion;
        }
    }

    private final Map<Fixture, DatosPuerta> puertaPorFixture = new HashMap<>();
    private DatosPuerta puertaPendiente; // se procesa después de world.step()

    public JuegoPrincipal(Principal game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Cámara del mundo (tamaño de una habitación)
        camara = new OrthographicCamera();
        camara.setToOrtho(false, 512, 512);

        shapeMundo = new ShapeRenderer();
        shapeMundo.setColor(Color.YELLOW);

        // ----- Generar mapa -----
        GeneradorMapa.Configuracion cfg = new GeneradorMapa.Configuracion();
        cfg.nivel = 1;
        generador = new GeneradorMapa(cfg);
        disposicion = generador.generar();

        salaActual = buscarPrimeraSalaDeTipo(disposicion, TipoSala.INICIO);
        if (salaActual == null) {
            throw new IllegalStateException("No se encontró sala de inicio.");
        }

        salasDescubiertas.clear();
        salasDescubiertas.add(salaActual);

        // ----- Física -----
        fisica = new FisicaMundo();

        // ----- Gestor de entidades -----
        gestorEntidades = new GestorDeEntidades(fisica, disposicion);
        gestorEntidades.crearJugadorEnSalaInicial(salaActual);
        gestorEntidades.crearPickupsEnSalasBotin();

        // Calculamos offset seguro usando el radio del jugador
        safeOffsetFromWall =
            GeneradorParedesSalas.WALL_THICKNESS
                + GeneradorParedesSalas.DOOR_WIDTH / 2f
                + gestorEntidades.getPlayerRadius()
                + 4f;

        // ----- Paredes + sensores de puertas -----
        GeneradorParedesSalas generadorParedes = new GeneradorParedesSalas(fisica, disposicion);
        generadorParedes.generar((fixture, origen, destino, dir) -> {
            DatosPuerta datos = new DatosPuerta(origen, destino, dir);
            fixture.setUserData(datos);
            puertaPorFixture.put(fixture, datos);
        });

        // ----- ContactListener (puertas + pickups) -----
        configurarContactListener();

        // ----- HUD -----
        Jugador jugador = gestorEntidades.getJugador();
        hud = new HudJuego(disposicion, salaActual, salasDescubiertas, jugador);

        System.out.println("[JuegoPrincipal] show() listo");
    }

    // ---------------------------------------------------------
    // ContactListener (puertas + pickups)
    // ---------------------------------------------------------
    private void configurarContactListener() {
        fisica.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();
                procesarContacto(a, b);
                procesarContacto(b, a);
            }

            @Override public void endContact(Contact contact) { }
            @Override public void preSolve(Contact contact, Manifold oldManifold) { }
            @Override public void postSolve(Contact contact, ContactImpulse impulse) { }
        });
    }

    private void procesarContacto(Fixture fA, Fixture fB) {
        if (!(fA.getUserData() instanceof Jugador)) return;
        Object data = fB.getUserData();

        if (data instanceof DatosPuerta datos) {
            // Marcar puerta para procesar luego de world.step()
            puertaPendiente = datos;
        } else if (gestorEntidades != null && gestorEntidades.esPickup(data)) {
            gestorEntidades.marcarPickupRecogido(data);
        }
    }

    // ---------------------------------------------------------
    // Render / loop principal
    // ---------------------------------------------------------
    @Override
    public void render(float delta) {
        // 1) Input / movimiento del jugador
        if (gestorEntidades != null) {
            gestorEntidades.actualizarMovimientoJugador();
        }

        // 2) Paso de física
        fisica.step();

        // 3) Cambios de sala después del step
        if (puertaPendiente != null) {
            onJugadorEntraPuerta(puertaPendiente);
            puertaPendiente = null;
        }

        // 4) Procesar pickups recogidos (aplica ítems al jugador)
        if (gestorEntidades != null) {
            gestorEntidades.procesarPickupsPendientes();
        }

        // 5) Limpiar pantalla
        Gdx.gl.glClearColor(0, 0, 0, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 6) Cámara centrada en la sala actual
        if (salaActual != null) {
            Habitacion h = salaActual.habitacion;
            float cx = salaActual.gx * h.ancho + h.ancho / 2f;
            float cy = salaActual.gy * h.alto + h.alto / 2f;
            camara.position.set(cx, cy, 0);
        }
        camara.update();

        // 7) Debug de Box2D
        fisica.debugDraw(camara);

        // 8) Dibujar al jugador como círculo amarillo (debug visual)
        Body jugadorBody = (gestorEntidades != null) ? gestorEntidades.getJugadorBody() : null;
        if (jugadorBody != null) {
            shapeMundo.setProjectionMatrix(camara.combined);
            shapeMundo.begin(ShapeRenderer.ShapeType.Line);
            shapeMundo.setColor(Color.YELLOW);
            float x = jugadorBody.getPosition().x;
            float y = jugadorBody.getPosition().y;
            shapeMundo.circle(x, y, gestorEntidades.getPlayerRadius());
            shapeMundo.end();
        }

        // 9) HUD
        if (hud != null) {
            hud.render();
        }
    }

    // ---------------------------------------------------------
    // Cambio de sala al atravesar una puerta
    // ---------------------------------------------------------
    private void onJugadorEntraPuerta(DatosPuerta puerta) {
        salaActual = puerta.destino;
        salasDescubiertas.add(salaActual);

        Habitacion hDest = salaActual.habitacion;
        Direccion entrada = puerta.direccion.opuesta(); // lado por el que ENTRAMOS a la nueva sala
        EspecificacionPuerta p = hDest.puertas.get(entrada);

        float baseX = salaActual.gx * hDest.ancho;
        float baseY = salaActual.gy * hDest.alto;

        float spawnX;
        float spawnY;

        if (p == null) {
            // fallback: centro de la sala si falta definición de puerta de entrada
            spawnX = baseX + hDest.ancho / 2f;
            spawnY = baseY + hDest.alto / 2f;
        } else {
            switch (entrada) {
                case NORTE -> {
                    spawnX = baseX + p.localX;
                    spawnY = baseY + hDest.alto - safeOffsetFromWall;
                }
                case SUR -> {
                    spawnX = baseX + p.localX;
                    spawnY = baseY + safeOffsetFromWall;
                }
                case ESTE -> {
                    spawnX = baseX + hDest.ancho - safeOffsetFromWall;
                    spawnY = baseY + p.localY;
                }
                case OESTE -> {
                    spawnX = baseX + safeOffsetFromWall;
                    spawnY = baseY + p.localY;
                }
                default -> {
                    spawnX = baseX + hDest.ancho / 2f;
                    spawnY = baseY + hDest.alto / 2f;
                }
            }
        }

        if (gestorEntidades != null) {
            gestorEntidades.moverJugadorA(spawnX, spawnY);
        }

        if (hud != null) {
            hud.setSalaActual(salaActual);
        }

        System.out.println("[JuegoPrincipal] Cambio a sala: " +
            salaActual.habitacion.nombreVisible + " (" +
            salaActual.gx + "," + salaActual.gy + ")");
    }

    // ---------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------
    private DisposicionMapa.Colocacion buscarPrimeraSalaDeTipo(DisposicionMapa disp, TipoSala tipo) {
        for (DisposicionMapa.Colocacion c : disp.todas()) {
            if (c.habitacion.tipo == tipo) return c;
        }
        return null;
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

    // ---------------------------------------------------------
    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (shapeMundo != null) shapeMundo.dispose();
        if (fisica != null) fisica.dispose();
        if (hud != null) hud.dispose();
    }
}

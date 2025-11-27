package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import fisica.FisicaMundo;
import io.github.principal.Principal;
import mapa.*;
import entidades.Jugador;
import entidades.Genero;
import entidades.Estilo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class JuegoPrincipal implements Screen {

    private final Principal game;

    // Cámara de mundo
    private OrthographicCamera camara;
    // Cámara de HUD (pantalla)
    private OrthographicCamera hudCam;   // 👈 HUD

    // Mundo físico
    private FisicaMundo fisica;

    // Jugador lógico y físico
    private Jugador jugador;
    private Body jugadorBody;

    // Debug y HUD shapes
    private ShapeRenderer debugShape;
    private Matrix4 projDebug;
    private Array<Body> cuerposTemp;
    private final Set<DisposicionMapa.Colocacion> salasDescubiertas = new HashSet<>();


    // Mapa de salas
    private DisposicionMapa disposicion;
    private DisposicionMapa.Colocacion salaActual;

    // Bounds del mapa para el minimapa
    private int minGX, maxGX, minGY, maxGY;   // 👈 HUD minimapa

    // Constantes de paredes y puertas
    private static final float GROSOR_MURO = 6f;
    private static final float ANCHO_PUERTA = 64f;
    private static final float SENSOR_OFFSET = 10f;

    // Datos de puerta
    private static class DatosPuerta {
        final DisposicionMapa.Colocacion origen;
        final DisposicionMapa.Colocacion destino;
        final Direccion direccion;
        final float worldX;
        final float worldY;

        DatosPuerta(DisposicionMapa.Colocacion origen,
                    DisposicionMapa.Colocacion destino,
                    Direccion direccion,
                    float worldX,
                    float worldY) {
            this.origen = origen;
            this.destino = destino;
            this.direccion = direccion;
            this.worldX = worldX;
            this.worldY = worldY;
        }
    }

    private final Map<Fixture, DatosPuerta> puertasPorFixture = new HashMap<>();
    private DatosPuerta puertaPendiente = null;

    public JuegoPrincipal(Principal game) {
        this.game = game;
    }

    @Override
    public void show() {
        // --- 1) Generar mapa ---
        GeneradorMapa.Configuracion cfg = new GeneradorMapa.Configuracion();
        cfg.nivel = 1;
        GeneradorMapa generador = new GeneradorMapa(cfg);
        disposicion = generador.generar();

        salaActual = buscarPrimeraSalaDeTipo(disposicion, TipoSala.INICIO);
        if (salaActual == null) {
            throw new IllegalStateException("No se encontró ninguna sala de tipo INICIO.");
        }
        Habitacion salaIni = salaActual.habitacion;

        // Calcular bounds para minimapa HUD
        calcularBoundsMapa();  // 👈 HUD

        // Al inicio, solo la sala actual está descubierta
        salasDescubiertas.clear();
        salasDescubiertas.add(salaActual);

        // --- 2) Cámara de mundo ---
        camara = new OrthographicCamera();
        camara.setToOrtho(false, salaIni.ancho, salaIni.alto);
        camara.position.set(
            salaActual.gx * salaIni.ancho + salaIni.ancho / 2f,
            salaActual.gy * salaIni.alto + salaIni.alto / 2f,
            0f
        );
        camara.update();

        // --- 3) Cámara de HUD (pantalla en píxeles) ---
        hudCam = new OrthographicCamera();          // 👈 HUD
        hudCam.setToOrtho(false,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight());
        hudCam.update();

        projDebug = new Matrix4();
        debugShape = new ShapeRenderer();
        cuerposTemp = new Array<>();

        // --- 4) Mundo físico y colisiones ---
        fisica = new FisicaMundo();
        crearColisionesSalas();

        // --- 5) Jugador lógico ---
        jugador = new Jugador("Thiago", Genero.HOMBRE, Estilo.CAZADOR);
        jugador.setVelocidad(200f);
        jugador.setVidaMaxima(3);
        jugador.setVida(3);

        // --- 6) Jugador físico ---
        crearJugadorEnSalaActual();

        // --- 7) Contact listener ---
        configurarContactListener();

        System.out.println("[JuegoPrincipal] show() listo");
    }

    // ---------------- Mapa helpers ----------------

    private void calcularBoundsMapa() {
        List<DisposicionMapa.Colocacion> todas = disposicion.todas();
        if (todas.isEmpty()) {
            minGX = maxGX = minGY = maxGY = 0;
            return;
        }
        minGX = maxGX = todas.get(0).gx;
        minGY = maxGY = todas.get(0).gy;
        for (DisposicionMapa.Colocacion c : todas) {
            if (c.gx < minGX) minGX = c.gx;
            if (c.gx > maxGX) maxGX = c.gx;
            if (c.gy < minGY) minGY = c.gy;
            if (c.gy > maxGY) maxGY = c.gy;
        }
    }

    private DisposicionMapa.Colocacion buscarPrimeraSalaDeTipo(DisposicionMapa disp, TipoSala tipo) {
        for (DisposicionMapa.Colocacion c : disp.todas()) {
            if (c.habitacion.tipo == tipo) return c;
        }
        return null;
    }

    private DisposicionMapa.Colocacion buscarSalaPorGrid(DisposicionMapa disp, int gx, int gy) {
        for (DisposicionMapa.Colocacion c : disp.todas()) {
            if (c.gx == gx && c.gy == gy) return c;
        }
        return null;
    }

    private boolean haySalaVecina(DisposicionMapa.Colocacion col, Direccion dir) {
        int gx2 = col.gx + dx(dir);
        int gy2 = col.gy + dy(dir);
        return buscarSalaPorGrid(disposicion, gx2, gy2) != null;
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
            case SUR -> -1;
            default -> 0;
        };
    }

    // ---------------- Colisiones salas ----------------

    private void crearColisionesSalas() {
        for (DisposicionMapa.Colocacion col : disposicion.todas()) {
            crearParedesSala(col);
            crearSensoresPuertas(col);
        }
    }

    private void crearParedesSala(DisposicionMapa.Colocacion col) {
        World world = fisica.world();
        Habitacion h = col.habitacion;

        float baseX = col.gx * h.ancho;
        float baseY = col.gy * h.alto;
        float w = h.ancho;
        float alto = h.alto;

        float doorHalf = ANCHO_PUERTA / 2f;

        // SUR
        boolean puertaSurActiva = h.tienePuerta(Direccion.SUR) && haySalaVecina(col, Direccion.SUR);
        if (puertaSurActiva) {
            float doorCenterX = baseX + w / 2f;

            float x1 = baseX;
            float x2 = doorCenterX - doorHalf;
            float width1 = x2 - x1;
            if (width1 > 0) {
                crearParedRectangular(world,
                    x1 + width1 / 2f,
                    baseY + GROSOR_MURO / 2f,
                    width1 / 2f,
                    GROSOR_MURO / 2f);
            }

            float x3 = doorCenterX + doorHalf;
            float x4 = baseX + w;
            float width2 = x4 - x3;
            if (width2 > 0) {
                crearParedRectangular(world,
                    x3 + width2 / 2f,
                    baseY + GROSOR_MURO / 2f,
                    width2 / 2f,
                    GROSOR_MURO / 2f);
            }

        } else {
            crearParedRectangular(world,
                baseX + w / 2f,
                baseY + GROSOR_MURO / 2f,
                w / 2f,
                GROSOR_MURO / 2f);
        }

        // NORTE
        boolean puertaNorteActiva = h.tienePuerta(Direccion.NORTE) && haySalaVecina(col, Direccion.NORTE);
        if (puertaNorteActiva) {
            float doorCenterX = baseX + w / 2f;

            float x1 = baseX;
            float x2 = doorCenterX - doorHalf;
            float width1 = x2 - x1;
            if (width1 > 0) {
                crearParedRectangular(world,
                    x1 + width1 / 2f,
                    baseY + alto - GROSOR_MURO / 2f,
                    width1 / 2f,
                    GROSOR_MURO / 2f);
            }

            float x3 = doorCenterX + doorHalf;
            float x4 = baseX + w;
            float width2 = x4 - x3;
            if (width2 > 0) {
                crearParedRectangular(world,
                    x3 + width2 / 2f,
                    baseY + alto - GROSOR_MURO / 2f,
                    width2 / 2f,
                    GROSOR_MURO / 2f);
            }

        } else {
            crearParedRectangular(world,
                baseX + w / 2f,
                baseY + alto - GROSOR_MURO / 2f,
                w / 2f,
                GROSOR_MURO / 2f);
        }

        // OESTE
        boolean puertaOesteActiva = h.tienePuerta(Direccion.OESTE) && haySalaVecina(col, Direccion.OESTE);
        if (puertaOesteActiva) {
            float doorCenterY = baseY + alto / 2f;

            float y1 = baseY;
            float y2 = doorCenterY - doorHalf;
            float height1 = y2 - y1;
            if (height1 > 0) {
                crearParedRectangular(world,
                    baseX + GROSOR_MURO / 2f,
                    y1 + height1 / 2f,
                    GROSOR_MURO / 2f,
                    height1 / 2f);
            }

            float y3 = doorCenterY + doorHalf;
            float y4 = baseY + alto;
            float height2 = y4 - y3;
            if (height2 > 0) {
                crearParedRectangular(world,
                    baseX + GROSOR_MURO / 2f,
                    y3 + height2 / 2f,
                    GROSOR_MURO / 2f,
                    height2 / 2f);
            }
        } else {
            crearParedRectangular(world,
                baseX + GROSOR_MURO / 2f,
                baseY + alto / 2f,
                GROSOR_MURO / 2f,
                alto / 2f);
        }

        // ESTE
        boolean puertaEsteActiva = h.tienePuerta(Direccion.ESTE) && haySalaVecina(col, Direccion.ESTE);
        if (puertaEsteActiva) {
            float doorCenterY = baseY + alto / 2f;

            float y1 = baseY;
            float y2 = doorCenterY - doorHalf;
            float height1 = y2 - y1;
            if (height1 > 0) {
                crearParedRectangular(world,
                    baseX + w - GROSOR_MURO / 2f,
                    y1 + height1 / 2f,
                    GROSOR_MURO / 2f,
                    height1 / 2f);
            }

            float y3 = doorCenterY + doorHalf;
            float y4 = baseY + alto;
            float height2 = y4 - y3;
            if (height2 > 0) {
                crearParedRectangular(world,
                    baseX + w - GROSOR_MURO / 2f,
                    y3 + height2 / 2f,
                    GROSOR_MURO / 2f,
                    height2 / 2f);
            }
        } else {
            crearParedRectangular(world,
                baseX + w - GROSOR_MURO / 2f,
                baseY + alto / 2f,
                GROSOR_MURO / 2f,
                alto / 2f);
        }
    }

    private void crearParedRectangular(World world,
                                       float centroX, float centroY,
                                       float halfWidth, float halfHeight) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(centroX, centroY);

        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfWidth, halfHeight);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 0f;
        fd.friction = 0f;
        fd.restitution = 0f;

        body.createFixture(fd);
        shape.dispose();
    }

    private void crearSensoresPuertas(DisposicionMapa.Colocacion col) {
        World world = fisica.world();
        Habitacion h = col.habitacion;

        float baseX = col.gx * h.ancho;
        float baseY = col.gy * h.alto;
        float w = h.ancho;
        float alto = h.alto;

        for (Direccion dir : h.puertas.keySet()) {
            int gx2 = col.gx + dx(dir);
            int gy2 = col.gy + dy(dir);
            DisposicionMapa.Colocacion destino = buscarSalaPorGrid(disposicion, gx2, gy2);
            if (destino == null) continue;

            float doorCenterX = 0;
            float doorCenterY = 0;

            switch (dir) {
                case NORTE -> {
                    doorCenterX = baseX + w / 2f;
                    doorCenterY = baseY + alto - GROSOR_MURO - SENSOR_OFFSET;
                }
                case SUR -> {
                    doorCenterX = baseX + w / 2f;
                    doorCenterY = baseY + GROSOR_MURO + SENSOR_OFFSET;
                }
                case ESTE -> {
                    doorCenterX = baseX + w - GROSOR_MURO - SENSOR_OFFSET;
                    doorCenterY = baseY + alto / 2f;
                }
                case OESTE -> {
                    doorCenterX = baseX + GROSOR_MURO + SENSOR_OFFSET;
                    doorCenterY = baseY + alto / 2f;
                }
            }

            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.StaticBody;
            bd.position.set(doorCenterX, doorCenterY);
            Body cuerpoPuerta = world.createBody(bd);

            PolygonShape shape = new PolygonShape();
            float halfW, halfH;
            if (dir == Direccion.NORTE || dir == Direccion.SUR) {
                halfW = ANCHO_PUERTA / 2f - 2f;
                halfH = 4f;
            } else {
                halfW = 4f;
                halfH = ANCHO_PUERTA / 2f - 2f;
            }
            shape.setAsBox(halfW, halfH);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.isSensor = true;

            Fixture fixture = cuerpoPuerta.createFixture(fd);
            shape.dispose();

            DatosPuerta datos = new DatosPuerta(col, destino, dir, doorCenterX, doorCenterY);
            fixture.setUserData(datos);
            puertasPorFixture.put(fixture, datos);
        }
    }

    // ---------------- Jugador ----------------

    private void crearJugadorEnSalaActual() {
        World world = fisica.world();
        Habitacion h = salaActual.habitacion;

        float x = salaActual.gx * h.ancho + h.ancho / 2f;
        float y = salaActual.gy * h.alto + h.alto / 2f;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);

        jugadorBody = world.createBody(bd);

        CircleShape circle = new CircleShape();
        circle.setRadius(16f);

        FixtureDef fd = new FixtureDef();
        fd.shape = circle;
        fd.density = 1f;
        fd.friction = 0.2f;
        fd.restitution = 0f;

        Fixture fx = jugadorBody.createFixture(fd);
        fx.setUserData(jugador);
        circle.dispose();
    }

    private void configurarContactListener() {
        fisica.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Fixture a = contact.getFixtureA();
                Fixture b = contact.getFixtureB();
                procesarContacto(a, b);
                procesarContacto(b, a);
            }

            private void procesarContacto(Fixture fJugador, Fixture fOtro) {
                if (!(fJugador.getUserData() instanceof Jugador)) return;

                Object data = fOtro.getUserData();
                if (data instanceof DatosPuerta datos) {
                    puertaPendiente = datos;
                }
            }

            @Override public void endContact(Contact contact) { }
            @Override public void preSolve(Contact contact, Manifold oldManifold) { }
            @Override public void postSolve(Contact contact, ContactImpulse impulse) { }
        });
    }

    private void onJugadorEntraPuerta(DatosPuerta puerta) {
        salaActual = puerta.destino;
        Habitacion hDest = salaActual.habitacion;
        salasDescubiertas.add(salaActual);
        System.out.println("[JuegoPrincipal] Cambio a sala: "
            + hDest.nombreVisible + " (" + salaActual.gx + "," + salaActual.gy + ")");


        Direccion dirDesdeDestino = puerta.direccion.opuesta();
        float baseX = salaActual.gx * hDest.ancho;
        float baseY = salaActual.gy * hDest.alto;

        float worldX;
        float worldY;
        float offset = 32f;

        switch (dirDesdeDestino) {
            case NORTE -> {
                worldX = baseX + hDest.ancho / 2f;
                worldY = baseY + hDest.alto - GROSOR_MURO - offset;
            }
            case SUR -> {
                worldX = baseX + hDest.ancho / 2f;
                worldY = baseY + GROSOR_MURO + offset;
            }
            case ESTE -> {
                worldX = baseX + hDest.ancho - GROSOR_MURO - offset;
                worldY = baseY + hDest.alto / 2f;
            }
            case OESTE -> {
                worldX = baseX + GROSOR_MURO + offset;
                worldY = baseY + hDest.alto / 2f;
            }
            default -> {
                worldX = baseX + hDest.ancho / 2f;
                worldY = baseY + hDest.alto / 2f;
            }
        }

        jugadorBody.setTransform(worldX, worldY, 0);
        jugadorBody.setLinearVelocity(0, 0);
    }

    // ---------------- Loop ----------------

    @Override
    public void render(float delta) {
        manejarInputJugador();

        fisica.step();

        if (puertaPendiente != null) {
            onJugadorEntraPuerta(puertaPendiente);
            puertaPendiente = null;
        }

        Habitacion h = salaActual.habitacion;
        float origenX = salaActual.gx * h.ancho;
        float origenY = salaActual.gy * h.alto;

        camara.position.set(origenX + h.ancho / 2f, origenY + h.alto / 2f, 0);
        camara.update();

        projDebug.setToOrtho2D(origenX, origenY, h.ancho, h.alto);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Mundo (debug)
        dibujarDebugSala(origenX, origenY, h.ancho, h.alto);
        dibujarDebugFisica();

        // HUD (pantalla)
        dibujarHUD();   // 👈 HUD
    }

    private void manejarInputJugador() {
        float speed = (jugador != null) ? jugador.getVelocidad() : 0f;

        float vx = 0f;
        float vy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) vy += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) vy -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) vx -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) vx += speed;

        if (jugadorBody != null) {
            jugadorBody.setLinearVelocity(vx, vy);
        }
    }

    // ---------------- HUD ----------------

    private void dibujarHUD() {
        if (debugShape == null) return;

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        debugShape.setProjectionMatrix(hudCam.combined);

        // 1) Corazones (vida) – arriba izquierda
        dibujarHUDVida(screenW, screenH);

        // 2) Minimapa – arriba derecha
        dibujarHUDMinimapa(screenW, screenH);

        // 3) Items – debajo del minimapa
        dibujarHUDItems(screenW, screenH);
    }

    private void dibujarHUDVida(int screenW, int screenH) {
        int vidaMax = jugador.getVidaMaxima();
        int vidaActual = jugador.getVida();

        float startX = 16f;
        float centerY = screenH - 24f;
        float size = 16f;
        float spacing = 4f;

        debugShape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < vidaMax; i++) {
            float x = startX + i * (size + spacing);
            // fondo corazón vacío
            debugShape.setColor(Color.DARK_GRAY);
            debugShape.rect(x, centerY - size / 2f, size, size);

            if (i < vidaActual) {
                // corazón lleno
                debugShape.setColor(Color.RED);
                debugShape.rect(x + 2, centerY - size / 2f + 2, size - 4, size - 4);
            }
        }
        debugShape.end();
    }

    private void dibujarHUDMinimapa(int screenW, int screenH) {
        float margin = 16f;
        float miniSize = 90f;

        float miniX = screenW - margin - miniSize;
        float miniY = screenH - margin - miniSize;

        int cols = (maxGX - minGX) + 1;
        int rows = (maxGY - minGY) + 1;
        if (cols <= 0 || rows <= 0) return;

        float cellW = miniSize / cols;
        float cellH = miniSize / rows;
        float cellSize = Math.min(cellW, cellH);

        debugShape.begin(ShapeRenderer.ShapeType.Filled);
        // fondo del minimapa
        debugShape.setColor(0f, 0f, 0f, 0.5f);
        debugShape.rect(miniX - 4, miniY - 4, miniSize + 8, miniSize + 8);

        for (DisposicionMapa.Colocacion c : salasDescubiertas) {
            int col = c.gx - minGX;
            int row = c.gy - minGY;

            float cx = miniX + col * cellSize;
            float cy = miniY + row * cellSize;

            Color colorTipo;
            switch (c.habitacion.tipo) {
                case INICIO -> colorTipo = Color.GREEN;
                case COMBATE -> colorTipo = Color.RED;
                case ACERTIJO -> colorTipo = Color.BLUE;
                case BOTIN -> colorTipo = Color.GOLD;
                case JEFE -> colorTipo = Color.PURPLE;
                default -> colorTipo = Color.GRAY;
            }

            debugShape.setColor(colorTipo);
            debugShape.rect(cx + 2, cy + 2, cellSize - 4, cellSize - 4);
        }


        debugShape.end();

        // resaltar sala actual con borde blanco
        debugShape.begin(ShapeRenderer.ShapeType.Line);
        debugShape.setColor(Color.WHITE);

        int colA = salaActual.gx - minGX;
        int rowA = salaActual.gy - minGY;

        float ax = miniX + colA * cellSize;
        float ay = miniY + rowA * cellSize;

        debugShape.rect(ax + 1, ay + 1, cellSize - 2, cellSize - 2);

        debugShape.end();
    }

    private void dibujarHUDItems(int screenW, int screenH) {
        float margin = 16f;
        float miniSize = 140f;

        float panelW = 180f;
        float panelH = 60f;

        float panelX = screenW - margin - panelW;
        float panelY = screenH - margin - miniSize - 8f - panelH; // justo debajo del minimapa

        debugShape.begin(ShapeRenderer.ShapeType.Filled);
        debugShape.setColor(0f, 0f, 0f, 0.5f);
        debugShape.rect(panelX, panelY, panelW, panelH);

        if (jugador != null && jugador.getObjetos() != null) {
            float size = 16f;
            float spacing = 4f;
            float startX = panelX + 8f;
            float centerY = panelY + panelH / 2f;

            int index = 0;
            for (var item : jugador.getObjetos()) {
                float x = startX + index * (size + spacing);
                debugShape.setColor(Color.LIGHT_GRAY);
                debugShape.rect(x, centerY - size / 2f, size, size);
                index++;
                if (x + size > panelX + panelW - 8f) break; // no se sale del panel
            }
        }

        debugShape.end();
    }

    // ---------------- Debug mundo ----------------

    private void dibujarDebugSala(float x, float y, float w, float h) {
        if (debugShape == null) return;

        debugShape.setProjectionMatrix(projDebug);
        debugShape.begin(ShapeRenderer.ShapeType.Filled);
        debugShape.setColor(Color.YELLOW);

        float margen = 32f;
        float grosor = GROSOR_MURO;

        float ix = x + margen;
        float iy = y + margen;
        float iw = w - margen * 2f;
        float ih = h - margen * 2f;

        debugShape.rect(ix, iy, iw, grosor);
        debugShape.rect(ix, iy + ih - grosor, iw, grosor);
        debugShape.rect(ix, iy, grosor, ih);
        debugShape.rect(ix + iw - grosor, iy, grosor, ih);

        debugShape.end();

        // puertas naranjas de la sala actual
        debugShape.setProjectionMatrix(projDebug);
        debugShape.begin(ShapeRenderer.ShapeType.Filled);
        debugShape.setColor(Color.ORANGE);

        Habitacion hab = salaActual.habitacion;
        float baseX = salaActual.gx * hab.ancho;
        float baseY = salaActual.gy * hab.alto;

        for (Direccion dir : hab.puertas.keySet()) {
            if (!haySalaVecina(salaActual, dir)) continue;

            switch (dir) {
                case NORTE -> {
                    float cx = baseX + hab.ancho / 2f;
                    float cy = baseY + hab.alto - GROSOR_MURO - SENSOR_OFFSET;
                    debugShape.rect(cx - ANCHO_PUERTA / 2f, cy - 4f,
                        ANCHO_PUERTA, 8f);
                }
                case SUR -> {
                    float cx = baseX + hab.ancho / 2f;
                    float cy = baseY + GROSOR_MURO + SENSOR_OFFSET;
                    debugShape.rect(cx - ANCHO_PUERTA / 2f, cy - 4f,
                        ANCHO_PUERTA, 8f);
                }
                case ESTE -> {
                    float cx = baseX + hab.ancho - GROSOR_MURO - SENSOR_OFFSET;
                    float cy = baseY + hab.alto / 2f;
                    debugShape.rect(cx - 4f, cy - ANCHO_PUERTA / 2f,
                        8f, ANCHO_PUERTA);
                }
                case OESTE -> {
                    float cx = baseX + GROSOR_MURO + SENSOR_OFFSET;
                    float cy = baseY + hab.alto / 2f;
                    debugShape.rect(cx - 4f, cy - ANCHO_PUERTA / 2f,
                        8f, ANCHO_PUERTA);
                }
            }
        }

        debugShape.end();
    }

    private void dibujarDebugFisica() {
        if (debugShape == null) return;

        debugShape.setProjectionMatrix(projDebug);
        debugShape.begin(ShapeRenderer.ShapeType.Line);
        debugShape.setColor(Color.YELLOW);

        cuerposTemp.clear();
        fisica.world().getBodies(cuerposTemp);

        for (Body body : cuerposTemp) {
            for (Fixture fixture : body.getFixtureList()) {
                Shape shape = fixture.getShape();

                switch (shape.getType()) {
                    case Circle -> {
                        CircleShape cs = (CircleShape) shape;
                        Vector2 pos = body.getWorldPoint(cs.getPosition());
                        float r = cs.getRadius();
                        debugShape.circle(pos.x, pos.y, r, 24);
                    }
                    case Polygon -> {
                        PolygonShape ps = (PolygonShape) shape;
                        int vCount = ps.getVertexCount();
                        if (vCount == 0) break;

                        Vector2 v0 = new Vector2();
                        Vector2 v1 = new Vector2();

                        ps.getVertex(0, v0);
                        v0 = body.getWorldPoint(v0);

                        for (int i = 1; i <= vCount; i++) {
                            ps.getVertex(i % vCount, v1);
                            v1 = body.getWorldPoint(v1);
                            debugShape.line(v0.x, v0.y, v1.x, v1.y);
                            v0.set(v1);
                        }
                    }
                    case Edge -> {
                        EdgeShape es = (EdgeShape) shape;
                        Vector2 a = new Vector2();
                        Vector2 b = new Vector2();
                        es.getVertex1(a);
                        es.getVertex2(b);
                        a = body.getWorldPoint(a);
                        b = body.getWorldPoint(b);
                        debugShape.line(a.x, a.y, b.x, b.y);
                    }
                    case Chain -> {
                        ChainShape ch = (ChainShape) shape;
                        int vCount = ch.getVertexCount();
                        if (vCount < 2) break;

                        Vector2 a = new Vector2();
                        Vector2 b = new Vector2();
                        ch.getVertex(0, a);
                        a = body.getWorldPoint(a);

                        for (int i = 1; i < vCount; i++) {
                            ch.getVertex(i, b);
                            b = body.getWorldPoint(b);
                            debugShape.line(a.x, a.y, b.x, b.y);
                            a.set(b);
                        }
                    }
                }
            }
        }

        debugShape.end();
    }

    // ---------------- Screen ----------------

    @Override
    public void resize(int width, int height) {
        if (hudCam != null) {
            hudCam.setToOrtho(false, width, height);
            hudCam.update();
        }
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (debugShape != null) debugShape.dispose();
        if (fisica != null) fisica.dispose();
    }
}

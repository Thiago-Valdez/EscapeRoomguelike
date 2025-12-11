package interfaces;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

import entidades.Jugador;
import entidades.Item;
import mapa.DisposicionMapa;
import mapa.Habitacion;
import mapa.TipoSala;

import java.util.Set;

/**
 * HUD simple:
 *  - Corazones de vida arriba-izquierda
 *  - Minimapa 5x5 arriba-derecha (solo salas descubiertas)
 *  - Lista de items debajo del minimapa
 */
public class HudJuego implements Disposable {

    private final DisposicionMapa disposicion;
    private Habitacion salaActual;
    private final Jugador jugador;

    private final OrthographicCamera cam;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;

    private int screenWidth;
    private int screenHeight;

    /**
     * Ahora el HUD solo necesita la disposición (para el minimapa)
     * y el jugador. Las salas descubiertas se obtienen de DisposicionMapa.
     */
    public HudJuego(DisposicionMapa disposicion, Jugador jugador) {
        this.disposicion = disposicion;
        this.jugador = jugador;

        // Por defecto usamos la sala de inicio; luego JuegoPrincipal
        // llamará a actualizarSalaActual() con la correcta.
        this.salaActual = disposicion.salaInicio();

        this.screenWidth = Gdx.graphics.getWidth();
        this.screenHeight = Gdx.graphics.getHeight();

        cam = new OrthographicCamera();
        cam.setToOrtho(false, screenWidth, screenHeight);

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont(); // placeholder; luego podés cambiar a tu fuente
    }

    /** Llamado por JuegoPrincipal cuando cambia de habitación. */
    public void actualizarSalaActual(Habitacion nuevaSala) {
        this.salaActual = nuevaSala;
    }

    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        cam.setToOrtho(false, width, height);
    }

    public void render() {
        cam.update();

        // --------- Texto (vida + items) ----------
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        dibujarVida();
        dibujarItems();

        batch.end();

        // --------- Minimapa (rectángulos) ----------
        shapes.setProjectionMatrix(cam.combined);
        dibujarMinimapa();
    }

    // =================== VIDA ====================

    private void dibujarVida() {
        // Muy simple: "HP: ♥♥♥"
        StringBuilder sb = new StringBuilder("Vida: ");
        int vidaActual = jugador.getVida();
        int vidaMax = jugador.getVidaMaxima();

        for (int i = 0; i < vidaMax; i++) {
            sb.append(i < vidaActual ? "♥" : "♡");
        }

        font.draw(batch, sb.toString(), 20, screenHeight - 20);
    }

    // =================== ITEMS ====================

    private void dibujarItems() {
        float x = screenWidth - 220f;
        float y = screenHeight - 120f; // debajo del minimapa

        font.draw(batch, "Items:", x, y);

        y -= 18f;
        for (Item item : jugador.getObjetos()) {
            font.draw(batch, "- " + item.getNombre(), x, y);
            y -= 16f;
        }
    }

    // =================== MINIMAPA ====================

    private void dibujarMinimapa() {
        // Medidas del minimapa (lo achicamos bastante)
        float cellSize = 10f;    // cada casilla 10x10 px
        float padding = 2f;
        float mapWidth = 5 * cellSize + 4 * padding;
        float mapHeight = 5 * cellSize + 4 * padding;

        float baseX = screenWidth - mapWidth - 20f;        // margen derecha
        float baseY = screenHeight - mapHeight - 40f;      // margen arriba

        // Set de salas descubiertas, lo obtenemos de DisposicionMapa
        Set<Habitacion> descubiertas = disposicion.getDescubiertas();

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Fondo del minimapa
        shapes.setColor(0f, 0f, 0f, 0.5f);
        shapes.rect(baseX - 4, baseY - 4, mapWidth + 8, mapHeight + 8);

        // Recorremos todas las habitaciones definidas en el enum
        for (Habitacion h : Habitacion.values()) {

            if (!descubiertas.contains(h)) {
                // No mostrar salas que el jugador aún no visitó
                continue;
            }

            int gx = h.gridX;
            int gy = h.gridY;

            float cx = baseX + gx * (cellSize + padding);
            float cy = baseY + gy * (cellSize + padding);

            // Color según tipo
            switch (h.tipo) {
                case INICIO -> shapes.setColor(1f, 1f, 1f, 1f);           // blanco
                case ACERTIJO -> shapes.setColor(0.3f, 0.6f, 1f, 1f);     // azul
                case COMBATE -> shapes.setColor(1f, 0.2f, 0.2f, 1f);      // rojo
                case BOTIN -> shapes.setColor(1f, 1f, 0.3f, 1f);          // amarillo
                case JEFE -> shapes.setColor(0.8f, 0.3f, 1f, 1f);         // violeta
            }

            shapes.rect(cx, cy, cellSize, cellSize);

            // Contorno especial si es la sala actual
            if (h == salaActual) {
                shapes.end();
                shapes.begin(ShapeRenderer.ShapeType.Line);
                shapes.setColor(1f, 1f, 1f, 1f);
                shapes.rect(cx - 1, cy - 1, cellSize + 2, cellSize + 2);
                shapes.end();
                shapes.begin(ShapeRenderer.ShapeType.Filled);
            }
        }

        shapes.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
    }
}

package interfaces;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import entidades.Jugador;
import mapa.DisposicionMapa;
import mapa.TipoSala;

import java.util.Set;

/**
 * Dibuja el HUD del juego:
 *  - Vida (corazones)
 *  - Minimapa
 *  - Lista de ítems del jugador
 */
public class HudJuego {

    private final OrthographicCamera hudCam;
    private final ShapeRenderer shape;
    private final SpriteBatch batch;
    private final BitmapFont font;

    private final DisposicionMapa disposicion;
    private DisposicionMapa.Colocacion salaActual;
    private final Set<DisposicionMapa.Colocacion> salasDescubiertas;
    private Jugador jugador;

    // Bounds del minimapa
    private int minGX, maxGX, minGY, maxGY;

    public HudJuego(DisposicionMapa disposicion,
                    DisposicionMapa.Colocacion salaActual,
                    Set<DisposicionMapa.Colocacion> salasDescubiertas,
                    Jugador jugador) {
        this.disposicion = disposicion;
        this.salaActual = salaActual;
        this.salasDescubiertas = salasDescubiertas;
        this.jugador = jugador;

        hudCam = new OrthographicCamera();
        hudCam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.3f);

        recalcularBoundsMapa();
    }

    public void setSalaActual(DisposicionMapa.Colocacion salaActual) {
        this.salaActual = salaActual;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    private void recalcularBoundsMapa() {
        minGX = Integer.MAX_VALUE;
        minGY = Integer.MAX_VALUE;
        maxGX = Integer.MIN_VALUE;
        maxGY = Integer.MIN_VALUE;

        for (DisposicionMapa.Colocacion c : disposicion.todas()) {
            if (c.gx < minGX) minGX = c.gx;
            if (c.gx > maxGX) maxGX = c.gx;
            if (c.gy < minGY) minGY = c.gy;
            if (c.gy > maxGY) maxGY = c.gy;
        }

        if (minGX == Integer.MAX_VALUE) {
            minGX = maxGX = minGY = maxGY = 0;
        }
    }

    public void render() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        hudCam.setToOrtho(false, screenW, screenH);
        hudCam.update();

        shape.setProjectionMatrix(hudCam.combined);
        batch.setProjectionMatrix(hudCam.combined);

        dibujarVida(screenW, screenH);
        dibujarMinimapa(screenW, screenH);
        dibujarItems(screenW, screenH);
    }

    // ---------- VIDA ----------
    private void dibujarVida(int screenW, int screenH) {
        if (jugador == null) return;

        int vida = jugador.getVida();
        int vidaMax = jugador.getVidaMaxima();

        float margin = 16f;
        float heartSize = 18f;
        float spacing = 4f;

        float x = margin;
        float y = screenH - margin - heartSize;

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < vidaMax; i++) {
            if (i < vida) {
                shape.setColor(Color.RED);
            } else {
                shape.setColor(Color.DARK_GRAY);
            }
            shape.rect(x + i * (heartSize + spacing), y, heartSize, heartSize);
        }
        shape.end();
    }

    // ---------- MINIMAPA ----------
    private void dibujarMinimapa(int screenW, int screenH) {
        if (salaActual == null) return;

        float margin = 16f;
        float miniSize = 90f;

        float miniX = screenW - margin - miniSize;
        float miniY = screenH - margin - miniSize;

        int cols = (maxGX - minGX + 1);
        int rows = (maxGY - minGY + 1);

        float cellSize = Math.min(miniSize / cols, miniSize / rows);

        // Fondo minimapa
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.5f);
        shape.rect(miniX, miniY, cellSize * cols, cellSize * rows);
        shape.end();

        // Salas descubiertas
        shape.begin(ShapeRenderer.ShapeType.Filled);
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

            shape.setColor(colorTipo);
            shape.rect(cx + 2, cy + 2, cellSize - 4, cellSize - 4);
        }
        shape.end();

        // Sala actual resaltada
        int curCol = salaActual.gx - minGX;
        int curRow = salaActual.gy - minGY;

        float curX = miniX + curCol * cellSize;
        float curY = miniY + curRow * cellSize;

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.WHITE);
        shape.rect(curX + 1, curY + 1, cellSize - 2, cellSize - 2);
        shape.end();
    }

    // ---------- ITEMS ----------
    private void dibujarItems(int screenW, int screenH) {
        if (jugador == null || jugador.getObjetos() == null || jugador.getObjetos().isEmpty()) {
            return;
        }

        float margin = 16f;
        float miniSize = 90f;

        float panelW = 200f;
        float panelH = 90f;

        float panelX = screenW - margin - panelW;
        float panelY = screenH - margin - miniSize - 8f - panelH;

        // Fondo
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.5f);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        float iconSize = 14f;
        float startX = panelX + 8f;
        float startY = panelY + panelH - 10f;
        float lineHeight = 20f;

        batch.begin();
        int index = 0;
        for (var item : jugador.getObjetos()) {
            float y = startY - index * lineHeight;
            if (y - iconSize < panelY + 8f) break;

            // icono placeholder
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.LIGHT_GRAY);
            shape.rect(startX, y - iconSize, iconSize, iconSize);
            shape.end();

            // nombre
            if (item != null) {
                font.draw(batch, item.getNombre(), startX + iconSize + 6f, y);
            }

            index++;
        }
        batch.end();
    }

    public void dispose() {
        shape.dispose();
        batch.dispose();
        font.dispose();
    }
}

package interfaces;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

import entidades.Jugador;
import entidades.Item;
import mapa.*;

import java.util.Set;

public class HudJuego implements Disposable, ListenerCambioSala {

    private final DisposicionMapa disposicion;
    private final Jugador jugador;

    private Habitacion salaActual;

    // Layout calculado desde el piso (si tu LayoutMinimapa aún recibe grafo, lo ajustamos después)
    private final LayoutMinimapa layout;

    private final OrthographicCamera cam;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;

    private int screenWidth;
    private int screenHeight;

    public HudJuego(DisposicionMapa disposicion, Jugador jugador) {
        this.disposicion = disposicion;
        this.jugador = jugador;

        this.salaActual = disposicion.salaInicio();

        // Si tu LayoutMinimapa actual requiere (disposicion, grafo),
        // dejalo como está por ahora, pero lo ideal es que use conexionesPiso.
        this.layout = LayoutMinimapa.construir(disposicion);

        this.screenWidth = Gdx.graphics.getWidth();
        this.screenHeight = Gdx.graphics.getHeight();

        cam = new OrthographicCamera();
        cam.setToOrtho(false, screenWidth, screenHeight);

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
    }

    public void actualizarSalaActual(Habitacion nuevaSala) {
        this.salaActual = nuevaSala;
    }

    @Override
    public void salaCambiada(Habitacion salaAnterior, Habitacion salaNueva) {
        actualizarSalaActual(salaNueva);
    }

    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        cam.setToOrtho(false, width, height);
    }

    public void render() {
        cam.update();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        dibujarVida();
        dibujarItems();
        batch.end();

        shapes.setProjectionMatrix(cam.combined);
        dibujarMinimapaExplorado();
    }

    private void dibujarVida() {
        StringBuilder sb = new StringBuilder("Vida: ");
        int vidaActual = jugador.getVida();
        int vidaMax = jugador.getVidaMaxima();
        for (int i = 0; i < vidaMax; i++) sb.append(i < vidaActual ? "♥" : "♡");
        font.draw(batch, sb.toString(), 20, screenHeight - 20);
    }

    private void dibujarItems() {
        float x = screenWidth - 260f;
        float y = screenHeight - 180f;
        font.draw(batch, "Items:", x, y);
        y -= 18f;

        for (Item item : jugador.getObjetos()) {
            font.draw(batch, "- " + item.getNombre(), x, y);
            y -= 16f;
        }
    }

    private void dibujarMinimapaExplorado() {
        Set<Habitacion> descubiertas = disposicion.getDescubiertas();

        final float roomW = 18f;
        final float roomH = 14f;
        final float gap = 8f;

        int minX = layout.minX(), maxX = layout.maxX();
        int minY = layout.minY(), maxY = layout.maxY();

        int widthCells = (maxX - minX + 1);
        int heightCells = (maxY - minY + 1);

        float mapW = widthCells * roomW + (widthCells - 1) * gap;
        float mapH = heightCells * roomH + (heightCells - 1) * gap;

        float baseX = screenWidth - mapW - 20f;
        float baseY = screenHeight - mapH - 40f;

        // Fondo
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(baseX - 6, baseY - 6, mapW + 12, mapH + 12);
        shapes.end();

        // Pasillos: solo conexionesPiso y solo entre descubiertas
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.85f, 0.85f, 0.85f, 1f);

        for (Habitacion h : disposicion.getSalasActivas()) {
            if (!descubiertas.contains(h)) continue;

            for (var e : disposicion.getConexionesEnPiso(h).entrySet()) {
                Habitacion dest = e.getValue();
                if (dest == null) continue;
                if (!descubiertas.contains(dest)) continue;

                // Pasillo según layout
                for (PosMini p : layout.pasilloEntre(h, dest)) {
                    float x = baseX + (p.x() - minX) * (roomW + gap);
                    float y = baseY + (p.y() - minY) * (roomH + gap);
                    shapes.rect(x + roomW * 0.25f, y + roomH * 0.25f, roomW * 0.5f, roomH * 0.5f);
                }
            }
        }
        shapes.end();

        // Salas
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Habitacion h : disposicion.getSalasActivas()) {
            if (!descubiertas.contains(h)) continue;

            PosMini p = layout.posiciones().get(h);
            if (p == null) continue;

            float x = baseX + (p.x() - minX) * (roomW + gap);
            float y = baseY + (p.y() - minY) * (roomH + gap);

            setColorSala(shapes, h);
            shapes.rect(x, y, roomW, roomH);
        }
        shapes.end();

        // Borde sala actual
        if (salaActual != null && descubiertas.contains(salaActual)) {
            PosMini p = layout.posiciones().get(salaActual);
            if (p != null) {
                float x = baseX + (p.x() - minX) * (roomW + gap);
                float y = baseY + (p.y() - minY) * (roomH + gap);

                shapes.begin(ShapeRenderer.ShapeType.Line);
                shapes.setColor(1f, 1f, 1f, 1f);
                shapes.rect(x - 1, y - 1, roomW + 2, roomH + 2);
                shapes.end();
            }
        }

        batch.begin();
        font.draw(batch, "Minimapa (explorado)", baseX, baseY - 12f);
        batch.end();
    }

    private void setColorSala(ShapeRenderer sr, Habitacion h) {
        switch (h.tipo) {
            case INICIO -> sr.setColor(0.95f, 0.95f, 0.95f, 1f);
            case ACERTIJO -> sr.setColor(0.35f, 0.65f, 1f, 1f);
            case COMBATE -> sr.setColor(1f, 0.25f, 0.25f, 1f);
            case BOTIN -> sr.setColor(1f, 1f, 0.35f, 1f);
            case JEFE -> sr.setColor(0.85f, 0.35f, 1f, 1f);
            default -> sr.setColor(0.6f, 0.6f, 0.6f, 1f);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
    }
}

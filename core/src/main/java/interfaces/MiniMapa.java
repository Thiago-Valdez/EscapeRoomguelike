package interfaces;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import mapa.Habitacion;
import mapa.Direccion;

import java.util.List;
import java.util.Set;

public class MiniMapa {

    private final List<Habitacion> salasDelPiso;
    private Set<Habitacion> salasVisitadas;
    private Habitacion salaActual;

    private static final float CELL = 20f;     // tamaño de un cuadro de sala
    private static final float GAP  = 6f;      // espacio entre salas
    private static final float OFFSET_X = 20f; // posición en pantalla
    private static final float OFFSET_Y = 20f;

    public MiniMapa(List<Habitacion> salasDelPiso) {
        this.salasDelPiso = salasDelPiso;
    }

    public void setSalaActual(Habitacion actual) {
        this.salaActual = actual;
    }

    public void setSalasVisitadas(Set<Habitacion> visitadas) {
        this.salasVisitadas = visitadas;
    }

    public void render(ShapeRenderer sr) {
        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (Habitacion h : salasDelPiso) {

            float x = OFFSET_X + h.gridX * (CELL + GAP);
            float y = OFFSET_Y + h.gridY * (CELL + GAP);

            // Color según tipo
            Color c = colorPorTipo(h);

            // Si no visitada → oscurecer
            if (salasVisitadas != null && !salasVisitadas.contains(h)) {
                c = c.cpy().lerp(Color.BLACK, 0.5f);
            }

            // Si es actual → resaltada
            if (h == salaActual) {
                c = Color.CYAN;
            }

            sr.setColor(c);
            sr.rect(x, y, CELL, CELL);

            // Conectores con salas vecinas
            dibujarConectores(sr, h, x, y);
        }

        sr.end();
    }

    private void dibujarConectores(ShapeRenderer sr, Habitacion h, float x, float y) {
        float cx = x + CELL / 2;
        float cy = y + CELL / 2;

        sr.setColor(Color.WHITE);
        float w = 2f; // grosor del conector

        for (Habitacion dest : salasDelPiso) {

            // NORTE
            if (dest.gridX == h.gridX && dest.gridY == h.gridY + 1) {
                sr.rectLine(cx, y + CELL, cx, y + CELL + GAP, w);
            }

            // SUR
            if (dest.gridX == h.gridX && dest.gridY == h.gridY - 1) {
                sr.rectLine(cx, y, cx, y - GAP, w);
            }

            // ESTE
            if (dest.gridX == h.gridX + 1 && dest.gridY == h.gridY) {
                sr.rectLine(x + CELL, cy, x + CELL + GAP, cy, w);
            }

            // OESTE
            if (dest.gridX == h.gridX - 1 && dest.gridY == h.gridY) {
                sr.rectLine(x, cy, x - GAP, cy, w);
            }
        }
    }


    private Color colorPorTipo(Habitacion h) {
        switch (h.tipo) {
            case INICIO:   return Color.GREEN;
            case JEFE:     return Color.RED;
            case BOTIN:    return Color.YELLOW;
            case ACERTIJO: return Color.CYAN;
            case COMBATE:  return Color.ORANGE;
        }
        return Color.GRAY;
    }
}

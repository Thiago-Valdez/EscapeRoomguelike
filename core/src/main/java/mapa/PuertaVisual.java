package mapa;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class PuertaVisual {
    public final float x;      // esquina inferior izquierda
    public final float y;
    public final float width;
    public final float height;

    public PuertaVisual(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(ShapeRenderer sr) {
        sr.setColor(Color.BLACK);
        sr.rect(x, y, width, height);
    }
}

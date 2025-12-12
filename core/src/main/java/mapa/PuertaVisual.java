package mapa;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;

public class PuertaVisual {

    private final float x, y;
    private final float width, height;

    public PuertaVisual(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(ShapeRenderer sr) {
        sr.setColor(Color.BLACK);
        sr.rect(
            x - width / 2f,
            y - height / 2f,
            width,
            height
        );
    }
}

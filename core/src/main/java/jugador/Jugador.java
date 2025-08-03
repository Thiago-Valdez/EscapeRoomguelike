package jugador;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Jugador {
    private float x, y;
    private float ancho, alto;
    private float velocidad;
    private Color color;

    public Jugador(float xInicial, float yInicial) {
        this.x = xInicial;
        this.y = yInicial;
        this.ancho = 50;
        this.alto = 50;
        this.velocidad = 200;
        this.color = Color.RED;
    }

    public void caminar(boolean arriba, boolean abajo, boolean izquierda, boolean derecha, float delta) {
        if (arriba)    y += velocidad * delta;
        if (abajo)     y -= velocidad * delta;
        if (izquierda) x -= velocidad * delta;
        if (derecha)   x += velocidad * delta;
    }

    public void dibujar(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(color);
        shapeRenderer.rect(x, y, ancho, alto);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setColor(Color nuevoColor) {
        this.color = nuevoColor;
    }

    public void setVelocidad(float nuevaVelocidad) {
        this.velocidad = nuevaVelocidad;
    }


}

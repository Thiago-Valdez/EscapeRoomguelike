package jugador;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public class ControlDelJugador implements InputProcessor {
    private float velocidad = 200;
    private boolean arriba, abajo, izquierda, derecha;

    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.W: case Input.Keys.UP:    arriba = true; break;
            case Input.Keys.S: case Input.Keys.DOWN:  abajo = true; break;
            case Input.Keys.A: case Input.Keys.LEFT:  izquierda = true; break;
            case Input.Keys.D: case Input.Keys.RIGHT: derecha = true; break;
        }
        return true;
    }

    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.W: case Input.Keys.UP:    arriba = false; break;
            case Input.Keys.S: case Input.Keys.DOWN:  abajo = false; break;
            case Input.Keys.A: case Input.Keys.LEFT:  izquierda = false; break;
            case Input.Keys.D: case Input.Keys.RIGHT: derecha = false; break;
        }
        return true;
    }

    public boolean keyTyped(char character) { return false; }

    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }

    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

    public boolean touchCancelled(int i, int i1, int i2, int i3) { return false; }

    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

    public boolean mouseMoved(int screenX, int screenY) { return false; }

    public boolean scrolled(float amountX, float amountY) { return false; }


    public float getVelocidad() { return velocidad; }
    public boolean vaArriba() { return arriba; }
    public boolean vaAbajo() { return abajo; }
    public boolean vaIzquierda() { return izquierda; }
    public boolean vaDerecha() { return derecha; }

}

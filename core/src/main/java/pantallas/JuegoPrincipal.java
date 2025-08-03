package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.principal.Principal;
import jugador.*;

public class JuegoPrincipal implements Screen {

    private ShapeRenderer shapeRenderer;
    private float x = 100;
    private float y = 100;
    private float velocidad = 200;
    Jugador jugador;
    private ControlDelJugador controlador;

    public JuegoPrincipal (Principal game) {

    }


    @Override
    public void show() {
        jugador = new Jugador(100, 100);
        shapeRenderer = new ShapeRenderer();
        controlador = new ControlDelJugador();
        Gdx.input.setInputProcessor(controlador);
    }
    @Override
    public void render(float delta) {

        jugador.caminar(controlador.vaArriba(), controlador.vaAbajo(), controlador.vaIzquierda(), controlador.vaDerecha(), delta);



        float vel = controlador.getVelocidad();
        if (controlador.vaArriba())    y += vel * delta;
        if (controlador.vaAbajo())     y -= vel * delta;
        if (controlador.vaIzquierda()) x -= vel * delta;
        if (controlador.vaDerecha())   x += vel * delta;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        jugador.dibujar(shapeRenderer);
        shapeRenderer.end();
    }

    @Override
    public void resize(int i, int i1) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}

package pantallas;

import com.badlogic.gdx.Screen;

import io.github.principal.Principal;
import juego.Partida;

/**
 * Pantalla fina: delega toda la lógica del gameplay a {@link Partida}.
 * La idea es que este Screen no crezca: init/render/dispose quedan centralizados.
 */
public class JuegoPrincipal implements Screen {

    private final Principal game;
    private Partida partida;

    public JuegoPrincipal(Principal game) {
        this.game = game;
    }

    @Override
    public void show() {
        partida = new Partida(game);
        partida.init();
    }

    @Override
    public void render(float delta) {
        if (partida != null) partida.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        if (partida != null) partida.resize(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (partida != null) {
            partida.dispose();
            partida = null;
        }
    }
}

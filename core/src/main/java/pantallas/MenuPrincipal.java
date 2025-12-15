package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.principal.Principal;

public class MenuPrincipal implements Screen {

    private final Principal game;
    private Stage escenario;
    private Skin skin;
    private Texture texturaLogo;

    public MenuPrincipal(Principal game) {
        this.game = game;
        escenario = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(escenario);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        texturaLogo = new Texture("goku.jpg");
        Image imagenLogo = new Image(texturaLogo);

        // Crear el botón "Jugar"
        TextButton botonJugar = new TextButton("Jugar", skin);
        botonJugar.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new JuegoPrincipal(game));
            }
        });

        Table tabla = new Table();
        tabla.setFillParent(true);
        tabla.center();

        tabla.add(imagenLogo).padBottom(50).row();
        tabla.add(botonJugar).padTop(20).row();

        escenario.addActor(tabla);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1); // Fondo negro
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        escenario.act(delta);
        escenario.draw();
    }

    @Override
    public void resize(int ancho, int alto) {
        escenario.getViewport().update(ancho, alto, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        escenario.dispose();
        skin.dispose();
        texturaLogo.dispose();
    }
}

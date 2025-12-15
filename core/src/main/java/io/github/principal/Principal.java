package io.github.principal;

import com.badlogic.gdx.Game;

import pantallas.JuegoPrincipal;
import pantallas.PantallaDeInicio;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Principal extends Game {



    @Override
    public void create() {
        setScreen(new JuegoPrincipal(this));

    }

    @Override
    public void dispose() {

    }
}

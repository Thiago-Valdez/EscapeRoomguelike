package juego.sistemas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import camara.CamaraDeSala;
import entidades.GestorDeEntidades;
import entidades.enemigos.Enemigo;
import entidades.personajes.Jugador;
import entidades.sprites.SpritesEntidad;
import fisica.FisicaMundo;
import interfaces.hud.HudJuego;
import juego.inicializacion.InicializadorSensoresPuertas;
import mapa.model.Habitacion;
import mapa.puertas.PuertaVisual;

/**
 * Se encarga SOLO del render (mapa, puertas, sprites, debug y HUD).
 * No actualiza gameplay.
 */
public final class CanalRenderizadoPartida {

    private final CamaraDeSala camaraSala;
    private final OrthogonalTiledMapRenderer mapaRenderer;
    private final ShapeRenderer shapeRendererMundo;
    private final SpriteBatch batch;
    private final FisicaMundo fisica;
    private final HudJuego hud;
    private final GestorDeEntidades gestorEntidades;
    private final SistemaSpritesEntidades sprites;
    PuertaVisual[] puertasVisuales;
    private java.util.List<InicializadorSensoresPuertas.RegistroPuerta> puertas;


    public CanalRenderizadoPartida(
            CamaraDeSala camaraSala,
            OrthogonalTiledMapRenderer mapaRenderer,
            ShapeRenderer shapeRendererMundo,
            SpriteBatch batch,
            FisicaMundo fisica,
            HudJuego hud,
            GestorDeEntidades gestorEntidades,
            SistemaSpritesEntidades sprites
    ) {
        this.camaraSala = camaraSala;
        this.mapaRenderer = mapaRenderer;
        this.shapeRendererMundo = shapeRendererMundo;
        this.batch = batch;
        this.fisica = fisica;
        this.hud = hud;
        this.gestorEntidades = gestorEntidades;
        this.sprites = sprites;
    }

    public void render(float delta, Habitacion salaActual, boolean debugFisica, Jugador jugador1, Jugador jugador2, java.util.List<mapa.botones.BotonVisual> botonesVisuales) {
        if (camaraSala == null) return;

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (mapaRenderer != null) {
            mapaRenderer.setView(camaraSala.getCamara());
            mapaRenderer.render();
        }

        if (shapeRendererMundo != null && gestorEntidades != null) {
            shapeRendererMundo.setProjectionMatrix(camaraSala.getCamara().combined);
            shapeRendererMundo.begin(ShapeRenderer.ShapeType.Filled);
            gestorEntidades.renderPuertas(shapeRendererMundo, salaActual);
            shapeRendererMundo.end();
        }

        if (batch != null) {
            batch.setProjectionMatrix(camaraSala.getCamara().combined);
            batch.begin();

            if (puertas != null && salaActual != null) {
                for (InicializadorSensoresPuertas.RegistroPuerta r : puertas) {
                    if (r == null) continue;
                    if (r.origen() != salaActual) continue;

                    PuertaVisual pv = r.visual();
                    if (pv == null) continue;

                    TextureRegion tr = pv.frameActual();
                    if (tr != null) {
                        batch.draw(tr, pv.x, pv.y, pv.width, pv.height);
                    }
                }
            }




// Botones (antes que enemigos/jugadores) - se dibujan al tamaño del rect en Tiled
if (botonesVisuales != null && salaActual != null) {
    for (mapa.botones.BotonVisual bv : botonesVisuales) {
        if (bv == null) continue;
        if (bv.sala != salaActual) continue;

        float x = bv.posCentro.x - bv.w / 2f;
        float y = bv.posCentro.y - bv.h / 2f;

        com.badlogic.gdx.graphics.g2d.TextureRegion fr = bv.frameActual();
        if (fr != null) batch.draw(fr, x, y, bv.w, bv.h);
    }
}

            // Enemigos primero (atrás)
            if (gestorEntidades != null && sprites != null) {
                for (Enemigo e : gestorEntidades.getEnemigosMundo()) {
                    SpritesEntidad s = sprites.get(e);
                    if (s != null) { s.update(delta); s.render(batch); }
                }
            }

            // Jugadores después
            if (sprites != null) {
                SpritesEntidad s1 = sprites.get(jugador1);
                if (s1 != null) { s1.update(delta); s1.render(batch); }
                SpritesEntidad s2 = sprites.get(jugador2);
                if (s2 != null) { s2.update(delta); s2.render(batch); }
            }

            batch.end();
        }

        if (debugFisica && fisica != null) {
            fisica.debugDraw(camaraSala.getCamara());
        }

        if (hud != null) {
            hud.render();
        }
    }

    public void setPuertas(java.util.List<InicializadorSensoresPuertas.RegistroPuerta> puertas) {
        this.puertas = puertas;
    }

}

package juego.inicializacion;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import mapa.model.Habitacion;
import mapa.model.TipoSala;
import mapa.puertas.DatosTrampilla;

public final class InicializadorTrampillaJefe {

    private InicializadorTrampillaJefe() {}

    public static void crearTrampillas(World world, Iterable<Habitacion> salasDelPiso) {
        if (world == null || salasDelPiso == null) return;

        for (Habitacion h : salasDelPiso) {
            if (h == null) continue;
            if (h.tipo != TipoSala.JEFE) continue;

            float cx = h.gridX * h.ancho + h.ancho / 2f;
            float cy = h.gridY * h.alto  + h.alto  / 2f;

            // Tamaño de la trampilla en píxeles (ajustá a gusto)
            float w = 48f;
            float t = 48f;

            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.StaticBody;
            bd.position.set(0f, 0f); // trabajamos en coords absolutas en el shape

            Body b = world.createBody(bd);

            PolygonShape shape = new PolygonShape();
            // setAsBox usa centro + halfExtents
            shape.setAsBox(w / 2f, t / 2f, new com.badlogic.gdx.math.Vector2(cx, cy), 0f);

            Fixture fx = b.createFixture(shape, 0f);
            fx.setSensor(true);
            fx.setUserData(new DatosTrampilla(h));

            shape.dispose();
        }
    }
}

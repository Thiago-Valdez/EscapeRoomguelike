package fisica;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Crea cuerpos estáticos de colisión a partir de la capa "colision"
 * de un mapa Tiled.
 *
 * Trabaja en píxeles: Tiled (0,0) arriba-izquierda,
 * y convertimos a mundo (0,0) abajo-izquierda.
 */
public class ColisionesDesdeTiled {

    public static void crearColisiones(TiledMap mapa, World world) {
        if (mapa == null || world == null) return;

        // Cambiá "colisiones" por el nombre real de tu layer en Tiled
        MapLayer capaColisiones = mapa.getLayers().get("colision");
        if (capaColisiones == null) return;

        for (MapObject objeto : capaColisiones.getObjects()) {
            if (objeto instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) objeto).getRectangle();

                BodyDef bd = new BodyDef();
                bd.type = BodyDef.BodyType.StaticBody;
                bd.position.set(rect.x + rect.width / 2f, rect.y + rect.height / 2f);

                Body cuerpo = world.createBody(bd);

                PolygonShape shape = new PolygonShape();
                shape.setAsBox(rect.width / 2f, rect.height / 2f);

                FixtureDef fd = new FixtureDef();
                fd.shape = shape;
                fd.friction = 0.5f;

                Fixture fx = cuerpo.createFixture(fd);
                fx.setUserData("colision"); // o algo más específico

                shape.dispose();
            }
        }
    }
}

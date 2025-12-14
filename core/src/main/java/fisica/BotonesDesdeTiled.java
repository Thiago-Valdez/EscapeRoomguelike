package fisica;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;

import mapa.DatosBoton;
import mapa.Habitacion;

public class BotonesDesdeTiled {

    public static void crearBotones(TiledMap map, World world) {
        MapLayer layer = map.getLayers().get("botones");
        if (layer == null) {
            Gdx.app.log("BotonesDesdeTiled", "No existe la capa 'botones'");
            return;
        }

        int creados = 0;

        for (MapObject obj : layer.getObjects()) {

            if (!(obj instanceof RectangleMapObject rmo)) {
                continue; // por ahora solo rectángulos
            }

            Rectangle rect = rmo.getRectangle();

            // Propiedades obligatorias: sala (string) y jugador (int)
            String salaStr = getString(obj, "sala", null);
            int jugadorId = getInt(obj, "jugador", -1);

            if (salaStr == null || jugadorId < 1 || jugadorId > 2) {
                Gdx.app.log("BotonesDesdeTiled",
                    "Botón inválido (faltan props): sala=" + salaStr + " jugador=" + jugadorId);
                continue;
            }

            Habitacion sala;
            try {
                // Debe coincidir EXACTO con el enum Habitacion (ej: ACERTIJO_5)
                sala = Habitacion.valueOf(salaStr);
            } catch (Exception e) {
                Gdx.app.log("BotonesDesdeTiled", "Sala inválida en botón: " + salaStr);
                continue;
            }

            // Crear body estático + sensor (en píxeles, como tu mundo)
            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.StaticBody;
            bd.position.set(rect.x + rect.width / 2f, rect.y + rect.height / 2f);

            Body body = world.createBody(bd);

            PolygonShape shape = new PolygonShape();
            shape.setAsBox(rect.width / 2f, rect.height / 2f);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.isSensor = true;

            Fixture fx = body.createFixture(fd);
            shape.dispose();

            // userData: DatosBoton(sala, jugadorId)
            fx.setUserData(new DatosBoton(sala, jugadorId));

            // opcional: para debug
            body.setUserData("boton");

            creados++;
        }

        Gdx.app.log("BotonesDesdeTiled", "Botones creados: " + creados);
    }

    private static String getString(MapObject obj, String key, String def) {
        Object v = obj.getProperties().get(key);
        return (v != null) ? String.valueOf(v) : def;
    }

    private static int getInt(MapObject obj, String key, int def) {
        Object v = obj.getProperties().get(key);
        if (v == null) return def;
        try {
            if (v instanceof Integer i) return i;
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}

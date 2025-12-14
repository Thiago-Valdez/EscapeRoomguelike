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

    private static final String LAYER_NAME = "botones";

    public static void crearBotones(TiledMap map, World world) {
        if (map == null || world == null) {
            Gdx.app.log("BotonesDesdeTiled", "map/world null -> no se crean botones");
            return;
        }

        MapLayer layer = map.getLayers().get(LAYER_NAME);
        if (layer == null) {
            Gdx.app.log("BotonesDesdeTiled", "No existe la capa '" + LAYER_NAME + "'");
            return;
        }

        int creados = 0;
        int ignorados = 0;

        for (MapObject obj : layer.getObjects()) {

            // Hoy soportamos rectángulos (lo más común en Tiled).
            if (!(obj instanceof RectangleMapObject rmo)) {
                ignorados++;
                Gdx.app.log("BotonesDesdeTiled",
                    "Objeto ignorado (no es rect): name=" + safeName(obj) +
                        " type=" + obj.getClass().getSimpleName());
                continue;
            }

            Rectangle rect = rmo.getRectangle();
            if (rect == null || rect.width <= 0 || rect.height <= 0) {
                ignorados++;
                Gdx.app.log("BotonesDesdeTiled",
                    "Rect inválido: name=" + safeName(obj) +
                        " rect=" + rect);
                continue;
            }

            // Props obligatorias
            String salaStr = getString(obj, "sala", null);
            int jugadorId  = getInt(obj, "jugador", -1);

            if (salaStr == null || salaStr.isBlank()) {
                ignorados++;
                Gdx.app.log("BotonesDesdeTiled",
                    "Botón inválido: falta prop 'sala'. name=" + safeName(obj));
                continue;
            }

            if (jugadorId < 1 || jugadorId > 2) {
                ignorados++;
                Gdx.app.log("BotonesDesdeTiled",
                    "Botón inválido: prop 'jugador' debe ser 1 o 2. name=" + safeName(obj) +
                        " jugador=" + jugadorId);
                continue;
            }

            Habitacion sala;
            try {
                // Debe coincidir EXACTO con el enum Habitacion (ej: ACERTIJO_5)
                sala = Habitacion.valueOf(salaStr.trim());
            } catch (Exception e) {
                ignorados++;
                Gdx.app.log("BotonesDesdeTiled",
                    "Sala inválida en botón: name=" + safeName(obj) +
                        " salaStr=" + salaStr);
                continue;
            }

            // ✅ Crear body estático + sensor (en píxeles)
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

            // 🔥 CLAVE: userData = DatosBoton(sala, jugadorId)
            fx.setUserData(new DatosBoton(sala, jugadorId));

            // opcional: debug
            body.setUserData("boton:" + sala.name() + ":J" + jugadorId);

            creados++;
        }

        Gdx.app.log("BotonesDesdeTiled",
            "Botones creados: " + creados + " | ignorados: " + ignorados);
    }

    private static String safeName(MapObject obj) {
        String n = obj.getName();
        return (n == null) ? "<sin-nombre>" : n;
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

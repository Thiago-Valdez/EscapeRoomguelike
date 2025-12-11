package fisica;

import com.badlogic.gdx.physics.box2d.*;

/**
 * Fábrica de cuerpos Box2D trabajando directamente en UNIDADES DE PÍXELES.
 *
 * Ojo: Box2D recomienda trabajar en metros, pero como todo tu juego
 * (cámara, Tiled, etc.) está en píxeles, acá también usamos píxeles para
 * que todo alinee visualmente.
 */
public class FabricaCuerpos {

    private final World world;

    public FabricaCuerpos(World world) {
        this.world = world;
    }

    /** Crea un cuerpo dinámico circular para el jugador. Coordenadas en píxeles. */
    public Body crearJugador(float xPx, float yPx, float radioPx) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        // posición en píxeles
        bd.position.set(xPx, yPx);
        bd.fixedRotation = true;

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        // radio en píxeles
        shape.setRadius(radioPx);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1f;
        fd.friction = 0.2f;
        fd.restitution = 0f;
        fd.filter.categoryBits = BitsColision.CATEGORIA_JUGADOR;
        fd.filter.maskBits = BitsColision.MASCARA_JUGADOR;

        body.createFixture(fd).setUserData("JUGADOR");
        shape.dispose();
        return body;
    }

    /** Crea una pared rectangular (estática). Coordenadas/tamaño en píxeles. */
    public Body crearPared(float xPx, float yPx, float anchoPx, float altoPx) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        // Box2D posiciona el cuerpo en el centro de la shape
        bd.position.set(xPx + anchoPx / 2f, yPx + altoPx / 2f);

        Body body = world.createBody(bd);

        PolygonShape box = new PolygonShape();
        // setAsBox recibe halfWidth / halfHeight
        box.setAsBox(anchoPx / 2f, altoPx / 2f);

        FixtureDef fd = new FixtureDef();
        fd.shape = box;
        fd.friction = 0.2f;
        fd.filter.categoryBits = BitsColision.CATEGORIA_PARED;
        fd.filter.maskBits = BitsColision.MASCARA_PARED;

        body.createFixture(fd).setUserData("PARED");
        box.dispose();
        return body;
    }

    /**
     * Crea una puerta como SENSOR (no bloquea, solo detecta contacto).
     * Coordenadas/tamaño en píxeles.
     */
    public Body crearPuertaSensor(float xPx, float yPx, float anchoPx, float altoPx, Object userDataPuerta) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        // centro de la puerta
        bd.position.set(xPx + anchoPx / 2f, yPx + altoPx / 2f);

        Body body = world.createBody(bd);

        PolygonShape box = new PolygonShape();
        box.setAsBox(anchoPx / 2f, altoPx / 2f);

        FixtureDef fd = new FixtureDef();
        fd.shape = box;
        fd.isSensor = true;
        fd.filter.categoryBits = BitsColision.CATEGORIA_PUERTA;
        fd.filter.maskBits = BitsColision.MASCARA_PUERTA;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(userDataPuerta); // ej.: DatosPuerta
        box.dispose();
        return body;
    }
}

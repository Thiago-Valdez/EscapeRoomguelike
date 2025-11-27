package fisica;

import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.math.Vector2;

public class FabricaCuerpos {

    private final World world;

    public FabricaCuerpos(World world) { this.world = world; }

    /** Crea un cuerpo dinámico circular para el jugador. */
    public Body crearJugador(float xPx, float yPx, float radioPx) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(px2m(xPx), px2m(yPx));
        bd.fixedRotation = true;

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(px2m(radioPx));

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

    /** Crea una pared rectangular (estática). */
    public Body crearPared(float xPx, float yPx, float anchoPx, float altoPx) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(px2m(xPx + anchoPx/2f), px2m(yPx + altoPx/2f)); // centro

        Body body = world.createBody(bd);

        PolygonShape box = new PolygonShape();
        box.setAsBox(px2m(anchoPx/2f), px2m(altoPx/2f));

        FixtureDef fd = new FixtureDef();
        fd.shape = box;
        fd.friction = 0.2f;
        fd.filter.categoryBits = BitsColision.CATEGORIA_PARED;
        fd.filter.maskBits = BitsColision.MASCARA_PARED;

        body.createFixture(fd).setUserData("PARED");
        box.dispose();
        return body;
    }

    /** Crea una puerta como SENSOR (no bloquea, solo detecta contacto). */
    public Body crearPuertaSensor(float xPx, float yPx, float anchoPx, float altoPx, Object userDataPuerta) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(px2m(xPx + anchoPx/2f), px2m(yPx + altoPx/2f)); // centro

        Body body = world.createBody(bd);

        PolygonShape box = new PolygonShape();
        box.setAsBox(px2m(anchoPx/2f), px2m(altoPx/2f));

        FixtureDef fd = new FixtureDef();
        fd.shape = box;
        fd.isSensor = true;
        fd.filter.categoryBits = BitsColision.CATEGORIA_PUERTA;
        fd.filter.maskBits = BitsColision.MASCARA_PUERTA;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(userDataPuerta); // ej.: "HAB:Inicio 1 -> ESTE"
        box.dispose();
        return body;
    }

    private static float px2m(float px) { return px / FisicaMundo.PPM; }
    public  static float m2px(float m)  { return m * FisicaMundo.PPM; }
}

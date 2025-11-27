package fisica;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;

public class FisicaMundo {

    public static final float PPM = 1f; // 1 unidad = 1 pixel

    private final World world;
    private final Box2DDebugRenderer debugRenderer;

    public FisicaMundo() {
        world = new World(new Vector2(0, 0), true); // top-down sin gravedad
        debugRenderer = new Box2DDebugRenderer();
    }

    public World world() { return world; }

    public void setContactListener(com.badlogic.gdx.physics.box2d.ContactListener listener) {
        world.setContactListener(listener);
    }

    public void step() {
        world.step(1f / 60f, 6, 2);
    }

    public void debugDraw(OrthographicCamera camara) {
        debugRenderer.render(world, camara.combined);
    }

    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
    }
}

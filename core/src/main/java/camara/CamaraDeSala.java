package camara;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import mapa.DisposicionMapa;
import mapa.Habitacion;

/**
 * Cámara ortográfica que siempre queda centrada en la habitación actual.
 * - Soporta "snap" instantáneo o una transición suave (lerp).
 * - Usa FitViewport para mantener relación de aspecto.
 *
 * Unidades de mundo: píxeles (asumiendo que tus habitaciones usan px).
 */
public class CamaraDeSala {

    private final OrthographicCamera camara;
    private final Viewport viewport;

    // Objetivo de la interpolación (centro objetivo)
    private float objetivoX;
    private float objetivoY;

    // Config de lerp (0 = salto instantáneo; 1 = no se mueve)
    private float factorLerp = 0f; // por defecto: salto instantáneo

    // Tamaño lógico del mundo visible (en px, por ejemplo, igual a una sala)
    private final float anchoMundoVisible;
    private final float altoMundoVisible;

    public CamaraDeSala(float anchoMundoVisible, float altoMundoVisible) {
        this.anchoMundoVisible = anchoMundoVisible;
        this.altoMundoVisible = altoMundoVisible;

        this.camara = new OrthographicCamera();
        this.viewport = new FitViewport(anchoMundoVisible, altoMundoVisible, camara);

        // Centro inicial (0,0); actualizaremos cuando fijemos sala.
        this.objetivoX = 0f;
        this.objetivoY = 0f;
        camara.position.set(objetivoX, objetivoY, 0f);
        camara.update();
    }

    /** Llama en tu Screen.resize(w,h). */
    public void resize(int width, int height) {
        viewport.update(width, height, true /* center camera */);
    }

    public OrthographicCamera getCamara() { return camara; }
    public Viewport getViewport() { return viewport; }

    /**
     * Define el "snapping" o transición:
     * - factor = 0  -> snap instantáneo
     * - factor ~ 0.1..0.25 -> transición suave rápida
     * - factor ~ 0.5 -> muy suave (lento)
     */
    public void setFactorLerp(float factor) {
        this.factorLerp = Math.max(0f, Math.min(0.99f, factor));
    }

    /**
     * Centra instantáneamente la cámara en la Colocación dada.
     */
    public void centrarEn(DisposicionMapa.Colocacion col) {
        float cx = centroX(col);
        float cy = centroY(col);
        this.objetivoX = cx;
        this.objetivoY = cy;
        camara.position.set(cx, cy, 0f);
        camara.update();
    }

    /**
     * Configura el objetivo y deja que el update haga el lerp.
     * Si factorLerp = 0, el resultado es equivalente a centrarEn (salto instantáneo).
     */
    public void moverHacia(DisposicionMapa.Colocacion col) {
        this.objetivoX = centroX(col);
        this.objetivoY = centroY(col);
        if (factorLerp == 0f) {
            camara.position.set(objetivoX, objetivoY, 0f);
            camara.update();
        }
    }

    /**
     * Llamar una vez por frame (ej. en render()) si usás transición suave.
     */
    public void update(float delta) {
        if (factorLerp > 0f) {
            float x = camara.position.x + (objetivoX - camara.position.x) * (1f - factorLerp);
            float y = camara.position.y + (objetivoY - camara.position.y) * (1f - factorLerp);
            camara.position.set(x, y, 0f);
            camara.update();
        }
    }

    // ==== Cálculo de centro de habitación en mundo ====

    private static float centroX(DisposicionMapa.Colocacion c) {
        Habitacion h = c.habitacion;
        float worldX = c.gx * h.ancho;
        return worldX + h.ancho * 0.5f;
    }

    private static float centroY(DisposicionMapa.Colocacion c) {
        Habitacion h = c.habitacion;
        float worldY = c.gy * h.alto;
        return worldY + h.alto * 0.5f;
    }
}

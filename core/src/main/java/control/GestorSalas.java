package control;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.math.Vector2;
import camara.CamaraDeSala;
import fisica.*;
import mapa.*;

import java.util.ArrayList;
import java.util.List;

public class GestorSalas {

    private final DisposicionMapa disposicion;
    private final FisicaMundo fisica;
    private final FabricaCuerpos fabrica;
    private final SistemaColisiones sistemaColisiones;
    private final CamaraDeSala camara;

    private DisposicionMapa.Colocacion salaActual;
    private final List<Body> cuerposSala = new ArrayList<>();

    public GestorSalas(DisposicionMapa disposicion,
                       FisicaMundo fisica,
                       CamaraDeSala camara,
                       DisposicionMapa.Colocacion salaInicial) {
        this.disposicion = disposicion;
        this.fisica = fisica;
        this.fabrica = new FabricaCuerpos(fisica.world());
        this.sistemaColisiones = new SistemaColisiones(fabrica);
        this.camara = camara;
        entrarSala(salaInicial);
    }

    /** Construye paredes/puertas de la sala y centra cámara. */
    public void entrarSala(DisposicionMapa.Colocacion sala) {
        limpiarSalaActual();
        salaActual = sala;
        cuerposSala.addAll(sistemaColisiones.construirParaSala(salaActual));
        camara.moverHacia(salaActual); // o centrarEn(...) si querés snap
        System.out.println("[SALA] Entraste a: " + sala.habitacion.nombreVisible + " @(" + sala.gx + "," + sala.gy + ")");
    }

    /** Llamado por ContactListener cuando el jugador toca una puerta. */
    public void irASalaVecinaPorPuerta(DatosPuerta puerta, Body cuerpoJugador) {
        // Calcular la celda vecina:
        int nx = puerta.gx + dx(puerta.direccion);
        int ny = puerta.gy + dy(puerta.direccion);

        DisposicionMapa.Colocacion vecina = disposicion.buscar(nx, ny);
        if (vecina == null) {
            System.out.println("[WARN] No hay sala vecina en dirección " + puerta.direccion + " desde (" + puerta.gx + "," + puerta.gy + ")");
            return;
        }

        // Entrar a la sala vecina
        entrarSala(vecina);

        // Reubicar jugador en la puerta opuesta dentro de la nueva sala
        Direccion op = puerta.direccion.opuesta();
        EspecificacionPuerta ep = vecina.habitacion.puertas.get(op);
        if (ep == null) {
            // fallback: centro de la sala
            float cx = vecina.gx * vecina.habitacion.ancho + vecina.habitacion.ancho / 2f;
            float cy = vecina.gy * vecina.habitacion.alto  + vecina.habitacion.alto  / 2f;
            cuerpoJugador.setTransform(cx / FisicaMundo.PPM, cy / FisicaMundo.PPM, 0);
            System.out.println("[REUBICACION] No hay puerta opuesta; movido al centro.");
        } else {
            // offset pequeño hacia adentro de la sala (para “aparecer” dentro)
            float offset = 48f;
            float baseX = vecina.gx * vecina.habitacion.ancho;
            float baseY = vecina.gy * vecina.habitacion.alto;
            float px = baseX + ep.localX;
            float py = baseY + ep.localY;

            switch (op) {
                case NORTE -> py -= offset;
                case SUR   -> py += offset;
                case ESTE  -> px -= offset;
                case OESTE -> px += offset;
            }

            cuerpoJugador.setLinearVelocity(Vector2.Zero);
            cuerpoJugador.setTransform(px / FisicaMundo.PPM, py / FisicaMundo.PPM, 0);
            System.out.println("[REUBICACION] Jugador colocado en puerta opuesta (" + op + ") de " + vecina.habitacion.nombreVisible);
        }
    }

    private void limpiarSalaActual() {
        if (!cuerposSala.isEmpty()) {
            World w = fisica.world();
            for (Body b : cuerposSala) w.destroyBody(b);
            cuerposSala.clear();
        }
    }

    private static int dx(Direccion d) {
        return switch (d) {
            case ESTE -> 1;
            case OESTE -> -1;
            default -> 0;
        };
    }

    private static int dy(Direccion d) {
        return switch (d) {
            case NORTE -> 1;
            case SUR -> -1;
            default -> 0;
        };
    }

    public DisposicionMapa.Colocacion salaActual() { return salaActual; }
}

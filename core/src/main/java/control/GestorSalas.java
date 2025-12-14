package control;

import camara.CamaraDeSala;
import entidades.Jugador;
import fisica.FisicaMundo;
import mapa.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;

public class GestorSalas {

    private final DisposicionMapa disposicion;
    private final CamaraDeSala camara;
    private final Jugador jugador;

    private Habitacion salaActual;

    public GestorSalas(DisposicionMapa disposicion,
                       FisicaMundo fisica,
                       CamaraDeSala camara,
                       Habitacion salaInicial,
                       Jugador jugador) {

        this.disposicion = disposicion;
        this.camara = camara;
        this.jugador = jugador;
        this.salaActual = salaInicial;

        if (camara != null) camara.centrarEn(salaInicial);

        Gdx.app.log("SALA", "Sala inicial: " + salaInicial.nombreVisible +
            " @(" + salaInicial.gridX + "," + salaInicial.gridY + ")");
    }

    public Habitacion getSalaActual() {
        return salaActual;
    }

    /** Cambio de sala cuando el jugador toca una puerta */
    public void irASalaVecinaPorPuerta(DatosPuerta puerta) {
        if (puerta == null) return;

        Body cuerpoJugador = jugador.getCuerpoFisico();
        if (cuerpoJugador == null) return;

        // Dirección efectiva según desde qué lado se cruza
        Direccion dirEfectiva;

        if (salaActual == puerta.origen()) {
            dirEfectiva = puerta.direccion();
        } else if (salaActual == puerta.destino()) {
            dirEfectiva = puerta.direccion().opuesta();
        } else {
            Gdx.app.log("GestorSalas",
                "ERROR: puerta no pertenece a salaActual=" + salaActual.nombreVisible +
                    " (origen=" + puerta.origen().nombreVisible + ", destino=" + puerta.destino().nombreVisible + ")");
            return;
        }

        // Fuente de verdad: conexionesPiso
        Habitacion nuevaSala = disposicion.getDestinoEnPiso(salaActual, dirEfectiva);
        if (nuevaSala == null) {
            Gdx.app.log("GestorSalas",
                "Puerta bloqueada (sin destino en piso): " + salaActual.nombreVisible + " por " + dirEfectiva);
            return;
        }

        Direccion dirEntrada = dirEfectiva.opuesta();

        // Cambiar sala
        salaActual = nuevaSala;

        // Reposicionar jugador “adentro”
        colocarJugadorEnSalaPorPuerta(nuevaSala, dirEntrada);

        // Centrar cámara
        camara.centrarEn(nuevaSala);

        // Descubrir sala (si querés hacerlo acá y no afuera)
        disposicion.descubrir(nuevaSala);

        Gdx.app.log("SALA", "Entraste a: " + nuevaSala.nombreVisible +
            " @(" + nuevaSala.gridX + "," + nuevaSala.gridY + ")");
    }

    private void colocarJugadorEnSalaPorPuerta(Habitacion sala, Direccion entrada) {

        Body cuerpoJugador = jugador.getCuerpoFisico();
        if (cuerpoJugador == null) return;

        float baseX = sala.gridX * sala.ancho;
        float baseY = sala.gridY * sala.alto;

        EspecificacionPuerta ep = sala.puertas.get(entrada);
        float px, py;

        if (ep == null) {
            px = baseX + sala.ancho / 2f;
            py = baseY + sala.alto / 2f;
            cuerpoJugador.setLinearVelocity(0, 0);
            cuerpoJugador.setTransform(px, py, 0);
            return;
        }

        px = baseX + ep.localX;
        py = baseY + ep.localY;

        // Offset grande para no re-tocar el sensor al aparecer
        float offset = 64f;
        switch (entrada) {
            case NORTE -> py -= offset;
            case SUR   -> py += offset;
            case ESTE  -> px -= offset;
            case OESTE -> px += offset;
        }

        cuerpoJugador.setLinearVelocity(0, 0);
        cuerpoJugador.setTransform(px, py, 0);

        Gdx.app.log("REUBICACION",
            "Jugador colocado por " + entrada + " en (" + px + "," + py + ")");
    }
}

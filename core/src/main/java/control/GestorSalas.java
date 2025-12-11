package control;

import camara.CamaraDeSala;
import entidades.Jugador;
import fisica.FisicaMundo;
import mapa.DisposicionMapa;
import mapa.DatosPuerta;
import mapa.Direccion;
import mapa.EspecificacionPuerta;
import mapa.Habitacion;
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

        if (camara != null)
            camara.centrarEn(salaInicial);

        Gdx.app.log("SALA", "Sala inicial: " + salaInicial.nombreVisible +
            " @(" + salaInicial.gridX + "," + salaInicial.gridY + ")");
    }

    public Habitacion getSalaActual() {
        return salaActual;
    }

    /** Cambio de sala cuando el jugador toca una puerta */
    public void irASalaVecinaPorPuerta(DatosPuerta puerta, Body cuerpoJugador) {

        Habitacion origen  = puerta.origen();
        Habitacion destino = puerta.destino();
        Direccion dirSalida = puerta.direccion(); // hacia dónde sale

        Habitacion nuevaSala;
        Direccion dirEntrada; // por dónde entra a la siguiente

        // Si estoy saliendo desde la sala actual
        if (salaActual == origen) {
            nuevaSala = destino;
            dirEntrada = dirSalida.opuesta();

            // Si el sensor detectó desde el otro lado
        } else if (salaActual == destino) {
            nuevaSala = origen;
            dirEntrada = dirSalida.opuesta();

        } else {
            Gdx.app.log("GestorSalas",
                "ERROR: puerta no pertenece a salaActual=" + salaActual.nombreVisible);
            return;
        }

        // Cambiar sala
        salaActual = nuevaSala;

        // Colocar al jugador en la sala nueva por la puerta opuesta
        colocarJugadorEnSalaPorPuerta(nuevaSala, dirEntrada, cuerpoJugador);

        // Centrar la cámara en la sala nueva
        camara.centrarEn(nuevaSala);

        Gdx.app.log("SALA", "Entraste a: " + nuevaSala.nombreVisible +
            " @(" + nuevaSala.gridX + "," + nuevaSala.gridY + ")");
    }


    /** Posiciona al jugador según la puerta por la que entra */
    private void colocarJugadorEnSalaPorPuerta(Habitacion sala,
                                               Direccion entrada,
                                               Body cuerpoJugador) {

        float baseX = sala.gridX * sala.ancho;
        float baseY = sala.gridY * sala.alto;

        EspecificacionPuerta ep = sala.puertas.get(entrada);
        float px, py;

        if (ep == null) {
            // fallback si la sala no definió ese borde
            px = baseX + sala.ancho / 2f;
            py = baseY + sala.alto / 2f;
            cuerpoJugador.setTransform(px, py, 0);
            return;
        }

        // posición EXACTA de la puerta
        px = baseX + ep.localX;
        py = baseY + ep.localY;

        // pequeño offset hacia adentro para evitar re-disparo del sensor
        float offset = 40f;
        switch (entrada) {
            case NORTE -> py -= offset;
            case SUR   -> py += offset;
            case ESTE  -> px -= offset;
            case OESTE -> px += offset;
        }

        // Reposicionar jugador
        cuerpoJugador.setLinearVelocity(0, 0);
        cuerpoJugador.setTransform(px, py, 0);

        if (jugador != null)
            jugador.setCuerpoFisico(cuerpoJugador);

        Gdx.app.log("REUBICACION",
            "Jugador colocado por " + entrada +
                " en (" + px + "," + py + ")");
    }
}

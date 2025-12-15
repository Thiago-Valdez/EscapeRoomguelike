package juego.sistemas;

import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Gdx;

import camara.CamaraDeSala;
import control.input.ControlJugador;
import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;
import entidades.GestorDeEntidades;
import entidades.enemigos.Enemigo;
import entidades.eventos.EventoDanio;
import entidades.eventos.EventoPickup;
import entidades.items.Item;
import entidades.personajes.Jugador;
import fisica.FisicaMundo;
import mapa.botones.EventoBoton;
import mapa.minimapa.DisposicionMapa;
import mapa.model.Habitacion;
import mapa.puertas.EventoPuerta;

/**
 * Agrupa el update del gameplay (sin render).
 * Mantiene a Partida como un orquestador chico y legible.
 */
public final class SistemaActualizacionPartida {

    private final GestorDeEntidades gestorEntidades;
    private final FisicaMundo fisica;
    private final CamaraDeSala camaraSala;
    private final SistemaTransicionSala transicionSala;
    private final ProcesadorColasEventos procesadorEventos;
    private final SistemaSpritesEntidades sprites;

    public SistemaActualizacionPartida(
            GestorDeEntidades gestorEntidades,
            FisicaMundo fisica,
            CamaraDeSala camaraSala,
            SistemaTransicionSala transicionSala,
            ProcesadorColasEventos procesadorEventos,
            SistemaSpritesEntidades sprites
    ) {
        this.gestorEntidades = gestorEntidades;
        this.fisica = fisica;
        this.camaraSala = camaraSala;
        this.transicionSala = transicionSala;
        this.procesadorEventos = procesadorEventos;
        this.sprites = sprites;
    }

    public Habitacion actualizar(
            float delta,
            Habitacion salaActual,
            Jugador jugador1,
            Jugador jugador2,
            ControlJugador controlJugador1,
            ControlJugador controlJugador2,
            // colas
            List<EventoPuerta> puertasPendientes,
            List<EventoPickup> itemsPendientes,
            Set<Item> itemsYaProcesados,
            List<EventoBoton> eventosBoton,
            List<EventoDanio> daniosPendientes,
            Set<Integer> jugadoresDanioFrame,
            List<mapa.botones.BotonVisual> botonesVisuales,
            // dependencias de sala
            ControlPuzzlePorSala controlPuzzle,
            GestorSalas gestorSalas,
            DisposicionMapa disposicion,
            java.util.function.BiConsumer<Habitacion, Habitacion> notificarCambioSala,
            com.badlogic.gdx.maps.tiled.TiledMap mapaTiled,
            com.badlogic.gdx.physics.box2d.World world
    ) {
        if (gestorEntidades == null || fisica == null) return salaActual;

        // 1) actualizar entidades (sin físicas)
        gestorEntidades.actualizar(delta, salaActual);

        // 2) input
        actualizarControles(delta, controlJugador1, controlJugador2);

        // 3) IA / enemigos
        gestorEntidades.actualizarEnemigos(delta, jugador1, jugador2);

        // 4) físicas
        fisica.step(delta);

        // 5) transición y colas
        if (transicionSala != null) {
            salaActual = transicionSala.procesarPuertasPendientes(
                    salaActual,
                    puertasPendientes,
                    controlPuzzle,
                    gestorSalas,
                    disposicion,
                    notificarCambioSala,
                    mapaTiled,
                    world,
                    gestorEntidades,
                    sprites
            );
            transicionSala.tickCooldown();
        } else {
            if (puertasPendientes != null) puertasPendientes.clear();
        }

        if (procesadorEventos != null) {
            procesadorEventos.procesarItemsPendientes(itemsPendientes, itemsYaProcesados, gestorEntidades);

            
            if (controlPuzzle == null) {
                if (eventosBoton != null) eventosBoton.clear();
            } else {
                java.util.function.Consumer<Habitacion> matarEnemigosDeSalaConAnim =
                        (sprites != null) ? sprites::matarEnemigosDeSalaConAnim : null;

                procesadorEventos.procesarBotonesPendientes(
                        eventosBoton,
                        salaActual,
                        controlPuzzle,
                        matarEnemigosDeSalaConAnim,
                        botonesVisuales
                );
            }
            procesadorEventos.procesarDaniosPendientes(daniosPendientes, jugadoresDanioFrame, gestorEntidades, sprites);
        }

        // 6) jugadores
        actualizarJugadores(delta, jugador1, jugador2);

        // 7) housekeeping sprites
        if (sprites != null) {
            sprites.registrarSpritesDeEnemigosVivos();
            sprites.procesarEnemigosEnMuerte();
            sprites.limpiarSpritesDeEntidadesMuertas();
        }

        // 8) cámara (en update, no en render)
        if (camaraSala != null) camaraSala.update(delta);

        return salaActual;
    }

    private void actualizarControles(float delta, ControlJugador c1, ControlJugador c2) {
        if (c1 != null) c1.actualizar(delta);
        if (c2 != null) c2.actualizar(delta);
    }

    private void actualizarJugadores(float delta, Jugador j1, Jugador j2) {
        if (j1 != null) {
            boolean estabaEnMuerte = j1.estaEnMuerte();
            j1.updateEstado(delta);
            j1.tick(delta);
            if (estabaEnMuerte && !j1.estaEnMuerte() && sprites != null) sprites.detenerMuerte(j1);
        }
        if (j2 != null) {
            boolean estabaEnMuerte = j2.estaEnMuerte();
            j2.updateEstado(delta);
            j2.tick(delta);
            if (estabaEnMuerte && !j2.estaEnMuerte() && sprites != null) sprites.detenerMuerte(j2);
        }
    }
}

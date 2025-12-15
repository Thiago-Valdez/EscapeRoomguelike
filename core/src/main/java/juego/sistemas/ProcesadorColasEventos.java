package juego.sistemas;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;

import control.puzzle.ControlPuzzlePorSala;
import entidades.Entidad;
import entidades.GestorDeEntidades;
import entidades.eventos.EventoDanio;
import entidades.eventos.EventoPickup;
import entidades.personajes.Jugador;
import entidades.sprites.SpritesEntidad;
import mapa.botones.DatosBoton;
import mapa.botones.EventoBoton;
import mapa.model.Habitacion;

/**
 * Procesa colas de eventos (pickup, botones, daño) para evitar modificar Box2D dentro de callbacks
 * y mantener la lógica del frame centralizada.
 */
public final class ProcesadorColasEventos {

    public void procesarItemsPendientes(
            List<EventoPickup> itemsPendientes,
            Set<entidades.items.Item> itemsYaProcesados,
            GestorDeEntidades gestorEntidades
    ) {
        if (itemsPendientes.isEmpty()) return;

        itemsYaProcesados.clear();

        for (EventoPickup ev : itemsPendientes) {
            if (!itemsYaProcesados.add(ev.item())) continue;
            gestorEntidades.recogerItem(ev.jugadorId(), ev.item());
        }

        itemsPendientes.clear();
    }

    public void procesarBotonesPendientes(
        List<EventoBoton> eventosBoton,
        Habitacion salaActual,
        ControlPuzzlePorSala controlPuzzle,
        Consumer<Habitacion> matarEnemigosDeSalaConAnim,
        List<mapa.botones.BotonVisual> botonesVisuales
    ) {
        if (eventosBoton.isEmpty()) return;

        for (EventoBoton ev : eventosBoton) {
            DatosBoton boton = ev.boton();
            int jugadorId = ev.jugadorId();

            if (boton.sala() != salaActual) continue;

            boolean valido = (jugadorId == boton.jugadorId());
            if (!valido) continue;

            // ✅ VISUAL: marcar DOWN/UP para TODOS los botones de esa sala + jugador
            if (botonesVisuales != null) {
                boolean down = ev.down();
                for (mapa.botones.BotonVisual bv : botonesVisuales) {
                    if (bv == null) continue;
                    if (bv.sala != salaActual) continue;
                    if (bv.jugadorId != jugadorId) continue;
                    bv.presionado = down;
                }
            }

            // ✅ LÓGICA PUZZLE
            if (ev.down()) {
                boolean desbloqueo = controlPuzzle.botonDown(salaActual, boton.jugadorId());
                if (desbloqueo) {
                    Gdx.app.log("PUZZLE", "Sala desbloqueada: " + salaActual.nombreVisible);

                    if (matarEnemigosDeSalaConAnim != null) {
                        matarEnemigosDeSalaConAnim.accept(salaActual);
                    }
                }
            } else {
                controlPuzzle.botonUp(salaActual, boton.jugadorId());
            }
        }

        eventosBoton.clear();
    }


    public void procesarDaniosPendientes(
            List<EventoDanio> daniosPendientes,
            Set<Integer> jugadoresDanioFrame,
            GestorDeEntidades gestorEntidades,
            SistemaSpritesEntidades sprites
    ) {
        if (daniosPendientes.isEmpty()) return;

        jugadoresDanioFrame.clear();

        for (EventoDanio ev : daniosPendientes) {
            int id = ev.jugadorId();
            if (!jugadoresDanioFrame.add(id)) continue; // evita daño duplicado en el mismo frame

            Jugador j = gestorEntidades.getJugador(id);
            if (j == null) continue;

            // respetar inmune / enMuerte / muerto
            if (!j.estaViva() || j.estaEnMuerte() || j.esInmune()) continue;

            Body body = j.getCuerpoFisico();
            if (body == null) continue;

            // =========================
            // 1) Separación anti-loop (antes de congelar)
            // =========================
            float px = body.getPosition().x;
            float py = body.getPosition().y;

            float dx = px - ev.ex();
            float dy = py - ev.ey();

            float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) {
                dx = 1f;
                dy = 0f;
                len2 = 1f;
            }

            float invLen = (float)(1.0 / Math.sqrt(len2));
            dx *= invLen;
            dy *= invLen;

            float separacion = 40f; // px
            body.setTransform(px + dx * separacion, py + dy * separacion, body.getAngle());
            body.setLinearVelocity(0f, 0f);
            body.setAngularVelocity(0f);

            // =========================
            // 2) Aplicar daño + cooldown anti re-hit
            // =========================
            j.recibirDanio();
            j.marcarHitCooldown(1.0f);

            // =========================
            // 3) Animación + feedback
            // =========================
            SpritesEntidad sp = (sprites != null) ? sprites.get(j) : null;
            if (sp != null) {
                sp.iniciarMuerte();
            }
        }

        daniosPendientes.clear();
    }
}

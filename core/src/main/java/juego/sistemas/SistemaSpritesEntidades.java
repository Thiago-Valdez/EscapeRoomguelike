package juego.sistemas;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import entidades.Entidad;
import entidades.GestorDeEntidades;
import entidades.enemigos.Enemigo;
import entidades.personajes.Jugador;
import entidades.sprites.SpritesEnemigo;
import entidades.sprites.SpritesEntidad;
import mapa.model.Habitacion;

/**
 * Centraliza el manejo de sprites (jugadores + enemigos) y la cola de muertes animadas.
 * - Evita que Partida tenga que conocer detalles de creación/limpieza de sprites.
 * - Permite que otros sistemas disparen animaciones sin tocar el mapa interno.
 */
public final class SistemaSpritesEntidades {

    private final GestorDeEntidades gestorEntidades;
    private final Map<Entidad, SpritesEntidad> spritesPorEntidad = new HashMap<>();
    private final Set<Enemigo> enemigosEnMuerte = new HashSet<>();

    public SistemaSpritesEntidades(GestorDeEntidades gestorEntidades) {
        this.gestorEntidades = gestorEntidades;
    }

    public void registrar(Entidad e, SpritesEntidad sprite, float offX, float offY) {
        if (e == null || sprite == null) return;
        sprite.setOffset(offX, offY);
        spritesPorEntidad.put(e, sprite);
    }

    public SpritesEntidad get(Entidad e) {
        return spritesPorEntidad.get(e);
    }

    /** Acceso solo lectura (para render). */
    public Map<Entidad, SpritesEntidad> getMapaSprites() {
        return spritesPorEntidad;
    }

    public void registrarSpritesDeEnemigosVivos() {
        if (gestorEntidades == null) return;
        for (Enemigo e : gestorEntidades.getEnemigosMundo()) {
            if (e == null) continue;
            if (spritesPorEntidad.containsKey(e)) continue;

            SpritesEnemigo se = new SpritesEnemigo(e, 48, 48);
            se.setOffset(0f, -2f);
            spritesPorEntidad.put(e, se);
        }
    }

    public void limpiarSpritesDeEntidadesMuertas() {
        if (gestorEntidades == null) return;

        Iterator<Map.Entry<Entidad, SpritesEntidad>> it = spritesPorEntidad.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entidad, SpritesEntidad> entry = it.next();
            Entidad ent = entry.getKey();

            if (ent == null) {
                if (entry.getValue() != null) entry.getValue().dispose();
                it.remove();
                continue;
            }

            if (ent instanceof Jugador) continue;

            if (ent instanceof Enemigo enemigo) {
                boolean sigueVivo = gestorEntidades.getEnemigosMundo().contains(enemigo);
                if (!sigueVivo) {
                    if (entry.getValue() != null) entry.getValue().dispose();
                    it.remove();
                }
            }
        }
    }

    /** Dispara muerte animada para todos los enemigos de una sala. */
    public void matarEnemigosDeSalaConAnim(Habitacion sala) {
        if (gestorEntidades == null) return;
        if (sala == null) return;

        for (Enemigo e : gestorEntidades.getEnemigosDeSala(sala)) {
            SpritesEntidad sp = spritesPorEntidad.get(e);
            if (sp != null) {
                sp.iniciarMuerte();
                enemigosEnMuerte.add(e);
            } else {
                gestorEntidades.eliminarEnemigo(e);
            }
        }
    }

    /** Elimina realmente enemigos cuya animación ya terminó. */
    public void procesarEnemigosEnMuerte() {
        if (gestorEntidades == null) return;
        if (enemigosEnMuerte.isEmpty()) return;

        Iterator<Enemigo> it = enemigosEnMuerte.iterator();
        while (it.hasNext()) {
            Enemigo e = it.next();
            SpritesEntidad sp = spritesPorEntidad.get(e);

            if (sp == null) {
                it.remove();
                continue;
            }

            if (sp.muerteTerminada()) {
                gestorEntidades.eliminarEnemigo(e);
                sp.dispose();
                spritesPorEntidad.remove(e);
                it.remove();
            }
        }
    }

    public void iniciarMuerte(Entidad e) {
        SpritesEntidad sp = spritesPorEntidad.get(e);
        if (sp != null) sp.iniciarMuerte();
    }

    public void detenerMuerte(Entidad e) {
        SpritesEntidad sp = spritesPorEntidad.get(e);
        if (sp != null) sp.detenerMuerte();
    }

    public void limpiarColaMuertes() {
        enemigosEnMuerte.clear();
    }

    public void dispose() {
        for (SpritesEntidad s : spritesPorEntidad.values()) {
            if (s != null) s.dispose();
        }
        spritesPorEntidad.clear();
        enemigosEnMuerte.clear();
    }
}

package mapa;

import java.util.*;

public class DisposicionMapa {

    /** El camino de habitaciones que el generador decidió para este nivel (EN ORDEN) */
    private final List<Habitacion> camino = new ArrayList<>();

    /** Habitaciones visitadas (útil para el minimapa, HUD) */
    private final Set<Habitacion> descubiertas = new HashSet<>();

    /** Agrega una sala al camino (la run actual) */
    public void agregarAlCamino(Habitacion h) {
        if (h == null) return;
        if (!camino.contains(h)) {
            camino.add(h);
        }
    }

    /** Devuelve el camino completo (salas activas de esta run) */
    public List<Habitacion> getCamino() {
        return camino;
    }

    /** Alias semántico: salas activas = camino */
    public List<Habitacion> getSalasActivas() {
        return camino;
    }

    public boolean esSalaActiva(Habitacion h) {
        return camino.contains(h);
    }

    /** Marca una sala como descubierta */
    public void descubrir(Habitacion h) {
        if (h != null) {
            descubiertas.add(h);
        }
    }

    /** Devuelve true si la sala ya se visitó */
    public boolean estaDescubierta(Habitacion h) {
        return descubiertas.contains(h);
    }

    /** Devuelve un set de todas las salas descubiertas */
    public Set<Habitacion> getDescubiertas() {
        return descubiertas;
    }

    /** Sala de inicio: la primera del camino, si existe; si no, INICIO_1 */
    public Habitacion salaInicio() {
        if (!camino.isEmpty()) {
            return camino.get(0);
        }
        return Habitacion.INICIO_1;
    }

    /** Busca la primera sala de cierto tipo ENTRE TODAS LAS HABITACIONES DEFINIDAS */
    public Habitacion buscarPrimeraDeTipo(TipoSala tipo) {
        for (Habitacion h : Habitacion.values()) {
            if (h.tipo == tipo) return h;
        }
        return null;
    }

    /** Helper: obtiene la sala que está en gridX,gridY entre TODAS las habitaciones definidas */
    public Habitacion salaEn(int gx, int gy) {
        for (Habitacion h : Habitacion.values()) {
            if (h.gridX == gx && h.gridY == gy) return h;
        }
        return null;
    }

    /**
     * Vecina conectada SEGÚN EL ENUM Habitacion y el CAMINO:
     * - origen tiene puerta en 'dir'
     * - hay una habitación en (gridX+dx, gridY+dy)
     * - esa habitación está en el camino
     * - dicha habitación tiene puerta en dir.opuesta()
     */
    public Habitacion getVecinaConectada(Habitacion origen, Direccion dir) {
        if (origen == null || dir == null) return null;

        int nx = origen.gridX + dir.dx;
        int ny = origen.gridY + dir.dy;

        for (Habitacion h : camino) {
            if (h.gridX == nx && h.gridY == ny) {
                if (origen.tienePuerta(dir) && h.tienePuerta(dir.opuesta())) {
                    return h;
                }
            }
        }
        return null;
    }
}

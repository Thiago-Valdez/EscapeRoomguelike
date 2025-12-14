package entidades;

public class SpritesJugador extends SpritesEntidad {

    private final Jugador jugador;

    public SpritesJugador(Jugador jugador, int frameW, int frameH) {
        super(jugador, frameW, frameH);
        this.jugador = jugador;

        cargar();
        construirAnimaciones();
    }

    @Override
    protected String pathQuieto() {
        String base = (jugador.getGenero() == Genero.FEMENINO) ? "jugador_fem" : "jugador_masc";
        return "Jugadores/" + base + "_quieto.png";
    }

    @Override
    protected String pathMovimiento() {
        String base = (jugador.getGenero() == Genero.FEMENINO) ? "jugador_fem" : "jugador_masc";
        return "Jugadores/" + base + "_movimiento.png";
    }

    @Override
    protected String pathMuerte() {
        String base = (jugador.getGenero() == Genero.FEMENINO) ? "jugador_fem" : "jugador_masc";
        return "Jugadores/" + base + "_muerte.png";
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        // ✅ Sync fuerte: el estado del sprite depende del estado real del jugador
        if (jugador.estaEnMuerte()) {
            iniciarMuerte(); // se mantiene en muerte mientras el jugador esté en stun
        } else {
            if (estaEnMuerte() || muerteTerminada()) {
                detenerMuerte(); // al levantarse vuelve a idle/move
            }
        }
    }
}

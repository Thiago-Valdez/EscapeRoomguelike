package mapa;

import entidades.Jugador;

public class PuzzleSala {

    private boolean resuelto = false; // Estado para saber si el puzzle ya se resolvió
    private boolean botonJugador1Presionado = false;
    private boolean botonJugador2Presionado = false;

    // Este método se llama para actualizar el estado del puzzle
    public void actualizarPuzzle(Jugador jugador1, Jugador jugador2) {
        // Si el puzzle ya está resuelto, no hacemos nada
        if (resuelto) return;

        // Verificamos si ambos jugadores tienen el botón presionado
        botonJugador1Presionado = jugador1.estaBotonPresionado();
        botonJugador2Presionado = jugador2.estaBotonPresionado();

        if (botonJugador1Presionado && botonJugador2Presionado) {
            // Ambos botones presionados: resolvemos el puzzle
            resolverPuzzle();
        }
    }

    // Método que resuelve el puzzle (abre las puertas)
    private void resolverPuzzle() {
        resuelto = true;
        // Aquí agregarías la lógica para abrir las puertas
        System.out.println("Puzzle resuelto, puertas abiertas.");
    }

    // Método que verifica si la sala ya fue resuelta
    public boolean esSalaResuelta() {
        return resuelto;
    }
}

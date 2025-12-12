// paquete: eventos
package interfaces;

import mapa.Habitacion;

public interface ListenerCambioSala {
    void salaCambiada(Habitacion salaAnterior, Habitacion salaNueva);
}

package modelo;

/**
 * Un campeón ya validado (es la entidad A de la práctica).
 * Al ser un record, equals() compara todos los campos, y eso lo uso en la importación
 * para saber si un campeón que llega es igual al que ya está guardado.
 *
 * @param id         identificador del campeón, por ejemplo "AHR"
 * @param nombre     nombre visible, por ejemplo "Ahri"
 * @param rol        top, jungla, mid, adc o support
 * @param region     región de origen
 * @param dificultad de 1 a 3
 */
public record Campeon(String id, String nombre, String rol, String region, int dificultad) {
}

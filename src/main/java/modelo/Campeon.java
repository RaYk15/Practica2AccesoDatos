package modelo;

import org.json.JSONObject;

/**
 * Clase correspondiente al campeón ya validado.
 * @param id
 * @param nombre
 * @param rol
 * @param region
 * @param dificultad
 */
public record Campeon(String id, String nombre, String rol, String region, int dificultad){ }
package de.mrjulsen.paw.util.collision;

import java.util.Optional;

import org.joml.Vector3f;

public interface IRayTraceShape {
    /**
     * Prüft, ob der Ray diesen Shape trifft.
     * @param rayOrigin Startpunkt des Strahls
     * @param rayDirection Normalisierter Richtungsvektor
     * @return Optional mit HitPosition (in Weltkoordinaten) wenn getroffen
     */
    Optional<Vector3f> intersects(Vector3f rayOrigin, Vector3f rayDirection);
}


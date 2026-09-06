package com.unbosque.afd.desktop.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

@FunctionalInterface
public interface PintorIcono {

    void pintar(Graphics2D g2, Rectangle2D area, Color color);
}

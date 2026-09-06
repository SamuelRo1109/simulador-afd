package com.unbosque.afd.desktop.render;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Tema {

    public interface Sensible {
        void aplicarTema();
    }

    public static final Color INICIAL = new Color(0x2E9E5B);
    public static final Color ACEPTACION = new Color(0xB5179E);
    public static final Color ACTIVO = new Color(0xF97316);
    public static final Color ACEPTADA = new Color(0x22A06B);
    public static final Color RECHAZADA = new Color(0xD93025);
    public static final Color ADVERTENCIA = new Color(0xE8A317);
    public static final Color SELECCION = new Color(0xF97316);
    public static final Color TINTA_SOBRE_ACENTO = new Color(0x1A1A19);

    private static final Color LUZ = new Color(0xFFFFFF);

    public static final Tema GRAFITO = new Tema("Grafito", true,
            0x1A1A19, 0x2C2B29, 0x212120, 0x2A2A28, 0x383836,
            0xF2F0EC, 0xA3A09B, 0x262625, 0xD4D0C8, 0x8A8781);

    public static final Tema PAPEL = new Tema("Papel", false,
            0xFAFAF9, 0xE4E1DB, 0xFFFFFF, 0xF4F2EE, 0xD8D4CE,
            0x1C1B1A, 0x6B6862, 0xFFFFFF, 0x3F3F46, 0x71717A);

    private static final List<Runnable> observadores = new ArrayList<>();
    private static Tema actual = GRAFITO;

    private final String nombre;
    private final boolean oscuro;
    private final Color lienzoFondo;
    private final Color lienzoReticula;
    private final Color panelFondo;
    private final Color panelElevado;
    private final Color panelBorde;
    private final Color textoPrimario;
    private final Color textoSecundario;
    private final Color estadoRelleno;
    private final Color estadoBorde;
    private final Color arista;

    private Tema(String nombre, boolean oscuro, int lienzoFondo, int lienzoReticula, int panelFondo,
                 int panelElevado, int panelBorde, int textoPrimario, int textoSecundario,
                 int estadoRelleno, int estadoBorde, int arista) {
        this.nombre = nombre;
        this.oscuro = oscuro;
        this.lienzoFondo = new Color(lienzoFondo);
        this.lienzoReticula = new Color(lienzoReticula);
        this.panelFondo = new Color(panelFondo);
        this.panelElevado = new Color(panelElevado);
        this.panelBorde = new Color(panelBorde);
        this.textoPrimario = new Color(textoPrimario);
        this.textoSecundario = new Color(textoSecundario);
        this.estadoRelleno = new Color(estadoRelleno);
        this.estadoBorde = new Color(estadoBorde);
        this.arista = new Color(arista);
    }

    public static Tema actual() {
        return actual;
    }

    public static void establecer(Tema tema) {
        Objects.requireNonNull(tema, "El tema no puede ser nulo");
        if (actual == tema) {
            return;
        }
        actual = tema;
        for (Runnable observador : new ArrayList<>(observadores)) {
            observador.run();
        }
    }

    public static void alternar() {
        establecer(actual == GRAFITO ? PAPEL : GRAFITO);
    }

    public static void agregarObservador(Runnable observador) {
        observadores.add(Objects.requireNonNull(observador, "El observador no puede ser nulo"));
    }

    public static void refrescarArbol(Component raiz) {
        if (raiz == null) {
            return;
        }
        if (raiz instanceof Sensible sensible) {
            sensible.aplicarTema();
        }
        if (raiz instanceof Container contenedor) {
            for (Component hijo : contenedor.getComponents()) {
                refrescarArbol(hijo);
            }
        }
        raiz.repaint();
    }

    public String nombre() {
        return nombre;
    }

    public boolean esOscuro() {
        return oscuro;
    }

    public Color lienzoFondo() {
        return lienzoFondo;
    }

    public Color lienzoReticula() {
        return lienzoReticula;
    }

    public Color panelFondo() {
        return panelFondo;
    }

    public Color panelElevado() {
        return panelElevado;
    }

    public Color panelBorde() {
        return panelBorde;
    }

    public Color textoPrimario() {
        return textoPrimario;
    }

    public Color textoSecundario() {
        return textoSecundario;
    }

    public Color estadoRelleno() {
        return estadoRelleno;
    }

    public Color estadoBorde() {
        return estadoBorde;
    }

    public Color arista() {
        return arista;
    }

    public Color luzSuperior() {
        return mezclar(LUZ, panelElevado, oscuro ? 0.06 : 0.85);
    }

    public Color sombraInferior() {
        return mezclar(TINTA_SOBRE_ACENTO, panelElevado, oscuro ? 0.30 : 0.10);
    }

    public static Color aclarar(Color color, double cantidad) {
        return mezclar(LUZ, color, cantidad);
    }

    public Color grisNeutro() {
        int nivel = luminancia(textoSecundario);
        return new Color(nivel, nivel, nivel);
    }

    public Color desactivado(Color color) {
        int nivel = (int) Math.round(luminancia(color) * 0.45 + luminancia(textoSecundario) * 0.55);
        int acotado = Math.max(0, Math.min(255, nivel));
        return new Color(acotado, acotado, acotado);
    }

    public Color superficieDesactivada() {
        return mezclar(grisNeutro(), panelFondo, oscuro ? 0.12 : 0.18);
    }

    private static int luminancia(Color color) {
        return (int) Math.round(
                0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
    }

    public Color celdaSinDefinir() {
        return mezclar(RECHAZADA, panelElevado, 0.15);
    }

    public Color pulgarDesplazamiento() {
        return mezclar(textoSecundario, panelFondo, 0.40);
    }

    public Color sobrevuelo() {
        return mezclar(textoPrimario, panelElevado, 0.10);
    }

    public static Color mezclar(Color frente, Color fondo, double opacidad) {
        double peso = Math.min(1, Math.max(0, opacidad));
        return new Color(
                (int) Math.round(frente.getRed() * peso + fondo.getRed() * (1 - peso)),
                (int) Math.round(frente.getGreen() * peso + fondo.getGreen() * (1 - peso)),
                (int) Math.round(frente.getBlue() * peso + fondo.getBlue() * (1 - peso)));
    }

    public static Color conAlfa(Color color, double alfa) {
        int valor = (int) Math.round(Math.min(1, Math.max(0, alfa)) * 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), valor);
    }
}

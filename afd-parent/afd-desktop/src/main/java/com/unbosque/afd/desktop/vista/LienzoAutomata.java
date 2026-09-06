package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.core.logica.DistribuidorEstados;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.PasoEjecucion;
import com.unbosque.afd.core.modelo.Punto;
import com.unbosque.afd.desktop.render.AristaGrafica;
import com.unbosque.afd.desktop.render.CalculadoraGeometria;
import com.unbosque.afd.desktop.render.MetricasTexto;
import com.unbosque.afd.desktop.render.NodoGrafico;
import com.unbosque.afd.desktop.render.Paleta;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LienzoAutomata extends JPanel {

    private static final double ESCALA_MINIMA = 0.25;
    private static final double ESCALA_MAXIMA = 4.0;
    private static final double FACTOR_ZOOM = 1.1;
    private static final float GROSOR_NODO = 2f;
    private static final float GROSOR_ARISTA = 1.8f;
    private static final float GROSOR_ACTIVO = 3f;
    private static final float GROSOR_HALO = 6f;
    private static final float GROSOR_RESULTADO = 4f;
    private static final int PADDING_ETIQUETA = 3;
    private static final int MARGEN_VISTA = 40;
    private static final String ACCION_AJUSTAR = "ajustarAVista";
    private static final double ESCALA_NATURAL = 1.0;

    private final List<NodoGrafico> nodos = new ArrayList<>();
    private final AffineTransform vista = new AffineTransform();
    private final Font fuenteEtiqueta = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    private AutomataFinitoDeterminista automata;
    private List<AristaGrafica> aristas = List.of();

    private NodoGrafico nodoArrastrado;
    private double desfaseArrastreX;
    private double desfaseArrastreY;
    private Point2D puntoPaneo;

    private NodoGrafico nodoActivo;
    private AristaGrafica aristaActiva;
    private NodoGrafico nodoResultado;
    private Boolean resultadoAceptada;

    public LienzoAutomata() {
        setBackground(Paleta.FONDO);
        setPreferredSize(new Dimension(860, 620));
        setFocusable(true);
        instalarInteraccion();
        instalarAtajos();
    }

    public void establecerAutomata(AutomataFinitoDeterminista nuevoAutomata, List<Punto> posiciones) {
        Objects.requireNonNull(nuevoAutomata, "El automata no puede ser nulo");
        Objects.requireNonNull(posiciones, "Las posiciones no pueden ser nulas");
        List<Estado> estados = List.copyOf(nuevoAutomata.estados());
        if (posiciones.size() != estados.size()) {
            throw new IllegalArgumentException("Se esperaban " + estados.size()
                    + " posiciones y se recibieron " + posiciones.size());
        }

        this.automata = nuevoAutomata;
        nodos.clear();
        for (int indice = 0; indice < estados.size(); indice++) {
            Punto punto = posiciones.get(indice);
            nodos.add(new NodoGrafico(estados.get(indice), punto.x(), punto.y()));
        }
        limpiarResaltado();
        recalcularAristas();
        ajustarAVista();
    }

    public void establecerAutomata(AutomataFinitoDeterminista nuevoAutomata) {
        Objects.requireNonNull(nuevoAutomata, "El automata no puede ser nulo");
        double centroX = anchoUtil() / 2.0;
        double centroY = altoUtil() / 2.0;
        double radio = Math.min(centroX, centroY) * 0.6;
        establecerAutomata(nuevoAutomata,
                DistribuidorEstados.circular(
                        nuevoAutomata.estados().size(), centroX, centroY, radio));
    }

    public AutomataFinitoDeterminista automata() {
        return automata;
    }

    public List<NodoGrafico> nodos() {
        return List.copyOf(nodos);
    }

    public List<AristaGrafica> aristas() {
        return aristas;
    }

    public void recalcularAristas() {
        aristas = automata == null ? List.of() : CalculadoraGeometria.calcularAristas(automata, nodos);
        repaint();
    }

    public void resaltar(PasoEjecucion paso) {
        Objects.requireNonNull(paso, "El paso no puede ser nulo");
        nodoActivo = paso.estadoDestino() == null
                ? buscarNodo(paso.estadoOrigen().nombre())
                : buscarNodo(paso.estadoDestino().nombre());
        aristaActiva = paso.estadoDestino() == null
                ? null
                : buscarArista(paso.estadoOrigen().nombre(), paso.estadoDestino().nombre(), paso.simbolo());
        repaint();
    }

    public void marcarResultado(boolean aceptada) {
        resultadoAceptada = aceptada;
        nodoResultado = nodoActivo != null ? nodoActivo : nodoInicial();
        repaint();
    }

    public void limpiarResaltado() {
        nodoActivo = null;
        aristaActiva = null;
        nodoResultado = null;
        resultadoAceptada = null;
        repaint();
    }

    public void acercar() {
        aplicarZoom(FACTOR_ZOOM, anchoUtil() / 2.0, altoUtil() / 2.0);
    }

    public void alejar() {
        aplicarZoom(1 / FACTOR_ZOOM, anchoUtil() / 2.0, altoUtil() / 2.0);
    }

    private void aplicarZoom(double factor, double centroX, double centroY) {
        double escalaResultante = vista.getScaleX() * factor;
        if (escalaResultante < ESCALA_MINIMA || escalaResultante > ESCALA_MAXIMA) {
            return;
        }
        AffineTransform zoom = new AffineTransform();
        zoom.translate(centroX, centroY);
        zoom.scale(factor, factor);
        zoom.translate(-centroX, -centroY);
        vista.preConcatenate(zoom);
        repaint();
    }

    public void restablecerVista() {
        vista.setToIdentity();
        repaint();
    }

    public void exportarPNG(File archivo) throws IOException {
        Objects.requireNonNull(archivo, "El archivo no puede ser nulo");
        int ancho = anchoUtil();
        int alto = altoUtil();

        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = imagen.createGraphics();
        try {
            pintarLienzo(g2, ancho, alto);
        } finally {
            g2.dispose();
        }
        ImageIO.write(imagen, "png", archivo);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            pintarLienzo(g2, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
    }

    public void pintarLienzo(Graphics2D g2, int ancho, int alto) {
        Objects.requireNonNull(g2, "El contexto grafico no puede ser nulo");
        configurarCalidad(g2);

        g2.setColor(Paleta.FONDO);
        g2.fillRect(0, 0, ancho, alto);
        g2.transform(vista);

        pintarAristas(g2);
        pintarNodos(g2);
        pintarEtiquetas(g2);
    }

    private static void configurarCalidad(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private void pintarAristas(Graphics2D g2) {
        for (AristaGrafica arista : aristas) {
            boolean activa = arista == aristaActiva;
            g2.setColor(activa ? Paleta.ACTIVO : Paleta.ARISTA);
            g2.setStroke(new BasicStroke(activa ? GROSOR_ACTIVO : GROSOR_ARISTA,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(arista.forma());
            g2.fill(arista.punta());
        }
    }

    private void pintarNodos(Graphics2D g2) {
        for (NodoGrafico nodo : nodos) {
            boolean activo = nodo == nodoActivo;
            boolean marcado = nodo == nodoResultado && resultadoAceptada != null;
            Color colorBorde = colorDelNodo(nodo, activo, marcado);

            if (activo) {
                g2.setColor(Paleta.ACTIVO);
                g2.setStroke(new BasicStroke(GROSOR_HALO));
                double radioHalo = nodo.radio() + GROSOR_HALO / 2;
                g2.draw(new Ellipse2D.Double(nodo.x() - radioHalo, nodo.y() - radioHalo,
                        radioHalo * 2, radioHalo * 2));
            }

            g2.setColor(Paleta.ESTADO_RELLENO);
            g2.fill(nodo.circulo());

            g2.setColor(colorBorde);
            g2.setStroke(new BasicStroke(marcado ? GROSOR_RESULTADO : GROSOR_NODO));
            g2.draw(nodo.circulo());

            if (nodo.estado().esAceptacion()) {
                g2.setColor(marcado || activo ? colorBorde : Paleta.ACEPTACION);
                g2.setStroke(new BasicStroke(GROSOR_NODO));
                g2.draw(nodo.circuloInterno());
            }

            if (nodo.estado().esInicial()) {
                g2.setColor(Paleta.INICIAL);
                g2.setStroke(new BasicStroke(GROSOR_NODO, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(CalculadoraGeometria.lineaEstadoInicial(nodo));
                g2.fill(CalculadoraGeometria.puntaEstadoInicial(nodo));
            }

            pintarNombre(g2, nodo, activo || marcado ? colorBorde : Paleta.ESTADO_BORDE);
        }
    }

    private Color colorDelNodo(NodoGrafico nodo, boolean activo, boolean marcado) {
        if (marcado) {
            return resultadoAceptada ? Paleta.ACEPTADA : Paleta.RECHAZADA;
        }
        if (activo) {
            return Paleta.ACTIVO;
        }
        return nodo.estado().esAceptacion() ? Paleta.ACEPTACION : Paleta.ESTADO_BORDE;
    }

    private void pintarNombre(Graphics2D g2, NodoGrafico nodo, Color color) {
        String nombre = nodo.nombre();
        g2.setFont(NodoGrafico.FUENTE_BASE);
        FontMetrics metrica = g2.getFontMetrics();
        double anchoTexto = metrica.stringWidth(nombre);
        g2.setColor(color);
        g2.drawString(nombre,
                (float) (nodo.x() - anchoTexto / 2),
                (float) (nodo.y() + metrica.getAscent() / 2.0 - metrica.getDescent() / 2.0));
    }

    private void pintarEtiquetas(Graphics2D g2) {
        g2.setFont(fuenteEtiqueta);
        FontMetrics metrica = g2.getFontMetrics();
        for (AristaGrafica arista : aristas) {
            Rectangle2D.Double marco = rectanguloEtiqueta(arista);

            g2.setColor(Paleta.FONDO);
            g2.fill(marco);

            g2.setColor(arista == aristaActiva ? Paleta.ACTIVO : Paleta.ESTADO_BORDE);
            g2.drawString(arista.etiqueta(),
                    (float) (marco.x + PADDING_ETIQUETA),
                    (float) (marco.y + PADDING_ETIQUETA + metrica.getAscent()));
        }
    }

    private Rectangle2D.Double rectanguloEtiqueta(AristaGrafica arista) {
        double ancho = MetricasTexto.ancho(fuenteEtiqueta, arista.etiqueta());
        double alto = MetricasTexto.alto(fuenteEtiqueta);
        return new Rectangle2D.Double(
                arista.puntoEtiqueta().getX() - ancho / 2 - PADDING_ETIQUETA,
                arista.puntoEtiqueta().getY() - alto / 2 - PADDING_ETIQUETA,
                ancho + PADDING_ETIQUETA * 2,
                alto + PADDING_ETIQUETA * 2);
    }

    public void ajustarAVista() {
        Rectangle2D limites = calcularLimites();
        if (limites == null) {
            return;
        }
        double disponibleAncho = anchoUtil() - MARGEN_VISTA * 2;
        double disponibleAlto = altoUtil() - MARGEN_VISTA * 2;
        if (disponibleAncho <= 0 || disponibleAlto <= 0) {
            return;
        }

        double escala = Math.min(ESCALA_NATURAL, Math.min(
                disponibleAncho / limites.getWidth(),
                disponibleAlto / limites.getHeight()));

        vista.setToIdentity();
        vista.translate(anchoUtil() / 2.0 - escala * limites.getCenterX(),
                altoUtil() / 2.0 - escala * limites.getCenterY());
        vista.scale(escala, escala);
        repaint();
    }

    private Rectangle2D calcularLimites() {
        Rectangle2D limites = null;
        for (NodoGrafico nodo : nodos) {
            limites = unir(limites, nodo.circulo().getBounds2D());
            if (nodo.estado().esInicial()) {
                limites = unir(limites, CalculadoraGeometria.lineaEstadoInicial(nodo).getBounds2D());
            }
        }
        for (AristaGrafica arista : aristas) {
            limites = unir(limites, arista.forma().getBounds2D());
            limites = unir(limites, arista.punta().getBounds2D());
            limites = unir(limites, rectanguloEtiqueta(arista));
        }
        return limites;
    }

    private static Rectangle2D unir(Rectangle2D acumulado, Rectangle2D nuevo) {
        if (acumulado == null) {
            return new Rectangle2D.Double(nuevo.getX(), nuevo.getY(), nuevo.getWidth(), nuevo.getHeight());
        }
        Rectangle2D.union(acumulado, nuevo, acumulado);
        return acumulado;
    }

    private int anchoUtil() {
        return getWidth() > 0 ? getWidth() : getPreferredSize().width;
    }

    private int altoUtil() {
        return getHeight() > 0 ? getHeight() : getPreferredSize().height;
    }

    private void instalarInteraccion() {
        MouseAdapter interaccion = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent evento) {
                Point2D mundo = aCoordenadasDelMundo(evento.getPoint());
                nodoArrastrado = nodoEn(mundo);
                if (nodoArrastrado != null) {
                    desfaseArrastreX = mundo.getX() - nodoArrastrado.x();
                    desfaseArrastreY = mundo.getY() - nodoArrastrado.y();
                    puntoPaneo = null;
                } else {
                    puntoPaneo = evento.getPoint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent evento) {
                if (nodoArrastrado != null) {
                    Point2D mundo = aCoordenadasDelMundo(evento.getPoint());
                    nodoArrastrado.mover(mundo.getX() - desfaseArrastreX, mundo.getY() - desfaseArrastreY);
                    recalcularAristas();
                } else if (puntoPaneo != null) {
                    double dx = evento.getX() - puntoPaneo.getX();
                    double dy = evento.getY() - puntoPaneo.getY();
                    vista.preConcatenate(AffineTransform.getTranslateInstance(dx, dy));
                    puntoPaneo = evento.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent evento) {
                nodoArrastrado = null;
                puntoPaneo = null;
            }

            @Override
            public void mouseMoved(MouseEvent evento) {
                boolean sobreNodo = nodoEn(aCoordenadasDelMundo(evento.getPoint())) != null;
                setCursor(Cursor.getPredefinedCursor(
                        sobreNodo ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent evento) {
                aplicarZoom(evento.getWheelRotation() < 0 ? FACTOR_ZOOM : 1 / FACTOR_ZOOM,
                        evento.getX(), evento.getY());
            }
        };

        addMouseListener(interaccion);
        addMouseMotionListener(interaccion);
        addMouseWheelListener(interaccion);
    }

    private void instalarAtajos() {
        InputMap entradas = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), ACCION_AJUSTAR);
        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.SHIFT_DOWN_MASK), ACCION_AJUSTAR);

        ActionMap acciones = getActionMap();
        acciones.put(ACCION_AJUSTAR, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                if (escribiendoEnTexto()) {
                    return;
                }
                ajustarAVista();
            }
        });
    }

    private static boolean escribiendoEnTexto() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                instanceof JTextComponent;
    }

    private Point2D aCoordenadasDelMundo(Point2D puntoPantalla) {
        try {
            return vista.inverseTransform(puntoPantalla, null);
        } catch (NoninvertibleTransformException excepcion) {
            return puntoPantalla;
        }
    }

    private NodoGrafico nodoEn(Point2D puntoMundo) {
        for (int indice = nodos.size() - 1; indice >= 0; indice--) {
            NodoGrafico nodo = nodos.get(indice);
            if (nodo.contiene(puntoMundo)) {
                return nodo;
            }
        }
        return null;
    }

    private NodoGrafico buscarNodo(String nombre) {
        return nodos.stream().filter(nodo -> nodo.nombre().equals(nombre)).findFirst().orElse(null);
    }

    private AristaGrafica buscarArista(String origen, String destino, char simbolo) {
        return aristas.stream()
                .filter(arista -> arista.conecta(origen, destino) && arista.transporta(simbolo))
                .findFirst()
                .orElse(null);
    }

    private NodoGrafico nodoInicial() {
        return nodos.stream().filter(nodo -> nodo.estado().esInicial()).findFirst().orElse(null);
    }
}

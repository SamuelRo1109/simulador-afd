package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.core.modelo.Estado;
import com.unbosque.afd.core.modelo.PasoEjecucion;
import com.unbosque.afd.core.modelo.Punto;
import com.unbosque.afd.desktop.componentes.CampoTexto;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.controlador.ControladorSimulacion;
import com.unbosque.afd.desktop.controlador.Herramienta;
import com.unbosque.afd.desktop.controlador.Seleccion;
import com.unbosque.afd.desktop.render.AristaGrafica;
import com.unbosque.afd.desktop.render.CalculadoraGeometria;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.MetricasTexto;
import com.unbosque.afd.desktop.render.NodoGrafico;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
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
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LienzoAutomata extends JPanel {

    private static final double ESCALA_MINIMA = 0.3;
    private static final double ESCALA_MAXIMA = 3.0;
    private static final double ESCALA_AJUSTE_MAXIMA = 1.5;
    private static final double FACTOR_ZOOM = 1.1;
    private static final float GROSOR_NODO = 2f;
    private static final float GROSOR_ARISTA = 1.8f;
    private static final float GROSOR_ACTIVO = 3f;
    private static final float GROSOR_HALO = 6f;
    private static final float GROSOR_VEREDICTO = 4f;
    private static final float SEPARACION_VEREDICTO = 5f;
    private static final double RADIO_DECORACION = 16;
    private static final double PLANITUD_LIMITES = 0.5;
    private static final float GROSOR_SELECCION = 2f;
    private static final double OPACIDAD_FANTASMA = 0.40;
    private static final double UMBRAL_RETICULA = 0.5;
    private static final int PADDING_ETIQUETA = 4;
    private static final int MARGEN_VISTA = 56;
    private static final int TOLERANCIA_ARISTA = 8;
    private static final int PERIODO_ANIMACION = 60;
    private static final int ANCHO_EDITOR = 120;
    private static final int ALTO_EDITOR = 28;

    private final ControladorAutomata controlador;
    private final List<NodoGrafico> nodos = new ArrayList<>();
    private final Map<String, NodoGrafico> nodosPorNombre = new LinkedHashMap<>();
    private final AffineTransform vista = new AffineTransform();
    private final Timer animacion;

    private AutomataFinitoDeterminista automata;
    private List<AristaGrafica> aristas = List.of();
    private ControladorSimulacion simulacion;

    private boolean vistaAjustadaPorUsuario;
    private float faseSeleccion;

    private NodoGrafico nodoArrastrado;
    private double desfaseArrastreX;
    private double desfaseArrastreY;
    private Point2D puntoPaneo;
    private Point2D punteroMundo;
    private NodoGrafico origenTransicion;
    private Point2D puntoElastico;
    private boolean espacioPresionado;
    private boolean huboPaneoConEspacio;

    private CampoTexto editorNombre;
    private String nombreEnEdicion;
    private SelectorSimbolos selector;

    private NodoGrafico nodoActivo;
    private AristaGrafica aristaActiva;
    private NodoGrafico nodoResultado;
    private Boolean resultadoAceptada;

    private Runnable alAlternarMatriz;
    private Runnable alAlternarCajon;

    public LienzoAutomata(ControladorAutomata controlador) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        setLayout(null);
        setOpaque(true);
        setPreferredSize(new Dimension(900, 620));
        setFocusable(true);

        animacion = new Timer(PERIODO_ANIMACION, evento -> animar());
        animacion.start();

        instalarInteraccion();
        instalarAtajos();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evento) {
                controlador.establecerTamanoLienzo(getSize());
                if (!vistaAjustadaPorUsuario) {
                    ajustarAVista();
                }
            }
        });

        controlador.agregarObservador(this::sincronizarDesdeControlador);
        controlador.establecerReencuadre(this::reencuadrar);
        sincronizarDesdeControlador();
    }

    public void establecerSimulacion(ControladorSimulacion nuevaSimulacion) {
        this.simulacion = Objects.requireNonNull(nuevaSimulacion, "La simulación no puede ser nula");
        simulacion.agregarObservador(this::sincronizarConSimulacion);
        sincronizarConSimulacion();
    }

    public void establecerAlternarMatriz(Runnable accion) {
        this.alAlternarMatriz = accion;
    }

    public void establecerAlternarCajon(Runnable accion) {
        this.alAlternarCajon = accion;
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

    public boolean vistaAjustadaPorUsuario() {
        return vistaAjustadaPorUsuario;
    }

    public NodoGrafico nodoActivo() {
        return nodoActivo;
    }

    public AristaGrafica aristaActiva() {
        return aristaActiva;
    }

    public NodoGrafico nodoResultado() {
        return nodoResultado;
    }

    public Point puntoDePantalla(String estado) {
        NodoGrafico nodo = nodosPorNombre.get(estado);
        if (nodo == null) {
            return null;
        }
        Point2D pantalla = vista.transform(new Point2D.Double(nodo.x(), nodo.y()), null);
        return new Point((int) Math.round(pantalla.getX()), (int) Math.round(pantalla.getY()));
    }

    public SelectorSimbolos selectorAbierto() {
        return selector;
    }

    public void agregarSuperposicion(JComponent superposicion) {
        add(superposicion, 0);
        revalidate();
        repaint();
    }

    public void sincronizarDesdeControlador() {
        cerrarSelector();
        cerrarEditorNombre(false);

        automata = controlador.automataActual();
        Map<String, Punto> posiciones = controlador.posiciones();

        nodos.clear();
        nodosPorNombre.clear();
        if (automata != null) {
            for (Estado estado : automata.estados()) {
                Punto punto = posiciones.getOrDefault(estado.nombre(), new Punto(0, 0));
                NodoGrafico nodo = new NodoGrafico(estado, punto.x(), punto.y());
                nodos.add(nodo);
                nodosPorNombre.put(estado.nombre(), nodo);
            }
        }
        recalcularAristas();
        actualizarCursor();
        if (!vistaAjustadaPorUsuario) {
            ajustarAVista();
        }
        repaint();
    }

    private void sincronizarConSimulacion() {
        nodoActivo = null;
        aristaActiva = null;
        nodoResultado = null;
        resultadoAceptada = null;

        if (simulacion != null && simulacion.hayResultado()) {
            Estado actual = simulacion.estadoActual();
            nodoActivo = actual == null ? null : nodosPorNombre.get(actual.nombre());

            PasoEjecucion paso = simulacion.pasoActual();
            if (paso != null && paso.estadoDestino() != null) {
                aristaActiva = buscarArista(paso.estadoOrigen().nombre(),
                        paso.estadoDestino().nombre(), paso.simbolo());
            }
            if (simulacion.hayVeredicto()) {
                resultadoAceptada = simulacion.resultado().aceptada();
                nodoResultado = nodoActivo;
            }
        }
        repaint();
    }

    public void recalcularAristas() {
        aristas = automata == null ? List.of() : CalculadoraGeometria.calcularAristas(automata, nodos);
        repaint();
    }

    public void acercar() {
        aplicarZoom(FACTOR_ZOOM, anchoUtil() / 2.0, altoUtil() / 2.0);
    }

    public void alejar() {
        aplicarZoom(1 / FACTOR_ZOOM, anchoUtil() / 2.0, altoUtil() / 2.0);
    }

    public void reencuadrar() {
        vistaAjustadaPorUsuario = false;
        ajustarAVista();
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
        vistaAjustadaPorUsuario = true;
        reubicarSuperposiciones();
        repaint();
    }

    public void ajustarAVista() {
        Rectangle2D limites = calcularLimites();
        if (limites == null || limites.getWidth() <= 0 || limites.getHeight() <= 0) {
            vista.setToIdentity();
            repaint();
            return;
        }
        double disponibleAncho = anchoUtil() - MARGEN_VISTA * 2.0;
        double disponibleAlto = altoUtil() - MARGEN_VISTA * 2.0;
        if (disponibleAncho <= 0 || disponibleAlto <= 0) {
            return;
        }

        double escala = Math.min(ESCALA_AJUSTE_MAXIMA, Math.min(
                disponibleAncho / limites.getWidth(),
                disponibleAlto / limites.getHeight()));
        escala = Math.max(ESCALA_MINIMA, Math.min(ESCALA_MAXIMA, escala));

        vista.setToIdentity();
        vista.translate(anchoUtil() / 2.0 - escala * limites.getCenterX(),
                altoUtil() / 2.0 - escala * limites.getCenterY());
        vista.scale(escala, escala);
        reubicarSuperposiciones();
        repaint();
    }

    public void exportarPNG(File archivo, double escala, boolean forzarPapel) throws IOException {
        Objects.requireNonNull(archivo, "El archivo no puede ser nulo");
        int ancho = anchoUtil();
        int alto = altoUtil();
        Tema tema = forzarPapel ? Tema.PAPEL : Tema.actual();

        BufferedImage imagen = new BufferedImage(
                (int) Math.round(ancho * escala), (int) Math.round(alto * escala), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = imagen.createGraphics();
        try {
            g2.scale(escala, escala);
            pintarLienzo(g2, ancho, alto, tema);
        } finally {
            g2.dispose();
        }
        ImageIO.write(imagen, "png", archivo);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            pintarLienzo(g2, getWidth(), getHeight(), Tema.actual());
            pintarAyudasDeEdicion(g2, Tema.actual());
            pintarRotuloPaso(g2);
        } finally {
            g2.dispose();
        }
    }

    public void pintarLienzo(Graphics2D g2, int ancho, int alto, Tema tema) {
        Objects.requireNonNull(g2, "El contexto gráfico no puede ser nulo");
        Medidas.calidad(g2);

        g2.setColor(tema.lienzoFondo());
        g2.fillRect(0, 0, ancho, alto);
        pintarReticula(g2, ancho, alto, tema);

        Graphics2D mundo = (Graphics2D) g2.create();
        try {
            mundo.transform(vista);
            pintarAristas(mundo, tema);
            pintarNodos(mundo, tema);
            pintarEtiquetas(mundo, tema);
        } finally {
            mundo.dispose();
        }
    }

    private void pintarAyudasDeEdicion(Graphics2D g2, Tema tema) {
        Graphics2D mundo = (Graphics2D) g2.create();
        try {
            Medidas.calidad(mundo);
            mundo.transform(vista);
            pintarLineaElastica(mundo);
            pintarSeleccion(mundo);
            pintarFantasma(mundo, tema);
        } finally {
            mundo.dispose();
        }
    }

    private void pintarReticula(Graphics2D g2, int ancho, int alto, Tema tema) {
        double escala = vista.getScaleX();
        if (escala < UMBRAL_RETICULA) {
            return;
        }
        double paso = Medidas.PASO_RETICULA;
        double desplazamientoX = vista.getTranslateX() % (paso * escala);
        double desplazamientoY = vista.getTranslateY() % (paso * escala);

        g2.setColor(tema.lienzoReticula());
        for (double x = desplazamientoX; x < ancho; x += paso * escala) {
            for (double y = desplazamientoY; y < alto; y += paso * escala) {
                g2.fillRect((int) Math.round(x), (int) Math.round(y), 1, 1);
            }
        }
    }

    private void pintarAristas(Graphics2D g2, Tema tema) {
        for (AristaGrafica arista : aristas) {
            boolean activa = arista == aristaActiva;
            boolean seleccionada = controlador.seleccion()
                    .esArista(arista.nodoOrigen().nombre(), arista.nodoDestino().nombre());
            g2.setColor(activa ? Tema.ACTIVO : seleccionada ? Tema.SELECCION : tema.arista());
            g2.setStroke(new BasicStroke(activa ? GROSOR_ACTIVO : GROSOR_ARISTA,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(arista.forma());
            g2.fill(arista.punta());
        }
    }

    private void pintarLineaElastica(Graphics2D g2) {
        if (origenTransicion == null || puntoElastico == null) {
            return;
        }
        g2.setColor(Tema.ACTIVO);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[] {7f, 6f}, faseSeleccion));
        g2.draw(new Line2D.Double(origenTransicion.x(), origenTransicion.y(),
                puntoElastico.getX(), puntoElastico.getY()));
        g2.fill(new Ellipse2D.Double(puntoElastico.getX() - 3, puntoElastico.getY() - 3, 6, 6));
    }

    private void pintarNodos(Graphics2D g2, Tema tema) {
        for (NodoGrafico nodo : nodos) {
            boolean activo = nodo == nodoActivo;
            boolean marcado = nodo == nodoResultado && resultadoAceptada != null;

            // El halo naranja cede su sitio al anillo de veredicto: dos aros concéntricos
            // alrededor del mismo nodo se leen como ruido.
            if (activo && !marcado) {
                g2.setColor(Tema.conAlfa(Tema.ACTIVO, 0.45));
                g2.setStroke(new BasicStroke(GROSOR_HALO));
                g2.draw(anillo(nodo, nodo.radio() + GROSOR_HALO / 2));
            }

            g2.setColor(tema.estadoRelleno());
            g2.fill(nodo.circulo());

            g2.setColor(activo ? Tema.ACTIVO : colorPropio(nodo, tema));
            g2.setStroke(new BasicStroke(GROSOR_NODO));
            g2.draw(nodo.circulo());

            if (marcado) {
                g2.setColor(resultadoAceptada ? Tema.ACEPTADA : Tema.RECHAZADA);
                g2.setStroke(new BasicStroke(GROSOR_VEREDICTO));
                g2.draw(anillo(nodo, radioVeredicto(nodo)));
            }

            if (nodo.estado().esAceptacion()) {
                g2.setColor(Tema.ACEPTACION);
                g2.setStroke(new BasicStroke(GROSOR_NODO));
                g2.draw(nodo.circuloInterno());
            }

            if (nodo.estado().esInicial()) {
                double desplazamiento = desplazamientoFlechaInicial(nodo);
                g2.setColor(Tema.INICIAL);
                g2.setStroke(new BasicStroke(GROSOR_NODO, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(CalculadoraGeometria.lineaEstadoInicial(nodo, desplazamiento));
                g2.fill(CalculadoraGeometria.puntaEstadoInicial(nodo, desplazamiento));
            }

            pintarNombre(g2, nodo, activo ? Tema.ACTIVO : tema.estadoBorde());
        }
    }

    private static double radioVeredicto(NodoGrafico nodo) {
        return nodo.radio() + SEPARACION_VEREDICTO + GROSOR_VEREDICTO / 2;
    }

    private double desplazamientoFlechaInicial(NodoGrafico nodo) {
        boolean marcado = nodo == nodoResultado && resultadoAceptada != null;
        return marcado
                ? radioVeredicto(nodo) + GROSOR_VEREDICTO / 2 + SEPARACION_VEREDICTO - nodo.radio()
                : 0;
    }

    private static Ellipse2D.Double anillo(NodoGrafico nodo, double radio) {
        return new Ellipse2D.Double(nodo.x() - radio, nodo.y() - radio, radio * 2, radio * 2);
    }

    private static Color colorPropio(NodoGrafico nodo, Tema tema) {
        return nodo.estado().esAceptacion() ? Tema.ACEPTACION : tema.estadoBorde();
    }

    private void pintarNombre(Graphics2D g2, NodoGrafico nodo, Color color) {
        if (nodo.nombre().equals(nombreEnEdicion)) {
            return;
        }
        g2.setFont(NodoGrafico.FUENTE_BASE);
        FontMetrics metrica = g2.getFontMetrics();
        double anchoTexto = metrica.stringWidth(nodo.nombre());
        g2.setColor(color);
        g2.drawString(nodo.nombre(),
                (float) (nodo.x() - anchoTexto / 2),
                (float) (nodo.y() + metrica.getAscent() / 2.0 - metrica.getDescent() / 2.0));
    }

    private void pintarEtiquetas(Graphics2D g2, Tema tema) {
        for (AristaGrafica arista : aristas) {
            boolean activa = arista == aristaActiva;
            Font fuente = activa ? TipografiaApp.MONO_FUERTE : TipografiaApp.MONO;
            g2.setFont(fuente);
            FontMetrics metrica = g2.getFontMetrics();
            Rectangle2D.Double marco = rectanguloEtiqueta(arista, fuente);

            g2.setColor(activa
                    ? Tema.mezclar(Tema.ACTIVO, tema.lienzoFondo(), 0.22)
                    : tema.lienzoFondo());
            g2.fill(new RoundRectangle2D.Double(marco.x, marco.y, marco.width, marco.height, 5, 5));

            g2.setColor(activa ? Tema.ACTIVO : tema.textoSecundario());
            g2.drawString(arista.etiqueta(),
                    (float) (marco.x + PADDING_ETIQUETA),
                    (float) (marco.y + PADDING_ETIQUETA + metrica.getAscent()));
        }
    }

    private void pintarSeleccion(Graphics2D g2) {
        Seleccion seleccion = controlador.seleccion();
        if (seleccion.tipo() != Seleccion.Tipo.ESTADO) {
            return;
        }
        NodoGrafico nodo = nodosPorNombre.get(seleccion.estado());
        if (nodo == null) {
            return;
        }
        boolean conVeredicto = nodo == nodoResultado && resultadoAceptada != null;
        double radio = conVeredicto
                ? radioVeredicto(nodo) + GROSOR_VEREDICTO
                : nodo.radio() + 6;
        g2.setColor(Tema.SELECCION);
        g2.setStroke(new BasicStroke(GROSOR_SELECCION, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[] {6f, 5f}, faseSeleccion));
        g2.draw(anillo(nodo, radio));
    }

    private void pintarFantasma(Graphics2D g2, Tema tema) {
        if (punteroMundo == null || !controlador.herramienta().insertaEstados()
                || nodoEn(punteroMundo) != null) {
            return;
        }
        double radio = NodoGrafico.RADIO_MINIMO;
        Ellipse2D.Double circulo = new Ellipse2D.Double(
                punteroMundo.getX() - radio, punteroMundo.getY() - radio, radio * 2, radio * 2);

        g2.setColor(Tema.conAlfa(tema.estadoRelleno(), OPACIDAD_FANTASMA));
        g2.fill(circulo);
        g2.setStroke(new BasicStroke(GROSOR_NODO));
        g2.setColor(Tema.conAlfa(tema.estadoBorde(), OPACIDAD_FANTASMA));
        g2.draw(circulo);

        if (controlador.herramienta() == Herramienta.ACEPTACION) {
            double interno = radio - NodoGrafico.SEPARACION_ACEPTACION;
            g2.setColor(Tema.conAlfa(Tema.ACEPTACION, OPACIDAD_FANTASMA));
            g2.draw(new Ellipse2D.Double(punteroMundo.getX() - interno, punteroMundo.getY() - interno,
                    interno * 2, interno * 2));
        }

        g2.setFont(NodoGrafico.FUENTE_BASE);
        FontMetrics metrica = g2.getFontMetrics();
        String nombre = controlador.modelo().siguienteNombreLibre();
        g2.setColor(Tema.conAlfa(tema.estadoBorde(), OPACIDAD_FANTASMA));
        g2.drawString(nombre,
                (float) (punteroMundo.getX() - metrica.stringWidth(nombre) / 2.0),
                (float) (punteroMundo.getY() + metrica.getAscent() / 2.0 - metrica.getDescent() / 2.0));
    }

    private void pintarRotuloPaso(Graphics2D g2) {
        if (simulacion == null || !simulacion.hayResultado()) {
            return;
        }
        String texto = simulacion.descripcionPaso();
        if (texto.isEmpty()) {
            return;
        }
        Tema tema = Tema.actual();
        g2.setFont(TipografiaApp.CUERPO);
        FontMetrics metrica = g2.getFontMetrics();
        int ancho = metrica.stringWidth(texto) + Medidas.paso(6);
        int alto = metrica.getHeight() + Medidas.paso(3);
        int x = Medidas.paso(4);
        int y = Medidas.paso(4);

        g2.setColor(Tema.conAlfa(tema.panelElevado(), 0.92));
        g2.fillRoundRect(x, y, ancho, alto, Medidas.RADIO_TARJETA, Medidas.RADIO_TARJETA);
        g2.setColor(Tema.mezclar(Tema.ACTIVO, tema.panelBorde(), 0.5));
        g2.drawRoundRect(x, y, ancho, alto, Medidas.RADIO_TARJETA, Medidas.RADIO_TARJETA);

        g2.setColor(tema.textoPrimario());
        g2.drawString(texto, x + Medidas.paso(3), y + alto / 2f + metrica.getAscent() / 2f - 2);
    }

    private Rectangle2D.Double rectanguloEtiqueta(AristaGrafica arista, Font fuente) {
        double ancho = MetricasTexto.ancho(fuente, arista.etiqueta());
        double alto = MetricasTexto.alto(fuente);
        return new Rectangle2D.Double(
                arista.puntoEtiqueta().getX() - ancho / 2 - PADDING_ETIQUETA,
                arista.puntoEtiqueta().getY() - alto / 2 - PADDING_ETIQUETA,
                ancho + PADDING_ETIQUETA * 2,
                alto + PADDING_ETIQUETA * 2);
    }

    private Rectangle2D calcularLimites() {
        Rectangle2D limites = null;
        for (NodoGrafico nodo : nodos) {
            limites = unir(limites, anillo(nodo, nodo.radio() + RADIO_DECORACION).getBounds2D());
            if (nodo.estado().esInicial()) {
                limites = unir(limites, CalculadoraGeometria
                        .lineaEstadoInicial(nodo, desplazamientoFlechaInicial(nodo)).getBounds2D());
            }
        }
        for (AristaGrafica arista : aristas) {
            limites = unir(limites, limitesAjustados(arista.forma()));
            limites = unir(limites, limitesAjustados(arista.punta()));
            // El marco de la etiqueta lleva relleno invisible: para encuadrar solo cuenta
            // la extensión real del texto.
            limites = unir(limites, sinRelleno(rectanguloEtiqueta(arista, TipografiaApp.MONO)));
        }
        return limites;
    }

    private static Rectangle2D sinRelleno(Rectangle2D marco) {
        return new Rectangle2D.Double(
                marco.getX() + PADDING_ETIQUETA, marco.getY() + PADDING_ETIQUETA,
                Math.max(0, marco.getWidth() - PADDING_ETIQUETA * 2),
                Math.max(0, marco.getHeight() - PADDING_ETIQUETA * 2));
    }

    /**
     * getBounds2D() de una curva devuelve la caja de sus puntos de control, mucho mayor que el
     * trazo real; aplanar la trayectoria da los límites que de verdad se ven.
     */
    private static Rectangle2D limitesAjustados(Shape forma) {
        PathIterator iterador = forma.getPathIterator(null, PLANITUD_LIMITES);
        double[] coordenadas = new double[6];
        Rectangle2D limites = null;
        while (!iterador.isDone()) {
            if (iterador.currentSegment(coordenadas) != PathIterator.SEG_CLOSE) {
                limites = unir(limites,
                        new Rectangle2D.Double(coordenadas[0], coordenadas[1], 0, 0));
            }
            iterador.next();
        }
        return limites == null ? forma.getBounds2D() : limites;
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

    private void animar() {
        faseSeleccion = (faseSeleccion + 1.2f) % 22f;
        boolean necesitaAnimacion = controlador.seleccion().tipo() == Seleccion.Tipo.ESTADO
                || origenTransicion != null;
        if (necesitaAnimacion) {
            repaint();
        }
    }

    private void instalarInteraccion() {
        MouseAdapter interaccion = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent evento) {
                requestFocusInWindow();
                cerrarEditorNombre(true);
                Point2D mundo = aCoordenadasDelMundo(evento.getPoint());
                punteroMundo = mundo;

                if (espacioPresionado || controlador.herramienta() == Herramienta.MANO) {
                    puntoPaneo = evento.getPoint();
                    return;
                }
                atender(mundo, evento);
            }

            @Override
            public void mouseDragged(MouseEvent evento) {
                Point2D mundo = aCoordenadasDelMundo(evento.getPoint());
                punteroMundo = mundo;

                if (puntoPaneo != null) {
                    double dx = evento.getX() - puntoPaneo.getX();
                    double dy = evento.getY() - puntoPaneo.getY();
                    vista.preConcatenate(AffineTransform.getTranslateInstance(dx, dy));
                    puntoPaneo = evento.getPoint();
                    vistaAjustadaPorUsuario = true;
                    huboPaneoConEspacio = espacioPresionado;
                    reubicarSuperposiciones();
                    repaint();
                } else if (nodoArrastrado != null) {
                    nodoArrastrado.mover(mundo.getX() - desfaseArrastreX, mundo.getY() - desfaseArrastreY);
                    controlador.actualizarPosicion(nodoArrastrado.nombre(),
                            nodoArrastrado.x(), nodoArrastrado.y());
                    recalcularAristas();
                } else if (origenTransicion != null) {
                    puntoElastico = mundo;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent evento) {
                Point2D mundo = aCoordenadasDelMundo(evento.getPoint());

                if (nodoArrastrado != null) {
                    controlador.finalizarMovimiento(nodoArrastrado.nombre());
                }
                if (origenTransicion != null) {
                    NodoGrafico destino = nodoEn(mundo);
                    NodoGrafico origen = origenTransicion;
                    origenTransicion = null;
                    puntoElastico = null;
                    if (destino != null) {
                        abrirSelector(origen, destino);
                    }
                    repaint();
                }
                nodoArrastrado = null;
                puntoPaneo = null;
            }

            @Override
            public void mouseMoved(MouseEvent evento) {
                punteroMundo = aCoordenadasDelMundo(evento.getPoint());
                actualizarCursor();
                if (controlador.herramienta().insertaEstados()) {
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent evento) {
                punteroMundo = null;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent evento) {
                if (evento.getClickCount() == 2
                        && controlador.herramienta() == Herramienta.SELECCIONAR) {
                    NodoGrafico nodo = nodoEn(aCoordenadasDelMundo(evento.getPoint()));
                    if (nodo != null) {
                        abrirEditorNombre(nodo);
                    }
                }
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

    private void atender(Point2D mundo, MouseEvent evento) {
        NodoGrafico nodo = nodoEn(mundo);
        AristaGrafica arista = nodo == null ? aristaEn(mundo) : null;

        switch (controlador.herramienta()) {
            case SELECCIONAR -> {
                if (nodo != null) {
                    controlador.seleccionar(Seleccion.deEstado(nodo.nombre()));
                    nodoArrastrado = nodo;
                    desfaseArrastreX = mundo.getX() - nodo.x();
                    desfaseArrastreY = mundo.getY() - nodo.y();
                    controlador.iniciarMovimiento();
                } else if (arista != null) {
                    controlador.seleccionar(Seleccion.deArista(
                            arista.nodoOrigen().nombre(), arista.nodoDestino().nombre()));
                } else {
                    controlador.seleccionar(Seleccion.NINGUNA);
                    puntoPaneo = evento.getPoint();
                }
            }
            case ESTADO -> {
                if (nodo == null) {
                    insertarYEditar(mundo, false);
                } else {
                    controlador.seleccionar(Seleccion.deEstado(nodo.nombre()));
                }
            }
            case ACEPTACION -> {
                if (nodo == null) {
                    insertarYEditar(mundo, true);
                } else {
                    controlador.alternarAceptacion(nodo.nombre());
                }
            }
            case INICIAL -> {
                if (nodo != null) {
                    controlador.marcarInicial(nodo.nombre());
                }
            }
            case TRANSICION -> {
                if (nodo != null) {
                    origenTransicion = nodo;
                    puntoElastico = mundo;
                }
            }
            case BORRAR -> {
                if (nodo != null) {
                    eliminarConConfirmacion(nodo.nombre());
                } else if (arista != null) {
                    controlador.eliminarArista(arista.nodoOrigen().nombre(), arista.nodoDestino().nombre());
                }
            }
            case MANO -> puntoPaneo = evento.getPoint();
            default -> {
                // Todas las herramientas quedan cubiertas por las ramas anteriores.
            }
        }
    }

    private void insertarYEditar(Point2D mundo, boolean aceptacion) {
        String nombre = controlador.insertarEstado(mundo.getX(), mundo.getY(), aceptacion);
        NodoGrafico nodo = nodosPorNombre.get(nombre);
        if (nodo != null) {
            abrirEditorNombre(nodo);
        }
    }

    private void eliminarConConfirmacion(String estado) {
        if (controlador.modelo().gradoDe(estado) > 2) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "El estado " + estado + " tiene " + controlador.modelo().gradoDe(estado)
                            + " transiciones asociadas.\nSe eliminarán junto con el estado. ¿Continuar?",
                    "Eliminar estado", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (respuesta != JOptionPane.OK_OPTION) {
                return;
            }
        }
        controlador.eliminarEstado(estado);
    }

    private void abrirEditorNombre(NodoGrafico nodo) {
        cerrarEditorNombre(false);
        nombreEnEdicion = nodo.nombre();

        editorNombre = new CampoTexto("nombre", 8);
        editorNombre.setText(nodo.nombre());
        editorNombre.selectAll();
        editorNombre.addActionListener(evento -> cerrarEditorNombre(true));
        editorNombre.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent evento) {
                cerrarEditorNombre(true);
            }
        });
        editorNombre.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelarEdicion");
        editorNombre.getActionMap().put("cancelarEdicion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                cerrarEditorNombre(false);
            }
        });

        add(editorNombre, 0);
        ubicarEditor(nodo);
        editorNombre.requestFocusInWindow();
        repaint();
    }

    private void ubicarEditor(NodoGrafico nodo) {
        Point2D pantalla = vista.transform(new Point2D.Double(nodo.x(), nodo.y()), null);
        editorNombre.setBounds(
                (int) Math.round(pantalla.getX() - ANCHO_EDITOR / 2.0),
                (int) Math.round(pantalla.getY() - ALTO_EDITOR / 2.0),
                ANCHO_EDITOR, ALTO_EDITOR);
    }

    private void cerrarEditorNombre(boolean confirmar) {
        if (editorNombre == null) {
            return;
        }
        CampoTexto campo = editorNombre;
        String anterior = nombreEnEdicion;
        editorNombre = null;
        nombreEnEdicion = null;

        remove(campo);
        repaint();

        if (confirmar && anterior != null) {
            String propuesto = campo.getText().trim();
            if (!propuesto.equals(anterior) && !controlador.renombrarEstado(anterior, propuesto)) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
        requestFocusInWindow();
    }

    private void abrirSelector(NodoGrafico origen, NodoGrafico destino) {
        cerrarSelector();
        selector = new SelectorSimbolos(controlador, origen.nombre(), destino.nombre(),
                simbolos -> aplicarSimbolos(origen.nombre(), destino.nombre(), simbolos),
                this::cerrarSelector);

        Dimension preferido = selector.getPreferredSize();
        Point2D centro = vista.transform(new Point2D.Double(
                (origen.x() + destino.x()) / 2, (origen.y() + destino.y()) / 2), null);

        double escala = vista.getScaleX();
        double abajo = Math.max(
                vista.transform(new Point2D.Double(origen.x(), origen.y()), null).getY()
                        + origen.radio() * escala,
                vista.transform(new Point2D.Double(destino.x(), destino.y()), null).getY()
                        + destino.radio() * escala);
        double arriba = Math.min(
                vista.transform(new Point2D.Double(origen.x(), origen.y()), null).getY()
                        - origen.radio() * escala,
                vista.transform(new Point2D.Double(destino.x(), destino.y()), null).getY()
                        - destino.radio() * escala);

        int x = (int) Math.round(centro.getX() - preferido.width / 2.0);
        int y = (int) Math.round(abajo + Medidas.paso(4));
        if (y + preferido.height > getHeight() - Medidas.PASO) {
            y = (int) Math.round(arriba - Medidas.paso(4) - preferido.height);
        }
        x = Math.max(Medidas.PASO, Math.min(x, getWidth() - preferido.width - Medidas.PASO));
        y = Math.max(Medidas.PASO, Math.min(y, getHeight() - preferido.height - Medidas.PASO));

        selector.setBounds(x, y, preferido.width, preferido.height);
        add(selector, 0);
        revalidate();
        repaint();
    }

    private void aplicarSimbolos(String origen, String destino, Set<Character> simbolos) {
        cerrarSelector();
        controlador.establecerSimbolosArista(origen, destino, simbolos);
    }

    private void cerrarSelector() {
        if (selector == null) {
            return;
        }
        remove(selector);
        selector = null;
        revalidate();
        repaint();
    }

    private void reubicarSuperposiciones() {
        if (editorNombre != null && nombreEnEdicion != null) {
            NodoGrafico nodo = nodosPorNombre.get(nombreEnEdicion);
            if (nodo != null) {
                ubicarEditor(nodo);
            }
        }
    }

    private void actualizarCursor() {
        Herramienta herramienta = controlador.herramienta();
        int cursor;
        if (espacioPresionado || herramienta == Herramienta.MANO) {
            cursor = Cursor.HAND_CURSOR;
        } else if (herramienta == Herramienta.BORRAR) {
            cursor = Cursor.CROSSHAIR_CURSOR;
        } else if (herramienta.insertaEstados() || herramienta == Herramienta.TRANSICION
                || herramienta == Herramienta.INICIAL) {
            cursor = Cursor.CROSSHAIR_CURSOR;
        } else if (punteroMundo != null && nodoEn(punteroMundo) != null) {
            cursor = Cursor.MOVE_CURSOR;
        } else {
            cursor = Cursor.DEFAULT_CURSOR;
        }
        setCursor(Cursor.getPredefinedCursor(cursor));
    }

    private void instalarAtajos() {
        InputMap entradas = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap acciones = getActionMap();

        for (Herramienta herramienta : Herramienta.values()) {
            String clave = "herramienta" + herramienta.name();
            entradas.put(KeyStroke.getKeyStroke(herramienta.atajo()), clave);
            acciones.put(clave, accion(() -> controlador.establecerHerramienta(herramienta)));
        }

        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "ajustar");
        acciones.put("ajustar", accion(this::reencuadrar));

        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "matriz");
        acciones.put("matriz", accion(() -> {
            if (alAlternarMatriz != null) {
                alAlternarMatriz.run();
            }
        }));

        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "borrarSeleccion");
        acciones.put("borrarSeleccion", accion(this::borrarSeleccion));

        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMaskEx()), "deshacer");
        acciones.put("deshacer", accion(controlador::deshacer));

        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK), "rehacer");
        acciones.put("rehacer", accion(controlador::rehacer));

        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), "espacioAbajo");
        acciones.put("espacioAbajo", accion(() -> {
            if (!espacioPresionado) {
                espacioPresionado = true;
                huboPaneoConEspacio = false;
                actualizarCursor();
            }
        }));

        entradas.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true), "espacioArriba");
        acciones.put("espacioArriba", accion(() -> {
            espacioPresionado = false;
            actualizarCursor();
            if (!huboPaneoConEspacio && alAlternarCajon != null) {
                alAlternarCajon.run();
            }
            huboPaneoConEspacio = false;
        }));
    }

    private void borrarSeleccion() {
        Seleccion seleccion = controlador.seleccion();
        if (seleccion.tipo() == Seleccion.Tipo.ESTADO) {
            eliminarConConfirmacion(seleccion.estado());
        } else if (seleccion.tipo() == Seleccion.Tipo.ARISTA) {
            controlador.eliminarArista(seleccion.origen(), seleccion.destino());
        }
    }

    private static AbstractAction accion(Runnable cuerpo) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                if (escribiendoEnTexto()) {
                    return;
                }
                cuerpo.run();
            }
        };
    }

    private static boolean escribiendoEnTexto() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
                instanceof JTextComponent;
    }

    private Point2D aCoordenadasDelMundo(Point puntoPantalla) {
        try {
            return vista.inverseTransform(puntoPantalla, null);
        } catch (NoninvertibleTransformException excepcion) {
            return new Point2D.Double(puntoPantalla.getX(), puntoPantalla.getY());
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

    private AristaGrafica aristaEn(Point2D puntoMundo) {
        double tolerancia = TOLERANCIA_ARISTA / Math.max(0.2, vista.getScaleX());
        BasicStroke engrosado = new BasicStroke((float) (tolerancia * 2));
        for (AristaGrafica arista : aristas) {
            if (engrosado.createStrokedShape(arista.forma()).contains(puntoMundo)
                    || arista.punta().contains(puntoMundo)
                    || rectanguloEtiqueta(arista, TipografiaApp.MONO).contains(puntoMundo)) {
                return arista;
            }
        }
        return null;
    }

    private AristaGrafica buscarArista(String origen, String destino, char simbolo) {
        return aristas.stream()
                .filter(arista -> arista.conecta(origen, destino) && arista.transporta(simbolo))
                .findFirst()
                .orElse(null);
    }
}

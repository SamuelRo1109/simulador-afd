package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.componentes.BotonAccion;
import com.unbosque.afd.desktop.componentes.BotonIcono;
import com.unbosque.afd.desktop.componentes.CampoTexto;
import com.unbosque.afd.desktop.componentes.DeslizadorVelocidad;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.controlador.ControladorSimulacion;
import com.unbosque.afd.desktop.render.IconosApp;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

public class CajonEjecucion extends JPanel implements Tema.Sensible {

    private static final int ALTO_ABIERTO = Medidas.ALTO_CAJON;
    private static final int ALTO_CERRADO = 28;
    private static final int ALTO_BANDA = 26;

    private final ControladorAutomata controlador;
    private final ControladorSimulacion simulacion;
    private final CintaCadena cinta;

    private final CampoTexto campoCadena = new CampoTexto("cadena a validar", 18);
    private final BotonAccion botonValidar = new BotonAccion("VALIDAR", BotonAccion.Estilo.PRIMARIO);
    private final JLabel avisoCadena = new JLabel(" ");
    private final JLabel encabezado = new JLabel("CAJÓN DE EJECUCIÓN");
    private final DeslizadorVelocidad deslizador =
            new DeslizadorVelocidad(ControladorSimulacion.VELOCIDAD_POR_DEFECTO);

    private final BotonIcono botonInicio = new BotonIcono(IconosApp::inicio, "Ir al inicio", 30, 18);
    private final BotonIcono botonAnterior = new BotonIcono(IconosApp::anterior, "Paso anterior", 30, 18);
    private final BotonIcono botonReproducir =
            new BotonIcono(IconosApp::reproducir, "Reproducir automáticamente", 30, 18);
    private final BotonIcono botonSiguiente = new BotonIcono(IconosApp::siguiente, "Paso siguiente", 30, 18);
    private final BotonIcono botonFin = new BotonIcono(IconosApp::fin, "Ir al final", 30, 18);
    private final BotonIcono botonReiniciar = new BotonIcono(IconosApp::reiniciar, "Reiniciar", 30, 18);
    private final BotonIcono botonCopiar = new BotonIcono(IconosApp::copiar, "Copiar la traza", 30, 18);
    private final BotonIcono botonExportarTraza =
            new BotonIcono(IconosApp::exportar, "Exportar la traza a .txt", 30, 18);

    private final BandaResultado banda = new BandaResultado();
    private final JPanel contenido = new JPanel(new BorderLayout(0, Medidas.paso(1)));

    private boolean colapsado;

    public CajonEjecucion(ControladorAutomata controlador, ControladorSimulacion simulacion) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        this.simulacion = Objects.requireNonNull(simulacion, "La simulación no puede ser nula");
        this.cinta = new CintaCadena(simulacion);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(
                Medidas.paso(1), Medidas.paso(4), Medidas.paso(2), Medidas.paso(4)));

        add(construirEncabezado(), BorderLayout.NORTH);

        contenido.setOpaque(false);
        contenido.add(construirFilaEntrada(), BorderLayout.NORTH);
        contenido.add(construirFilaTransporte(), BorderLayout.CENTER);
        contenido.add(banda, BorderLayout.SOUTH);
        add(contenido, BorderLayout.CENTER);

        instalarFiltroDeAlfabeto();
        botonValidar.addActionListener(evento -> validar());
        campoCadena.addActionListener(evento -> validar());

        botonInicio.addActionListener(evento -> simulacion.inicio());
        botonAnterior.addActionListener(evento -> simulacion.anterior());
        botonReproducir.addActionListener(evento -> simulacion.alternarReproduccion());
        botonSiguiente.addActionListener(evento -> simulacion.siguiente());
        botonFin.addActionListener(evento -> simulacion.fin());
        botonReiniciar.addActionListener(evento -> simulacion.reiniciar());
        botonCopiar.addActionListener(evento -> copiarTraza());
        botonExportarTraza.addActionListener(evento -> exportarTraza());
        deslizador.addChangeListener(evento -> simulacion.establecerVelocidad(deslizador.getValue()));

        controlador.agregarObservador(this::actualizar);
        simulacion.agregarObservador(this::actualizar);
        aplicarTema();
        actualizar();
    }

    public void alternarColapso() {
        colapsado = !colapsado;
        contenido.setVisible(!colapsado);
        revalidate();
        repaint();
        if (getParent() != null) {
            getParent().revalidate();
        }
    }

    public boolean colapsado() {
        return colapsado;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, altoVigente());
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, altoVigente());
    }

    private int altoVigente() {
        if (colapsado) {
            return ALTO_CERRADO;
        }
        return banda.isVisible() ? ALTO_ABIERTO + ALTO_BANDA + Medidas.PASO : ALTO_ABIERTO;
    }

    @Override
    public void aplicarTema() {
        Tema tema = Tema.actual();
        encabezado.setForeground(tema.textoSecundario());
        avisoCadena.setForeground(Tema.ADVERTENCIA);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            Tema tema = Tema.actual();
            g2.setColor(tema.panelFondo());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(tema.panelBorde());
            g2.drawLine(0, 0, getWidth(), 0);
            g2.setColor(tema.luzSuperior());
            g2.drawLine(0, 1, getWidth(), 1);
        } finally {
            g2.dispose();
        }
    }

    private JComponent construirEncabezado() {
        JPanel barra = new JPanel();
        barra.setOpaque(false);
        barra.setLayout(new BoxLayout(barra, BoxLayout.X_AXIS));
        barra.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        barra.setToolTipText("Contraer o desplegar el cajón (Espacio)");
        barra.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                alternarColapso();
            }
        });

        encabezado.setFont(TipografiaApp.ETIQUETA_FUERTE);

        barra.add(encabezado);
        barra.add(Box.createHorizontalGlue());
        return barra;
    }

    private JComponent construirFilaEntrada() {
        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));

        JLabel etiqueta = new JLabel("Cadena  ");
        etiqueta.setFont(TipografiaApp.ETIQUETA_FUERTE);
        etiqueta.setForeground(Tema.actual().textoSecundario());

        campoCadena.setMaximumSize(new Dimension(260, 32));
        avisoCadena.setFont(TipografiaApp.ETIQUETA);

        fila.add(etiqueta);
        fila.add(campoCadena);
        fila.add(Box.createHorizontalStrut(Medidas.paso(2)));
        fila.add(botonValidar);
        fila.add(Box.createHorizontalStrut(Medidas.paso(3)));
        fila.add(avisoCadena);
        fila.add(Box.createHorizontalGlue());
        return fila;
    }

    private JComponent construirFilaTransporte() {
        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));

        cinta.setPreferredSize(new Dimension(320, 62));

        JPanel transporte = new JPanel();
        transporte.setOpaque(false);
        transporte.setLayout(new BoxLayout(transporte, BoxLayout.X_AXIS));
        transporte.add(botonInicio);
        transporte.add(botonAnterior);
        transporte.add(botonReproducir);
        transporte.add(botonSiguiente);
        transporte.add(botonFin);
        transporte.add(Box.createHorizontalStrut(Medidas.paso(2)));
        transporte.add(botonReiniciar);
        transporte.add(Box.createHorizontalStrut(Medidas.paso(3)));
        transporte.add(deslizador);
        transporte.add(Box.createHorizontalStrut(Medidas.paso(3)));
        transporte.add(botonCopiar);
        transporte.add(botonExportarTraza);

        fila.add(cinta);
        fila.add(Box.createHorizontalGlue());
        fila.add(transporte);
        return fila;
    }

    private void validar() {
        if (!controlador.esValido()) {
            return;
        }
        simulacion.simular(campoCadena.getText());
    }

    private void instalarFiltroDeAlfabeto() {
        ((AbstractDocument) campoCadena.getDocument()).setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(FilterBypass bypass, int desplazamiento, String texto,
                                     AttributeSet atributos) throws BadLocationException {
                bypass.insertString(desplazamiento, depurar(texto), atributos);
            }

            @Override
            public void replace(FilterBypass bypass, int desplazamiento, int longitud, String texto,
                                AttributeSet atributos) throws BadLocationException {
                bypass.replace(desplazamiento, longitud, depurar(texto), atributos);
            }

            @Override
            public void remove(FilterBypass bypass, int desplazamiento, int longitud)
                    throws BadLocationException {
                anunciarDescarte(false);
                bypass.remove(desplazamiento, longitud);
            }
        });

        campoCadena.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent evento) {
                alEditarCadena();
            }

            @Override
            public void removeUpdate(DocumentEvent evento) {
                alEditarCadena();
            }

            @Override
            public void changedUpdate(DocumentEvent evento) {
                alEditarCadena();
            }
        });
    }

    private String depurar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        StringBuilder aceptado = new StringBuilder(texto.length());
        boolean huboDescarte = false;
        for (char simbolo : texto.toCharArray()) {
            if (controlador.modelo().contieneSimbolo(simbolo)) {
                aceptado.append(simbolo);
            } else {
                huboDescarte = true;
            }
        }
        anunciarDescarte(huboDescarte);
        return aceptado.toString();
    }

    private void anunciarDescarte(boolean huboDescarte) {
        avisoCadena.setText(huboDescarte ? "Solo se aceptan símbolos de Σ" : " ");
        campoCadena.establecerAviso(huboDescarte ? Tema.ADVERTENCIA : null);
    }

    private void alEditarCadena() {
        simulacion.cancelar();
        actualizar();
    }

    private void copiarTraza() {
        String traza = simulacion.traza();
        if (traza.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(traza), null);
        avisoCadena.setText("Traza copiada al portapapeles");
    }

    private void exportarTraza() {
        String traza = simulacion.traza();
        if (traza.isEmpty()) {
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Exportar la traza de simulación");
        selector.setSelectedFile(new File("traza.txt"));
        selector.setFileFilter(new FileNameExtensionFilter("Texto plano", "txt"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".txt")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".txt");
        }
        try {
            Files.writeString(destino.toPath(), traza, StandardCharsets.UTF_8);
            avisoCadena.setText("Traza exportada");
        } catch (IOException excepcion) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar la traza:\n" + excepcion.getMessage(),
                    "Exportar traza", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void actualizar() {
        boolean valido = controlador.esValido();
        boolean hayResultado = simulacion.hayResultado();

        campoCadena.setEnabled(valido);
        botonValidar.setEnabled(valido);
        deslizador.setEnabled(valido);

        botonInicio.setEnabled(hayResultado && simulacion.puedeRetroceder());
        botonAnterior.setEnabled(hayResultado && simulacion.puedeRetroceder());
        botonSiguiente.setEnabled(hayResultado && simulacion.puedeAvanzar());
        botonFin.setEnabled(hayResultado && simulacion.puedeAvanzar());
        botonReproducir.setEnabled(hayResultado && simulacion.totalPasos() > 0);
        botonReiniciar.setEnabled(hayResultado);
        botonCopiar.setEnabled(hayResultado);
        botonExportarTraza.setEnabled(hayResultado);

        botonReproducir.establecerPintor(simulacion.reproduciendo()
                ? IconosApp::pausar : IconosApp::reproducir);
        botonReproducir.setToolTipText(simulacion.reproduciendo() ? "Pausar" : "Reproducir automáticamente");

        if (!valido) {
            encabezado.setText("CAJÓN DE EJECUCIÓN — el AFD no es válido, revisa la pestaña Validación");
            avisoCadena.setText(" ");
        } else {
            encabezado.setText("CAJÓN DE EJECUCIÓN");
        }

        banda.actualizar();
        cinta.revalidate();
        cinta.repaint();
        repaint();
    }

    private final class BandaResultado extends JComponent implements Tema.Sensible {

        private String mensaje = "";
        private Color color = Tema.ACEPTADA;
        private boolean visible;

        @Override
        public void aplicarTema() {
            repaint();
        }

        private void actualizar() {
            visible = simulacion.hayResultado() && simulacion.hayVeredicto();
            if (visible) {
                mensaje = simulacion.mensajeVeredicto();
                color = simulacion.resultado().aceptada() ? Tema.ACEPTADA : Tema.RECHAZADA;
            }
            setVisible(visible);
            CajonEjecucion.this.revalidate();
            if (CajonEjecucion.this.getParent() != null) {
                CajonEjecucion.this.getParent().revalidate();
            }
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, visible ? ALTO_BANDA : 0);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, visible ? ALTO_BANDA : 0);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!visible) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Medidas.calidad(g2);
                Tema tema = Tema.actual();
                g2.setColor(Tema.mezclar(color, tema.panelFondo(), 0.20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),
                        Medidas.RADIO_BOTON, Medidas.RADIO_BOTON);
                g2.setColor(Tema.mezclar(color, tema.panelFondo(), 0.55));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                        Medidas.RADIO_BOTON, Medidas.RADIO_BOTON);

                g2.setFont(TipografiaApp.CUERPO_FUERTE);
                FontMetrics metrica = g2.getFontMetrics();
                g2.setColor(tema.esOscuro()
                        ? Tema.mezclar(color, tema.textoPrimario(), 0.35) : color.darker());
                g2.drawString(mensaje, Medidas.paso(3),
                        (getHeight() + metrica.getAscent() - metrica.getDescent()) / 2f);
            } finally {
                g2.dispose();
            }
        }
    }
}

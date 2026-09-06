package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.componentes.BotonAccion;
import com.unbosque.afd.desktop.componentes.BotonIcono;
import com.unbosque.afd.desktop.componentes.CampoTexto;
import com.unbosque.afd.desktop.componentes.ChipEstadoValidacion;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.render.IconosApp;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.Objects;

public class BarraSuperior extends JPanel implements Tema.Sensible {

    private static final String MARCA = "PLANO SINTÁCTICO";
    private static final String LEMA = "Simulador de AFD";

    private final ControladorAutomata controlador;
    private final ChipEstadoValidacion chip;
    private final JLabel etiquetaNombre = new JLabel();
    private final CampoTexto campoNombre = new CampoTexto("nombre del autómata", 18);
    private final JPanel nombre = new JPanel(new CardLayout());
    private final BotonIcono botonTema;

    public BarraSuperior(ControladorAutomata controlador,
                         Runnable alAbrirValidacion,
                         Runnable alAlternarTema,
                         Runnable alExportar,
                         JComponent menu) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        this.chip = new ChipEstadoValidacion(alAbrirValidacion);

        setLayout(new BorderLayout(Medidas.paso(4), 0));
        setBorder(BorderFactory.createEmptyBorder(0, Medidas.paso(4), 0, Medidas.paso(4)));
        setOpaque(false);

        botonTema = new BotonIcono(IconosApp::tema, "Conmutar tema Grafito / Papel", 34, 20);
        botonTema.addActionListener(evento -> alAlternarTema.run());

        BotonAccion exportar = new BotonAccion("Exportar", BotonAccion.Estilo.SECUNDARIO);
        exportar.addActionListener(evento -> alExportar.run());

        add(construirIzquierda(menu), BorderLayout.WEST);
        add(construirCentro(), BorderLayout.CENTER);
        add(construirDerecha(exportar), BorderLayout.EAST);

        controlador.agregarObservador(this::refrescar);
        aplicarTema();
        refrescar();
    }

    private JComponent construirIzquierda(JComponent menu) {
        JPanel izquierda = new JPanel();
        izquierda.setOpaque(false);
        izquierda.setLayout(new BoxLayout(izquierda, BoxLayout.X_AXIS));
        izquierda.add(new Wordmark());
        izquierda.add(Box.createHorizontalStrut(Medidas.paso(6)));
        if (menu != null) {
            izquierda.add(menu);
        }
        return izquierda;
    }

    private JComponent construirCentro() {
        etiquetaNombre.setFont(TipografiaApp.TITULO);
        etiquetaNombre.setHorizontalAlignment(JLabel.CENTER);
        etiquetaNombre.setToolTipText("Doble clic para renombrar el autómata");
        etiquetaNombre.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        etiquetaNombre.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                if (evento.getClickCount() == 2) {
                    editarNombre();
                }
            }
        });

        campoNombre.addActionListener(evento -> confirmarNombre());
        campoNombre.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent evento) {
                confirmarNombre();
            }
        });

        nombre.setOpaque(false);
        nombre.add(centrar(etiquetaNombre), "etiqueta");
        nombre.add(centrar(campoNombre), "campo");
        return nombre;
    }

    private static JComponent centrar(JComponent contenido) {
        JPanel envoltura = new JPanel();
        envoltura.setOpaque(false);
        envoltura.setLayout(new BoxLayout(envoltura, BoxLayout.Y_AXIS));
        contenido.setAlignmentX(CENTER_ALIGNMENT);
        envoltura.add(Box.createVerticalGlue());
        envoltura.add(contenido);
        envoltura.add(Box.createVerticalGlue());
        return envoltura;
    }

    private JComponent construirDerecha(BotonAccion exportar) {
        JPanel derecha = new JPanel();
        derecha.setOpaque(false);
        derecha.setLayout(new BoxLayout(derecha, BoxLayout.X_AXIS));
        derecha.add(Box.createVerticalStrut(Medidas.ALTO_BARRA_SUPERIOR));
        derecha.add(chip);
        derecha.add(Box.createHorizontalStrut(Medidas.paso(3)));
        derecha.add(botonTema);
        derecha.add(Box.createHorizontalStrut(Medidas.paso(2)));
        derecha.add(exportar);
        return derecha;
    }

    private void editarNombre() {
        campoNombre.setText(controlador.modelo().nombre());
        ((CardLayout) nombre.getLayout()).show(nombre, "campo");
        campoNombre.requestFocusInWindow();
        campoNombre.selectAll();
    }

    private void confirmarNombre() {
        ((CardLayout) nombre.getLayout()).show(nombre, "etiqueta");
        controlador.establecerNombre(campoNombre.getText());
    }

    public void refrescar() {
        etiquetaNombre.setText(controlador.modelo().nombre());
        chip.actualizar(controlador.validacionActual(), controlador.modelo().estados().isEmpty());
        botonTema.setToolTipText("Tema actual: " + Tema.actual().nombre() + " — clic para conmutar");
        revalidate();
        repaint();
    }

    @Override
    public void aplicarTema() {
        etiquetaNombre.setForeground(Tema.actual().textoPrimario());
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, Medidas.ALTO_BARRA_SUPERIOR);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, Medidas.ALTO_BARRA_SUPERIOR);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, Medidas.ALTO_BARRA_SUPERIOR);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema tema = Tema.actual();
            g2.setColor(tema.panelFondo());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(tema.panelBorde());
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        } finally {
            g2.dispose();
        }
    }

    private static final class Wordmark extends JComponent implements Tema.Sensible {

        private static final Font FUENTE_MARCA = TipografiaApp.MARCA.deriveFont(
                Map.of(TextAttribute.TRACKING, 0.18));

        @Override
        public void aplicarTema() {
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrica = getFontMetrics(FUENTE_MARCA);
            return new Dimension(metrica.stringWidth(MARCA) + Medidas.paso(2),
                    Medidas.ALTO_BARRA_SUPERIOR);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Medidas.calidad(g2);
                Tema tema = Tema.actual();

                g2.setFont(FUENTE_MARCA);
                g2.setColor(tema.textoPrimario());
                g2.drawString(MARCA, 0, 24);

                g2.setFont(TipografiaApp.MARCA_SECUNDARIA);
                g2.setColor(tema.textoSecundario());
                g2.drawString(LEMA, 1, 38);

                g2.setColor(Tema.ACTIVO);
                g2.fillRect(0, 43, 22, 2);
            } finally {
                g2.dispose();
            }
        }
    }
}

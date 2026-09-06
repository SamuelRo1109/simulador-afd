package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.componentes.BotonHerramienta;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.controlador.Herramienta;
import com.unbosque.afd.desktop.render.IconosApp;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.PintorIcono;
import com.unbosque.afd.desktop.render.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class RielHerramientas extends JPanel implements Tema.Sensible {

    private final ControladorAutomata controlador;
    private final Map<Herramienta, BotonHerramienta> botones = new EnumMap<>(Herramienta.class);

    public RielHerramientas(ControladorAutomata controlador, LienzoAutomata lienzo) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        Objects.requireNonNull(lienzo, "El lienzo no puede ser nulo");

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(Medidas.paso(2), 0, Medidas.paso(2), 0));
        setOpaque(false);

        for (Herramienta herramienta : Herramienta.values()) {
            BotonHerramienta boton = new BotonHerramienta(
                    pintorDe(herramienta), herramienta.descripcion(), String.valueOf(herramienta.atajo()));
            boton.setAlignmentX(CENTER_ALIGNMENT);
            boton.addActionListener(evento -> controlador.establecerHerramienta(herramienta));
            botones.put(herramienta, boton);
            add(boton);
            add(Box.createVerticalStrut(Medidas.PASO));
        }

        add(Box.createVerticalStrut(Medidas.paso(2)));
        add(new Separador());
        add(Box.createVerticalStrut(Medidas.paso(2)));

        add(accion(IconosApp::ajustar, "Ajustar a la vista", "F", lienzo::reencuadrar));
        add(Box.createVerticalStrut(Medidas.PASO));
        add(accion(IconosApp::acercar, "Acercar", "+", lienzo::acercar));
        add(Box.createVerticalStrut(Medidas.PASO));
        add(accion(IconosApp::alejar, "Alejar", "−", lienzo::alejar));
        add(Box.createVerticalStrut(Medidas.PASO));
        add(accion(IconosApp::reorganizar, "Reorganizar en círculo", "R",
                controlador::reorganizarEnCirculo));

        add(Box.createVerticalGlue());

        controlador.agregarObservador(this::refrescar);
        refrescar();
    }

    private BotonHerramienta accion(PintorIcono pintor, String descripcion, String atajo, Runnable cuerpo) {
        BotonHerramienta boton = new BotonHerramienta(pintor, descripcion, atajo);
        boton.setAlignmentX(CENTER_ALIGNMENT);
        boton.addActionListener(evento -> cuerpo.run());
        return boton;
    }

    private static PintorIcono pintorDe(Herramienta herramienta) {
        return switch (herramienta) {
            case SELECCIONAR -> IconosApp::seleccionar;
            case ESTADO -> IconosApp::estado;
            case ACEPTACION -> IconosApp::aceptacion;
            case INICIAL -> IconosApp::inicial;
            case TRANSICION -> IconosApp::transicion;
            case BORRAR -> IconosApp::borrar;
            case MANO -> IconosApp::mano;
        };
    }

    public void refrescar() {
        for (Map.Entry<Herramienta, BotonHerramienta> entrada : botones.entrySet()) {
            entrada.getValue().establecerActivo(entrada.getKey() == controlador.herramienta());
        }
        repaint();
    }

    @Override
    public void aplicarTema() {
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Medidas.ANCHO_RIEL, 0);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(Medidas.ANCHO_RIEL, 0);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Medidas.ANCHO_RIEL, Integer.MAX_VALUE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Tema tema = Tema.actual();
            g2.setColor(tema.panelFondo());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(tema.panelBorde());
            g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        } finally {
            g2.dispose();
        }
    }

    private static final class Separador extends JPanel implements Tema.Sensible {

        private Separador() {
            setOpaque(false);
            setAlignmentX(CENTER_ALIGNMENT);
        }

        @Override
        public void aplicarTema() {
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Medidas.ANCHO_RIEL, 1);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Medidas.ANCHO_RIEL, 1);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Tema.actual().panelBorde());
            g.drawLine(Medidas.paso(3), 0, getWidth() - Medidas.paso(3), 0);
        }
    }
}

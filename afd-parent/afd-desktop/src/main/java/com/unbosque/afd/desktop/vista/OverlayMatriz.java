package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.componentes.BotonIcono;
import com.unbosque.afd.desktop.componentes.PanelTarjeta;
import com.unbosque.afd.desktop.render.IconosApp;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class OverlayMatriz extends PanelTarjeta {

    private static final int ANCHO = 340;
    private static final int ALTO = 236;
    private static final int ALTO_MINIMIZADO = 44;
    private static final int MARGEN = Medidas.paso(4);

    private final PanelMatrizTransiciones matriz;
    private final JPanel encabezado;
    private final BotonIcono botonMinimizar;

    private boolean minimizado;
    private Point desfaseArrastre;

    public OverlayMatriz(PanelMatrizTransiciones matriz, Runnable alCerrar) {
        this.matriz = Objects.requireNonNull(matriz, "La matriz no puede ser nula");
        setLayout(new BorderLayout(0, Medidas.paso(1)));
        setBorder(BorderFactory.createEmptyBorder(
                Medidas.paso(2), Medidas.paso(3), Medidas.paso(3), Medidas.paso(3)));
        setSize(ANCHO, ALTO);

        botonMinimizar = new BotonIcono(IconosApp::minimizar, "Minimizar la matriz", 24, 16);
        botonMinimizar.addActionListener(evento -> alternarMinimizado());

        BotonIcono botonCerrar = new BotonIcono(IconosApp::cerrar, "Ocultar la matriz (M)", 24, 16);
        botonCerrar.addActionListener(evento -> {
            if (alCerrar != null) {
                alCerrar.run();
            }
        });

        encabezado = construirEncabezado(botonCerrar);
        add(encabezado, BorderLayout.NORTH);
        add(matriz, BorderLayout.CENTER);

        instalarArrastre();
    }

    private JPanel construirEncabezado(BotonIcono botonCerrar) {
        JPanel barra = new JPanel();
        barra.setOpaque(false);
        barra.setLayout(new BoxLayout(barra, BoxLayout.X_AXIS));
        barra.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        JLabel titulo = new JLabel("MATRIZ DE TRANSICIONES  δ");
        titulo.setFont(TipografiaApp.ETIQUETA_FUERTE);
        titulo.setForeground(Tema.actual().textoSecundario());

        barra.add(titulo);
        barra.add(Box.createHorizontalGlue());
        barra.add(botonMinimizar);
        barra.add(Box.createHorizontalStrut(Medidas.PASO));
        barra.add(botonCerrar);
        return barra;
    }

    private void instalarArrastre() {
        MouseAdapter arrastre = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evento) {
                desfaseArrastre = evento.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent evento) {
                desfaseArrastre = null;
            }

            @Override
            public void mouseDragged(MouseEvent evento) {
                if (desfaseArrastre == null) {
                    return;
                }
                Container padre = getParent();
                if (padre == null) {
                    return;
                }
                int x = getX() + evento.getX() - desfaseArrastre.x;
                int y = getY() + evento.getY() - desfaseArrastre.y;
                x = Math.max(0, Math.min(x, padre.getWidth() - getWidth()));
                y = Math.max(0, Math.min(y, padre.getHeight() - getHeight()));
                setLocation(x, y);
            }
        };
        encabezado.addMouseListener(arrastre);
        encabezado.addMouseMotionListener(arrastre);
    }

    public void alternarMinimizado() {
        minimizado = !minimizado;
        matriz.setVisible(!minimizado);
        botonMinimizar.establecerPintor(minimizado ? IconosApp::matriz : IconosApp::minimizar);
        botonMinimizar.setToolTipText(minimizado ? "Restaurar la matriz" : "Minimizar la matriz");
        setSize(ANCHO, minimizado ? ALTO_MINIMIZADO : ALTO);
        acomodarEnPadre();
        revalidate();
        repaint();
    }

    public void acomodarEnPadre() {
        Container padre = getParent();
        if (padre == null || padre.getWidth() == 0) {
            return;
        }
        int x = Math.min(getX(), padre.getWidth() - getWidth() - MARGEN);
        int y = Math.min(getY(), padre.getHeight() - getHeight() - MARGEN);
        setLocation(Math.max(MARGEN, x), Math.max(MARGEN, y));
    }

    public void ubicarEnEsquina() {
        Container padre = getParent();
        if (padre == null || padre.getWidth() == 0) {
            return;
        }
        setLocation(Math.max(MARGEN, padre.getWidth() - getWidth() - MARGEN),
                Math.max(MARGEN, padre.getHeight() - getHeight() - MARGEN));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ANCHO, minimizado ? ALTO_MINIMIZADO : ALTO);
    }
}

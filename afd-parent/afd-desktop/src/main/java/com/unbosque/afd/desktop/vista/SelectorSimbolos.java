package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.componentes.BotonAccion;
import com.unbosque.afd.desktop.componentes.ChipAlternable;
import com.unbosque.afd.desktop.componentes.PanelTarjeta;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.render.Medidas;
import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class SelectorSimbolos extends PanelTarjeta {

    private static final int ANCHO_MAXIMO = 300;

    private final ControladorAutomata controlador;
    private final String origen;
    private final String destino;
    private final Consumer<Set<Character>> alAplicar;
    private final Runnable alCancelar;
    private final List<ChipAlternable> chips = new ArrayList<>();
    private final JLabel aviso = new JLabel(" ");

    public SelectorSimbolos(ControladorAutomata controlador,
                            String origen,
                            String destino,
                            Consumer<Set<Character>> alAplicar,
                            Runnable alCancelar) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        this.origen = origen;
        this.destino = destino;
        this.alAplicar = alAplicar;
        this.alCancelar = alCancelar;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(
                Medidas.paso(3), Medidas.paso(3), Medidas.paso(3), Medidas.paso(3)));

        add(construirTitulo());
        add(Box.createVerticalStrut(Medidas.paso(2)));
        add(construirChips());
        add(Box.createVerticalStrut(Medidas.paso(1)));

        aviso.setFont(TipografiaApp.ETIQUETA);
        aviso.setForeground(Tema.ADVERTENCIA);
        aviso.setAlignmentX(LEFT_ALIGNMENT);
        add(aviso);
        add(Box.createVerticalStrut(Medidas.paso(2)));
        add(construirPie());

        actualizarConflictos();
    }

    public void marcar(char simbolo, boolean valor) {
        for (ChipAlternable chip : chips) {
            if (chip.simbolo() == simbolo) {
                chip.establecerMarcado(valor);
            }
        }
        actualizarConflictos();
    }

    public boolean hayConflictos() {
        return !controlador.conflictosDeterminismo(origen, destino, seleccionados()).isEmpty();
    }

    public String avisoActual() {
        return aviso.getText();
    }

    public void aplicarSeleccion() {
        confirmar();
    }

    private JLabel construirTitulo() {
        String flecha = origen.equals(destino)
                ? "Bucle en " + origen
                : origen + "  →  " + destino;
        JLabel titulo = new JLabel("δ(" + origen + ", ·) = " + destino);
        titulo.setToolTipText(flecha);
        titulo.setFont(TipografiaApp.CUERPO_FUERTE);
        titulo.setForeground(Tema.actual().textoPrimario());
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Medidas.paso(3)));
        return titulo;
    }

    private JPanel construirChips() {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, Medidas.paso(2), Medidas.paso(1)));
        fila.setOpaque(false);
        fila.setAlignmentX(LEFT_ALIGNMENT);

        Set<Character> actuales = new LinkedHashSet<>(controlador.modelo().simbolosEntre(origen, destino));
        if (controlador.modelo().simbolos().isEmpty()) {
            JLabel vacio = new JLabel("Σ está vacío: agrega símbolos en el inspector");
            vacio.setFont(TipografiaApp.ETIQUETA);
            vacio.setForeground(Tema.actual().textoSecundario());
            fila.add(vacio);
            return fila;
        }

        for (char simbolo : controlador.modelo().simbolos()) {
            ChipAlternable chip = new ChipAlternable(simbolo, actuales.contains(simbolo), this::actualizarConflictos);
            chips.add(chip);
            fila.add(chip);
        }
        fila.setMaximumSize(new Dimension(ANCHO_MAXIMO, Integer.MAX_VALUE));
        return fila;
    }

    private JPanel construirPie() {
        JPanel pie = new JPanel();
        pie.setOpaque(false);
        pie.setLayout(new BoxLayout(pie, BoxLayout.X_AXIS));
        pie.setAlignmentX(LEFT_ALIGNMENT);

        BotonAccion cancelar = new BotonAccion("Cancelar", BotonAccion.Estilo.SECUNDARIO);
        cancelar.addActionListener(evento -> {
            if (alCancelar != null) {
                alCancelar.run();
            }
        });

        BotonAccion aplicar = new BotonAccion("Aplicar", BotonAccion.Estilo.PRIMARIO);
        aplicar.addActionListener(evento -> confirmar());

        pie.add(cancelar);
        pie.add(Box.createHorizontalGlue());
        pie.add(aplicar);
        return pie;
    }

    private void confirmar() {
        Set<Character> elegidos = seleccionados();
        List<ControladorAutomata.ConflictoDeterminismo> conflictos =
                controlador.conflictosDeterminismo(origen, destino, elegidos);

        if (!conflictos.isEmpty() && !confirmarReemplazo(conflictos)) {
            if (alCancelar != null) {
                alCancelar.run();
            }
            return;
        }
        if (alAplicar != null) {
            alAplicar.accept(elegidos);
        }
    }

    private boolean confirmarReemplazo(List<ControladorAutomata.ConflictoDeterminismo> conflictos) {
        StringBuilder detalle = new StringBuilder();
        for (ControladorAutomata.ConflictoDeterminismo conflicto : conflictos) {
            detalle.append("δ(").append(origen).append(", ").append(conflicto.simbolo())
                    .append(") ya va a ").append(conflicto.destinoActual()).append('\n');
        }
        Object[] opciones = {"Reemplazar", "Cancelar"};
        int respuesta = JOptionPane.showOptionDialog(this,
                detalle + "\nDefinir un segundo destino para el mismo par rompería el determinismo.\n"
                        + "¿Reemplazar el destino anterior?",
                "Conflicto de determinismo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, opciones, opciones[0]);
        return respuesta == 0;
    }

    private void actualizarConflictos() {
        List<ControladorAutomata.ConflictoDeterminismo> conflictos =
                controlador.conflictosDeterminismo(origen, destino, seleccionados());

        for (ChipAlternable chip : chips) {
            ControladorAutomata.ConflictoDeterminismo conflicto = conflictos.stream()
                    .filter(candidato -> candidato.simbolo() == chip.simbolo())
                    .findFirst()
                    .orElse(null);
            chip.establecerConflicto(conflicto != null, conflicto == null
                    ? "Disparar esta transición con '" + chip.simbolo() + "'"
                    : "δ(" + origen + ", " + chip.simbolo() + ") ya va a " + conflicto.destinoActual());
        }

        if (conflictos.isEmpty()) {
            aviso.setText(" ");
        } else {
            ControladorAutomata.ConflictoDeterminismo primero = conflictos.get(0);
            aviso.setText("δ(" + origen + "," + primero.simbolo() + ") ya va a " + primero.destinoActual()
                    + (conflictos.size() > 1 ? " (+" + (conflictos.size() - 1) + ")" : ""));
        }
        revalidate();
        repaint();
    }

    public Set<Character> seleccionados() {
        Set<Character> elegidos = new LinkedHashSet<>();
        for (ChipAlternable chip : chips) {
            if (chip.marcado()) {
                elegidos.add(chip.simbolo());
            }
        }
        return elegidos;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Medidas.calidad(g2);
            g2.setColor(Tema.conAlfa(Tema.actual().lienzoFondo(), 0.55));
            g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 4,
                    Medidas.RADIO_TARJETA, Medidas.RADIO_TARJETA);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

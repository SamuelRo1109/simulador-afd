package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.render.Paleta;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.Objects;

public class PanelAlfabeto extends JPanel {

    private final ControladorAutomata controlador;
    private final JTextField campoSimbolo = new JTextField(2);
    private final JPanel contenedorSimbolos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final JLabel mensaje = new JLabel(" ");

    public PanelAlfabeto(ControladorAutomata controlador) {
        this.controlador = Objects.requireNonNull(controlador, "El controlador no puede ser nulo");
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Alfabeto (Σ)"));
        setOpaque(false);

        limitarAUnCaracter();
        add(construirEntrada(), BorderLayout.NORTH);

        contenedorSimbolos.setOpaque(false);
        add(contenedorSimbolos, BorderLayout.CENTER);

        mensaje.setForeground(Paleta.RECHAZADA);
        mensaje.setFont(mensaje.getFont().deriveFont(Font.PLAIN, 11f));
        add(mensaje, BorderLayout.SOUTH);
    }

    public void refrescar() {
        contenedorSimbolos.removeAll();
        for (char simbolo : controlador.modelo().simbolos()) {
            contenedorSimbolos.add(construirChip(simbolo));
        }
        if (controlador.modelo().simbolos().isEmpty()) {
            JLabel vacio = new JLabel("Sin simbolos");
            vacio.setForeground(Paleta.TEXTO_TENUE);
            contenedorSimbolos.add(vacio);
        }
        contenedorSimbolos.revalidate();
        contenedorSimbolos.repaint();
    }

    private JPanel construirEntrada() {
        JPanel entrada = new JPanel();
        entrada.setOpaque(false);
        entrada.setLayout(new BoxLayout(entrada, BoxLayout.X_AXIS));

        campoSimbolo.setMaximumSize(new Dimension(48, 26));
        campoSimbolo.addActionListener(evento -> agregar());

        JButton agregar = new JButton("Agregar");
        agregar.addActionListener(evento -> agregar());

        entrada.add(new JLabel("Simbolo: "));
        entrada.add(campoSimbolo);
        entrada.add(Box.createHorizontalStrut(6));
        entrada.add(agregar);
        entrada.add(Box.createHorizontalGlue());
        return entrada;
    }

    private JPanel construirChip(char simbolo) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        chip.setBackground(Paleta.ESTADO_RELLENO);
        chip.setBorder(BorderFactory.createLineBorder(Paleta.BORDE_SUAVE));

        JLabel etiqueta = new JLabel(String.valueOf(simbolo));
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 13f));
        etiqueta.setForeground(Paleta.ESTADO_BORDE);

        JButton quitar = new JButton("×");
        quitar.setToolTipText("Quitar el simbolo '" + simbolo + "' y sus transiciones");
        quitar.setFont(quitar.getFont().deriveFont(Font.BOLD, 14f));
        quitar.setMargin(new Insets(0, 0, 0, 0));
        quitar.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        quitar.setContentAreaFilled(false);
        quitar.setFocusPainted(false);
        quitar.setFocusable(false);
        quitar.setForeground(Paleta.RECHAZADA);
        quitar.addActionListener(evento -> controlador.quitarSimbolo(simbolo));

        chip.add(etiqueta);
        chip.add(quitar);
        return chip;
    }

    private void agregar() {
        String texto = campoSimbolo.getText();
        if (controlador.agregarSimbolo(texto)) {
            mensaje.setText(" ");
        } else {
            mensaje.setText(texto.isBlank() ? "Escribe un simbolo de un caracter" : "El simbolo ya esta en Σ");
        }
        campoSimbolo.setText("");
        campoSimbolo.requestFocusInWindow();
    }

    private void limitarAUnCaracter() {
        ((AbstractDocument) campoSimbolo.getDocument()).setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(FilterBypass bypass, int desplazamiento, String texto, AttributeSet atributos)
                    throws BadLocationException {
                reemplazar(bypass, desplazamiento, 0, texto, atributos);
            }

            @Override
            public void replace(FilterBypass bypass, int desplazamiento, int longitud, String texto,
                                AttributeSet atributos) throws BadLocationException {
                reemplazar(bypass, desplazamiento, longitud, texto, atributos);
            }

            private void reemplazar(FilterBypass bypass, int desplazamiento, int longitud, String texto,
                                    AttributeSet atributos) throws BadLocationException {
                String limpio = texto == null ? "" : texto.trim();
                if (limpio.isEmpty()) {
                    return;
                }
                String candidato = limpio.substring(0, 1);
                bypass.replace(0, bypass.getDocument().getLength(), candidato, atributos);
            }
        });
    }
}

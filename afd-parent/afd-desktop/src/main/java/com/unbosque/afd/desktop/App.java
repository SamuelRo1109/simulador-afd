package com.unbosque.afd.desktop;

import com.unbosque.afd.desktop.vista.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            aplicarLookAndFeelDelSistema();
            new VentanaPrincipal().setVisible(true);
        });
    }

    private static void aplicarLookAndFeelDelSistema() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedOperationException
                 | javax.swing.UnsupportedLookAndFeelException excepcion) {
            System.err.println("No se pudo aplicar el look and feel del sistema: " + excepcion.getMessage());
        }
    }
}

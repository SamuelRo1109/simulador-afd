package com.unbosque.afd.desktop;

import com.unbosque.afd.desktop.render.Tema;
import com.unbosque.afd.desktop.render.TipografiaApp;
import com.unbosque.afd.desktop.vista.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Toolkit;
import java.util.Map;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            prepararInterfaz();
            new VentanaPrincipal().setVisible(true);
        });
    }

    public static void prepararInterfaz() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedOperationException
                 | javax.swing.UnsupportedLookAndFeelException excepcion) {
            System.err.println("No se pudo aplicar el look and feel base: " + excepcion.getMessage());
        }

        alinearMetricasDeTexto();

        FontUIResource cuerpo = new FontUIResource(TipografiaApp.CUERPO);
        for (String clave : new String[] {"Label.font", "Button.font", "MenuItem.font", "Menu.font",
                "CheckBox.font", "TextField.font", "ComboBox.font", "Table.font",
                "TableHeader.font", "OptionPane.messageFont", "OptionPane.buttonFont", "ToolTip.font"}) {
            UIManager.put(clave, cuerpo);
        }

        Tema tema = Tema.actual();
        UIManager.put("ToolTip.background", tema.panelElevado());
        UIManager.put("ToolTip.foreground", tema.textoPrimario());
        UIManager.put("Panel.background", tema.panelFondo());
        UIManager.put("OptionPane.background", tema.panelFondo());
        UIManager.put("OptionPane.messageForeground", tema.textoPrimario());
    }

    private static void alinearMetricasDeTexto() {
        Object pistas = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (pistas instanceof Map<?, ?> mapa) {
            mapa.forEach((clave, valor) -> UIManager.put(clave, valor));
        }
    }
}

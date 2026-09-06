package com.unbosque.afd.desktop.vista;

import com.unbosque.afd.core.logica.ConstructorEjemplos;
import com.unbosque.afd.core.modelo.AutomataFinitoDeterminista;
import com.unbosque.afd.desktop.controlador.ControladorAutomata;
import com.unbosque.afd.desktop.render.Paleta;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class VentanaPrincipal extends JFrame {

    private static final String TITULO = "Simulador de Autómatas Finitos Deterministas";
    private static final int ANCHO_CONFIGURACION = 380;
    private static final int ANCHO_MINIMO_CONFIGURACION = 300;
    private static final int ANCHO_MINIMO_LIENZO = 420;

    private final ControladorAutomata controlador = new ControladorAutomata();
    private final LienzoAutomata lienzo = new LienzoAutomata();
    private final PanelAlfabeto panelAlfabeto = new PanelAlfabeto(controlador);
    private final PanelEstados panelEstados = new PanelEstados(controlador);
    private final PanelMatrizTransiciones panelMatriz = new PanelMatrizTransiciones(controlador);
    private final PanelValidacion panelValidacion = new PanelValidacion(controlador);

    public VentanaPrincipal() {
        super(TITULO);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        setJMenuBar(construirMenu());
        setContentPane(construirContenido());

        controlador.registrarVista(lienzo, panelAlfabeto, panelEstados, panelMatriz, panelValidacion);
        controlador.sincronizar();
    }

    public ControladorAutomata controlador() {
        return controlador;
    }

    public LienzoAutomata lienzo() {
        return lienzo;
    }

    private JComponent construirContenido() {
        JScrollPane configuracion = new JScrollPane(construirPanelConfiguracion());
        configuracion.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        configuracion.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Paleta.BORDE_SUAVE));
        configuracion.setMinimumSize(new Dimension(ANCHO_MINIMO_CONFIGURACION, 0));
        configuracion.getVerticalScrollBar().setUnitIncrement(16);

        lienzo.setMinimumSize(new Dimension(ANCHO_MINIMO_LIENZO, 0));

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configuracion, lienzo);
        division.setDividerLocation(ANCHO_CONFIGURACION);
        division.setResizeWeight(0);
        division.setContinuousLayout(true);
        division.setOneTouchExpandable(false);
        return division;
    }

    private JPanel construirPanelConfiguracion() {
        JPanel configuracion = new JPanel();
        configuracion.setBackground(Paleta.FONDO);
        configuracion.setLayout(new BoxLayout(configuracion, BoxLayout.Y_AXIS));
        configuracion.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        configuracion.add(dimensionar(panelAlfabeto, 120));
        configuracion.add(Box.createVerticalStrut(8));
        configuracion.add(dimensionar(panelEstados, 220));
        configuracion.add(Box.createVerticalStrut(8));
        configuracion.add(dimensionar(panelMatriz, 220));
        configuracion.add(Box.createVerticalStrut(8));
        configuracion.add(dimensionar(panelValidacion, 240));
        configuracion.add(Box.createVerticalGlue());
        return configuracion;
    }

    private static JPanel dimensionar(JPanel panel, int altoPreferido) {
        panel.setPreferredSize(new Dimension(ANCHO_CONFIGURACION - 40, altoPreferido));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, altoPreferido));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private JMenuBar construirMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.setMnemonic(KeyEvent.VK_A);
        archivo.add(elemento("Nuevo", KeyEvent.VK_N, evento -> nuevo()));
        archivo.add(elemento("Exportar PNG...", KeyEvent.VK_E, evento -> exportarPNG()));
        archivo.addSeparator();
        archivo.add(elemento("Salir", KeyEvent.VK_Q, evento -> dispose()));
        barra.add(archivo);

        JMenu ejemplos = new JMenu("Ejemplos");
        ejemplos.setMnemonic(KeyEvent.VK_J);
        ejemplos.add(ejemplo("Cantidad par de ceros", ConstructorEjemplos::cantidadParDeCeros));
        ejemplos.add(ejemplo("Contiene la subcadena 00", ConstructorEjemplos::contieneSubcadena00));
        ejemplos.add(ejemplo("Cantidad par de unos", ConstructorEjemplos::cantidadParDeUnos));
        ejemplos.add(ejemplo("Termina en cero", ConstructorEjemplos::terminaEnCero));
        barra.add(ejemplos);

        JMenu vista = new JMenu("Vista");
        vista.setMnemonic(KeyEvent.VK_V);
        vista.add(elemento("Ajustar a la vista (F)", KeyEvent.VK_F, evento -> lienzo.ajustarAVista()));
        vista.add(elemento("Acercar", KeyEvent.VK_MINUS, evento -> lienzo.acercar()));
        vista.add(elemento("Alejar", KeyEvent.VK_L, evento -> lienzo.alejar()));
        barra.add(vista);

        JMenu ayuda = new JMenu("Ayuda");
        ayuda.setMnemonic(KeyEvent.VK_Y);
        ayuda.add(elemento("Acerca de", KeyEvent.VK_C, evento -> mostrarAcercaDe()));
        barra.add(ayuda);

        return barra;
    }

    private JMenuItem elemento(String texto, int mnemonico, java.awt.event.ActionListener accion) {
        JMenuItem item = new JMenuItem(texto);
        item.setMnemonic(mnemonico);
        item.addActionListener(accion);
        return item;
    }

    private JMenuItem ejemplo(String texto, Supplier<AutomataFinitoDeterminista> proveedor) {
        JMenuItem item = new JMenuItem(texto);
        item.addActionListener(evento -> controlador.cargarEjemplo(proveedor.get()));
        return item;
    }

    private void nuevo() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "Se descartara el automata en edicion. Continuar?",
                "Nuevo automata", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (respuesta == JOptionPane.OK_OPTION) {
            controlador.nuevo();
        }
    }

    private void exportarPNG() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Exportar diagrama a PNG");
        selector.setSelectedFile(new File("automata.png"));
        selector.setFileFilter(new FileNameExtensionFilter("Imagen PNG", "png"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".png")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".png");
        }
        try {
            lienzo.exportarPNG(destino);
            JOptionPane.showMessageDialog(this, "Diagrama exportado en:\n" + destino.getAbsolutePath(),
                    "Exportar PNG", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException excepcion) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar la imagen:\n" + excepcion.getMessage(),
                    "Exportar PNG", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarAcercaDe() {
        JOptionPane.showMessageDialog(this,
                TITULO + "\n\nUniversidad El Bosque - Compiladores\n"
                        + "Edicion de Σ, Q y δ con validacion en vivo.\n"
                        + "Arrastra los estados, rueda para zoom, F para ajustar a la vista.",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE);
    }
}

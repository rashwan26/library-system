package main;

import fileio.ApplicationLock;
import fileio.FileHandler;
import gui.LibraryGUI;
import java.awt.GraphicsEnvironment;
import javax.swing.*;

public final class Main {
    private static ApplicationLock applicationLock;
    private Main(){}
    public static void main(String[] args) {
        if(GraphicsEnvironment.isHeadless()) {
            System.err.println("Library System needs a desktop display. Use test.sh or test.bat for headless verification.");
            return;
        }
        SwingUtilities.invokeLater(()->{
            try {
                applicationLock=ApplicationLock.acquire(FileHandler.dataDirectory());
                Runtime.getRuntime().addShutdownHook(new Thread(()->{try{applicationLock.close();}catch(Exception ignored){}}));
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new LibraryGUI().setVisible(true);
            } catch(Exception e) {
                JOptionPane.showMessageDialog(null,e.getMessage(),"Library startup failed",JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}

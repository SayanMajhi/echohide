package com.sayan.echohide;

import com.sayan.echohide.cli.CliController;
import com.sayan.echohide.gui.EchoHideGui;

import javax.swing.*;
import java.awt.GraphicsEnvironment;

/**
 * Main application entry point for EchoHide.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && !args[0].equalsIgnoreCase("--gui")) {
            try {
                CliController.run(args);
            } catch (Exception e) {
                System.err.println("[EchoHide Execution Error] " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return;
        }

        // GUI Launch mode
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[EchoHide] Headless environment detected. Running CLI help:");
            CliController.printHelp();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // Set Nimbus or System Look & Feel
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored2) {
                }
            }

            EchoHideGui gui = new EchoHideGui();
            gui.setVisible(true);
        });
    }
}


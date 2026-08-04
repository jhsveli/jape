package jts.gui;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
//import java.io.File;
//import java.io.IOException;

public class JapeAbout extends JDialog {
    public JapeAbout(Component parent) {
        // Use the parent itself when it is a window (see OptionDialog).
        super((parent instanceof Window) ? (Window) parent
                        : SwingUtilities.getWindowAncestor(parent), "About JAME",
                Dialog.ModalityType.APPLICATION_MODAL);

        // Create body panel
        JPanel body = new JPanel();
        body.setLayout(new BorderLayout());
        this.setContentPane(body);

        // Add icon

        // Text Panel
        JPanel textPanel = new InsetPanel(5, 5, 5, 5);
        textPanel.setLayout(new GridLayout(4, 1));
        textPanel.setBackground(UIManager.getColor("Panel.background"));

        textPanel.add(new JLabel("JAME: A Jagged Alliance 2 Merc Editor", SwingConstants.CENTER));
        textPanel.add(new JLabel("Version 0.5", SwingConstants.CENTER));
        textPanel.add(new JLabel("https://github.com/jhsveli/jape", SwingConstants.CENTER));
        textPanel.add(new JLabel("Originally created by Douglas Greiman. http://www.duggelz.org/", SwingConstants.CENTER));

        // Button bar
        JPanel buttonPanel = new InsetPanel(0, 5, 0, 5);
        buttonPanel.setLayout(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(okButton);

        // Add elements to dialog
        body.add(buttonPanel, BorderLayout.SOUTH);
        body.add(textPanel, BorderLayout.CENTER);

        // Handle window events
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        this.pack();

        // Put this dialog in the center of the parent frame
        if (parent != null) {
            Point parentLoc = parent.getLocation();
            Dimension parentSize = parent.getSize();
            Dimension size = this.getSize();
            Point loc = new Point(
                    parentLoc.x + (parentSize.width - size.width) / 2,
                    parentLoc.y + (parentSize.height - size.height) / 2
            );
            this.setLocation(loc);
        }
    }
}

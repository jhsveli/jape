package jts.gui;/*
  Written 1999 by Douglas Greiman.
 
  This software may be used and distributed according to the terms
  of the GNU Public License, incorporated herein by reference.
*/


import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.StringTokenizer;

public class OptionDialog extends JDialog {
    // Option types
    public static final int DEFAULT_OPTION = -1;
    public static final int OK_ONLY_OPTION = -1;
    public static final int YES_NO_OPTION = 0;
    public static final int YES_NO_CANCEL_OPTION = 1;
    public static final int OK_CANCEL_OPTION = 2;

    /**
     * Return value from class method if YES is chosen.
     */
    public static final int YES_OPTION = 0;
    /**
     * Return value from class method if NO is chosen.
     */
    public static final int NO_OPTION = 1;
    /**
     * Return value from class method if CANCEL is chosen.
     */
    public static final int CANCEL_OPTION = 2;
    /**
     * Return value form class method if OK is chosen.
     */
    public static final int OK_OPTION = 0;
    /**
     * Return value from class method if user closes window without selecting
     * anything, more than likely this should be treated as either a
     * CANCEL_OPTION or NO_OPTION.
     */
    public static final int CLOSED_OPTION = -1;

    // Message types. Used by the UI to determine what icon to display,
    // and possibly what behavior to give based on the type.
    /**
     * Used for error messages.
     */
    public static final int ERROR_MESSAGE = 0;
    /**
     * Used for warning messages.
     */
    public static final int WARNING_MESSAGE = 2;

    private int value;

    public OptionDialog(
            Component parent,
            Object message,
            String title,
            int optionType,
            int messageType) {
        // Use the parent itself when it is a window: getWindowAncestor
        // returns null for a top-level window, and an ownerless
        // application-modal dialog may fail to display.
        super((parent instanceof Window) ? (Window) parent
                        : SwingUtilities.getWindowAncestor(parent), title,
                Dialog.ModalityType.APPLICATION_MODAL);

        // Set fields
        this.value = CLOSED_OPTION;

        // Create body panel
        JPanel body = new JPanel();
        body.setLayout(new BorderLayout());
        this.setContentPane(body);

        // Add icon

        // Text Panel
        JPanel textPanel = new InsetPanel(5, 5, 5, 5);
        textPanel.setLayout(new GridLayout(0, 1));
        textPanel.setBackground(UIManager.getColor("Panel.background"));

        // Create various lines of text
        String messageStr = message.toString();
        // It would be more intelligent to parse into words using java.text.BreakIterator
        StringTokenizer st = new StringTokenizer(messageStr, "\n");
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            JLabel label = new JLabel(line, SwingConstants.LEFT);
            textPanel.add(label);
        }

        // Button bar
        JPanel buttonPanel = buttonBar(optionType);

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
        Point parentLoc = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = this.getSize();
        Point loc = new Point(
                parentLoc.x + (parentSize.width - size.width) / 2,
                parentLoc.y + (parentSize.height - size.height) / 2
        );
        this.setLocation(loc);
    }

    private JPanel buttonBar(int optionType) {
        JPanel buttonPanel = new InsetPanel(0, 5, 0, 5);
        buttonPanel.setLayout(new FlowLayout());

        // Various buttons
        if (optionType == OK_ONLY_OPTION ||
                optionType == OK_CANCEL_OPTION) {
            JButton button = new JButton("OK");
            button.addActionListener(e -> setValue(OK_OPTION));
            buttonPanel.add(button);
        }
        if (optionType == YES_NO_OPTION ||
                optionType == YES_NO_CANCEL_OPTION) {
            JButton button = new JButton("Yes");
            button.addActionListener(e -> setValue(YES_OPTION));
            buttonPanel.add(button);
        }
        if (optionType == YES_NO_OPTION ||
                optionType == YES_NO_CANCEL_OPTION) {
            JButton button = new JButton("No");
            button.addActionListener(e -> setValue(NO_OPTION));
            buttonPanel.add(button);
        }
        if (optionType == OK_CANCEL_OPTION ||
                optionType == YES_NO_CANCEL_OPTION) {
            JButton button = new JButton("Cancel");
            button.addActionListener(e -> setValue(CANCEL_OPTION));
            buttonPanel.add(button);
        }
        return buttonPanel;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
        dispose();
    }

    public static void showMessageDialog(
            Component parent,
            Object message,
            String title,
            int messageType) {
        OptionDialog dialog = new OptionDialog(
                parent,
                message,
                title,
                DEFAULT_OPTION,
                messageType);
        dialog.setVisible(true);
    }

    public static int showConfirmDialog(
            Component parent,
            Object message,
            String title,
            int optionType,
            int messageType) {
        OptionDialog dialog = new OptionDialog(
                parent,
                message,
                title,
                optionType,
                messageType);
        dialog.setVisible(true);
        return dialog.getValue();
    }
}

package jts.gui;

/*
  Smoke tests for the Swing stat panel. Constructs the panel headless,
  binds a real actor loaded from the sample save game, and verifies
  refresh/commit behavior for the number and choice fields.
*/

import jts.data.Actor;
import jts.data.SaveGame;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StatPanelSmokeTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * The nth JNumberView child (0-based), in construction order.
     */
    private JNumberView findNumber(StatPanel panel, int which) {
        int seen = 0;
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof JNumberView) {
                if (seen == which) {
                    return (JNumberView) c;
                }
                ++seen;
            }
        }
        return null;
    }

    private JChoiceView findChoice(StatPanel panel, int which) {
        int seen = 0;
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof JChoiceView) {
                if (seen == which) {
                    return (JChoiceView) c;
                }
                ++seen;
            }
        }
        return null;
    }

    private Actor loadActor() throws Exception {
        SaveGame sg = new SaveGame();
        File sample = new File(StatPanelSmokeTest.class.getResource(
                "/savegames/version102_empty.sav").toURI());
        sg.load(sample.getPath());
        return sg.getActor(0);
    }

    @Test
    public void constructsWithSwingComponents() {
        StatPanel panel = new StatPanel(null);
        assertTrue("panel should be a Swing container", panel instanceof javax.swing.JPanel);

        int labels = 0, numbers = 0, choices = 0;
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof javax.swing.JLabel) ++labels;
            if (c instanceof JNumberView) ++numbers;
            if (c instanceof JChoiceView) ++choices;
        }
        assertEquals("expected 47 labels", 47, labels);
        assertEquals("expected 33 number views", 33, numbers);
        assertEquals("expected 2 choice views", 2, choices);
        assertEquals("expected 82 children total", 82, panel.getComponentCount());
    }

    @Test
    public void bindsActorValues() throws Exception {
        StatPanel panel = new StatPanel(null);
        Actor actor = loadActor();
        panel.setActor(actor, null);

        assertEquals(actor.get("Name"), findNumber(panel, 0).getText());
        assertEquals(actor.get("Health"), findNumber(panel, 2).getText());
        assertEquals(actor.get("Skill1"), findChoice(panel, 0).getSelectedItem());
    }

    @Test
    public void editCommitsToStruct() throws Exception {
        StatPanel panel = new StatPanel(null);
        Actor actor = loadActor();
        panel.setActor(actor, null);

        findNumber(panel, 2).setText("99"); // Health
        assertEquals(99, actor.getInt("Health"));
        assertTrue("panel should be marked modified", panel.isModified());
    }

    @Test
    public void choiceSelectionCommits() throws Exception {
        StatPanel panel = new StatPanel(null);
        Actor actor = loadActor();
        panel.setActor(actor, null);

        findChoice(panel, 0).setSelectedItem("Lockpicking");
        assertEquals("Lockpicking", actor.get("Skill1"));
        assertTrue("panel should be marked modified", panel.isModified());
    }
}

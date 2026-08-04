package jts.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.Test;

/**
 * Regression tests for the save game version 102 format used by the
 * newer open source engine (JA2 Stracciatella).
 *
 * Version 102 keeps the classic (versions 95-99) encrypted layout, so it
 * must parse with the same offsets.  The two sample files in
 * src/test/resources/savegames were created from more or less the same
 * game state, so their parsed content must match field for field.
 */
public class SaveGame102Test
{
    private static final String V102 = "/savegames/version102_empty.sav";
    private static final String V95 = "/savegames/saveversion103_empty.sav";
    private static final String V102_4SQUAD = "/savegames/straciatella_4squad.sav";

    private static final String[] FIELDS = {
	"Name", "Nickname", "Medical", "Strength", "Max Health", "Level Inc",
	"Health Inc", "Agility Inc", "Dexterity Inc", "Wisdom Inc",
	"Marksmanship Inc", "Medical Inc", "Mechanical Inc", "Explosives Inc",
	"Strength Inc", "Leadership Inc", "Kills", "Assists", "Shots Fired",
	"Shots Hit", "Battles", "Wounds", "Health", "Dexterity", "Personality",
	"Skill1", "Explosives", "Skill2", "Leadership", "Level",
	"Marksmanship", "Wisdom", "Agility", "Mechanical", "Checksum"
    };

    private static File sample(String resource) throws Exception {
	return new File(SaveGame102Test.class.getResource(resource).toURI());
    }

    /** Loading succeeds only when the code table search, decryption and
     * every actor checksum all validate, so a successful load is itself
     * the main assertion. */
    @Test
    public void version102SampleLoads() throws Exception {
	SaveGame sg = new SaveGame();
	sg.load(sample(V102).getPath());
	assertEquals(102, sg.uiSavedGameVersion);
	assertEquals(SaveGame.MERCPROFILESTRUCT_Count, sg.actorCount);
	assertEquals(0, sg.mercCount);
	for (int idx = 0; idx < sg.actorCount; ++idx) {
	    assertTrue(sg.actors[idx] != null);
	}
    }

    @Test
    public void version102UsesClassicLayout() throws Exception {
	SaveGame sg = new SaveGame();
	sg.load(sample(V102).getPath());
	// The classic layout offsets/values, not the version 103 ones.
	assertEquals(SaveGame.SAVED_GAME_HEADER_Length_99,
		     sg.SAVED_GAME_HEADER_Length);
	assertEquals(SaveGame.TacticalStatusType_Offset_99,
		     sg.TacticalStatusType_Offset);
	assertEquals(SaveGame.STRATEGICEVENT_Offset_99,
		     sg.STRATEGICEVENT_Offset);
	assertEquals(SaveGame.MAXITEMS_99, sg.MAXITEMS);
	assertEquals(SaveGame.MERCPROFILESTRUCT_Length_99,
		     sg.MERCPROFILESTRUCT_Length);
	assertEquals(SaveGame.SOLDIERTYPE_Length_99,
		     sg.SOLDIERTYPE_Length);
    }

    @Test
    public void version102SampleMatchesVersion95Sample() throws Exception {
	SaveGame a = new SaveGame();
	a.load(sample(V95).getPath());
	SaveGame b = new SaveGame();
	b.load(sample(V102).getPath());

	// Same header game state.
	assertEquals(headerInt(a.filename, 0x118), headerInt(b.filename, 0x118));
	assertEquals(headerByte(a.filename, 0x11C), headerByte(b.filename, 0x11C));
	assertEquals(headerByte(a.filename, 0x11D), headerByte(b.filename, 0x11D));
	assertEquals(headerInt(a.filename, 0x124), headerInt(b.filename, 0x124));

	assertEquals(a.actorCount, b.actorCount);
	assertEquals(a.mercCount, b.mercCount);
	for (int idx = 0; idx < a.actorCount; ++idx) {
	    for (String field : FIELDS) {
		assertEquals("actor[" + idx + "] " + field,
			     a.actors[idx].get(field),
			     b.actors[idx].get(field));
	    }
	}
    }

    @Test
    public void straciatellaFourSquadLoads() throws Exception {
	// A version 102 save with real squads exercises the SOLDIERTYPE
	// (merc) reading path, which the empty saves do not.
	SaveGame sg = new SaveGame();
	sg.load(sample(V102_4SQUAD).getPath());
	assertEquals(102, sg.uiSavedGameVersion);
	assertTrue("expected at least one hired merc", sg.mercCount > 0);
	for (int idx = 0; idx < sg.mercCount; ++idx) {
	    assertTrue(sg.mercs[idx] != null);
	}
    }

    @Test
    public void version102RoundTripIsByteIdentical() throws Exception {
	File original = sample(V102);
	File copy = File.createTempFile("jape102", ".sav");
	try {
	    Files.copy(original.toPath(), copy.toPath(),
		       StandardCopyOption.REPLACE_EXISTING);
	    SaveGame sg = new SaveGame();
	    sg.load(copy.getPath());
	    sg.save();
	    byte[] saved = Files.readAllBytes(copy.toPath());
	    byte[] expected = Files.readAllBytes(original.toPath());
	    assertEquals("save() must reproduce the sample bytes exactly",
			 expected.length, saved.length);
	    for (int i = 0; i < expected.length; ++i) {
		if (expected[i] != saved[i]) {
		    fail("byte " + i + " differs after save()");
		}
	    }
	} finally {
	    copy.delete();
	}
    }

    @Test
    public void straciatellaFourSquadRoundTripIsByteIdentical() throws Exception {
	File original = sample(V102_4SQUAD);
	File copy = File.createTempFile("jape102squad", ".sav");
	try {
	    Files.copy(original.toPath(), copy.toPath(),
		       StandardCopyOption.REPLACE_EXISTING);
	    SaveGame sg = new SaveGame();
	    sg.load(copy.getPath());
	    sg.save();
	    byte[] saved = Files.readAllBytes(copy.toPath());
	    byte[] expected = Files.readAllBytes(original.toPath());
	    assertEquals(expected.length, saved.length);
	    for (int i = 0; i < expected.length; ++i) {
		if (expected[i] != saved[i]) {
		    fail("byte " + i + " differs after save()");
		}
	    }
	} finally {
	    copy.delete();
	}
    }

    private static int headerInt(String filename, long offset) throws Exception {
	RandomAccessFile f = new RandomAccessFile(filename, "r");
	try {
	    f.seek(offset);
	    return Integer.reverseBytes(f.readInt());
	} finally {
	    f.close();
	}
    }

    private static int headerByte(String filename, long offset) throws Exception {
	RandomAccessFile f = new RandomAccessFile(filename, "r");
	try {
	    f.seek(offset);
	    return f.readUnsignedByte();
	} finally {
	    f.close();
	}
    }
}

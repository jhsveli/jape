##  Written 1999 by Douglas Greiman.
## 
##  This software may be used and distributed according to the terms
##  of the GNU Public License, incorporated herein by reference.
##

## Constants
JAVAC = javac
JAVACFLAGS = -classpath ../.. -g -target 1.5 -source 1.5
JAVA = java
JVIEW = jview
JVIEWFLAGS = /d:JIT=none /vst
JAR = jar
JEXEGEN = jexegen
JEXEGENFLAGS = /v /w
ZIP = zip
ZIPFLAGS = -X

## Source and object files
SOURCES = JapeGui.java no.jts.JapeFrame.java no.jts.JapeAbout.java no.jts.StatPanel.java no.jts.ItemPanel.java no.jts.ItemDetailPanel.java \
	no.jts.FieldView.java no.jts.TextView.java no.jts.NumberView.java no.jts.ChoiceView.java no.jts.ItemView.java \
	no.jts.DataChangeMixin.java no.jts.DataChangeListener.java no.jts.DataChangeEvent.java \
	no.jts.InsetPanel.java no.jts.OptionDialog.java \
	no.jts.Actor.java no.jts.Mercenary.java no.jts.SaveGame.java no.jts.Skill.java no.jts.Item.java no.jts.ItemExemplar.java \
	no.jts.Field.java no.jts.ByteField.java no.jts.ShortField.java no.jts.IntField.java no.jts.StringField.java no.jts.ChoiceField.java \
	no.jts.JapeConst.java no.jts.JapeAlg.java


# This isn't the compelete list of class files
# It doesn't include all those damn little inner class files
OBJS = $(SOURCES:.java=.class)

# JAR file stuff
MANIFEST = no.jts.Jape.mft
JARFILE = no.jts.Jape.jar
EXEFILE = no.jts.Jape.exe
MAINCLASS = JapeGui.class
PACKAGEPATH = duggelz/jape/
PACKAGEPATHUP = ../..
ZIPFILE = JAPE041.ZIP
SRCZIPFILE = JAPE041SRC.ZIP

# Generic Rules
%.class : %.java
	$(JAVAC) $(JAVACFLAGS) $<

# Couldn't figure out how to get this to work on Windows, so
# wrote a helper batch file.
# cd $(PACKAGEPATHUP) ; $(JAR) cvfm $(PACKAGEPATH)$@ $(PACKAGEPATH)$< $(PACKAGEPATH)*.class
%.jar : %.mft
	-rm $@
	jarhelper.bat

# Targets for this project
all: jar zip

jar: $(JARFILE)

zip: $(ZIPFILE) $(SRCZIPFILE)

exe: $(EXEFILE)

$(JARFILE) : $(OBJS) $(MANIFEST)

$(EXEFILE): $(OBJS)
	$(JEXEGEN) $(JEXEGENFLAGS) /out:$(EXEFILE) /base:$(PACKAGEPATHUP) /main:duggelz.jape.JapeGui $(PACKAGEPATH)*.class

$(ZIPFILE) : $(JARFILE) no.jts.Jape.html no.jts.Jape.txt
	-rm $(ZIPFILE)
	$(ZIP) $(ZIPFLAGS) $(ZIPFILE) $^

$(SRCZIPFILE) : *.java *.html *.txt Makefile *.bat
	-rm $(SRCZIPFILE)
	$(ZIP) $(ZIPFLAGS) $(SRCZIPFILE) $^

test: $(OBJS)
	java -classpath $(PACKAGEPATHUP) duggelz.jape.no.jts.SaveGame

run: run-java

run-exe: $(EXEFILE)
	./$(EXEFILE)

run-jview: $(JARFILE)
	$(JVIEW) $(JVIEWFLAGS) /cp:a $(JARFILE) $(PACKAGEPATH)$(MAINCLASS)

run-java: $(JARFILE)
	$(JAVA) $(JAVAFLAGS) -jar $(JARFILE)

run-obj: $(OBJS)
	$(JAVA) $(JAVAFLAGS) -classpath ../.. duggelz.jape.JapeGui

clean:
	-rm *.class $(JARFILE) $(EXEFILE) $(ZIPFILE) $(SRCZIPFILE)


JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = Sender.java Receiver.java

all: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class channelInfo recvInfo

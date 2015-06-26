all: NodeIf.class

Measurement.class: Measurement.java

NodeIfMsg.class:NodeIf.java
	javac $<

NodeIf.class: NodeIf.java NodeIfMsg.java RssiMsg.java Measurement.java
	javac $^

RssiMsg.java: RssiMessages.h
	nescc-mig java -java-classname=RssiMsg $< RssiMsg -o $@

NodeIfMsg.java: ../NodeIfMessages.h
	nescc-mig java -java-classname=NodeIfMsg $< NodeIfMsg -o $@

clean: 
	rm *.class NodeIfMsg.java


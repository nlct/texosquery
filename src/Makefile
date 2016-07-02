APP_VERSION:=$(shell grep "public static final String versionNum" java/TeXOSQuery.java | sed "s/public\sstatic\sfinal\sString\sversionNum=//" | tr -d "\"\; ")

test	: bin/texosquery.jar tests/test.tex
	cd tests; arara test

bin/texosquery.jar	: java/Manifest.txt \
			classes/com/dickimawbooks/texosquery/TeXOSQuery.class
			cd classes; \
			jar cmf ../java/Manifest.txt ../bin/texosquery.jar \
			com/dickimawbooks/texosquery/*.class

classes/com/dickimawbooks/texosquery/TeXOSQuery.class	: classes/com/dickimawbooks/texosquery \
	java/*.java
	cd java; \
	javac -d ../classes \
	-Xlint:unchecked -Xlint:deprecation \
	*.java

classes/com/dickimawbooks/texosquery	:
	mkdir -p classes/com/dickimawbooks/texosquery

clean	:
	\rm -f classes/com/dickimawbooks/texosquery/*.class

squeaky	:
	\rm -f bin/texosquery.jar

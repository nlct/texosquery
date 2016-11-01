test	: bin/texosquery.jar tex/texosquery.* tests/*.tex
	make -C tests

bin/texosquery.jar	: java/Manifest.txt \
			classes/com/dickimawbooks/texosquery/TeXOSQuery.class
			cd classes; \
			jar cmf ../java/Manifest.txt ../bin/texosquery.jar \
			com/dickimawbooks/texosquery/*.class

classes/com/dickimawbooks/texosquery/TeXOSQuery.class	: classes/com/dickimawbooks/texosquery \
	java/*.java
	cd java; \
	javac -source 1.7 -target 1.7 -d ../classes \
	-Xlint:unchecked -Xlint:deprecation -Xlint:-options \
	*.java

classes/com/dickimawbooks/texosquery	:
	mkdir -p classes/com/dickimawbooks/texosquery

clean	:
	\rm -f classes/com/dickimawbooks/texosquery/*.class

squeaky	:
	\rm -f bin/texosquery.jar

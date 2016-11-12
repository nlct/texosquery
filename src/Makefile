test	: bin/texosquery.jar bin/texosquery-jre8.jar bin/texosquery-jre5.jar tex/texosquery.* tests/*.tex
	make -C tests

bin/texosquery.jar	: java/Manifest-jre7.txt \
			classes/com/dickimawbooks/texosquery/TeXOSQueryJRE7.class
			cd classes; \
			jar cmf ../java/Manifest-jre7.txt ../bin/texosquery.jar \
			com/dickimawbooks/texosquery/TeXOSQueryJRE7.class \
			com/dickimawbooks/texosquery/TeXOSQuery.class \
			com/dickimawbooks/texosquery/QueryAction*.class \
			com/dickimawbooks/texosquery/TeXOSQuery\$$*.class

bin/texosquery-jre8.jar	: java/Manifest-jre8.txt \
			classes/com/dickimawbooks/texosquery/TeXOSQueryJRE8.class
			cd classes; \
			jar cmf ../java/Manifest-jre8.txt ../bin/texosquery-jre8.jar \
			com/dickimawbooks/texosquery/TeXOSQueryJRE8.class \
			com/dickimawbooks/texosquery/TeXOSQuery.class \
			com/dickimawbooks/texosquery/QueryAction*.class \
			com/dickimawbooks/texosquery/TeXOSQuery\$$*.class

bin/texosquery-jre5.jar	: java/Manifest-jre5.txt \
			classes/com/dickimawbooks/texosquery/TeXOSQueryJRE5.class
			cd classes; \
			jar cmf ../java/Manifest-jre5.txt ../bin/texosquery-jre5.jar \
			com/dickimawbooks/texosquery/TeXOSQueryJRE5.class \
			com/dickimawbooks/texosquery/TeXOSQuery.class \
			com/dickimawbooks/texosquery/QueryAction*.class \
			com/dickimawbooks/texosquery/TeXOSQuery\$$*.class

classes/com/dickimawbooks/texosquery/TeXOSQueryJRE7.class	: classes/com/dickimawbooks/texosquery \
	java/TeXOSQuery.java java/TeXOSQueryJRE7.java java/QueryAction*.java
	cd java; \
	javac -source 1.7 -target 1.7 -d ../classes \
	-Xlint:unchecked -Xlint:deprecation -Xlint:-options \
	TeXOSQuery.java QueryAction*.java TeXOSQueryJRE7.java

classes/com/dickimawbooks/texosquery/TeXOSQueryJRE8.class	: classes/com/dickimawbooks/texosquery \
	java/TeXOSQuery.java java/TeXOSQueryJRE8.java java/QueryAction*.java
	cd java; \
	javac -source 1.8 -target 1.8 -d ../classes \
	-Xlint:unchecked -Xlint:deprecation -Xlint:-options \
	TeXOSQuery.java QueryAction*.java TeXOSQueryJRE8.java

classes/com/dickimawbooks/texosquery/TeXOSQueryJRE5.class	: classes/com/dickimawbooks/texosquery \
	java/TeXOSQuery.java java/TeXOSQueryJRE5.java java/QueryAction*.java
	cd java; \
	javac -source 1.5 -target 1.5 -d ../classes \
	-Xlint:unchecked -Xlint:deprecation -Xlint:-options \
	TeXOSQuery.java QueryAction*.java TeXOSQueryJRE5.java

classes/com/dickimawbooks/texosquery	:
	mkdir -p classes/com/dickimawbooks/texosquery

clean	:
	\rm -f classes/com/dickimawbooks/texosquery/*.class

squeaky	:
	\rm -f bin/texosquery.jar

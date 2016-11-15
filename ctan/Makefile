dist	:
	make -C ../src/tex
	make -C ../src
	mkdir -p texosquery/java
	cp ../src/bin/texosquery.jar texosquery
	cp ../src/bin/texosquery-jre5.jar texosquery
	cp ../src/bin/texosquery-jre8.jar texosquery
	cp ../src/tex/texosquery.dtx texosquery
	cp ../src/tex/texosquery.ins texosquery
	cp ../src/tex/texosquery.pdf texosquery
	cp ../src/java/TeXOSQuery*.java texosquery/java
	cp ../src/java/Manifest*.txt texosquery/java
	cp ../README.md texosquery
	cp CHANGES texosquery
	zip --exclude="*.swp" -r texosquery.zip texosquery

clean	:
	\rm -r -f texosquery

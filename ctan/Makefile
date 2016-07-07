dist	:
	make -C ../src/tex
	make -C ../src
	mkdir -p texosquery
	cp texosquery.sh texosquery/texosquery
	cp ../src/bin/texosquery.jar texosquery
	cp ../src/tex/texosquery.dtx texosquery
	cp ../src/tex/texosquery.ins texosquery
	cp ../src/tex/texosquery.pdf texosquery
	cp ../src/java/TeXOSQuery.java texosquery
	cp ../src/java/Manifest.txt texosquery
	cp ../README.md texosquery
	zip --exclude="*.swp" -r texosquery.zip texosquery

clean	:
	\rm -r -f texosquery

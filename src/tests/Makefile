all	: test.pdf test-plain.pdf test-ltx.pdf test-xeltx.pdf

texosquery.tex	: ../tex/texosquery.dtx ../tex/texosquery.ins
		cd ../tex; latex texosquery.ins; mv texosquery.{tex,sty} ../tests

texosquery.sty	: ../tex/texosquery.dtx ../tex/texosquery.ins
		cd ../tex; latex texosquery.ins; mv texosquery.{tex,sty} ../tests

test.pdf	: test.tex ../bin/texosquery.jar
		arara test

test-plain.pdf	: texosquery.tex test-plain.tex ../bin/texosquery.jar
		arara test-plain

test-ltx.pdf	: texosquery.sty test-ltx.tex ../bin/texosquery.jar
		arara test-ltx

test-xeltx.pdf	: texosquery.sty test-xeltx.tex ../bin/texosquery.jar
		arara test-xeltx

clean	:
	\rm -f *.{aux,log,pdf}

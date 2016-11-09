all	: test.pdf test-plain.pdf test-ltx.pdf test-xeltx.pdf \
	  test-locale-data-ltx.pdf \
	  test-locale-data-xe.pdf \
	  test-locale-data-xeltx.pdf

texosquery.tex	: ../tex/texosquery.dtx ../tex/texosquery.ins
		cd ../tex; latex -file-line-error texosquery.ins; mv texosquery.{tex,sty} ../tests

texosquery.sty	: ../tex/texosquery.dtx ../tex/texosquery.ins
		cd ../tex; latex -file-line-error texosquery.ins; mv texosquery.{tex,sty} ../tests

test.pdf	: test.tex ../bin/texosquery.jar
		arara test

test-plain.pdf	: texosquery.tex test-plain.tex ../bin/texosquery.jar
		arara test-plain

test-ltx.pdf	: texosquery.sty test-ltx.tex ../bin/texosquery.jar
		arara test-ltx

test-xeltx.pdf	: texosquery.sty test-xeltx.tex ../bin/texosquery.jar
		arara test-xeltx

test-locale-data-ltx.pdf	: texosquery.sty test-locale-data-ltx.tex ../bin/texosquery.jar
		arara test-locale-data-ltx

test-locale-data-xe.pdf	: texosquery.sty test-locale-data-xe.tex ../bin/texosquery.jar
		arara test-locale-data-xe

test-locale-data-xeltx.pdf	: texosquery.sty test-locale-data-xeltx.tex ../bin/texosquery.jar
		arara test-locale-data-xeltx

clean	:
	\rm -f *.{aux,log,pdf}

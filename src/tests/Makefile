all	: test.pdf test-plain.pdf test-ltx.pdf

texosquery.tex	: ../tex/texosquery.dtx ../tex/texosquery.ins
		cd ../tex; latex texosquery.ins; mv texosquery.{tex,sty} ../tests

texosquery.sty	: ../tex/texosquery.dtx ../tex/texosquery.ins
		cd ../tex; latex texosquery.ins; mv texosquery.{tex,sty} ../tests

test.pdf	: test.tex
		arara test

test-plain.pdf	: texosquery.tex test-plain.tex
		arara test-plain

test-ltx.pdf	: texosquery.sty test-ltx.tex
		arara test-ltx

clean	:
	\rm -f *.{aux,log,pdf}

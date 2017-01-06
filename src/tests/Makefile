BUILD=arara --verbose
RM=\rm -f
MV=mv
DEP=texosquery.tex ../bin/texosquery.jar

all	: test.pdf test-plain.pdf test-ltx.pdf test-xeltx.pdf \
	  test-locale-data-ltx.pdf \
	  test-locale-data-xe.pdf \
	  test-locale-data-xeltx.pdf \
	  test-patterns.pdf

texosquery.tex	: ../tex/texosquery.dtx ../tex/texosquery.ins
		cd ../tex; latex -file-line-error texosquery.ins; \
		$(MV) texosquery.tex ../tests; \
		$(MV) texosquery.sty ../tests; 

test.pdf	: test.tex $(DEP)
		$(BUILD) test

test-plain.pdf	: test-plain.tex $(DEP)
		$(BUILD) test-plain

test-ltx.pdf	: test-ltx.tex $(DEP)
		$(BUILD) test-ltx

test-xeltx.pdf	: test-xeltx.tex $(DEP)
		$(BUILD) test-xeltx

test-locale-data-ltx.pdf	: test-locale-data-ltx.tex $(DEP)
		$(BUILD) test-locale-data-ltx

test-locale-data-xe.pdf	: test-locale-data-xe.tex $(DEP)
		$(BUILD) test-locale-data-xe

test-locale-data-xeltx.pdf	: test-locale-data-xeltx.tex $(DEP)
		$(BUILD) test-locale-data-xeltx

test-patterns.pdf	: test-patterns.tex $(DEP)
		$(BUILD) test-patterns

clean	:
	$(RM) *.aux
	$(RM) *.log
	$(RM) *.pdf

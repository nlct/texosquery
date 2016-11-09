texosquery.pdf	: texosquery.dtx
		pdflatex -file-line-error texosquery.dtx
		makeindex -s gglo.ist -t texosquery.glg -o texosquery.gls texosquery.glo
		pdflatex texosquery.dtx
		makeindex -s gind.ist texosquery.idx
		pdflatex texosquery.dtx
		pdflatex texosquery.dtx

clean	:
	\rm -f texosquery.{out,aux,log,glo,gls,idx,ind,tex,sty,pdf,ilg,glg}

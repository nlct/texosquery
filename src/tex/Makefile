all	: texosquery.pdf texosquery.tex

texosquery.pdf	: texosquery.dtx
		pdflatex -file-line-error texosquery.dtx
		makeglossaries texosquery
		makeindex -s gglo.ist -t texosquery.glg -o texosquery.gls texosquery.glo
		pdflatex texosquery.dtx
		makeindex -s gind.ist texosquery.idx
		pdflatex texosquery.dtx
		pdflatex texosquery.dtx

texosquery.tex	: texosquery.dtx texosquery.ins
	tex texosquery.ins

clean	:
	\rm -f texosquery*.{out,aux,log,glo*,gls*,idx,ind,tex,sty,pdf,ilg,glg*,bat,sh,dvi,toc,ist,cfg}

# texosquery
cross-platform Java application to query OS information designed for use in TeX's shell escape mechanism

Example useage:


```latex
% arara: pdflatex: {shell: on}
\documentclass{article}

\begin{document}
Test document.

\ttfamily

\input{|"texosquery --locale"}

\input{|"texosquery --pdfnow"}

\input{|"texosquery --pdfdate \jobname.tex"}


\end{document}
```

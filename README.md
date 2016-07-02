# texosquery
cross-platform Java application to query OS information designed for use in TeX's shell escape mechanism

Example usage:


```latex
% arara: pdflatex: {shell: on}
\documentclass{article}

\begin{document}
Test document.

\ttfamily

\input{|"texosquery --locale"}

\input{|"texosquery --pdfnow"}

\input{|"texosquery --pdfdate \jobname.tex"}

\input{|"texosquery --cwd"}

\input{|"texosquery --filesize \jobname.tex"}

\end{document}
```

The bash script in `bin/texosquery` just provides a convenient way
of calling `java -jar texosquery.jar`. The jar file (and the bash
script, if required) should be placed on the system's path.

A DOS batch file can similarly be created. For example:
```dos
@start javaw -jar "%~dp0\texosquery.jar" %*
```

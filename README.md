# texosquery
cross-platform Java application to query OS information designed for use in TeX's shell escape mechanism

The actual Java application is in texosquery.jar.
There are files provided for easy access in TeX documents:

 - texosquery.tex : generic TeX code
 - texosquery.sty : LaTeX package

The bash script in `bin/texosquery` just provides a convenient way
of calling `java -jar texosquery.jar`. The jar file (and the bash
script, if required) should be placed on the system's path.

A DOS batch file can similarly be created. For example:
```dos
@start javaw -jar "%~dp0\texosquery.jar" %*
```

Example usage:

Plain TeX:

```tex
% arara: pdftex: {shell: on}
\input texosquery

\tt
\TeXOSQueryLocale{\result}
locale: \result.

\TeXOSQueryCwd{\result}
cwd: \result.

\TeXOSQueryNow{\result}
now: \result.

\TeXOSQueryFileDate{\result}{\jobname.tex}
file date: \result.

\TeXOSQueryFileSize{\result}{\jobname.tex}
file size: \result.

\bye
```

LaTeX:

```latex
% arara: pdflatex: {shell: on}
\documentclass{article}

\usepackage{texosquery}

\begin{document}
\ttfamily

\TeXOSQueryLocale{\result}
locale: \result.

\TeXOSQueryCwd{\result}
cwd: \result.

\TeXOSQueryNow{\result}
now: \result.

\TeXOSQueryFileDate{\result}{\jobname.tex}
file date: \result.

\TeXOSQueryFileSize{\result}{\jobname.tex}
file size: \result.

\end{document}
```

Alternatively use the shell escape explicitly:

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

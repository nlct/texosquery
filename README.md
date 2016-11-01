# texosquery
Cross-platform Java application to query OS information designed for use in 
TeX's shell escape mechanism.

I was aiming at Java 1.5 as a lower bound for the version,
but the language tags in java.util.Locale were only introduced
in Java 1.7. Since the primary purpose of this application is
to query locale information using TeX's shell escape mechanism
this means that the oldest Java version has to be 1.7 for this
application to work. (All the non-locale related functions
were thrown in as extras. It seemed like overkill to write a Jave
application that just returned the language and region information
so I added a few other functions that might be useful.)
My original attempt was in Lua rather than Java, but it wasn't
possible to consistently access the locale information
cross-platform. See also http://ctan.org/pkg/tracklang

The application can query the following:

 - locale and codeset
 - current working directory
 - user home directory
 - temporary directory
 - OS name, arch and version
 - Current date and time in PDF format
   (for TeX formats that don't provide `\pdfcreationdate`)
 - Date-time stamp of a file in PDF format
   (for TeX formats that don't provide `\pdffilemoddate`)
 - Size of a file in bytes
   (for TeX formats that don't provide `\pdffilesize`)
 - Contents of a directory (captured as a list)
 - Directory contents filtered by regular expression
   (captured as a list)
 - URI of a file
 - Canonical path of a file

All paths use a forward slash as directory divider so results
can be used, for example, in commands like `\includegraphics`.

There are files provided for easy access in TeX documents:

 - `texosquery.tex` : generic TeX code
 - `texosquery.sty` : LaTeX package

This provides commands to run `texosquery` using TeX's shell
escape mechanism and capture the result in a control sequence.
The category code of most of TeX's default special characters 
(and some other potentially problematic characters) is temporarily 
changed to 12 while reading the result.

## Installation

The actual Java application is in `texosquery.jar`.
The `.tex` and `.sty` files can be extracted from `texosquery.dtx`
using `tex texosquery.ins`. In the following, replace TEXMF with the
path to your TEXMF directory.

 - Move `texosquery.tex` to `TEXMF/tex/generic/texosquery/`
 - Move `texosquery.sty` to `TEXMF/tex/latex/texosquery/`
 - Move `texosquery.pdf` to `TEXMF/doc/generic/texosquery/`
 - Move `texosquery.jar` to `TEXMF/scripts/texosquery/`

Unix-like users: move the bash script `texosquery` to somewhere on
your path.

Windows users: TeX Live's generic wrapper should be able to locate
the `texosquery.jar` file, but if not or you're not using TL,
then (untested) create a file called `texosquery.bat` that contains:
```dos
@ECHO OFF
FOR /F %%I IN ('kpsewhich --progname=texosquery --format=texmfscripts texosquery.jar') DO SET JARPATH=%%I
START javaw -jar "%JARPATH%\texosquery.jar" %*
```
(or skip the `FOR` line and replace `%JARPATH%` with the full path name to 
`texosquery.jar`.) Put this file on your system's path.

## Examples:

Plain TeX:

```tex
% arara: pdftex: {shell: on}
\input texosquery

\TeXOSQueryLocale{\result}
locale: \result.

\TeXOSQueryCwd{\result}
cwd: {\tt \result}.

\TeXOSQueryNow{\result}
now: \result.

\TeXOSQueryFileDate{\result}{\jobname.tex}
file date: \result.

\TeXOSQueryFileSize{\result}{\jobname.tex}
file size: \result bytes.

\bye
```

LaTeX:

```latex
% arara: pdflatex: {shell: on}
\documentclass{article}

\usepackage{texosquery}
\usepackage{etoolbox}

\begin{document}

\TeXOSQueryLocale{\result}
locale: \result.

\TeXOSQueryCwd{\result}
cwd: \texttt{\result}.

\TeXOSQueryNow{\result}
now: \result.

\TeXOSQueryFileDate{\result}{\jobname.tex}
file date: \result.

\TeXOSQueryFileSize{\result}{\jobname.tex}
file size: \result bytes.

\TeXOSQueryFilterFileList{\result}{,}{.+\string\.(png|jpg)}{.}
All jpg and png files in current directory:

\renewcommand{\do}[1]{\texttt{#1}.\par}
\expandafter\docsvlist\expandafter{\result}

\end{document}
```

For a full list of available commands, see the documentation
(`texosquery.pdf`).

You can omit `texosquery.tex` and directly use `texosquery`
in TeX's shell escape, but take care of special characters
occurring in the result or if double-quotes occur in
`\jobname`.

## Source code

The `.tex` and `.sty` files and documentation are contained in
`texosquery.dtx`. To compile the documentation:
```bash
pdflatex texosquery.dtx
makeindex -s gglo.ist -t texosquery.glg -o texosquery.gls texosquery.glo
makeindex -s gind.ist texosquery.idx
pdflatex texosquery.dtx
pdflatex texosquery.dtx
```

The Java source is in `TeXOSQuery.java` and the manifest for the
`.jar` file is `Manifest.txt`. Assuming the following directory
structure:
```
java/TeXOSQuery.java
java/Manifest.txt
classes/com/dickimawbooks/texosquery/
```
Then to create `texosquery.jar`, do:
```bash
cd java 
javac -d ../classes TeXOSQuery.java
cd ../classes
jar cmf ../java/Manifest.txt ../texosquery.jar com/dickimawbooks/texosquery/*.class
```

---

Source on GitHub: https://github.com/nlct/texosquery

Author Home Page: http://www.dickimaw-books.com/

License: LPPL 1.3+

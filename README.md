# texosquery
Cross-platform Java application to query OS information designed for use in 
TeX's shell escape mechanism.

The application can query the following:

 - locale information
 - default file encoding
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

In the following, replace TEXMF with the path to your TEXMF directory.

### Installing the Application

There are now three Java applications:

 - `texosquery.jar` (requires at least Java 7)
 - `texosquery-jre8.jar` (requires at least Java 8)
 - `texosquery-jre5.jar` (requires at least Java 5)

There are corresponding bash scripts for Unix-like users:

 - `texosquery.sh`
 - `texosquery-jre8.sh`
 - `texosquery-jre5.sh`

Each bash script uses `kpsewhich` to find the corresponding `.jar` file
and run it. Put the `.jar` files in TEXMF/scripts/texosquery/ and the
`.sh` files somewhere on your path. To test the installation, run
```bash
texosquery.sh -v
```
in a terminal (replace `texosquery.sh` with `texosquery-jre8.sh` or 
`texosquery-jre5.sh`, if appropriate). If successful, it should show 
the version number.

Windows users can ignore these `.sh` files. The TeX distribution may
contain a `.exe` wrapper for each `.jar` file (`texosquery.exe`,
`texosquery-jre8.exe` and `texosquery-jre5.exe`). Put these files
somewhere on your path. To test the installation, run
```bash
texosquery -v
```
in the command prompt (replace `texosquery` with `texosquery-jre8`
or `texosquery-jre5`, if appropriate). If successful, it should show 
the version number.

### Installing the TeX Code

The `.tex`, `.cfg` and `.sty` files can be extracted from `texosquery.dtx`
using `tex texosquery.ins`.

 - Move `texosquery.tex` to `TEXMF/tex/generic/texosquery/`
 - Move `texosquery.cfg` to `TEXMF/tex/generic/texosquery/`
 - Move `texosquery.sty` to `TEXMF/tex/latex/texosquery/`
 - Move `texosquery.pdf` to `TEXMF/doc/generic/texosquery/`

The `texosquery.cfg` file allows you to specify which application you
want to use. First check which version of the Java Runtime
Environment (JRE) you have installed:
```bash
java -version
```
This should display the version information. (For example, `"1.8.0_92"`)
If this starts with `1.8` (“Java 8”), then you can use `texosquery-jre8.jar`.
This is the full application. You can use the `texosquery` or 
`texosquery-jre5` as well, but the locale support will be more limited. 
The `texosquery-jre8.sh` bash script invokes Java with 
`-Djava.locale.providers=CLDR,JRE`.
Windows users will need to find some way to set this if they want to use the
[Unicode Common Locale Data Repository (CLDR)](http://cldr.unicode.org/) 
installed on their system. (The pending Java 9 should include this
by default, so this will only be relevant to Java 8.)

If the version information starts with `1.7` (“Java 7”), then you can use 
`texosquery.jar`. This is the default application and provides most
of the functions of the full application, but there's less locale support.
You can also use `texosquery-jre5`, but you can't use
`texosquery-jre8`.

If the version information starts with `1.5` (“Java 5”) or `1.6`
(“Java 6”), then you can _only_ use `texosquery-jre5`.  The locale
support is significantly reduced in this case and there's no support
for language scripts.

Once you have determined which application you want to use, edit the
`texosquery.cfg` so that `\TeXOSInvokerName` expands to your chosen
application. For example, a Linux user with Java 8:
```tex
\def\TeXOSInvokerName{texosquery-jre8.sh}
```
or a Windows user with Java 5 or 6:
```tex
\def\TeXOSInvokerName{texosquery-jre5}
```

## Examples:

Plain TeX:

```tex
% arara: pdftex: {shell: on}
\input texosquery

\TeXOSQuery{\result}{-b}
IEFT BCP 47 language tag: \result.

\TeXOSQueryLocale{\result}
POSIX locale: \result.

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

\TeXOSQuery{\result}{-b}
IEFT BCP 47 language tag: \result.

\TeXOSQueryLocale{\result}
POSIX locale: \result.

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

## Source code

The `.tex`, `.cfg` and `.sty` files and documentation are contained in
`texosquery.dtx`. To compile the documentation:
```bash
pdflatex texosquery.dtx
makeindex -s gglo.ist -t texosquery.glg -o texosquery.gls texosquery.glo
makeindex -s gind.ist texosquery.idx
pdflatex texosquery.dtx
pdflatex texosquery.dtx
```

The Java source is in the `.java` files and the manifest for each the
`.jar` file is `Manifest*.txt` (`Manifest-jre8.txt` for `texosquery-jre8` etc). 
Assuming the following directory structure:
```
java/TeXOSQuery.java
java/TeXOSQueryJRE5.java
java/TeXOSQueryJRE7.java
java/TeXOSQueryJRE8.java
java/Manifest-jre5.txt
java/Manifest-jre7.txt
java/Manifest-jre8.txt
classes/com/dickimawbooks/texosquery/
```
Then to create `texosquery-jre8.jar`, do (for JDK version 1.8):
```bash
cd java 
javac -d ../classes TeXOSQuery.java TeXOSQueryJRE8.java
cd ../classes
jar cmf ../java/Manifest-jre8.txt ../texosquery.jar com/dickimawbooks/texosquery/*.class
```

---

Source on GitHub: https://github.com/nlct/texosquery

Author Home Page: http://www.dickimaw-books.com/

License: LPPL 1.3+

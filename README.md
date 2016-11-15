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

There are files provided for easy access in (La)TeX documents:

 - `texosquery.tex` : generic TeX code
 - `texosquery.sty` : LaTeX package

This provides the command `\TeXOSQuery` to run `texosquery` using
TeX's shell escape mechanism and capture the result in a control
sequence.  The category code of most of TeX's default special
characters (and some other potentially problematic characters) is
temporarily changed to 12 while reading the result. Some of the
`texosquery` output contains short markup commands (such as `\wrp`)
which are internally converted within `\TeXOSQuery`.

## Installation

In the following, replace _TEXMF_ with the path to your TEXMF directory.

### Extracting the Files

Aside from the `.jar` files, the `texosquery` files are all
bundled inside `texosquery.dtx` with the extraction commands
provided in `texosquery.ins`. To extract all the files do:
```bash
tex texosquery.ins
```
The `.sh` files are bash scripts for Unix-like users. These will
need to be set to executable using `chmod`. Windows users can
deleted these files.

The `.bat` files are batch scripts for Windows users. Unix-like
users can deleted these files.

### Installing the Application

There are now three Java applications:

 - `texosquery.jar` (requires at least Java 7)
 - `texosquery-jre8.jar` (requires at least Java 8)
 - `texosquery-jre5.jar` (requires at least Java 5)

There are corresponding bash scripts for Unix-like users:

 - `texosquery.sh`
 - `texosquery-jre8.sh`
 - `texosquery-jre5.sh`

and batch scripts for Windows users:

 - `texosquery.bat`
 - `texosquery-jre8.bat`
 - `texosquery-jre5.bat`

Each script uses `kpsewhich` to find the corresponding `.jar` file
and run it.

Put the `.jar` files in _TEXMF_`/scripts/texosquery/` and the
`.sh` or `.bat` files somewhere on your path. To test the installation, run
```bash
texosquery.sh -v
```
in a terminal (Unix-like users) or
```bash
texosquery -v
```
in the command prompt (Windows users).

If successful, it should show the version number.

### Installing the TeX Code

The `.tex`, `.cfg` and `.sty` files should all be extracted from `texosquery.dtx`
with `tex texosquery.ins` as described above.

 - Move `texosquery.tex` to _TEXMF_`/tex/generic/texosquery/`
 - Move `texosquery.cfg` to _TEXMF_`/tex/generic/texosquery/`
 - Move `texosquery.sty` to _TEXMF_`/tex/latex/texosquery/`
 - Move `texosquery.pdf` to _TEXMF_`/doc/generic/texosquery/`

The `texosquery.cfg` file allows you to specify which application you
want to use. First check which version of the Java Runtime
Environment (JRE) you have installed:
```bash
java -version
```
This should display the version information. (For example, `"1.8.0_92"`)

If the version number starts with `1.8` (“Java 8”), then you can use `texosquery-jre8.jar`.
This is the full application. You can use `texosquery` or 
`texosquery-jre5` as well, but there's less locale support with them. 
The `texosquery-jre8.sh` bash script and `texosquery-jre8.bat` batch script invokes Java with 
`-Djava.locale.providers=CLDR,JRE`.

If Windows users have simply been supplied with an executable versions
of the `texosquery-jre8.jar` file (`texosquery-jre8.exe`) then you'll have to set the
`JAVA_TOOL_OPTIONS` environment variable instead.
 
This `java.locale.providers` setting allows Java to access locale
information from the [Unicode Common Locale Data Repository
(CLDR)](http://cldr.unicode.org/) installed on their system. The
pending Java 9 should include this by default, so this will only be
relevant to Java 8. Earlier versions of Java (7 or below) don't have
this option, which limits the locale support to that which is
natively provided by the JRE.

If the version information starts with `1.7` (“Java 7”), then you can use 
`texosquery.jar`. This is the default application and provides most
of the functions of the full application, but there's less locale support.
You can also use the even more limited `texosquery-jre5`, but you can't use
`texosquery-jre8`, so you can't take advantage of the CLDR.

If the version information starts with `1.5` (“Java 5”) or `1.6`
(“Java 6”), then you can _only_ use `texosquery-jre5`.  The locale
support is significantly reduced in this case and there's no support
for language scripts. Note that these versions of Java are
deprecated.

Once you have determined which application you want to use, edit the
`texosquery.cfg` so that `\TeXOSInvokerName` is defined to the appropriate invocation. For example, a Linux user with Java 8:
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

To compile the documentation:
```bash
pdflatex texosquery.dtx
makeindex -s gglo.ist -t texosquery.glg -o texosquery.gls texosquery.glo
makeindex -s gind.ist texosquery.idx
pdflatex texosquery.dtx
pdflatex texosquery.dtx
```

The Java source is in the `.java` files and the manifest for each 
`.jar` file is `Manifest*.txt` (`Manifest-jre8.txt` for `texosquery-jre8` etc). 
Assuming the following directory structure:
```
java/TeXOSQuery.java
java/TeXOSQueryJRE5.java
java/TeXOSQueryJRE7.java
java/TeXOSQueryJRE8.java
java/QueryAction.java
java/QueryActionType.java
java/Manifest-jre5.txt
java/Manifest-jre7.txt
java/Manifest-jre8.txt
classes/com/dickimawbooks/texosquery/
```
Then to create `texosquery-jre8.jar`, do (for JDK version 1.8):
```bash
cd java 
javac -d ../classes TeXOSQuery.java QueryAction.java QueryActionType.java TeXOSQueryJRE8.java
cd ../classes
jar cmf ../java/Manifest-jre8.txt ../texosquery-jre8.jar com/dickimawbooks/texosquery/*.class
```

Similarly for the other `.jar` files, replacing `8` with `7` or `5`.
(The Java 7 version should really be `texosquery-jre7.jar`, but is
simply named `texosquery.jar` in the distribution for backward compatibility reasons. If you compile the code, the `.jar` name is your choice.)

---

Source on GitHub: https://github.com/nlct/texosquery

Author Home Page: http://www.dickimaw-books.com/

License: LPPL 1.3+

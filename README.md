# texosquery
Cross-platform Java application to query OS information designed for use in 
TeX's shell escape mechanism.

## Licence

This material is subject to the LaTeX Project Public License.
See http://www.ctan.org/license/lppl1.3
for the details of that license.

Copyright 2016-2020 Nicola Talbot

## Description

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

## Important Notes

The TeX code uses a piped shell escape to capture the result from
`texosquery`. This means that you must have the piped shell escape
enabled to make use of this feature. MiKTeX users need the
`--enable-pipes` option on the TeX command line to enable this.
The alternative is to run `texosquery` outside of TeX and
capture the output in a temporary file, which can then be
read using `\TeXOSQueryFromFile`.

You need to correctly set up the configuration file `texosquery.cfg`
to match your system. The TeX package managers can't do this for you
automatically. Copy the file to either your `TEXMFHOME` or
`TEXMFLOCAL` tree to prevent it from being overwritten by subsequent
updates.

`texosquery-jre8` is on the restricted list for TeX Live 2017. To 
take advantage of this, you must have at least Java 8 installed and
edit the `texosquery.cfg` file to:
```tex
\def\TeXOSInvokerName{texosquery-jre8}
\TeXOSQueryAllowRestricted
```
(Note that the second line above has been uncommented.)

Note that due to TeX's security measures, quotes can't be used in the
restricted mode's shell escape. This means that if you need to pass
a file name containing a space as an argument to `texosquery`,
you'll have to use the unrestricted mode.

## Installation

Installation is best done using your TeX package manager.
Manual installation instructions are described below.
For more detail, see the documentation.

### Compiling the Documentation

To compile the documentation:
```bash
pdflatex texosquery.dtx
makeglossaries texosquery
makeindex -s gglo.ist -t texosquery.glg -o texosquery.gls texosquery.glo
pdflatex texosquery.dtx
makeindex -s gind.ist texosquery.idx
pdflatex texosquery.dtx
pdflatex texosquery.dtx
```

### Extracting the Files

Except for the `.jar` files, the `texosquery` files are all
bundled inside `texosquery.dtx` with the extraction commands
provided in `texosquery.ins`. To extract all the files do:
```bash
tex texosquery.ins
```

The `.sh` files are bash scripts for Unix-like users. These will
need to be set to executable using `chmod`. I recommend
that you also remove the `.sh` extension. Windows users can
deleted these files.

The `.batch` files are batch scripts for Windows users. Unix-like
users can delete these files. These files are given the extension
`.batch` as TeX on Windows doesn't allow the creation of `.bat`
files. The extension will need to be changed to `.bat` after the
files have been extracted.

### Installing the Application

In the following, replace _TEXMF_ with the path to your TEXMF directory.
You can find your home TEXMF directory using
```bash
kpsewhich -var-value=TEXMFHOME
```

There are now three Java applications provided by this package:

 - `texosquery.jar` (requires at least Java 7)
 - `texosquery-jre8.jar` (requires at least Java 8)
 - `texosquery-jre5.jar` (requires at least Java 5)

(See the ["Security"](#security) section below.)

There are corresponding bash scripts for Unix-like users (the `.sh`
extension added by `texosquery.ins` should be removed and the files
made executable):

 - `texosquery`
 - `texosquery-jre8`
 - `texosquery-jre5`

There are also corresponding batch scripts for Windows users (the `.batch`
extension created by the `.ins` file should be changed to `.bat`):

 - `texosquery.bat`
 - `texosquery-jre8.bat`
 - `texosquery-jre5.bat`

Each script uses `kpsewhich` to find the corresponding `.jar` file
and runs it.

Put the `.jar` files in _TEXMF_`/scripts/texosquery/` and the
bash or `.bat` files somewhere on your path. You may or may not need
to refresh the TeX database.

To test the installation, run
```bash
texosquery -v
```
in the command prompt or terminal. (Also try with `texosquery-jre8`
and `texosquery-jre5`.)

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

If the version number starts with `1.8` ("Java 8"), then you can use `texosquery-jre8.jar`.
This is the full application. You can use `texosquery` or 
`texosquery-jre5` as well, but there's less locale support with them. 
The `texosquery-jre8` bash script and `texosquery-jre8.bat` batch script invokes Java with 
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
natively provided by the JRE. Note that Java 7 and earlier versions
[have reached their end of life](http://www.oracle.com/technetwork/java/eol-135779.html) and are now **deprecated**.

If the version information starts with `1.7` ("Java 7"), then you can use 
`texosquery.jar`. This is the default application and provides most
of the functions of the full application, but there's less locale support.
You can also use the even more limited `texosquery-jre5`, but you can't use
`texosquery-jre8`, so you can't take advantage of the CLDR.

If the version information starts with `1.5` ("Java 5") or `1.6`
("Java 6"), then you can _only_ use `texosquery-jre5`.  The locale
support is significantly reduced in this case and there's no support
for language scripts.

Once you have determined which application you want to use, edit the
`texosquery.cfg` so that `\TeXOSInvokerName` is defined to the appropriate invocation. For example, with Java 8:
```tex
\def\TeXOSInvokerName{texosquery-jre8}
```
or with Java 5 or 6:
```tex
\def\TeXOSInvokerName{texosquery-jre5}
```
(bash users will have to add the `.sh` extension if it hasn't
been removed, as per the above instructions.)

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

## Troubleshooting

If something goes wrong with the call to `texosquery`, the control
sequence will be set to empty. If this happens, here are the steps
to diagnose the problem:

  1. Check the log file for any lines starting with `TeXOSQuery:`
     For example: `TeXOSQuery: texosquery-jre8 --pdfnow`
     If found, this means that the dry run mode was on and the shell
     escape wasn't used. Ensure that the shell escape is
     enabled when you build the document.
  2. If the log file contains `(|texosquery` _options_`)` then the
     shell escape was used but `texosquery` returned an empty value.
     In this case, copy the part between `(|` and `)` and paste it
     into a command prompt or terminal but insert `--debug` at the
     start of the options list. For example, if the log file
     contains `(|texosquery-jre8 --cwd)` then run
```sh
texosquery-jre8 --debug --cwd
```

This should now display an error message explaining the
problem. (For example, read access forbidden or file not found
or a security exception.)

## Security

As from version 1.2, all variants of `texosquery` (JRE5, 7 and 8)
obey TeX Live's `openin_any` setting. Any of the actions that involve
reading file information won't work if read access is forbidden by 
`openin_any` or by the operating system. MiKTeX doesn't use the
`openin_any` setting, so if not set `texosquery` will assume `a`
(any).

In addition to obeying `openin_any`, the file listing actions (such
as `--list`) for the JRE7 and 8 variants also prohibit listing the
contents outside of the current working directory's path even if `openin_any=a`.
This means that you can't, for example, list the contents of `..` (the current
working directory's parent) nor can you try walking the entire file
system.  The `--walk` action additionally won't descend hidden
directories. This extra restriction is designed to prevent malicious
code from trying to use `texosquery` to look around your filing
system.

The Java 5 version `texosquery-jre5` is the least secure and doesn't
have this additional restriction on the file listing actions. However,
it still obeys `openin_any`. The `--walk` action is not available
with `texosquery-jre5`.

Examples:
```bash
texosquery-jre8 --debug --walk ',' '.*' /
```
This action is forbidden:
```
texosquery-jre8: Can't walk directory: /
```
Changing `/` to `.` is allowed, but not for `texosquery-jre5`:
```bash
texosquery-jre5 --debug --walk ',' '.*' .
```
returns:
```
texosquery-jre5: walk requires at least JRE 7 version
```
With `openin_any=a`, the `--userhome` action is usually allowed
(depends on the operating system and Java's security manager).
```bash
texosquery-jre8 --debug --userhome
```
returns
```
\fslh home\fslh nlct
```
(which `\TeXOSQuery` converts to `/home/nlct`.)

However, unless this also happens to be the current working
directory, it's not possible to obtain a file listing:
```bash
texosquery-jre8 --debug --list ',' /home/nlct
```
returns
```
texosquery-jre8: Unable to list contents of: /home/nlct
texosquery-jre8: Listing outside cwd path not permitted: /home/nlct
```

## Source code

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
java/FileSortType.java
java/FileSortComparator.java
java/FileListType.java
java/FileWalkVisitor.java
java/Manifest-jre5.txt
java/Manifest-jre7.txt
java/Manifest-jre8.txt
classes/com/dickimawbooks/texosquery/
```
Then to create `texosquery-jre8.jar`, do (for JDK version 1.8):
```bash
cd java 
javac -d ../classes TeXOSQuery.java QueryAction.java QueryActionType.java TeXOSQueryJRE8.java FileSortType.java FileSortComparator.java FileListType.java FileWalkVisitor.java
cd ../classes
jar cmf ../java/Manifest-jre8.txt ../texosquery-jre8.jar com/dickimawbooks/texosquery/*.class
```

Similarly for the other `.jar` files, replacing `8` with `7` or `5`.
(The Java 7 version should really be `texosquery-jre7.jar`, but is
simply named `texosquery.jar` in the distribution for backward compatibility reasons. If you compile the code, the `.jar` name is your choice.)

---

Source on GitHub: https://github.com/nlct/texosquery

Author Home Page: http://www.dickimaw-books.com/


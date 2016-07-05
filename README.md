# texosquery
cross-platform Java application to query OS information designed for use in TeX's shell escape mechanism

The actual Java application is in `texosquery.jar`.

The bash script `texosquery` just provides a convenient way
of calling `java -jar texosquery.jar`.

A DOS batch file can similarly be created. For example:
```dos
@start javaw -jar "%~dp0\texosquery.jar" %*
```

The jar file and the bash script (or batch file) should be placed on the 
system's path. Alternatively, supply the path to the `.jar` file:
```bash
java -jar /path/to/texosquery.jar
```

There are files provided for easy access in TeX documents:

 - texosquery.tex : generic TeX code
 - texosquery.sty : LaTeX package

The application can query the following:

 - locale and codeset
 - current working directory
 - user home directory
 - temporary directory
 - OS name, arch and version
 - Current date and time in PDF format
 - Date-time stamp of a file in PDF format
 - Size of a file in bytes
 - Contents of a directory
 - Directory contents filtered by regular expression
 - URI of a file
 - Canonical path of a file

All paths use a forward slash as directory divider.
Interface commands in texosquery.tex change the category code
of most special characters while reading the result.

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
file size: \result bytes.

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
file size: \result bytes.

\end{document}
```

For a full list of available commands, see the documentation.

You can omit texosquery.tex and directly use `texosquery`
in TeX's shell escape, but take care of special characters
occurring in the result or if double-quotes occur in
`\jobname`.

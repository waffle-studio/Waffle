#!/bin/sh

cd `dirname $0`
mkdir -p build

#rm *.pdf

platex --interaction=nonstopmode ./main.tex >./main.log 2>&1
bibtex main >./bib.log 2>&1
platex --interaction=nonstopmode ./main.tex >/dev/null 2>&1
platex --interaction=nonstopmode ./main.tex >/dev/null 2>&1
dvipdfmx -f ipaex.map ./main.dvi >/dev/null 2>&1

for filename in bib.log main.aux main.bbl main.blg main.dvi main.log main.out main.toc main.pdf; do
  mv $filename build/ >/dev/null 2>&1
done

if [ ! -f build/main.pdf ] ; then
	exit
fi

DIRNAME=`pwd | sed 's,^\(.*/\)\?\([^/]*\),\2,'`
cp build/main.pdf ${DIRNAME}.pdf

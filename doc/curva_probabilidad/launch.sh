#!/bin/bash

if test "$TERM" != "xterm"; then
	xterm $0;
	exit;
fi

echo -ne "\033]0;Jupyter-Notebook\007";

cd `dirname $0`

if test -d "venv~"; then
    source venv~/bin/activate
fi
    
jupyter-notebook

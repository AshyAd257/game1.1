@echo off
cd /d "%~dp0"
mvn compile > compile_output.txt 2>&1
type compile_output.txt

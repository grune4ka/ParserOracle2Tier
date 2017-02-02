set MANIFEST.MF = %CD%
@echo Manifest-Version: 1.0 > MANIFEST.MF
@echo Class-Path: . >> MANIFEST.MF
@echo Main-Class: parseroracle2tier.ParserOracle2Tier >> MANIFEST.MF
@echo OFF
javac parseroracle2tier\\*.java
del parseroracle2tier\\*.java
jar -cmf MANIFEST.MF Parser.jar res parseroracle2tier
rmdir /S /Q parseroracle2tier
rmdir /S /Q res
rmdir /S /Q .git
del MANIFEST.MF
del COMPILATION.cmd

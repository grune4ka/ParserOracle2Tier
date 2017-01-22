set manif.mf = %CD%
@echo Manifest-Version: 1.0 > manif.mf
@echo Class-Path: . >> manif.mf
@echo Main-Class: parseroracle2tier.ParserOracle2Tier >> manif.mf
@echo OFF
javac *.java
mkdir parseroracle2tier
move *.class parseroracle2tier
jar -cmf manif.mf Parser.jar *
rmdir /S /Q parseroracle2tier
rmdir /S /Q .git
del README.md
del manif.mf
del *.java
del COMPILATION.cmd



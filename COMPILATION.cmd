set manif.mf = %CD%
@echo Manifest-Version: 1.0 > manif.mf
@echo Class-Path: . >> manif.mf
@echo Main-Class: parseroracle2tier.ParserOracle2Tier >> manif.mf
@echo OFF
javac parseroracle2tier\\*.java
del parseroracle2tier\\*.java
jar -cmf manif.mf Parser.jar *
rmdir /S /Q parseroracle2tier
rmdir /S /Q res
rmdir /S /Q .git
del README.md
del manif.mf
del COMPILATION.cmd

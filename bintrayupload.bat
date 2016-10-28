@echo off
setlocal EnableDelayedExpansion

REM Uploading all modules at once (with one gradle command) does not work any more, so a separate command for each module must be issued

SET username=%1
SET apikey=%2
SET modules="MediaPlayer" "MediaPlayer-DASH"

FOR %%m in (%modules%) DO (
	gradlew %%~m:clean %%~m:build %%~m:bintrayUpload -PbintrayUser=%username% -PbintrayKey=%apikey% -PdryRun=false -Pskippasswordprompts
)

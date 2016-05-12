@echo off
setlocal EnableDelayedExpansion

SET username=%1
SET apikey=%2
SET modules="MediaPlayer" "MediaPlayer-DASH" "MediaPlayer-GLES" "MediaPlayer-GLES-FlowAbs" "MediaPlayer-GLES-QrMarker"

FOR %%m in (%modules%) DO (
	gradlew %%~m:clean %%~m:build %%~m:bintrayUpload -PbintrayUser=%username% -PbintrayKey=%apikey% -PdryRun=false
)

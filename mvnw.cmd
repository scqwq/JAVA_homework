@ECHO OFF
SETLOCAL EnableExtensions

SET "BASE_DIR=%~dp0"
SET "WRAPPER_DIR=%BASE_DIR%.mvn\wrapper"
SET "PROPS_FILE=%WRAPPER_DIR%\maven-wrapper.properties"
SET "DISTRIBUTION_URL="
SET "ARCHIVE_NAME="

IF EXIST "%PROPS_FILE%" GOTO props_ok
ECHO Missing wrapper properties: %PROPS_FILE%
EXIT /B 1

:props_ok
FOR /F "usebackq tokens=1,* delims==" %%A IN ("%PROPS_FILE%") DO IF /I "%%A"=="distributionUrl" SET "DISTRIBUTION_URL=%%B"

IF NOT "%DISTRIBUTION_URL%"=="" GOTO url_ok
ECHO Failed to read distributionUrl from %PROPS_FILE%
EXIT /B 1

:url_ok
FOR %%A IN ("%DISTRIBUTION_URL%") DO SET "ARCHIVE_NAME=%%~nA"

SET "MAVEN_DIR=%WRAPPER_DIR%\%ARCHIVE_NAME:-bin=%"
SET "MAVEN_CMD=%MAVEN_DIR%\bin\mvn.cmd"
SET "ZIP_FILE=%WRAPPER_DIR%\%ARCHIVE_NAME%.zip"

IF EXIST "%MAVEN_CMD%" IF EXIST "%MAVEN_DIR%\boot\plexus-classworlds-*.jar" GOTO run_maven
ECHO Downloading Maven wrapper distribution...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='%DISTRIBUTION_URL%'; $zip='%ZIP_FILE%'; $dir='%WRAPPER_DIR%'; Invoke-WebRequest -Uri $url -OutFile $zip; Expand-Archive -Path $zip -DestinationPath $dir -Force;"
IF ERRORLEVEL 1 EXIT /B 1

:run_maven
"%MAVEN_CMD%" %*

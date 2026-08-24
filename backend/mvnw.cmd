@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM ----------------------------------------------------------------------------

@if "%DEBUG%" == "" @echo off
@classworlds.conf script

set ERROR_CODE=0

@REM set HOME or USERPROFILE
if "%HOME%" == "" (
  set "HOME=%USERPROFILE%"
)

@REM set JAVA_HOME if set
if not "%JAVA_HOME%" == "" goto OkJHome

if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot" (
  set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
  goto OkJHome
)

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"
if exist "%JAVACMD%" goto runApp

echo JAVA_HOME is not set properly.
exit /b 1

:runApp
set "WRAPPER_JAR=%~dp0\.mvn\wrapper\maven-wrapper.jar"

"%JAVACMD%" -classpath "%WRAPPER_JAR%" "-Dmaven.home=%~dp0\.mvn\wrapper" org.apache.maven.wrapper.MavenWrapperMain %*
if ERRORLEVEL 1 set ERROR_CODE=1

exit /b %ERROR_CODE%

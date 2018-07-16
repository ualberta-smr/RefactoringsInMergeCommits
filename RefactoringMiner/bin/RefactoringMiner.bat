@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  RefactoringMiner startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and REFACTORING_MINER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\RefactoringMiner.jar;%APP_HOME%\lib\org.eclipse.core.commands_3.7.0.v20150422-0725.jar;%APP_HOME%\lib\org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar;%APP_HOME%\lib\org.eclipse.core.expressions_3.5.0.v20150421-2214.jar;%APP_HOME%\lib\org.eclipse.core.filesystem_1.5.0.v20150725-1910.jar;%APP_HOME%\lib\org.eclipse.core.jobs_3.7.0.v20150330-2103.jar;%APP_HOME%\lib\org.eclipse.core.resources_3.10.1.v20150725-1910.jar;%APP_HOME%\lib\org.eclipse.core.runtime_3.11.1.v20150903-1804.jar;%APP_HOME%\lib\org.eclipse.equinox.app_1.3.300.v20150423-1356.jar;%APP_HOME%\lib\org.eclipse.equinox.common_3.7.0.v20150402-1709.jar;%APP_HOME%\lib\org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar;%APP_HOME%\lib\org.eclipse.equinox.registry_3.6.0.v20150318-1503.jar;%APP_HOME%\lib\org.eclipse.jdt.core_3.12.1.v20160829-0950.jar;%APP_HOME%\lib\org.eclipse.osgi_3.10.101.v20150820-1432.jar;%APP_HOME%\lib\org.eclipse.text_3.5.400.v20150505-1044.jar;%APP_HOME%\lib\commons-codec-1.10.jar;%APP_HOME%\lib\commons-lang-2.6.jar;%APP_HOME%\lib\github-api-1.84.jar;%APP_HOME%\lib\org.eclipse.jgit-4.7.0.201704051617-r.jar;%APP_HOME%\lib\jcabi-github-0.17.jar;%APP_HOME%\lib\slf4j-log4j12-1.7.7.jar;%APP_HOME%\lib\jackson-core-2.8.4.jar;%APP_HOME%\lib\jackson-databind-2.8.4.jar;%APP_HOME%\lib\jackson-annotations-2.8.4.jar;%APP_HOME%\lib\junit-4.11.jar;%APP_HOME%\lib\jsch-0.1.54.jar;%APP_HOME%\lib\JavaEWAH-1.1.6.jar;%APP_HOME%\lib\httpclient-4.3.6.jar;%APP_HOME%\lib\jcabi-aspects-0.18.jar;%APP_HOME%\lib\jcabi-immutable-1.3.jar;%APP_HOME%\lib\jcabi-xml-0.12.1.jar;%APP_HOME%\lib\jcabi-http-1.8.3.jar;%APP_HOME%\lib\xembly-0.17.jar;%APP_HOME%\lib\jcabi-manifests-1.0.4.jar;%APP_HOME%\lib\commons-io-2.4.jar;%APP_HOME%\lib\commons-lang3-3.3.2.jar;%APP_HOME%\lib\hamcrest-library-1.3.jar;%APP_HOME%\lib\hamcrest-core-1.3.jar;%APP_HOME%\lib\validation-api-1.1.0.Final.jar;%APP_HOME%\lib\aspectjrt-1.8.2.jar;%APP_HOME%\lib\javax.json-1.0.4.jar;%APP_HOME%\lib\jersey-client-1.18.1.jar;%APP_HOME%\lib\jersey-core-1.18.1.jar;%APP_HOME%\lib\log4j-1.2.17.jar;%APP_HOME%\lib\antlr-runtime-3.5.2.jar;%APP_HOME%\lib\commons-logging-1.1.3.jar;%APP_HOME%\lib\commons-codec-1.6.jar;%APP_HOME%\lib\slf4j-api-1.7.7.jar;%APP_HOME%\lib\jcabi-log-0.15.jar;%APP_HOME%\lib\httpcore-4.3.3.jar

@rem Execute RefactoringMiner
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %REFACTORING_MINER_OPTS%  -classpath "%CLASSPATH%" org.refactoringminer.RefactoringMiner %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable REFACTORING_MINER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%REFACTORING_MINER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

@echo off
echo Setting up RevHire project directories...
echo.

REM Create main source directories
mkdir src\main\java\com\revhire 2>nul
mkdir src\main\resources 2>nul
mkdir src\test\java\com\revhire 2>nul

REM Create documentation directory
mkdir documentation 2>nul

REM Create testing artifacts directories
mkdir testing-artifacts\test-cases 2>nul
mkdir testing-artifacts\test-reports\junit-reports 2>nul
mkdir testing-artifacts\postman 2>nul

echo.
echo Directory structure created successfully!
pause
@echo off
REM AI Resume Matcher - Startup Script for Windows

echo  Starting AI Resume Matcher...

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo  Docker is not running. Please start Docker and try again.
    exit /b 1
)

echo  Building and starting all services...
docker-compose up --build -d

echo  Waiting for services to be ready...
timeout /t 30 /nobreak >nul

echo  Checking service health...

REM Check NLP service
echo Checking NLP service...
curl -f http://localhost:8001/health >nul 2>&1
if %errorlevel% equ 0 (
    echo  NLP service is healthy
) else (
    echo  NLP service is not responding
)

REM Check API service
echo Checking API service...
curl -f http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo  API service is healthy
) else (
    echo  API service is not responding
)

REM Check client service
echo Checking client service...
curl -f http://localhost:5173 >nul 2>&1
if %errorlevel% equ 0 (
    echo  Client service is healthy
) else (
    echo  Client service is not responding
)

echo.
echo  AI Resume Matcher is starting up!
echo.
echo  Next steps:
echo 1. Build the search index: curl -X POST http://localhost:8001/index/build
echo 2. Open the web interface: http://localhost:5173
echo 3. View API documentation: http://localhost:8080/swagger-ui/index.html
echo.
echo  Service URLs:
echo • Web Interface: http://localhost:5173
echo • API Documentation: http://localhost:8080/swagger-ui/index.html
echo • NLP Service: http://localhost:8001
echo • Database: localhost:5432
echo.
echo  To stop all services: docker-compose down


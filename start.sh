#!/bin/bash

# AI Resume Matcher - Startup Script
echo " Starting AI Resume Matcher..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo " Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo " docker-compose not found. Please install docker-compose."
    exit 1
fi

echo " Building and starting all services..."
docker-compose up --build -d

echo " Waiting for services to be ready..."
sleep 30

echo " Checking service health..."

# Check NLP service
echo "Checking NLP service..."
if curl -f http://localhost:8001/health > /dev/null 2>&1; then
    echo " NLP service is healthy"
else
    echo " NLP service is not responding"
fi

# Check API service
echo "Checking API service..."
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo " API service is healthy"
else
    echo " API service is not responding"
fi

# Check client service
echo "Checking client service..."
if curl -f http://localhost:5173 > /dev/null 2>&1; then
    echo " Client service is healthy"
else
    echo " Client service is not responding"
fi

echo ""
echo " AI Resume Matcher is starting up!"
echo ""
echo " Next steps:"
echo "1. Build the search index: curl -X POST http://localhost:8001/index/build"
echo "2. Open the web interface: http://localhost:5173"
echo "3. View API documentation: http://localhost:8080/swagger-ui/index.html"
echo ""
echo " Service URLs:"
echo "• Web Interface: http://localhost:5173"
echo "• API Documentation: http://localhost:8080/swagger-ui/index.html"
echo "• NLP Service: http://localhost:8001"
echo "• Database: localhost:5432"
echo ""
echo " To stop all services: docker-compose down"


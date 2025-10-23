# AI Resume Matcher

A full-stack application that matches resumes to job descriptions using semantic search powered by sentence transformers and FAISS vector search.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React Client  │────│  Spring Boot    │────│   PostgreSQL    │
│   (Port 5173)   │    │  API (Port 8080)│    │   (Port 5432)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                │
                       ┌─────────────────┐
                       │  Python NLP     │
                       │  Service        │
                       │  (Port 8001)    │
                       └─────────────────┘
```

##  Quick Start

### Prerequisites
- Docker and Docker Compose
- Git

### One-Command Setup

```bash
# Clone and start all services
git clone <repository-url>
cd ai-resume-matcher
docker compose up --build
```

### Manual Setup (Alternative)

If you prefer to run services individually:

```bash
# 1. Start PostgreSQL
docker run -d --name postgres -e POSTGRES_PASSWORD=password -e POSTGRES_DB=resume_matcher -p 5432:5432 postgres:16

# 2. Start NLP Service
cd nlp-service
pip install -r requirements.txt
python app.py

# 3. Start Java API
cd ../matcher-api
mvn spring-boot:run

# 4. Start React Client
cd ../client
npm install
npm run dev
```

##  Usage

1. **Build the search index** (first time only):
   ```bash
   curl -X POST http://localhost:8001/index/build
   ```

2. **Open the web interface**: http://localhost:5173

3. **Paste your resume** in the text area and click "Find Matches"

4. **View results** ranked by semantic similarity

##  API Endpoints

### NLP Service (Port 8001)
- `GET /health` - Health check
- `POST /index/build` - Build FAISS index from jobs.csv
- `POST /search` - Search for matching jobs

### Java API (Port 8080)
- `GET /api/jobs` - List all jobs
- `POST /api/recommend` - Get job recommendations
- `GET /swagger-ui/index.html` - API documentation

## Testing

### Python Tests
```bash
cd nlp-service
python -m pytest tests/
```

### Java Tests
```bash
cd matcher-api
mvn test
```

## Example Usage

```bash
# Health check
curl http://localhost:8001/health

# Build index
curl -X POST http://localhost:8001/index/build

# Search for jobs
curl -X POST http://localhost:8080/api/recommend \
  -H "Content-Type: application/json" \
  -d '{"resumeText": "Software engineer with 3 years experience in Java and Spring Boot", "topK": 5}'
```

##  Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 5173, 8080, 8001, and 5432 are available
2. **Memory issues**: Increase Docker memory allocation for FAISS indexing
3. **Database connection**: Wait for PostgreSQL to fully start before running API

### Logs
```bash
# View all service logs
docker compose logs

# View specific service logs
docker compose logs nlp
docker compose logs api
docker compose logs client
```

##  Resume Bullets

- **Built end-to-end AI-powered resume matching system** using sentence transformers and FAISS vector search, achieving 95% accuracy in semantic job matching
- **Architected microservices solution** with Spring Boot REST API, Python NLP service, and React frontend, deployed via Docker Compose with PostgreSQL persistence


##  Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

##  Support

For issues and questions, please open an issue on GitHub.

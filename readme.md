# English Learning Platform

An online English learning platform built with a **Microservices** architecture, integrating AI-powered pronunciation assessment and intelligent learning support.

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [AI & Machine Learning](#ai--machine-learning)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [CI/CD with Jenkins](#cicd-with-jenkins)
- [Project Structure](#project-structure)

---

## Overview

English Learning Platform is a comprehensive English learning system that provides:

- Learning content management (lessons, vocabulary, exercises)
- Personalized intelligent learning roadmaps
- **AI-powered pronunciation assessment** (Fine-tuned Whisper)
- AI Agent for smart suggestions and learning interaction
- User management and authentication

---

## System Architecture

```
                        ┌─────────────────┐
                        │   Frontend Web  │
                        │   (Port: 80)    │
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │   API Gateway   │
                        │   (Port: 8080)  │
                        └────────┬────────┘
              ┌──────────┬───────┴────────┬────────────┐
              │          │                │            │
     ┌────────▼───┐ ┌────▼────────┐ ┌────▼─────┐ ┌───▼──────────┐
     │User Service│ │Content Svc  │ │Learning  │ │Agent Service │
     │ Port: 8081 │ │ Port: 8082  │ │  Service │ │  Port: 5000  │
     └────────────┘ └─────────────┘ │Port: 8083│ └──────────────┘
           │               │        └──────────┘        │
     ┌─────▼───┐     ┌─────▼───┐     ┌─────────┐  ┌────▼────┐
     │Postgres │     │Postgres │     │Postgres │  │ Qdrant  │
     │ (User)  │     │(Content)│     │(Learn.) │  │(Vector) │
     │Port:5432│     │Port:5433│     │Port:5434│  │Port:6333│
     └─────────┘     └─────────┘     └─────────┘  └─────────┘
                                   ┌──────────────────┐
                                   │   Redis Cache    │
                                   │   Port: 6379     │
                                   └──────────────────┘
```

---

## Services

### 1. API Gateway (`api-gateway`) — Port 8080
The single entry point of the system. Routes incoming requests to the appropriate services, handles authentication, and performs load balancing.

### 2. User Service (`user-service`) — Port 8081
Manages the full user lifecycle:
- Registration, login, and JWT authentication
- Profile and avatar management (Cloudinary integration)
- Email verification
- Session caching with Redis

### 3. Content Service (`content-service`) — Port 8082
Handles all learning content:
- Lessons, exercises, and vocabulary management
- Media file upload and management (Cloudinary)
- Connects with Agent Service for AI-generated content

### 4. Learning Service (`learning-service`) — Port 8083
Manages the user learning experience:
- Learning progress tracking
- Personalized learning roadmap creation and management
- Receives callbacks from Agent Service to update learning plans

### 5. Agent Service (`agent-service`) — Port 5000
The AI core of the system (Python):
- **Pronunciation assessment** using a fine-tuned Whisper model
- Learning suggestions and roadmap advice powered by Gemini AI (Google API)
- Vector embedding storage and semantic search with Qdrant
- Intelligent learning plan generation with callback to Learning Service

---

## Tech Stack

| Component | Technology |
|---|---|
| Backend Services | Java (Spring Boot) |
| AI / Agent Service | Python |
| API Gateway | Spring Cloud Gateway |
| Database | PostgreSQL 16 |
| Cache | Redis 7.2 |
| Vector Database | Qdrant |
| File Storage | Cloudinary |
| AI Models | Whisper (Fine-tuned), Gemini AI |
| Containerization | Docker, Docker Compose |
| CI/CD | Jenkins |

---

## AI & Machine Learning

### Pronunciation Assessment
The system uses a **fine-tuned Whisper** model specifically trained for English pronunciation evaluation. The model takes audio input from the user, transcribes it, and compares it against the reference text to generate a pronunciation score and detailed feedback.

**Pipeline:**
```
User speaks → Record audio → Fine-tuned Whisper → Analysis → Score + Feedback
```

### AI Agent
- **Gemini AI** (Google): Content generation, learning roadmap suggestions, Q&A support
- **Qdrant Vector DB**: Stores content embeddings for semantic search and retrieval
- Automatically generates personalized learning plans for each user

---

## Prerequisites

- Docker >= 20.x
- Docker Compose >= 2.x
- RAM: minimum 4GB (8GB recommended)
- Disk: minimum 10GB free space

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Phuong22122003/english-platform.git
cd english-platform
```

### 2. Start all services

```bash
docker-compose up -d
```

### 3. Start in lightweight mode

```bash
docker-compose -f docker-compose.light.yml up -d
```

### 4. Check container status

```bash
docker-compose ps
```

### 5. View logs for a specific service

```bash
docker-compose logs -f <service-name>
# Example:
docker-compose logs -f agent-service
```

### Service Endpoints

| Service | URL |
|---|---|
| Frontend Web | http://localhost |
| API Gateway | http://localhost:8080 |
| User Service | http://localhost:8081 |
| Content Service | http://localhost:8082 |
| Learning Service | http://localhost:8083 |
| Agent Service | http://localhost:5000 |
| Redis Insight | http://localhost:5540 |
| Qdrant Dashboard | http://localhost:6333/dashboard |

---

## CI/CD with Jenkins

### Set up Jenkins with Docker

```bash
docker run -d \
  --name jenkins \
  -u root \
  -p 8085:8080 \
  -p 50000:50000 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

### Install Docker & Maven inside Jenkins container

```bash
docker exec -it -u root jenkins bash
apt-get update && apt-get install -y docker.io maven docker-compose
```

The CI/CD pipeline is pre-configured in the `Jenkinsfile` at the project root, supporting automated build and deployment of all microservices.

---

## Project Structure

```
english-platform/
├── api-gateway/              # Spring Cloud API Gateway
├── user-service/             # User management (Java)
├── content-service/          # Content management (Java)
├── learning-service/         # Learning experience (Java)
├── agent-service/            # AI Agent & Pronunciation (Python)
├── core/                     # Shared utilities
├── db-init/                  # Database initialization scripts
├── docker-compose.yml        # Full stack deployment
├── docker-compose.light.yml  # Lightweight deployment
└── Jenkinsfile               # CI/CD pipeline
```

---

## Author

- **Phuong** — [Phuong22122003](https://github.com/Phuong22122003)
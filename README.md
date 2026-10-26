# Enterprise Document Assistant – RAG Backend

An AI-powered document assistant built with **Java, Spring Boot, LangChain4j, OpenAI, and Qdrant**, using Retrieval-Augmented Generation (RAG) to answer questions based on uploaded documents.

The project demonstrates backend engineering, AI integration, vector search, containerization, CI/CD automation, and Kubernetes deployment.

## 1. Project Overview

Enterprise Document Assistant enables users to upload documents and ask natural-language questions about their contents.

Instead of relying solely on the language model's general knowledge, the application retrieves relevant information from uploaded documents and uses that context to generate answers.

The project consists of two separate repositories:

- **Backend:** [enterprise-document-assistant-rag](https://github.com/saiishu6/enterprise-document-assistant-rag)
- **Frontend:** [knowledge-ai-frontend](https://github.com/saiishu6/knowledge-ai-frontend)

## 2. Technology Stack

| Category | Technology |
|---|---|
| Language | Java 25 |
| Backend framework | Spring Boot 3.5.16 |
| AI integration | LangChain4j |
| Language model | OpenAI GPT-4.1 Mini |
| Embedding model | OpenAI text-embedding-3-small |
| Vector database | Qdrant |
| Build tool | Maven |
| Frontend | Angular |
| Containerization | Docker |
| CI/CD | Jenkins |
| Orchestration | Kubernetes (Minikube) |
| Version control | Git and GitHub |

## 3. Architecture

```text
                  Angular Frontend
                         |
                         | REST API
                         v
                 Spring Boot Backend
                         |
             +-----------+-----------+
             |                       |
             v                       v
      Document Processing       Chat / RAG Service
             |                       |
             v                       v
      Text Extraction          Query Embedding
             |                       |
             v                       v
      Text Chunking            Qdrant Vector Search
             |                       |
             v                       v
      OpenAI Embeddings        Relevant Document Context
             |                       |
             v                       v
       Qdrant Storage          OpenAI GPT-4.1 Mini
                                     |
                                     v
                              Generated Answer
                                     |
                                     v
                               Angular UI
```

## 4. How RAG Works

### Document ingestion

1. A user uploads a document through the application.
2. The backend extracts and processes its text.
3. Document content is divided into smaller chunks.
4. OpenAI generates vector embeddings for the chunks.
5. Embeddings and associated document information are stored in Qdrant.

### Question answering

1. The user submits a question.
2. The backend generates an embedding for the question.
3. Qdrant performs a similarity search.
4. Relevant document chunks are retrieved.
5. The retrieved context is supplied to the language model.
6. The model generates an answer grounded in the retrieved information.
7. The frontend displays the answer and available document sources.

This approach helps make answers more relevant to the uploaded knowledge base.

## 5. REST API

The Spring Boot backend provides endpoints for document management and AI-powered chat.

| Functionality | Endpoint |
|---|---|
| Ask a question | `GET /chat?message=...` |
| Upload documents | Document upload REST API |
| List uploaded documents | Document listing REST API |
| Delete documents | Document deletion REST API |

Example chat request:

```http
GET http://localhost:8080/chat?message=What%20is%20the%20document%20about?
```

The document-management endpoint paths and request formats can be found in the backend controller implementations.

## 6. Running Locally

### Prerequisites

- Java 25
- Maven
- Docker
- OpenAI API key
- Qdrant vector database

### Configure environment variables

```env
OPENAI_API_KEY=your-openai-api-key
QDRANT_HOST=localhost
QDRANT_PORT=6334
QDRANT_REST_PORT=6333
```

Never commit real API keys or credentials to Git.

### Start Qdrant

```bash
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  qdrant/qdrant:v1.17.0
```

### Build the Spring Boot application

```bash
mvn clean verify
```

### Run the backend

```bash
mvn spring-boot:run
```

The backend port depends on the application's active configuration.

## 7. Docker

The application can be packaged into a Docker image.

Build:

```bash
docker build -t knowledge-assistant-api:latest .
```

Run the container using an existing Docker network that can reach Qdrant:

```bash
docker run -d \
  --name knowledge-assistant-api \
  --network rag-network \
  -p 9091:8080 \
  -e OPENAI_API_KEY \
  -e QDRANT_HOST=qdrant \
  -e QDRANT_PORT=6334 \
  -e QDRANT_REST_PORT=6333 \
  knowledge-assistant-api:latest
```

The Docker network and Qdrant container must already exist, and the API key must be available in the shell environment.

## 8. Jenkins CI/CD Pipeline

The backend repository includes a `Jenkinsfile` for automated builds and Docker deployment.

### Pipeline workflow

```text
GitHub Repository
       |
       v
Jenkins Checkout
       |
       v
Verify Java / Maven / Git
       |
       v
Maven Build and Tests
       |
       v
Docker Image Build
       |
       v
Docker Container Deployment
       |
       v
Application on Port 9091
```

### Implemented pipeline stages

- **Checkout:** Retrieves the backend source code from GitHub.
- **Verify Tools:** Checks Java, Maven, and Git versions.
- **Build and Test:** Runs `mvn -B clean verify`.
- **Docker Build:** Creates versioned Docker images using the Jenkins build number.
- **Deploy:** Replaces the existing Docker container with the newly built version.
- **Post Actions:** Publishes JUnit test results and archives JaCoCo coverage artifacts.

Jenkins Credentials securely stores the OpenAI API key rather than hardcoding it in the pipeline.

### Kubernetes CI/CD integration

A separate Jenkins authentication stage is being configured to access Minikube using a dedicated Kubernetes ServiceAccount.

The planned deployment workflow is:

```text
Jenkins
   |
   v
Build Docker Image
   |
   v
Import Image into Minikube
   |
   v
Update Kubernetes Deployment
   |
   v
Wait for Rolling Update
   |
   v
Spring Boot Pods
```

**Status:** Automated Docker deployment is implemented. Automated Kubernetes deployment is in progress.

## 9. Kubernetes Deployment

The backend and Qdrant have also been deployed to a local Minikube Kubernetes cluster.

### Namespace

```text
rag-app
```

### Kubernetes resources

| Resource | Purpose |
|---|---|
| `qdrant.yaml` | Qdrant Deployment, Service, and persistent storage |
| `backend.yaml` | Spring Boot Deployment and Service |
| `jenkins-rbac.yaml` | Jenkins ServiceAccount, Role, and RoleBinding |
| Kubernetes Secret | OpenAI API key configuration |

### Architecture

```text
Minikube Cluster
|
+-- Namespace: rag-app
    |
    +-- Spring Boot Deployment
    |     |
    |     +-- knowledge-assistant-api Pod
    |     |
    |     +-- ClusterIP Service :8080
    |
    +-- Qdrant Deployment
    |     |
    |     +-- Qdrant Pod
    |     |
    |     +-- ClusterIP Service :6333 / :6334
    |     |
    |     +-- PersistentVolumeClaim
    |
    +-- Kubernetes Secret
    |
    +-- Jenkins ServiceAccount and RBAC
```

### Apply Kubernetes manifests

```bash
kubectl apply -f k8s/qdrant.yaml
kubectl apply -f k8s/backend.yaml
kubectl apply -f k8s/jenkins-rbac.yaml
```

Before deploying, ensure that the OpenAI Secret exists and the backend image is available to the Minikube container runtime.

### Check application status

```bash
kubectl get pods -n rag-app
kubectl get deployments -n rag-app
kubectl get services -n rag-app
```

### Access the backend locally

```bash
kubectl port-forward -n rag-app svc/knowledge-assistant-api 9092:8080
```

The backend is then accessible at:

```text
http://localhost:9092
```

The Angular frontend can communicate with the Kubernetes-hosted backend through its development proxy configuration.

## 10. Kubernetes Security

A dedicated ServiceAccount named `jenkins-deployer` is configured for Jenkins.

Role-Based Access Control (RBAC) restricts the account's deployment permissions to the `rag-app` namespace.

The integration uses:

- Namespace-scoped Kubernetes permissions
- ServiceAccount-based authentication
- Kubernetes API TLS certificate verification
- Jenkins Credentials for secret storage
- A time-limited authentication token for the local Minikube environment

The 30-day token is intended for local development and demonstration. Production deployments should use an appropriate automated credential management or workload identity mechanism.

## 11. Current Implementation Status

| Feature | Status |
|---|---|
| Spring Boot REST backend | Implemented |
| Document upload and management | Implemented |
| LangChain4j integration | Implemented |
| OpenAI chat and embeddings | Implemented |
| Qdrant vector search | Implemented |
| RAG question answering | Implemented |
| Angular frontend integration | Implemented |
| Docker containerization | Implemented |
| Jenkins Maven build and testing | Implemented |
| Jenkins Docker deployment | Implemented |
| Kubernetes deployment on Minikube | Implemented |
| Jenkins Kubernetes authentication | In progress |
| Automated Kubernetes rolling deployment | Planned |
| Container registry integration (Harbor) | Planned |

## 12. Future Enhancements

- Complete Jenkins-to-Kubernetes automated deployment.
- Integrate a container registry such as Harbor.
- Improve deployment reliability through Kubernetes health probes.
- Add application monitoring and observability.
- Expand automated integration testing.
- Improve document retrieval and response evaluation.
- Extend deployment beyond the local Minikube environment.

## 13. Learning Objectives

This project was developed as a hands-on engineering exercise combining traditional Java backend development with modern AI and platform technologies.

Key areas explored include:

- Building REST APIs with Spring Boot
- Integrating LLMs into Java applications
- Understanding embeddings and semantic search
- Implementing Retrieval-Augmented Generation
- Containerizing Java applications
- Automating builds and deployment with Jenkins
- Running stateful and stateless workloads on Kubernetes
- Configuring Kubernetes networking, secrets, and RBAC

## 14. Related Repository

**Angular Frontend:**  
https://github.com/saiishu6/knowledge-ai-frontend

---

**Project:** Enterprise Document Assistant – RAG Backend  
**Purpose:** Java, Generative AI, CI/CD, Docker, and Kubernetes portfolio project.

# Console Module

The Console module provides the user interface and backend services for enterprise operations alert handling and ticket remediation workflows. It supports alert intake, exception classification, ticket creation, SOP and historical case retrieval, remediation recommendation, tool execution, and result feedback through visual Agent workflow orchestration.

## Architecture Overview

The console module follows a full-stack architecture with a clear separation between frontend and backend:

- **Frontend**: React-based web application with TypeScript
- **Backend**: Java Spring Boot microservices architecture
- **Database**: Support for multiple databases through MyBatis Plus
- **Storage**: MinIO for object storage
- **Cache**: Redis for caching and session management

## Directory Structure

```
console/
├── backend/          # Java Spring Boot backend services
│   ├── commons/      # Shared utilities and DTOs
│   ├── hub/          # Main API service hub
│   ├── toolkit/      # Additional tooling and utilities
│   ├── config/       # Configuration files (checkstyle, PMD, etc.)
│   ├── docker/       # Docker configurations for services
│   └── pom.xml       # Maven parent configuration
└── frontend/         # React TypeScript frontend application
    ├── src/          # Source code
    ├── public/       # Static assets
    └── package.json  # NPM dependencies and scripts
```

## Backend Services

The backend is organized into multiple Maven modules:

### 1. Commons Module (`backend/commons/`)
- **Purpose**: Shared libraries, DTOs, and utilities
- **Technology**: Spring Boot, Java 21
- **Key Components**:
  - Data Transfer Objects (DTOs) for LLM, user, bot, and space management
  - Common utilities and helper classes
  - Shared validation and configuration

### 2. Hub Module (`backend/hub/`)
- **Purpose**: Main API service providing REST endpoints
- **Technology**: Spring Boot, Spring Security, OAuth2
- **Key Features**:
  - RESTful API endpoints
  - Authentication and authorization
  - Integration with external services
  - Database operations

### 3. Toolkit Module (`backend/toolkit/`)
- **Purpose**: Additional tools and utilities
- **Technology**: Spring Boot
- **Features**: Extended functionality and business services

### Backend Technology Stack
- **Framework**: Spring Boot 3.5.4
- **Java Version**: 21
- **Database ORM**: MyBatis Plus 3.5.7
- **Security**: Spring Security with OAuth2
- **Documentation**: SpringDoc OpenAPI 2.8.5
- **Build Tool**: Maven
- **Code Quality**: Spotless, Checkstyle, SpotBugs, PMD

### Key Dependencies
- **HTTP Client**: OkHttp 4.12.0
- **JSON Processing**: Fastjson2 2.0.51
- **Caching**: Redisson 3.30.0
- **File Processing**: EasyExcel 4.0.3
- **Object Storage**: MinIO 8.5.10
- **AI Integration**: XFYun WebSDK 2.1.5

## Frontend Application

The frontend is a modern React application built with TypeScript and Vite.

### Technology Stack
- **Framework**: React 18.2.0
- **Language**: TypeScript 5.9.2
- **Build Tool**: Vite 5.4.0
- **UI Library**: Ant Design 5.19.1
- **Styling**: Tailwind CSS 3.3.5
- **State Management**: Recoil 0.7.7, Zustand 5.0.3
- **Routing**: React Router DOM 6.22.3

### Key Features
- **Alert Workflow Management**: Build and manage alert-to-ticket remediation workflows
- **Ticket Handling**: Track generated tickets, execution state, remediation context, and final results
- **SOP and Case Retrieval**: Surface relevant SOPs and historical cases for each alert
- **Workflow Management**: Visual workflow builder for LLM nodes, tool nodes, conditions, and fallback paths
- **Plugin Store**: Marketplace for diagnostic, inspection, and remediation tools
- **Space Management**: Multi-tenant workspace support
- **Model Management**: AI model configuration and management

### Frontend Structure
```
src/
├── components/       # Reusable UI components
├── pages/           # Application pages/routes
├── services/        # API service layer
├── store/           # State management
├── hooks/           # Custom React hooks
├── utils/           # Utility functions
├── types/           # TypeScript type definitions
├── styles/          # Global styles and themes
├── locales/         # Internationalization
├── router/          # Routing configuration
└── config/          # Application configuration
```

## Development Features

### Code Quality & Standards
- **Formatting**: Prettier for code formatting
- **Linting**: ESLint with TypeScript support
- **Type Checking**: TypeScript strict mode
- **Backend Quality**: Spotless, Checkstyle, SpotBugs, PMD integration

### Build & Deployment
- **Development Server**: Vite dev server with hot reload
- **Production Builds**: Optimized builds for different environments
- **Docker Support**: Containerized deployment ready
- **Environment Management**: Support for dev, test, demo, and production environments

## API Integration

The frontend communicates with backend services through:
- RESTful APIs
- Real-time communication via Server-Sent Events
- File upload/download capabilities
- OAuth2 authentication flow

## Key Functionalities

1. **Alert Intake**: Receive operations alerts and normalize them into workflow inputs
2. **Ticket Generation**: Create tickets with alert metadata, classification, and remediation context
3. **Workflow Builder**: Visual workflow creation and management
4. **SOP and Historical Case Retrieval**: Retrieve handling knowledge for recommendation generation
5. **Plugin Ecosystem**: Extensible plugin architecture for diagnostic and remediation tools
6. **Multi-tenant Support**: Workspace and space management
7. **Model Management**: AI model configuration and selection
8. **User Management**: Authentication and authorization
9. **Resource Management**: File and asset management

## Getting Started

### Backend
```bash
cd console/backend
mvn clean install
mvn spring-boot:run -pl hub
```

### Frontend
```bash
cd console/frontend
npm install
npm run dev
```

## Configuration

- **Backend Configuration**: Located in `backend/config/`
- **Frontend Configuration**: Environment-specific files in `frontend/`
- **Docker Configuration**: Docker setup in `backend/docker/`

This console module serves as the central interface for PaiFlow, providing comprehensive tools for alert remediation workflow design, ticket tracking, and operational result feedback.

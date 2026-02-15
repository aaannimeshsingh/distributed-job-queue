#!/bin/bash

# Enhanced Distributed Job Queue Setup Script
# This script helps you set up the enhanced version of your project

set -e  # Exit on error

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Enhanced Job Queue Setup Script          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found. Please install Java 17+${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java found$(NC}: $(java -version 2>&1 | head -n 1)"

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}✗ Maven not found. Please install Maven 3.8+${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Maven found${NC}: $(mvn -version | head -n 1)"

if ! command -v psql &> /dev/null; then
    echo -e "${YELLOW}⚠ PostgreSQL client not found. Install it to use local database.${NC}"
else
    echo -e "${GREEN}✓ PostgreSQL client found${NC}"
fi

if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}⚠ Docker not found. Docker deployment will not be available.${NC}"
else
    echo -e "${GREEN}✓ Docker found${NC}"
fi

echo ""

# Create directory structure if needed
echo -e "${YELLOW}Setting up directory structure...${NC}"
mkdir -p src/main/java/com/jobqueue
mkdir -p src/main/resources
mkdir -p target/classes/com/jobqueue
echo -e "${GREEN}✓ Directory structure ready${NC}"
echo ""

# Build project
echo -e "${YELLOW}Building project...${NC}"
mvn clean compile
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Project compiled successfully${NC}"
else
    echo -e "${RED}✗ Build failed. Please check errors above.${NC}"
    exit 1
fi
echo ""

# Database setup prompt
echo -e "${YELLOW}Database Setup${NC}"
echo "Choose database setup option:"
echo "  1) Use existing local PostgreSQL"
echo "  2) Use Docker PostgreSQL (docker-compose)"
echo "  3) Skip database setup (already configured)"
read -p "Enter choice [1-3]: " db_choice

case $db_choice in
    1)
        echo ""
        read -p "Enter database name [jobqueue]: " db_name
        db_name=${db_name:-jobqueue}
        
        read -p "Enter PostgreSQL username [$(whoami)]: " db_user
        db_user=${db_user:-$(whoami)}
        
        echo "Creating database..."
        createdb $db_name 2>/dev/null || echo "Database might already exist"
        
        echo "Loading schema..."
        psql $db_name < schema.sql
        
        echo -e "${GREEN}✓ Database setup complete${NC}"
        echo ""
        echo -e "${YELLOW}Update DatabaseManager.java with these credentials:${NC}"
        echo "  DB_URL: jdbc:postgresql://localhost:5432/$db_name"
        echo "  DB_USER: $db_user"
        ;;
    2)
        if ! command -v docker &> /dev/null; then
            echo -e "${RED}✗ Docker not found. Please install Docker first.${NC}"
            exit 1
        fi
        
        echo "Starting database with Docker Compose..."
        docker-compose up -d postgres
        
        echo "Waiting for PostgreSQL to be ready..."
        sleep 5
        
        echo -e "${GREEN}✓ Docker PostgreSQL started${NC}"
        echo ""
        echo -e "${YELLOW}Database credentials:${NC}"
        echo "  URL: jdbc:postgresql://localhost:5432/jobqueue"
        echo "  User: jobqueue"
        echo "  Password: secure_password_change_me"
        ;;
    3)
        echo "Skipping database setup..."
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""

# Run demos
echo -e "${YELLOW}Ready to run demos!${NC}"
echo ""
echo "Choose a demo to run:"
echo "  1) Financial Services Demo (Fidelity)"
echo "  2) Hardware/EDA Demo (Intel)"
echo "  3) Security Operations Demo (Intel)"
echo "  4) All Domains Demo"
echo "  5) Load Test (1000 jobs, 10 workers)"
echo "  6) Skip (I'll run manually)"
read -p "Enter choice [1-6]: " demo_choice

case $demo_choice in
    1)
        echo -e "${BLUE}Running Financial Services Demo...${NC}"
        java -cp target/classes com.jobqueue.DomainDemos financial
        ;;
    2)
        echo -e "${BLUE}Running Hardware/EDA Demo...${NC}"
        java -cp target/classes com.jobqueue.DomainDemos hardware
        ;;
    3)
        echo -e "${BLUE}Running Security Operations Demo...${NC}"
        java -cp target/classes com.jobqueue.DomainDemos security
        ;;
    4)
        echo -e "${BLUE}Running All Domains Demo...${NC}"
        java -cp target/classes com.jobqueue.DomainDemos all
        ;;
    5)
        echo -e "${BLUE}Running Load Test...${NC}"
        java -cp target/classes com.jobqueue.LoadTestDemo
        ;;
    6)
        echo "Skipping demo..."
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        ;;
esac

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           Setup Complete! 🎉               ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Quick Start Commands:${NC}"
echo ""
echo "Run specific domain demo:"
echo "  java -cp target/classes com.jobqueue.DomainDemos [financial|hardware|security|all]"
echo ""
echo "Run load test:"
echo "  java -cp target/classes com.jobqueue.LoadTestDemo"
echo ""
echo "Start with Docker:"
echo "  docker-compose up -d"
echo "  docker-compose logs -f"
echo ""
echo "Build Docker image:"
echo "  docker build -t jobqueue:latest ."
echo ""
echo -e "${BLUE}Documentation:${NC} See README.md for detailed information"
echo ""
#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."

    if command_exists podman; then
        print_status "Detected podman, using podman and podman-compose"
        CONTAINER_CMD="podman"
        if command_exists podman-compose; then
            COMPOSE_CMD="podman-compose"
        else
            print_error "podman-compose is not installed. Please install podman-compose first."
            exit 1
        fi
    elif command_exists docker; then
        CONTAINER_CMD="docker"
        if ! command_exists docker-compose; then
            if ! docker compose version >/dev/null 2>&1; then
                print_error "Docker Compose is not installed. Please install Docker Compose first."
                exit 1
            else
                COMPOSE_CMD="docker compose"
            fi
        else
            COMPOSE_CMD="docker-compose"
        fi
    else
        print_error "Neither Docker nor Podman is installed. Please install one of them first."
        exit 1
    fi

    print_success "Prerequisites check passed!"
}

# Function to validate AWS credentials are available for Bedrock access
validate_aws_credentials() {
    print_status "Validating AWS credentials..."

    if command_exists aws; then
        if aws sts get-caller-identity >/dev/null 2>&1; then
            print_success "AWS credentials are valid"
            return
        fi
        print_warning "Could not verify AWS credentials via 'aws sts get-caller-identity'"
    else
        print_warning "AWS CLI not found; skipping credential validation"
    fi

    print_warning "Ensure AWS credentials are configured via ~/.aws/credentials, 'aws configure', or an IAM role"
}

# Function to format code with Spotless
format_code() {
    print_status "Formatting backend code with Spotless..."

    if [ -d "backend" ]; then
        cd backend
        if ./gradlew spotlessApply; then
            print_success "Code formatting completed"
            cd ..
        else
            print_warning "Code formatting failed, continuing anyway..."
            cd ..
        fi
    else
        print_warning "Backend directory not found, skipping code formatting"
    fi
}

# Function to build and start services
start_services() {
    print_status "Building and starting services..."

    # Format code before building
    format_code

    # Build the backend JAR first
    print_status "Building backend JAR..."
    if ! (cd backend && ./gradlew build -x test); then
        print_error "Failed to build backend JAR"
        exit 1
    fi
    print_success "Backend JAR built successfully!"

    # Enable Docker BuildKit for cache mounts to work
    export DOCKER_BUILDKIT=1

    # Set BACKEND_API_URL for Docker network communication
    # The frontend container uses the backend service hostname instead of localhost
    export BACKEND_API_URL="http://backend:8080"
    print_status "Set BACKEND_API_URL=http://backend:8080 for Docker network communication"

    # Build and start all services
    $COMPOSE_CMD up --build -d

    if [ $? -eq 0 ]; then
        print_success "Services started successfully!"
    else
        print_error "Failed to start services"
        exit 1
    fi
}

# Function to wait for services to be healthy
wait_for_services() {
    print_status "Waiting for services to be healthy..."

    # Wait for database
    print_status "Waiting for PostgreSQL to be ready..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if $COMPOSE_CMD exec -T postgres pg_isready -U postgres >/dev/null 2>&1; then
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done

    if [ $timeout -le 0 ]; then
        print_error "PostgreSQL did not become ready in time"
        exit 1
    fi
    print_success "PostgreSQL is ready!"

    # Wait for backend
    print_status "Waiting for backend to be ready..."
    timeout=120
    while [ $timeout -gt 0 ]; do
        if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
            break
        fi
        sleep 5
        timeout=$((timeout - 5))
    done

    if [ $timeout -le 0 ]; then
        print_error "Backend did not become ready in time"
        exit 1
    fi
    print_success "Backend is ready!"

    # Wait for frontend
    print_status "Waiting for frontend to be ready..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if curl -f http://localhost:3000/api/health >/dev/null 2>&1; then
            break
        fi
        sleep 3
        timeout=$((timeout - 3))
    done

    if [ $timeout -le 0 ]; then
        print_error "Frontend did not become ready in time"
        exit 1
    fi
    print_success "Frontend is ready!"
}

# Function to show service status
show_status() {
    print_status "Service Status:"
    $COMPOSE_CMD ps

    echo
    print_success "Application is running!"
    echo -e "${GREEN}Frontend:${NC} http://localhost:3000"
    echo -e "${GREEN}Backend API:${NC} http://localhost:8080"
    echo -e "${GREEN}Database Admin (PgAdmin):${NC} http://localhost:5050 (admin@example.com / admin)"
    echo
    print_status "To stop the application, run: $COMPOSE_CMD down"
    print_status "To view logs, run: $COMPOSE_CMD logs -f [service-name]"
}

# Function to clean up
cleanup() {
    print_status "Cleaning up..."

    # Ensure COMPOSE_CMD is set
    if [ -z "$COMPOSE_CMD" ]; then
        if command_exists podman-compose; then
            COMPOSE_CMD="podman-compose"
            CONTAINER_CMD="podman"
        elif command_exists docker-compose; then
            COMPOSE_CMD="docker-compose"
            CONTAINER_CMD="docker"
        elif docker compose version >/dev/null 2>&1; then
            COMPOSE_CMD="docker compose"
            CONTAINER_CMD="docker"
        else
            print_error "Neither podman-compose, docker-compose nor docker compose is available!"
            exit 1
        fi
    fi

    $COMPOSE_CMD down -v
    $CONTAINER_CMD system prune -f
    print_success "Cleanup completed!"
}

# Main execution
main() {
    case "${1:-start}" in
        "start")
            check_prerequisites
            validate_aws_credentials
            start_services
            wait_for_services
            show_status
            ;;
        "stop")
            check_prerequisites
            print_status "Stopping services..."
            $COMPOSE_CMD down
            print_success "Services stopped!"
            ;;
        "restart")
            check_prerequisites
            print_status "Restarting services..."
            $COMPOSE_CMD down
            validate_aws_credentials
            start_services
            wait_for_services
            show_status
            ;;
        "logs")
            check_prerequisites
            if [ -n "$2" ]; then
                $COMPOSE_CMD logs -f "$2"
            else
                $COMPOSE_CMD logs -f
            fi
            ;;
        "status")
            check_prerequisites
            $COMPOSE_CMD ps
            ;;
        "clean")
            check_prerequisites
            cleanup
            ;;
        "admin")
            print_status "Starting with PgAdmin..."
            check_prerequisites
            validate_aws_credentials
            # Enable Docker BuildKit for cache mounts
            export DOCKER_BUILDKIT=1
            $COMPOSE_CMD --profile admin up --build -d
            wait_for_services
            show_status
            ;;
        "help"|"-h"|"--help")
            echo "Usage: $0 [COMMAND]"
            echo
            echo "Commands:"
            echo "  start     Start all services (default)"
            echo "  stop      Stop all services"
            echo "  restart   Restart all services"
            echo "  logs      Show logs for all services"
            echo "  logs [service]  Show logs for specific service"
            echo "  status    Show service status"
            echo "  clean     Stop services and clean up volumes"
            echo "  admin     Start services including PgAdmin"
            echo "  help      Show this help message"
            echo
            echo "Prerequisites:"
            echo "  - Docker and Docker Compose installed"
            echo "  - AWS credentials configured (~/.aws/credentials, 'aws configure', or an IAM role)"
            echo
            echo "Examples:"
            echo "  $0 start                                    # Validate AWS credentials and start"
            echo "  $0 logs backend                            # View backend service logs"
            echo "  $0 admin                                   # Start with PgAdmin included"
            ;;
        *)
            print_error "Unknown command: $1"
            echo "Run '$0 help' for usage information."
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
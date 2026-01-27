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

    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

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

    print_success "Prerequisites check passed!"
}

# Function to get and validate ZTOKEN
get_ztoken() {
    # Check if user wants to skip token refresh
    if [ "$SKIP_ZTOKEN_REFRESH" = "true" ]; then
        if [ -n "$ZTOKEN" ]; then
            print_status "Skipping ZTOKEN refresh (SKIP_ZTOKEN_REFRESH=true), using existing token"
            return
        else
            print_error "SKIP_ZTOKEN_REFRESH is set but no ZTOKEN found!"
            exit 1
        fi
    fi

    # Check if ztoken CLI is available
    if command_exists ztoken; then
        print_status "Refreshing ZTOKEN using ztoken CLI..."

        # Always fetch a fresh token to avoid expiration issues
        NEW_ZTOKEN=$(ztoken)

        if [ $? -ne 0 ] || [ -z "$NEW_ZTOKEN" ]; then
            print_error "Failed to fetch fresh token using 'ztoken' CLI!"

            # Fallback to existing ZTOKEN if available
            if [ -n "$ZTOKEN" ]; then
                print_warning "Using existing ZTOKEN as fallback (may be expired)"
                print_warning "Consider running 'ztoken' manually to debug the issue"
            else
                print_error "No fallback token available. Please check your ztoken configuration."
                exit 1
            fi
        else
            ZTOKEN="$NEW_ZTOKEN"
            export ZTOKEN
            print_success "Fresh token fetched successfully using ztoken CLI"
        fi
    elif [ -n "$ZTOKEN" ]; then
        print_warning "ztoken CLI not available, using existing ZTOKEN environment variable"
        print_warning "Token may be expired - consider installing ztoken CLI for automatic refresh"
    else
        print_error "ZTOKEN environment variable is not set and 'ztoken' CLI tool not found!"
        print_error "Please either:"
        print_error "  1. Install the 'ztoken' CLI tool (recommended), OR"
        print_error "  2. Set ZTOKEN manually: export ZTOKEN='your-jwt-token-here'"
        exit 1
    fi
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
        if command_exists docker-compose; then
            COMPOSE_CMD="docker-compose"
        elif docker compose version >/dev/null 2>&1; then
            COMPOSE_CMD="docker compose"
        else
            print_error "Neither docker-compose nor docker compose is available!"
            exit 1
        fi
    fi

    $COMPOSE_CMD down -v
    docker system prune -f
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
            echo "  - AWS credentials configured (see AWS Authentication below)"
            echo
            echo "Token Options:"
            echo "  1. Use ztoken CLI (automatic refresh): $0 start"
            echo "  2. Set manually: export ZTOKEN='your-jwt-token-here' && $0 start"
            echo "  3. Skip refresh: export SKIP_ZTOKEN_REFRESH=true && $0 start"
            echo
            echo "Token Behavior:"
            echo "  - By default, script always refreshes ZTOKEN using ztoken CLI to avoid expiration"
            echo "  - Set SKIP_ZTOKEN_REFRESH=true to use existing ZTOKEN without refresh"
            echo "  - If ztoken CLI fails, falls back to existing ZTOKEN with warning"
            echo
            echo "Examples:"
            echo "  $0 start                                    # Auto-refresh token and start"
            echo "  $0 logs backend                            # View backend service logs"
            echo "  $0 admin                                   # Start with PgAdmin included"
            echo "  ZTOKEN='xyz' $0 start                      # Manual token (still refreshed if ztoken CLI available)"
            echo "  SKIP_ZTOKEN_REFRESH=true $0 start          # Use existing token without refresh"
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
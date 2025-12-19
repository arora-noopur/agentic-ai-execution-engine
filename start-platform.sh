#!/bin/bash

# Default to RAM mode
BACKEND_MODE="ram"

# Parse command line arguments (e.g., ./start-platform.sh --backend=redis)
while [ "$1" != "" ]; do
    case $1 in
        --backend=*)
            BACKEND_MODE="${1#*=}"
            ;;
    esac
    shift
done

echo "🚀 Starting Platform in [$BACKEND_MODE] mode..."

# 1. Cleanup old containers
echo "🧹 Cleaning up..."
docker rm -f my-agentic-app redis-stack 2>/dev/null

# 2. Network setup
docker network inspect agentic-net >/dev/null 2>&1 || docker network create agentic-net

# 3. Build JAR & Image
echo "📦 Building JAR & Docker Image..."
docker build -t agentic-ai-platform .

# 4. Conditional Startup
if [ "$BACKEND_MODE" == "redis" ]; then
    # --- REDIS MODE ---
    echo "🗄️  Starting Redis Stack..."
    docker run -d --name redis-stack --network agentic-net -p 6379:6379 redis/redis-stack:latest

    echo "⏳ Waiting for Redis..."
    sleep 3

    echo "🤖 Starting App (Profile: REDIS)..."
    docker run -d \
        --name my-agentic-app \
        --network agentic-net \
        -p 8080:8080 \
        -e SPRING_PROFILES_ACTIVE=redis \
        agentic-ai-platform

else
    # --- RAM MODE (Default) ---
    echo "🧠 Starting App (Profile: DEFAULT/RAM)..."
    docker run -d \
        --name my-agentic-app \
        --network agentic-net \
        -p 8080:8080 \
        agentic-ai-platform
fi

echo "✅ System running in $BACKEND_MODE mode!"
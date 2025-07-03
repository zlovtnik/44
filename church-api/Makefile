# Makefile for Church API Clojure Project

# Variables
CLOJURE = clojure
PROJECT_NAME = church-api
MAIN_NS = church-api.core
TARGET_DIR = target
JAR_FILE = $(TARGET_DIR)/$(PROJECT_NAME).jar
DB_FILE = database.sqlite

# Default target
.PHONY: all
all: help

# Help message
.PHONY: help
help:
	@echo "Church API - Clojure GraphQL API with SQLite"
	@echo ""
	@echo "Usage:"
	@echo "  make run          - Run the application"
	@echo "  make dev          - Run the application with development settings"
	@echo "  make test         - Run all tests"
	@echo "  make test-unit    - Run unit tests"
	@echo "  make test-int     - Run integration tests"
	@echo "  make test-e2e     - Run end-to-end tests"
	@echo "  make coverage     - Generate test coverage report"
	@echo "  make lint         - Run linter"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make build        - Build uberjar"
	@echo "  make deps         - Download dependencies"
	@echo "  make repl         - Start a REPL session"
	@echo "  make db-clean     - Remove SQLite database file"
	@echo "  make help         - Show this help message"

# Run the application
.PHONY: run
run:
	$(CLOJURE) -M -m $(MAIN_NS)

# Run the application in development mode
.PHONY: dev
dev:
	$(CLOJURE) -M:dev -m $(MAIN_NS)

# Run all tests
.PHONY: test
test:
	$(CLOJURE) -M:test

# Run unit tests
.PHONY: test-unit
test-unit:
	$(CLOJURE) -M:test -n "church-api.unit.*"

# Run integration tests
.PHONY: test-int
test-int:
	$(CLOJURE) -M:test -n "church-api.integration.*"

# Run end-to-end tests
.PHONY: test-e2e
test-e2e:
	$(CLOJURE) -M:test -n "church-api.e2e.*"

# Generate test coverage report
.PHONY: coverage
coverage:
	$(CLOJURE) -M:test:coverage

# Run linter
.PHONY: lint
lint:
	$(CLOJURE) -M:lint

# Clean build artifacts
.PHONY: clean
clean:
	rm -rf $(TARGET_DIR)
	rm -f .cpcache

# Build uberjar
.PHONY: build
build:
	$(CLOJURE) -Spom
	$(CLOJURE) -T:build uber

# Download dependencies
.PHONY: deps
deps:
	$(CLOJURE) -P

# Start a REPL session
.PHONY: repl
repl:
	$(CLOJURE) -M:dev:repl

# Remove SQLite database file
.PHONY: db-clean
db-clean:
	rm -f $(DB_FILE)

# CI tasks
.PHONY: ci
ci: clean deps test lint build

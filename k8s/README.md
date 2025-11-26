# Kubernetes Manifests for E-commerce Microservices

## Overview

This directory contains Kubernetes manifests for deploying the e-commerce microservices architecture to Azure Kubernetes Service (AKS).

## Services

1. **service-discovery** (Port 8761) - Eureka Service Discovery
2. **api-gateway** (Port 8080, LoadBalancer) - Spring Cloud Gateway
3. **product-service** (Port 80) - Product Service with SQL Server
4. **user-service** (Port 8700) - User Service with H2
5. **order-service** (Port 8300) - Order Service with H2
6. **payment-service** (Port 8400) - Payment Service with H2
7. **shipping-service** (Port 8600) - Shipping Service with H2
8. **favourite-service** (Port 8800) - Favourite Service with H2
9. **proxy-client** (Port 8900) - Proxy Client with H2

## Resource Limits

All services are configured with:
- **Memory Request:** 128Mi
- **Memory Limit:** 350Mi
- **CPU Request:** 100m
- **CPU Limit:** 500m
- **JAVA_OPTS:** -Xmx256m -Xms128m

## Database Configuration

- **product-service:** Uses Azure SQL Server (configured via environment variables)
- **All other services:** Use H2 in-memory database

## Required GitHub Secrets

The following secrets must be configured in GitHub:

- `ACR_LOGIN_SERVER` - Azure Container Registry login server URL
- `ACR_USERNAME` - ACR username
- `ACR_PASSWORD` - ACR password
- `SQL_PASSWORD` - SQL Server administrator password
- `SQL_HOST` - SQL Server FQDN
- `SQL_DATABASE` - SQL Database name (default: ecommerce)
- `AZURE_CREDENTIALS` - Azure service principal credentials (JSON)
- `AZURE_SUBSCRIPTION_ID` - Azure subscription ID
- `AKS_RESOURCE_GROUP` - AKS resource group name
- `AKS_CLUSTER_NAME` - AKS cluster name

## Deployment Order

The deployment follows this sequence to handle dependencies:

1. **Phase 1:** service-discovery
2. **Wait:** 80 seconds for Eureka to start
3. **Phase 2:** api-gateway, product-service
4. **Phase 3:** user-service, order-service, payment-service, shipping-service, favourite-service, proxy-client

## Usage

The manifests use environment variable substitution via `envsubst`. Variables:
- `${ACR_LOGIN_SERVER}` - Replaced with ACR login server
- `${TAG}` - Replaced with image tag (GitHub SHA)
- `${SQL_HOST}` - Replaced with SQL Server hostname
- `${SQL_DATABASE}` - Replaced with database name


# Manual de Operaciones Básico

## 1. Introducción

Este documento describe los procedimientos operativos estándar para la gestión, monitoreo y mantenimiento del sistema de microservicios de e-commerce.

**Alcance**: Operaciones diarias, troubleshooting básico y procedimientos de emergencia.

## 2. Arquitectura del Sistema

### 2.1 Componentes Principales

| Servicio | Puerto | Propósito |
|----------|--------|-----------|
| API Gateway | 8080 | Punto de entrada único |
| Service Discovery | 8761 | Registro de servicios |
| Cloud Config | 8888 | Configuración centralizada |
| User Service | 8081 | Gestión de usuarios |
| Product Service | 8082 | Catálogo de productos |
| Order Service | 8083 | Procesamiento de órdenes |
| Payment Service | 8084 | Procesamiento de pagos |
| Shipping Service | 8085 | Gestión de envíos |
| Favourite Service | 8086 | Gestión de favoritos |
| Proxy Client | 3000 | Cliente web |

### 2.2 Dependencias Externas

- **PostgreSQL**: Base de datos relacional
- **Zipkin**: Tracing distribuido (9411)
- **Prometheus**: Métricas (9090)
- **Grafana**: Visualización (3000)
- **Elasticsearch/Kibana**: Logs centralizados

## 3. Operaciones Básicas

### 3.1 Inicio del Sistema

#### Opción A: Docker Compose (Desarrollo/Testing)

```bash
# Iniciar todos los servicios
docker-compose up -d

# Verificar estado
docker-compose ps

# Ver logs
docker-compose logs -f [service-name]
```

#### Opción B: Kubernetes (Producción)

```bash
# Aplicar configuraciones
kubectl apply -f k8s/namespaces.yaml
kubectl apply -f k8s/

# Verificar despliegue
kubectl get pods -n dev
kubectl get services -n dev
kubectl get ingress -n dev
```

### 3.2 Detención del Sistema

#### Docker Compose
```bash
# Detener servicios
docker-compose stop

# Detener y eliminar contenedores
docker-compose down

# Detener y eliminar volúmenes
docker-compose down -v
```

#### Kubernetes
```bash
# Escalar a cero réplicas
kubectl scale deployment --all --replicas=0 -n dev

# Eliminar recursos
kubectl delete -f k8s/ -n dev
```

### 3.3 Reinicio de Servicios

#### Docker Compose
```bash
# Reiniciar servicio específico
docker-compose restart [service-name]

# Reiniciar todos los servicios
docker-compose restart
```

#### Kubernetes
```bash
# Reiniciar deployment
kubectl rollout restart deployment/[service-name] -n dev

# Reiniciar pod específico
kubectl delete pod [pod-name] -n dev
```

## 4. Monitoreo y Diagnóstico

### 4.1 Verificación de Estado

#### Healthchecks

```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Service Discovery
curl http://localhost:8761/actuator/health

# Servicios individuales
curl http://localhost:808[1-6]/actuator/health
```

#### Estado de Servicios Kubernetes

```bash
# Pods
kubectl get pods -n dev -o wide

# Servicios
kubectl get svc -n dev

# Logs en tiempo real
kubectl logs -f deployment/[service-name] -n dev

# Logs de contenedor específico
kubectl logs [pod-name] -n dev -c [container-name]
```

### 4.2 Métricas y Observabilidad

#### Acceso a Herramientas

- **Service Discovery**: http://localhost:8761
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Zipkin**: http://localhost:9411
- **Kibana**: http://localhost:5601

#### Consultas Prometheus Útiles

```promql
# CPU por servicio
rate(process_cpu_usage[5m])

# Memoria utilizada
jvm_memory_used_bytes{area="heap"}

# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Latencia P95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

### 4.3 Análisis de Logs

#### Búsqueda por Nivel

```bash
# Docker Compose
docker-compose logs [service] | grep ERROR

# Kubernetes
kubectl logs deployment/[service] -n dev | grep ERROR
```

#### Kibana Queries

```
# Errores en últimas 24h
level:ERROR AND @timestamp:[now-24h TO now]

# Por servicio
service.name:"order-service" AND level:ERROR

# Por correlation ID
correlation_id:"[trace-id]"
```

## 5. Troubleshooting

### 5.1 Problemas Comunes

#### Servicio No Responde

```bash
# 1. Verificar que el servicio esté ejecutándose
kubectl get pods -n dev | grep [service-name]

# 2. Verificar logs
kubectl logs deployment/[service-name] -n dev --tail=100

# 3. Verificar eventos
kubectl describe pod [pod-name] -n dev

# 4. Verificar conectividad
kubectl exec -it [pod-name] -n dev -- curl http://[service]:8080/actuator/health
```

#### Servicio No Se Registra en Discovery

```bash
# 1. Verificar que Service Discovery esté activo
kubectl get pods -n dev | grep service-discovery

# 2. Verificar configuración
kubectl describe configmap -n dev

# 3. Verificar conectividad de red
kubectl exec -it [pod-name] -n dev -- nslookup service-discovery
```

#### Base de Datos Inaccesible

```bash
# 1. Verificar pod de PostgreSQL
kubectl get pods -n dev | grep postgres

# 2. Verificar secretos
kubectl get secrets -n dev

# 3. Verificar conectividad
kubectl exec -it [pod-name] -n dev -- nc -zv postgres-service 5432

# 4. Acceder a PostgreSQL
kubectl exec -it [postgres-pod] -n dev -- psql -U [user] -d [database]
```

#### Alta Latencia

1. Verificar métricas en Prometheus/Grafana
2. Revisar logs para errores o timeouts
3. Verificar recursos (CPU/Memoria):
   ```bash
   kubectl top pods -n dev
   kubectl top nodes
   ```
4. Analizar traces en Zipkin para identificar cuellos de botella

#### Memory Leaks

```bash
# Monitorear heap usage
kubectl exec [pod-name] -n dev -- jcmd 1 GC.heap_info

# Generar heap dump
kubectl exec [pod-name] -n dev -- jcmd 1 GC.heap_dump /tmp/heap.hprof

# Copiar heap dump
kubectl cp dev/[pod-name]:/tmp/heap.hprof ./heap.hprof
```

### 5.2 Rollback de Despliegue

```bash
# Ver historial de revisiones
kubectl rollout history deployment/[service-name] -n dev

# Rollback a versión anterior
kubectl rollout undo deployment/[service-name] -n dev

# Rollback a revisión específica
kubectl rollout undo deployment/[service-name] --to-revision=[number] -n dev

# Verificar estado del rollback
kubectl rollout status deployment/[service-name] -n dev
```

## 6. Mantenimiento

### 6.1 Actualización de Servicios

#### Proceso Estándar

1. **Preparación**
   ```bash
   # Backup de base de datos
   kubectl exec [postgres-pod] -n dev -- pg_dump -U [user] [db] > backup.sql
   ```

2. **Despliegue**
   ```bash
   # Actualizar imagen
   kubectl set image deployment/[service] [container]=[image]:[tag] -n dev
   
   # Monitorear rollout
   kubectl rollout status deployment/[service] -n dev
   ```

3. **Verificación**
   ```bash
   # Healthcheck
   kubectl exec [pod] -n dev -- curl localhost:8080/actuator/health
   
   # Logs
   kubectl logs -f deployment/[service] -n dev --tail=50
   ```

### 6.2 Limpieza de Recursos

```bash
# Limpiar pods evicted/failed
kubectl delete pods --field-selector status.phase=Failed -n dev

# Limpiar imágenes no utilizadas
docker system prune -a

# Limpiar volúmenes huérfanos
docker volume prune
```

### 6.3 Backup y Restore

#### Base de Datos

```bash
# Backup
kubectl exec [postgres-pod] -n dev -- pg_dumpall -U postgres > full_backup_$(date +%Y%m%d).sql

# Restore
cat backup.sql | kubectl exec -i [postgres-pod] -n dev -- psql -U postgres
```

#### Configuraciones

```bash
# Export
kubectl get configmap -n dev -o yaml > configmaps_backup.yaml
kubectl get secret -n dev -o yaml > secrets_backup.yaml

# Restore
kubectl apply -f configmaps_backup.yaml
kubectl apply -f secrets_backup.yaml
```

## 7. Escalamiento

### 7.1 Escalamiento Horizontal

```bash
# Manual
kubectl scale deployment/[service] --replicas=[number] -n dev

# Autoscaling
kubectl autoscale deployment/[service] --min=2 --max=10 --cpu-percent=70 -n dev

# Verificar HPA
kubectl get hpa -n dev
```

### 7.2 Escalamiento Vertical

```yaml
# Actualizar recursos en deployment
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

```bash
kubectl apply -f k8s/[service].yaml
kubectl rollout status deployment/[service] -n dev
```

## 8. Seguridad Operacional

### 8.1 Rotación de Secretos

```bash
# Crear nuevo secret
kubectl create secret generic [secret-name] --from-literal=key=value -n dev --dry-run=client -o yaml | kubectl apply -f -

# Forzar recreación de pods
kubectl rollout restart deployment/[service] -n dev
```

### 8.2 Auditoría

```bash
# Eventos del cluster
kubectl get events -n dev --sort-by='.lastTimestamp'

# Accesos a API
kubectl logs -n kube-system [apiserver-pod] | grep [resource]
```

## 9. Procedimientos de Emergencia

### 9.1 Caída Total del Sistema

1. **Verificar infraestructura base**
   ```bash
   kubectl get nodes
   kubectl get pods -n kube-system
   ```

2. **Reiniciar servicios críticos en orden**
   ```bash
   kubectl apply -f k8s/cloud-config.yaml
   kubectl apply -f k8s/service-discovery.yaml
   kubectl apply -f k8s/api-gateway.yaml
   # Esperar a que estén healthy
   kubectl apply -f k8s/
   ```

3. **Validar funcionalidad**
   - Verificar service discovery: http://localhost:8761
   - Ejecutar smoke tests básicos
   - Verificar métricas en Prometheus

### 9.2 Pérdida de Datos

1. **Detener escrituras**
   ```bash
   kubectl scale deployment --all --replicas=0 -n dev
   ```

2. **Restaurar desde backup**
   ```bash
   cat backup.sql | kubectl exec -i [postgres-pod] -n dev -- psql -U postgres
   ```

3. **Verificar integridad**
   ```sql
   SELECT * FROM pg_stat_database;
   ```

4. **Reiniciar servicios gradualmente**

### 9.3 Incident Response

1. **Detección**: Alerta de monitoreo o reporte de usuario
2. **Triage**: Evaluar severidad e impacto
3. **Comunicación**: Notificar a stakeholders
4. **Mitigación**: Aplicar fix temporal si es necesario
5. **Resolución**: Implementar solución definitiva
6. **Post-mortem**: Documentar incidente y lecciones aprendidas

## 10. Contactos y Escalamiento

### 10.1 Niveles de Soporte

- **L1**: Operaciones básicas, monitoring (Equipo DevOps)
- **L2**: Troubleshooting avanzado, performance tuning (Equipo SRE)
- **L3**: Cambios arquitectónicos, bugs críticos (Equipo Desarrollo)

### 10.2 SLA

- **P0 (Crítico)**: Sistema caído - Respuesta: 15 min
- **P1 (Alto)**: Funcionalidad crítica afectada - Respuesta: 1 hora
- **P2 (Medio)**: Degradación parcial - Respuesta: 4 horas
- **P3 (Bajo)**: Issues menores - Respuesta: 1 día laboral

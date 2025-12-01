#!/bin/bash
# filepath: observability/install.sh
# Script de instalación del stack de observabilidad - VERSIÓN FINAL ELASTIC OFICIAL

set -e

echo "========================================"
echo "Instalando Stack de Observabilidad (Elastic Oficial)"
echo "========================================"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. Agregar repositorios
echo -e "${GREEN}Agregando repositorios de Helm...${NC}"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add elastic https://helm.elastic.co
helm repo update

# 2. Crear namespace
echo -e "${GREEN}Asegurando namespace 'monitoring'...${NC}"
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

# 3. Instalar Prometheus + Grafana
echo -e "${GREEN}Instalando Prometheus y Grafana...${NC}"
# Nota: Usamos timeout corto y sin wait para evitar bloqueos
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values prometheus-values.yaml \
  --timeout 10m

# 4. Instalar Elasticsearch (OFICIAL) - Configuración "Bonsái"
echo -e "${GREEN}Instalando Elasticsearch (Oficial)...${NC}"
helm upgrade --install elasticsearch elastic/elasticsearch \
  --namespace monitoring \
  --set replicas=1 \
  --set minimumMasterNodes=1 \
  --set resources.requests.memory=512Mi \
  --set resources.limits.memory=1Gi \
  --set esJavaOpts="-Xmx512m -Xms512m" \
  --set persistence.enabled=false \
  --timeout 10m

# 5. Instalar Kibana (OFICIAL)
echo -e "${GREEN}Instalando Kibana (Oficial)...${NC}"
helm upgrade --install kibana elastic/kibana \
  --namespace monitoring \
  --set resources.requests.memory=256Mi \
  --set resources.limits.memory=512Mi \
  --set service.type=LoadBalancer \
  --timeout 10m

# 6. Instalar Filebeat (OFICIAL)
echo -e "${GREEN}Instalando Filebeat (Oficial)...${NC}"
helm upgrade --install filebeat elastic/filebeat \
  --namespace monitoring \
  --set daemonset.filebeatConfig.filebeat.yml.output.elasticsearch.hosts='["elasticsearch-master:9200"]' \
  --timeout 10m

# 7. Aplicar Monitor
echo -e "${GREEN}Aplicando ServiceMonitor...${NC}"
kubectl apply -f spring-boot-servicemonitor.yaml

echo ""
echo -e "${GREEN}¡Instalación enviada! Los pods arrancarán en unos minutos.${NC}"
echo "Verifica el estado con: kubectl get pods -n monitoring"

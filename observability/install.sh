#!/bin/bash
# filepath: observability/install.sh
# Script de instalación del stack de observabilidad - OPTIMIZADO PARA AKS STUDENT TIER

set -e

echo "========================================"
echo "🚀 Instalando Stack de Observabilidad"
echo "========================================"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Función para esperar a que un deployment esté listo
wait_for_deployment() {
  local namespace=$1
  local deployment=$2
  local timeout=$3
  
  echo -e "${YELLOW}Esperando a $deployment (timeout: ${timeout}s)...${NC}"
  kubectl wait --for=condition=available --timeout=${timeout}s deployment/$deployment -n $namespace || true
}

# 1. Agregar repositorios de Helm
echo -e "${GREEN}Agregando repositorios de Helm...${NC}"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add elastic https://helm.elastic.co
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# 2. Crear namespace
echo -e "${GREEN}Creando namespace 'monitoring'...${NC}"
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

# 3. Instalar Prometheus + Grafana (CON TIMEOUT EXTENDIDO)
echo -e "${GREEN}Instalando Prometheus y Grafana (esto puede tardar 15-20 minutos)...${NC}"
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values prometheus-values.yaml \
  --wait \
  --timeout 40m \
  --atomic \
  --cleanup-on-fail

echo -e "${GREEN}Prometheus instalado${NC}"

# Esperar a que Grafana esté listo
wait_for_deployment monitoring kube-prometheus-stack-grafana 300

# 4. Instalar Elasticsearch
echo -e "${GREEN}Instalando Elasticsearch...${NC}"
helm upgrade --install elasticsearch bitnami/elasticsearch \
  --namespace monitoring \
  --values elastic-values.yaml \
  --wait \
  --timeout 30m \
  --atomic \
  --cleanup-on-fail

echo -e "${GREEN}Elasticsearch instalado${NC}"

# 5. Instalar Kibana
echo -e "${GREEN}Instalando Kibana...${NC}"
helm upgrade --install kibana bitnami/kibana \
  --namespace monitoring \
  --values kibana-values.yaml \
  --wait \
  --timeout 20m \
  --atomic \
  --cleanup-on-fail

echo -e "${GREEN}Kibana instalado${NC}"

# 6. Instalar Filebeat
echo -e "${GREEN}Instalando Filebeat...${NC}"
helm upgrade --install filebeat elastic/filebeat \
  --namespace monitoring \
  --values filebeat-values.yaml \
  --wait \
  --timeout 15m

echo -e "${GREEN}Filebeat instalado${NC}"

# 7. Aplicar ServiceMonitor
echo -e "${GREEN}Aplicando ServiceMonitor para Spring Boot...${NC}"
kubectl apply -f spring-boot-servicemonitor.yaml

echo ""
echo "========================================"
echo -e "${YELLOW}ESTADO DE LA INSTALACIÓN${NC}"
echo "========================================"

echo ""
echo -e "${GREEN}Pods en namespace monitoring:${NC}"
kubectl get pods -n monitoring

echo ""
echo -e "${GREEN}Servicios en namespace monitoring:${NC}"
kubectl get svc -n monitoring

echo ""
echo "========================================"
echo -e "${YELLOW}ACCESO A LOS SERVICIOS${NC}"
echo "========================================"

# Grafana
echo ""
echo -e "${GREEN}Grafana:${NC}"
echo "  Usuario: admin"
echo "  Contraseña: (se muestra abajo)"
if kubectl get secret -n monitoring kube-prometheus-stack-grafana &>/dev/null; then
  GRAFANA_PASSWORD=$(kubectl get secret -n monitoring kube-prometheus-stack-grafana -o jsonpath="{.data.admin-password}" 2>/dev/null | base64 --decode 2>/dev/null || echo "admin123")
  echo "  Contraseña: $GRAFANA_PASSWORD"
else
  echo "  Secret no encontrado aún, espera 1-2 minutos"
fi
echo ""
echo "  Port-forward:"
echo "  kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80"
echo "  http://localhost:3000"
echo ""

# Prometheus
echo -e "${GREEN}Prometheus:${NC}"
echo "  kubectl port-forward -n monitoring svc/kube-prometheus-stack-prometheus 9090:9090"
echo "  http://localhost:9090"
echo ""

# Kibana
echo -e "${GREEN}Kibana:${NC}"
echo "  kubectl port-forward -n monitoring svc/kibana 5601:5601"
echo "  http://localhost:5601"
echo ""

# Elasticsearch
echo -e "${GREEN}Elasticsearch:${NC}"
echo "  kubectl port-forward -n monitoring svc/elasticsearch 9200:9200"
echo "  http://localhost:9200"
echo ""

echo "========================================"
echo -e "${YELLOW}PRÓXIMOS PASOS${NC}"
echo "========================================"
echo ""
echo "1. Agrega el label 'monitoring: true' a todos los Services:"
echo "   metadata:"
echo "     labels:"
echo "       monitoring: \"true\""
echo ""
echo "2. Reindeployea tus microservicios:"
echo "   kubectl rollout restart deployment -n default"
echo ""
echo "3. Verifica que Prometheus esté scrapeando:"
echo "   kubectl port-forward -n monitoring svc/kube-prometheus-stack-prometheus 9090:9090"
echo "   Luego visita: http://localhost:9090/targets"
echo ""
echo -e "${GREEN}¡Instalación completada!${NC}"

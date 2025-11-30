#!/bin/bash
# filepath: k8s/observability/install.sh
# Script de instalación del stack de observabilidad

set -e

echo "========================================"
echo "Instalando Stack de Observabilidad"
echo "========================================"

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Agregar repositorios de Helm
echo -e "${GREEN}Agregando repositorios de Helm...${NC}"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add elastic https://helm.elastic.co
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# 2. Crear namespace
echo -e "${GREEN}Creando namespace 'monitoring'...${NC}"
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

# 3. Instalar Prometheus + Grafana
echo -e "${GREEN}Instalando Prometheus y Grafana...${NC}"
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values prometheus-values.yaml \
  --wait \
  --timeout 25m

# 4. Instalar Elasticsearch
echo -e "${GREEN}Instalando Elasticsearch...${NC}"
helm upgrade --install elasticsearch bitnami/elasticsearch \
  --namespace monitoring \
  --values elastic-values.yaml \
  --wait \
  --timeout 25m

# 5. Instalar Kibana
echo -e "${GREEN}Instalando Kibana...${NC}"
helm upgrade --install kibana bitnami/kibana \
  --namespace monitoring \
  --values kibana-values.yaml \
  --wait \
  --timeout 25m

# 6. Instalar Filebeat
echo -e "${GREEN}Instalando Filebeat...${NC}"
helm upgrade --install filebeat elastic/filebeat \
  --namespace monitoring \
  --values filebeat-values.yaml \
  --wait \
  --timeout 25m

# 7. Aplicar ServiceMonitor
echo -e "${GREEN}Aplicando ServiceMonitor para Spring Boot...${NC}"
kubectl apply -f spring-boot-servicemonitor.yaml

# 8. Verificar despliegue
echo ""
echo -e "${GREEN}Verificando pods...${NC}"
kubectl get pods -n monitoring

echo ""
echo "========================================"
echo -e "${YELLOW}ACCESO A LOS SERVICIOS${NC}"
echo "========================================"

# Grafana
GRAFANA_PASSWORD=$(kubectl get secret -n monitoring kube-prometheus-stack-grafana -o jsonpath="{.data.admin-password}" | base64 --decode)
echo -e "${GREEN}Grafana:${NC}"
echo "  Usuario: admin"
echo "  Password: $GRAFANA_PASSWORD"
echo "  URL externa: http://$(kubectl get svc -n monitoring kube-prometheus-stack-grafana -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
echo ""
echo "  O usa port-forward:"
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
echo "  URL externa: http://$(kubectl get svc -n monitoring kibana -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):5601"
echo ""
echo "  O usa port-forward:"
echo "  kubectl port-forward -n monitoring svc/kibana 5601:5601"
echo "  http://localhost:5601"
echo ""

# Elasticsearch
echo -e "${GREEN}Elasticsearch:${NC}"
echo "  kubectl port-forward -n monitoring svc/elasticsearch 9200:9200"
echo "  http://localhost:9200"
echo ""

echo -e "${GREEN}Instalación completada!${NC}"

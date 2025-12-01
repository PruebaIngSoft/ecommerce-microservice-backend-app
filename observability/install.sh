#!/bin/bash
# filepath: observability/install.sh
# Script de instalación del stack de observabilidad - OPTIMIZADO PARA AKS STUDENT TIER

set -e

echo "========================================"
echo "Instalando Stack de Observabilidad"
echo "========================================"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. Agregar repositorios de Helm
echo -e "${GREEN}Agregando repositorios de Helm...${NC}"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add elastic https://helm.elastic.co
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# 2. Crear namespace
echo -e "${GREEN}Creando namespace 'monitoring'...${NC}"
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

echo -e "${YELLOW}Limpiando instalaciones previas bloqueadas (Fix 'operation in progress')...${NC}"
# Intentamos desinstalar para liberar el candado. El '|| true' evita que falle si no existe.
helm uninstall kube-prometheus-stack -n monitoring --wait || true
helm uninstall elasticsearch -n monitoring --wait || true
helm uninstall kibana -n monitoring --wait || true
helm uninstall filebeat -n monitoring --wait || true
echo -e "${GREEN}Limpieza terminada. Iniciando instalación limpia...${NC}"

# 3. Instalar Prometheus + Grafana (MODO ASÍNCRONO)
# Quitamos --wait para que no bloquee el pipeline si tarda mucho en arrancar
echo -e "${GREEN}Instalando Prometheus y Grafana...${NC}"
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values prometheus-values.yaml \
  --timeout 10m

echo -e "${GREEN}Orden de instalación de Prometheus enviada.${NC}"

# 4. Instalar Elasticsearch (MODO ASÍNCRONO)
echo -e "${GREEN}Instalando Elasticsearch...${NC}"
helm upgrade --install elasticsearch bitnami/elasticsearch \
  --namespace monitoring \
  --values elastic-values.yaml \
  --timeout 10m

echo -e "${GREEN}Orden de instalación de Elasticsearch enviada.${NC}"

# 5. Instalar Kibana (MODO ASÍNCRONO)
echo -e "${GREEN}Instalando Kibana...${NC}"
helm upgrade --install kibana bitnami/kibana \
  --namespace monitoring \
  --values kibana-values.yaml \
  --timeout 10m

echo -e "${GREEN}Orden de instalación de Kibana enviada.${NC}"

# 6. Instalar Filebeat (MODO ASÍNCRONO)
echo -e "${GREEN}Instalando Filebeat...${NC}"
helm upgrade --install filebeat elastic/filebeat \
  --namespace monitoring \
  --values filebeat-values.yaml \
  --timeout 10m

echo -e "${GREEN}Orden de instalación de Filebeat enviada.${NC}"

# 7. Aplicar ServiceMonitor
echo -e "${GREEN}Aplicando ServiceMonitor para Spring Boot...${NC}"
kubectl apply -f spring-boot-servicemonitor.yaml

# 8. Verificar estado actual (Informativo)
echo ""
echo -e "${GREEN}Estado actual de los pods (pueden estar creando contenedores):${NC}"
kubectl get pods -n monitoring

echo ""
echo "========================================"
echo -e "${YELLOW}ACCESO A LOS SERVICIOS${NC}"
echo "========================================"
echo "Nota: Los servicios pueden tardar unos minutos en estar disponibles (Running)."

# Grafana
echo ""
echo -e "${GREEN}Grafana:${NC}"
echo "  Usuario: admin"
# Intentamos obtener la contraseña, si falla (porque el secreto aun no existe) mostramos aviso
if kubectl get secret -n monitoring kube-prometheus-stack-grafana &>/dev/null; then
  GRAFANA_PASSWORD=$(kubectl get secret -n monitoring kube-prometheus-stack-grafana -o jsonpath="{.data.admin-password}" 2>/dev/null | base64 --decode 2>/dev/null || echo "admin123")
  echo "  Contraseña: $GRAFANA_PASSWORD"
else
  echo "  Contraseña: (El secreto aún se está creando, revisa luego)"
fi
echo ""
echo "  Port-forward:"
echo "  kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80"
echo "  URL: http://localhost:3000"
echo ""

# Prometheus
echo -e "${GREEN}Prometheus:${NC}"
echo "  kubectl port-forward -n monitoring svc/kube-prometheus-stack-prometheus 9090:9090"
echo "  URL: http://localhost:9090"
echo ""

# Kibana
echo -e "${GREEN}Kibana:${NC}"
echo "  kubectl port-forward -n monitoring svc/kibana 5601:5601"
echo "  URL: http://localhost:5601"
echo ""

# Elasticsearch
echo -e "${GREEN}Elasticsearch:${NC}"
echo "  kubectl port-forward -n monitoring svc/elasticsearch 9200:9200"
echo "  URL: http://localhost:9200"
echo ""

echo -e "${GREEN}¡Instalación finalizada exitosamente!${NC}"

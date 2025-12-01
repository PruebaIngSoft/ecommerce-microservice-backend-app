#!/bin/bash
# filepath: observability/install.sh
# Script de instalación del stack de observabilidad - VERSIÓN FINAL CORREGIDA (Filebeat Fix)

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

# --- LIMPIEZA PROFUNDA ---
echo -e "${YELLOW}Limpiando instalaciones previas y residuos...${NC}"
# Borramos todo para asegurar una instalación limpia
helm uninstall kube-prometheus-stack -n monitoring --wait || true
helm uninstall elasticsearch -n monitoring --wait || true
helm uninstall kibana -n monitoring --wait || true
helm uninstall filebeat -n monitoring --wait || true

# Borrar ConfigMaps basura que bloquean la reinstalación
kubectl delete configmap kibana-kibana-helm-scripts -n monitoring --ignore-not-found || true
kubectl delete configmap elasticsearch-master-helm-scripts -n monitoring --ignore-not-found || true
echo -e "${GREEN}Limpieza terminada.${NC}"
# -------------------------

# 3. Instalar Prometheus + Grafana
echo -e "${GREEN}Instalando Prometheus y Grafana...${NC}"
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values prometheus-values.yaml \
  --timeout 10m

echo -e "${GREEN}Orden de instalación de Prometheus enviada.${NC}"

# 4. Instalar Elasticsearch (OFICIAL)
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

echo -e "${GREEN}Orden de instalación de Elasticsearch enviada.${NC}"

echo "Esperando 30s para estabilizar el cluster..."
sleep 30

# 5. Instalar Kibana (OFICIAL)
echo -e "${GREEN}Instalando Kibana (Oficial)...${NC}"
helm upgrade --install kibana elastic/kibana \
  --namespace monitoring \
  --set resources.requests.memory=256Mi \
  --set resources.limits.memory=512Mi \
  --set service.type=LoadBalancer \
  --timeout 10m \
  --no-hooks

echo -e "${GREEN}Orden de instalación de Kibana enviada.${NC}"

# 6. Instalar Filebeat (OFICIAL) - FIX: Usando archivo de valores generado
echo -e "${GREEN}Generando configuración de Filebeat...${NC}"

# Creamos el archivo de configuración al vuelo para evitar errores de formato con --set
cat <<'EOF' > filebeat-config-generated.yaml
filebeatConfig:
  filebeat.yml: |
    filebeat.inputs:
    - type: container
      paths:
        - /var/log/containers/*.log
      processors:
      - add_kubernetes_metadata:
          host: ${NODE_NAME}
          matchers:
          - logs_path:
              logs_path: "/var/log/containers/"
    output.elasticsearch:
      hosts: ["elasticsearch-master:9200"]
      protocol: http
EOF

echo -e "${GREEN}Instalando Filebeat (Oficial)...${NC}"
helm upgrade --install filebeat elastic/filebeat \
  --namespace monitoring \
  -f filebeat-config-generated.yaml \
  --timeout 10m

echo -e "${GREEN}Orden de instalación de Filebeat enviada.${NC}"

# 7. Aplicar Monitor
echo -e "${GREEN}Aplicando ServiceMonitor...${NC}"
kubectl apply -f spring-boot-servicemonitor.yaml

echo ""
echo -e "${GREEN}¡Instalación finalizada exitosamente!${NC}"
echo "Verifica el estado con: kubectl get pods -n monitoring"

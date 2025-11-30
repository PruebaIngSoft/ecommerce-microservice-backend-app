# 📋 Gestión de Cambios (Change Management)

Este documento define el proceso de gestión de cambios para el proyecto **ecommerce-microservice-backend-app**.

> **Contexto:** Proyecto académico desarrollado por un equipo de 2 personas, enfocado en demostrar prácticas DevOps.

---

## 1. Estrategia de Versionado

Utilizamos **Semantic Versioning (SemVer)** con el formato `vX.Y.Z`:

| Componente | Significado | Ejemplo |
|------------|-------------|---------|
| **X (Major)** | Cambios incompatibles con versiones anteriores | v2.0.0 |
| **Y (Minor)** | Nueva funcionalidad compatible hacia atrás | v1.1.0 |
| **Z (Patch)** | Correcciones de bugs | v1.0.1 |

### Tags Automáticos

Los releases se generan automáticamente con el formato:
```
v{YYYY.MM.DD}-{SHORT_SHA}
```
Ejemplo: `v2025.11.29-abc1234`

---

## 2. Política de Ramas (GitFlow Simplificado)

```
main (producción) ──── Requiere aprobación
  │
  └── dev (integración/desarrollo) ──── Deploy automático
        │
        ├── feature/nueva-funcionalidad
        └── fix/correccion-bug
```

### Flujo de Trabajo

1. **Desarrollo de Features:**
   - Crear rama desde `dev`: `feature/nombre-descriptivo`
   - Desarrollar y hacer commits
   - Push a la rama feature
   - Crear PR a `dev` → Se ejecuta CI automáticamente
   - Merge a `dev` → Deploy automático a ambiente dev

2. **Release a Producción:**
   - Crear PR de `dev` a `main`
   - **Requiere aprobación** (GitHub Environment: production)
   - Merge a `main` → Deploy a producción + Release automático

---

## 3. Proceso de Cambios

### Para desarrollo (dev)
1. Crear rama `feature/` o `fix/` desde `dev`
2. Hacer los cambios y commits
3. Crear Pull Request a `dev`
4. Verificar que CI pase (tests, Trivy)
5. Merge → Deploy automático

### Para producción (main)
1. Crear Pull Request de `dev` a `main`
2. Revisar cambios acumulados
3. Aprobar en GitHub (environment: production)
4. Merge → Deploy + Release Notes automáticos

---

## 4. Plan de Rollback

En caso de problemas después de un despliegue:

### Opción A: Rollback Inmediato (Kubernetes)

Para revertir rápidamente un deployment:

```bash
# Rollback a la revisión anterior
kubectl rollout undo deployment/<nombre-servicio> -n prod

# Verificar estado
kubectl rollout status deployment/<nombre-servicio> -n prod
```

**Rollback de todos los servicios:**
```bash
SERVICES="api-gateway product-service user-service order-service payment-service shipping-service favourite-service"
for svc in $SERVICES; do
  kubectl rollout undo deployment/$svc -n prod
done
```

### Opción B: Rollback vía Git

Para rollback con trazabilidad completa:

```bash
# 1. Identificar el commit problemático
git log --oneline -10

# 2. Revertir el commit
git revert <commit-sha>

# 3. Push a main (dispara pipeline automático)
git push origin main
```

### ¿Cuándo usar cada opción?

| Situación | Opción |
|-----------|--------|
| Servicio caído, urgente | A (Kubernetes) |
| Bug crítico en producción | A (Kubernetes) |
| Problema complejo, necesita análisis | B (Git Revert) |

---

## 5. Releases Automáticos

Cada deploy exitoso a producción genera:

1. **Tag de Git** con formato `v{fecha}-{sha}`
2. **GitHub Release** con notas auto-generadas
3. **Instrucciones de rollback** incluidas

### Commits Descriptivos

Usamos prefijos para commits claros:

- `feat:` Nueva funcionalidad
- `fix:` Corrección de bug
- `docs:` Documentación
- `chore:` Mantenimiento
- `ci:` Cambios en pipelines

**Ejemplos:**
```
feat: add product search endpoint
fix: correct order total calculation
ci: add OWASP ZAP security scan
```

---

## 6. Limpieza de Recursos (Dev)

Para evitar saturar el cluster en ambiente de desarrollo, el pipeline incluye limpieza automática antes de cada deploy:

```yaml
- name: Clean up old deployments
  run: |
    kubectl delete deployment --all -n dev || true
    kubectl delete svc --all -n dev || true
```

> **Nota:** Esto solo aplica para `dev`. En `prod` nunca se hace limpieza automática.

---

## 7. Resumen del Flujo

```
feature/* ──► PR a dev ──► CI ──► Merge ──► Deploy Dev
                                              │
                                              ▼
                                    PR a main ──► Aprobación ──► Deploy Prod + Release
```

---

*Documento creado para el proyecto final de Ingeniería de Software V*
*Equipo: 2 integrantes*

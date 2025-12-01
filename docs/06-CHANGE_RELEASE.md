# 6. Gestión de Cambios y Release Notes

Se define el proceso de gestión de cambios y generación de releases para el proyecto.

**Contexto:** Mismo equipo de 2 personas, Azure for Students, GitHub Actions como plataforma CI/CD.

---

## 6.1 Estrategia de Versionado

El proyecto utiliza versionado semántico basado en fecha y commit hash.

### 6.1.1 Formato de Versión

Los releases se generan automáticamente con el formato:

```
v{YYYY.MM.DD}-{SHORT_SHA}
```

**Ejemplo:** `v2025.11.29-abc1234`

**Componentes:**
- `YYYY.MM.DD`: Fecha del release
- `SHORT_SHA`: Primeros 7 caracteres del commit hash

---

## 6.2 Política de Ramas

Estrategia GitFlow simplificada para equipo de 2 personas:

```
main (prod) ──── Requiere aprobación
  │
  └── dev ──── Deploy automático
        │
        └── feature/* o fix/*
```

### 6.2.1 Flujo de Trabajo

**Desarrollo (dev):**
1. Crear rama `feature/` o `fix/` desde `dev`
2. Desarrollar y hacer commits
3. Crear Pull Request a `dev`
4. CI ejecuta tests, SonarCloud y Trivy
5. Merge → Deploy automático a namespace dev

**Producción (main):**
1. Crear Pull Request de `dev` a `main`
2. Revisar cambios acumulados
3. Aprobación requerida (GitHub Environment: production)
4. Merge → Deploy a namespace prod + GitHub Release automático

---

## 6.3 Proceso Formal de Change Management

### 6.3.1 Categorización de Cambios

| Tipo | Rama | Aprobación | Deploy |
|------|------|-----------|---------|
| Feature nueva | `feature/*` → `dev` | Revisión de código | Automático a dev |
| Corrección de bug | `fix/*` → `dev` | Revisión de código | Automático a dev |
| Release a producción | `dev` → `main` | Aprobador en GitHub Environment | Manual a prod |

### 6.3.2 Controles de Calidad

Antes de cada merge, el pipeline CI valida:
- Compilación exitosa de todos los módulos
- Ejecución de tests unitarios
- Análisis de calidad con SonarCloud
- Escaneo de vulnerabilidades con Trivy

### 6.3.3 Gate de Aprobación

Para despliegues a producción:
- Se requiere aprobación manual en GitHub Environment "production"
- El aprobador revisa los cambios del PR
- Solo después de aprobación se ejecuta el pipeline de deployment

---

## 6.4 Planes de Rollback

### 6.4.1 Rollback Inmediato (Kubernetes)

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

### 6.4.2 Rollback con Trazabilidad (Git)

```bash
# 1. Identificar el commit problemático
git log --oneline -10

# 2. Revertir el commit
git revert <commit-sha>

# 3. Push a main (dispara pipeline automático)
git push origin main
```

### 6.4.3 Criterios de Selección

| Situación | Estrategia | Justificación |
|-----------|------------|---------------|
| Servicio caído | Kubernetes rollback | Inmediato, sin esperar pipeline |
| Bug crítico | Kubernetes rollback | Reversión rápida |
| Issue complejo | Git revert | Trazabilidad completa |

---

## 6.5 Generación Automática de Release Notes

### 6.5.1 Proceso Automático

El workflow `deploy-prod.yml` genera automáticamente al finalizar el deployment:

1. **Tag de Git:** `v{YYYY.MM.DD}-{SHORT_SHA}`
2. **GitHub Release:** Creado con el mismo tag
3. **Release Notes:** Generadas automáticamente listando commits desde el último release

**Configuración del workflow:**

```yaml
- name: Create GitHub Release
  uses: actions/create-release@v1
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  with:
    tag_name: ${{ env.VERSION_TAG }}
    release_name: Release ${{ env.VERSION_TAG }}
    body: |
      Automated release from main branch
      
      Commit: ${{ github.sha }}
      Triggered by: ${{ github.actor }}
      
      For rollback instructions, see documentation.
    draft: false
    prerelease: false
```

### 6.5.2 Convención de Commits

Usamos prefijos para commits claros:

| Prefijo | Propósito | Ejemplo |
|---------|-----------|---------|
| `feat:` | Nueva funcionalidad | `feat: add product search endpoint` |
| `fix:` | Corrección de bug | `fix: correct order total calculation` |
| `docs:` | Documentación | `docs: update API documentation` |
| `chore:` | Mantenimiento | `chore: update dependencies` |
| `ci:` | Cambios en CI/CD | `ci: add OWASP ZAP security scan` |

---

## 6.6 Sistema de Etiquetado de Releases

### 6.6.1 Tags en Git

Cada release a producción crea automáticamente:

**Git Tag:**
```bash
git tag v2025.11.30-a1b2c3d
git push origin v2025.11.30-a1b2c3d
```

**GitHub Release:** Visible en la sección Releases del repositorio con:
- Nombre del release
- Descripción generada automáticamente
- Commit asociado
- Artefactos (opcional)

### 6.6.2 Tags de Imágenes Docker

Las imágenes en Azure Container Registry se etiquetan con:

| Tag | Uso | Persistencia |
|-----|-----|--------------|
| `{commit-sha}` | Identificación única | Permanente |
| `dev` | Última versión en dev | Sobrescrito |
| `latest` | Última versión en prod | Sobrescrito |

**Ejemplo:**
```
acrproyectoing.azurecr.io/api-gateway:a1b2c3d
acrproyectoing.azurecr.io/api-gateway:dev
acrproyectoing.azurecr.io/api-gateway:latest
```

---

## 6.7 Limpieza Automática (Ambiente Dev)

Para optimizar recursos en el namespace dev:

```yaml
- name: Clean up old deployments
  run: |
    kubectl delete deployment --all -n dev || true
    kubectl delete svc --all -n dev || true
```

## 6.8 Resumen del Flujo de Cambios

```
1. feature/fix → dev (PR + CI)
   ↓
2. Merge → Deploy automático a namespace dev
   ↓
3. dev → main (PR + Revisión)
   ↓
4. Aprobación manual en GitHub
   ↓
5. Merge → Deploy a prod + GitHub Release + Tag
```

**Controles implementados:**
- CI obligatorio antes de merge
- SonarCloud y Trivy en cada PR
- Aprobación manual para producción
- Release Notes automáticos
- Rollback documentado y probado



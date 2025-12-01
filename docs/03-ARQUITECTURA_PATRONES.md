## **Patrones de Diseño utilizados en la arquitectura existente**

### api-gateway

- Patrones Utilizados:
    - Gateway Routing y Load Balancing
- En donde se pueden evidenciar:
    - En la clase principal de este microservicio, específicamente en ApiGatewayApplication.java, se logra evidenciar la integración con Service Discovery usando @EnableEurekaClient
    - En api-gateway/src/main/resources/application.yml, la sección:
    
    ```yaml
    spring.cloud.gateway.routes:
    ```
    
    Se definen rutas a lb://ORDER-SERVICE, lb://PRODUCT-SERVICE, lb://PROXY-CLIENT, etc. Esto es Gateway + Client-side load balancing
    
- Estos patrones se evidencian porque el microservicio implementa Spring Cloud Gateway y actua como punto de entrada central a partir de rutas como lb:// y predicates Path.

### cloud-config

- Patrones Utilizados:
    - External Configuration
- En donde se pueden evidenciar:
    - En la clase principal de este microservicio, específicamente en CloudConfigApplication.java. Lo podemos observar al usar las anotaciones como @EnableConfigServer y @EnableEurekaClient y el uso de git URI en cloud-config y spring-cloud-starter-config. Esto ofrece centralización, soporte por perfil y desacoplamiento del ciclo de vida de la configuración
- El microservicio implementa de manera clara el patrón external configuration mediante un Spring Cloud Config Server que clona configuraciones de un repo Git con clientes que importan esa configuración al arrancar.

### product-service, user-service, order-service, payment-service, shipping-service, favourite-service, proxy-client

La mayoría de estos microservicios comparten los mismos patrones, como por ejemplo

- Patrón Utilizado:
    - Circuit Breaker / Resilience4j
- En donde se puede evidenciar:
    - La ruta común de los microservicios es  */src/main/resources/application.yml, en el apartado de Resilience4j config
- Las llamdas entre los microservicios pueden fallar, circuit breaker evita cascadas de fallo, mejora tiempo de recuperación y permite degradación controlada o fallbacks. En el proyecto se configura para aplicar límites y politicas por instancia/cliente.

### Observabilidad mínima - Vinculo con patrones de resiliencia

- Patrón Utilizado:
    - Health Endpoints Monitoring y uso de Zipkin
- En donde se puede evidenciar:
    - application.yml contiene management.endpoints.web.exposure.include:  y spring.zipkin.*
- Health Endpoint permiten a orquestadores como Kubernetes o scripts de monitoreo saber si una instancia está sana y si debe recibir tráfico. Es esencial donde hay circuit breakers, donde un servicio abierto puede seguir estando activo pero no sano para atender tráfico.

# **Patrones de Resiliencia y Configuración**

En el desarrollo de arquitecturas basadas en microservicios, la adopción de patrones de resiliencia, configuración dinámica y observabilidad resulta fundamental para garantizar la estabilidad operativa, el aislamiento de fallos y la capacidad de evolucionar sin interrupciones del servicio. En este apartado se describen los patrones implementados y las mejoras propuestas dentro del ecosistema de microservicios del proyecto, destacando su aporte a la calidad y robustez de la solución.

---

## **A. Implementación del patrón de resiliencia Bulkhead**

El patrón Bulkhead se implementa con el fin de evitar que una sobrecarga en una dependencia externa afecte la estabilidad general del microservicio consumidor. Su principio de operación consiste en compartimentar recursos (principalmente hilos o semáforos) de modo que, en escenarios de alta concurrencia o degradación externa, la capacidad del servicio no colapse en cascada.

### Justificación

La arquitectura del proyecto incluye múltiples interacciones entre microservicios (por ejemplo, product-service → payment-service o proxy-client → user-service). Dado que estas interacciones son potencialmente bloqueantes, resulta necesario limitar la cantidad de llamadas concurrentes para evitar:

* agotamiento del pool de hilos del microservicio,
* aumento descontrolado de la latencia,
* saturación de recursos compartidos,
* fallos sistémicos encadenados.

### Integración con Resilience4j

El patrón Bulkhead se implementa mediante la librería Resilience4j, la cual permite configurar aislamiento mediante semáforos o pools de hilos. La configuración se realiza de forma declarativa en cada microservicio que actúa como consumidor.

**Dependencias:**

```xml
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
  <version>2.0.2</version>
</dependency>
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-micrometer</artifactId>
  <version>2.0.2</version>
</dependency>
```

### Configuración del Bulkhead

```yaml
resilience4j:
  bulkhead:
    instances:
      externalServiceBulkhead:
        maxConcurrentCalls: 10
        maxWaitDuration: 0s
```

Esta configuración limita a diez el número máximo de llamadas simultáneas hacia una dependencia remota. Cuando el límite es alcanzado, se rechazan nuevas solicitudes sin bloquear hilos adicionales, evitando así la congestión interna.

### Aplicación en la capa de servicio

```java
@Service
public class ExternalClientService {

    @Bulkhead(name = "externalServiceBulkhead", type = Bulkhead.Type.SEMAPHORE)
    public ProductDto getProductFromRemote(String id) {
        // llamada hacia servicio externo
    }
}
```

Opcionalmente se incorpora un método de fallback, permitiendo responder de manera degradada cuando el Bulkhead se encuentra saturado.

---

## **B. Implementación del Patrón de Configuración — Feature Toggle**

Con el objetivo de habilitar la activación y desactivación dinámica de funcionalidades, se incorpora el patrón Feature Toggle, permitiendo al equipo controlar comportamientos sin necesidad de redeployar los servicios. Este patrón mejora sustancialmente la capacidad de experimentación, despliegues incrementales y mitigación de riesgos.

### Racional técnico

Dado que los microservicios del proyecto utilizan Spring Cloud Config Server, se aprovecha este mecanismo para centralizar configuraciones y habilitar toggles configurables desde el repositorio de configuración. Con ello se obtiene:

* habilitación de nuevas funcionalidades en caliente,
* control por ambiente (dev, stage, prod),
* despliegues seguros mediante dark-launch,
* rollback inmediato mediante cambio de configuración.

### Propiedad de configuración en Config Server

```yaml
features:
  enable-new-pricing: false
```

### Activación condicional de componentes

Mediante @ConditionalOnProperty se habilita o deshabilita por completo la carga de un bean asociado a una funcionalidad específica:

```java
@Configuration
@ConditionalOnProperty(prefix = "features", name = "enable-new-pricing", havingValue = "true")
public class NewPricingConfiguration {
    @Bean
    public PricingService newPricingService() {
        return new NewPricingServiceImpl();
    }
}
```

### Uso dinámico mediante Refresh Scope

```java
@RefreshScope
@Service
public class PricingFacade {

    @Value("${features.enable-new-pricing:false}")
    private boolean enableNewPricing;

    public PriceResponse calculate(...) {
        return enableNewPricing
            ? newPricingService.calculate(...)
            : legacyPricingService.calculate(...);
    }
}
```

La activación de una nueva funcionalidad puede realizarse mediante actualización del repositorio y ejecución del endpoint /actuator/refresh.

---

## **C. Mejora del Sistema de Observabilidad**

La observabilidad constituye un pilar crítico para garantizar diagnósticos efectivos, trazabilidad completa y monitoreo en tiempo real de la arquitectura distribuida. Se fortaleció la observabilidad mediante la integración unificada de métricas, trazas y logs correlacionados.

### Objetivos de la mejora

1. Exponer métricas operacionales y de resiliencia mediante Prometheus.
2. Implementar trazabilidad distribuida hacia Zipkin.
3. Integrar identificadores de traza en los logs para facilitar diagnóstico cruzado.
4. Monitorear el estado de los mecanismos de resiliencia implementados.
5. Proveer soporte para paneles de visualización y alertas mediante Grafana.

---

## 1. Integración de Prometheus

Se habilitan endpoints operacionales que exponen métricas estándar de Spring Boot, métricas propias de la JVM, y métricas relacionadas con Resilience4j.

**Dependencias:**

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## 2. Trazabilidad distribuida con Tracing y Zipkin

Se integran librerías para la propagación automática de `traceId` y `spanId` entre servicios, lo cual permite reconstruir flujos completos de usuario.

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
  <groupId>io.zipkin.brave</groupId>
  <artifactId>brave-instrumentation-http</artifactId>
</dependency>
```

**Configuración de muestreo:**

```yaml
spring:
  zipkin:
    base-url: http://zipkin:9411
  sleuth:
    sampler:
      probability: 0.1
```

---


## 3. Métricas de Resilience4j

Gracias a `resilience4j-micrometer`, se exponen métricas relacionadas con:

* estado de circuit-breakers,
* rechazos del Bulkhead,
* tiempos de espera,
* número de llamadas fallidas y exitosas.

Estas métricas se consumen desde Prometheus y permiten la construcción de dashboards de resiliencia en Grafana.

---

## 4. Exposición segura de endpoints

Para evitar filtración de información sensible, se limita la exposición de endpoints:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

---

# Conclusiones del apartado

La incorporación del patrón Bulkhead, el uso de Feature Toggles basados en configuración externa y la mejora integral del sistema de observabilidad fortalecen significativamente la resiliencia, escalabilidad y capacidad de operación del ecosistema de microservicios. La aplicación de estas técnicas permite:

* aislamiento efectivo de fallos,
* despliegue seguro de nuevas funcionalidades,
* diagnósticos rápidos y precisos,
* monitoreo continuo del estado del sistema,
* optimización del rendimiento operativo,
* capacidad para reaccionar proactivamente ante degradación.

Estos patrones constituyen un soporte arquitectónico esencial para garantizar un sistema robusto, confiable y adaptable a cambios futuros.
from random import choice

from locust import HttpUser, task, between


class UserBehavior(HttpUser):
    """
    Simula un usuario navegando por el catálogo de productos a través del API Gateway.

    Flujo:
      1) GET /product-service/api/products  -> se cachean los IDs reales.
      2) GET /product-service/api/products/{id} usando solo IDs válidos.

    Ejecutar (ejemplo):
      locust -f tests/load-test/locustfile.py --users 50 --spawn-rate 5 --host http://localhost:8080
    """

    # Espera entre 1 y 3 segundos entre peticiones
    wait_time = between(1, 3)

    # Cache local de IDs de productos válidos obtenidos del listado
    product_ids = []

    @task(3)
    def list_products(self):
        """
        Lista de productos y cacheo de IDs para usar en los detalles.
        Se le da más peso para simular más tráfico de navegación general.
        """
        with self.client.get(
            "/product-service/api/products", name="GET /products", catch_response=True
        ) as response:
            if response.ok:
                try:
                    data = response.json()
                    # El DTO de colección tiene normalmente la lista de items en alguna propiedad,
                    # aquí asumimos "content" o "items". Ajusta según tu contrato real.
                    items = (
                        data.get("content")
                        or data.get("items")
                        or data.get("dtoCollection")
                        or []
                    )
                    ids = [item.get("id") for item in items if item.get("id") is not None]
                    if ids:
                        self.product_ids = ids
                except Exception:
                    # No romper el test de carga si el JSON no es el esperado
                    pass

    @task(1)
    def product_detail(self):
        """
        Detalle de producto usando únicamente IDs conocidos.
        Si aún no hay IDs cacheados, primero fuerza una llamada a la lista.
        """
        if not self.product_ids:
            # Poblar IDs antes de hacer llamadas de detalle
            self.list_products()

        if not self.product_ids:
            # Si aún no hay IDs válidos, no intentamos el detalle
            return

        product_id = choice(self.product_ids)
        self.client.get(
            f"/product-service/api/products/{product_id}",
            name="GET /products/{id}",
        )


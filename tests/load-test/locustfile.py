import random
import string
import time
import warnings
from locust import HttpUser, task, between, events
from locust.exception import RescheduleTask

# Suprimir warnings de SSL para logs más limpios
warnings.filterwarnings('ignore', message='Unverified HTTPS request')

class EcommerceUser(HttpUser):
    """
    Simula un usuario completo realizando flujo de compra en e-commerce.
    
    Flujo realista:
      1. Navega catálogo (alta frecuencia)
      2. Se registra (baja frecuencia, solo una vez por sesión)
      3. Crea carrito (obligatorio antes de ordenar)
      4. Realiza pedido (frecuencia media, crítico)
    
    Ejecución:
      locust -f tests/load-test/locustfile.py \
             --host https://ecommerce.20.245.95.38.nip.io \
             --users 100 --spawn-rate 10 --run-time 5m \
             --html report.html
    """
    
    # Tiempo de espera entre peticiones (simula usuario real)
    wait_time = between(1, 4)
    
    def on_start(self):
        """Inicialización al comenzar la sesión del usuario virtual."""
        self.product_ids = []
        self.user_id = None
        self.cart_id = None
        self.registered = False
        self.cart_created = False
        
        # Datos del usuario para esta sesión
        timestamp = int(time.time() * 1000)
        self.username = f"loadtest_user_{timestamp}_{self._random_string(6)}"
        self.email = f"{self.username}@loadtest.local"
        self.password = self._generate_strong_password()
        
    def _random_string(self, length=8):
        """Genera string aleatorio para unicidad."""
        return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))
    
    def _generate_strong_password(self):
        """Genera contraseña que cumple requisitos de seguridad."""
        chars = string.ascii_letters + string.digits + "!@#$%"
        password = ''.join(random.choices(chars, k=12))
        return f"Test{password}!"
    
    # ==================== NAVEGACIÓN (Alta Frecuencia) ====================
    
    @task(3)
    def list_products(self):
        """
        Lista todos los productos disponibles.
        Peso: 3 - Simula navegación frecuente del catálogo.
        """
        endpoint = "/product-service/api/products"
        
        with self.client.get(
            endpoint,
            name="GET /products [LIST]",
            catch_response=True,
            verify=False,
            timeout=10
        ) as response:
            try:
                if not response.ok:
                    error_msg = (
                        f"FALLO EN LIST_PRODUCTS\n"
                        f"   URL: {self.client.base_url}{endpoint}\n"
                        f"   Status: {response.status_code}\n"
                        f"   Body: {response.text[:500]}"
                    )
                    print(error_msg)
                    response.failure(f"HTTP {response.status_code}")
                    return
                
                data = response.json()
                
                # Manejo del campo 'collection'
                if isinstance(data, list):
                    items = data
                elif isinstance(data, dict):
                    items = (
                        data.get("collection") or
                        data.get("content") or 
                        data.get("items") or 
                        data.get("dtoCollection") or 
                        data.get("products") or
                        []
                    )
                else:
                    items = []
                
                # Extraer IDs de productos
                product_ids = []
                for item in items:
                    if isinstance(item, dict):
                        pid = (
                            item.get("productId") or 
                            item.get("id") or 
                            item.get("product_id")
                        )
                        if pid:
                            product_ids.append(pid)
                
                if product_ids:
                    self.product_ids = product_ids
                    print(f"Productos cacheados: {len(product_ids)} items (IDs: {product_ids[:5]}...)")
                    response.success()
                else:
                    sample_item = items[0] if items else None
                    error_msg = (
                        f"No se encontraron IDs de productos\n"
                        f"   Total items: {len(items)}\n"
                        f"   Sample item: {sample_item}"
                    )
                    print(error_msg)
                    response.failure(error_msg)
                    
            except Exception as e:
                error_msg = f"Error: {type(e).__name__}: {str(e)}"
                print(error_msg)
                response.failure(error_msg)
    
    @task(2)
    def get_product_detail(self):
        """
        Obtiene detalle de un producto específico.
        Peso: 2 - Usuario interesado ve detalles antes de comprar.
        """
        if not self.product_ids:
            raise RescheduleTask()
        
        product_id = random.choice(self.product_ids)
        endpoint = f"/product-service/api/products/{product_id}"
        
        with self.client.get(
            endpoint,
            name="GET /products/{id} [DETAIL]",
            catch_response=True,
            verify=False,
            timeout=10
        ) as response:
            try:
                if not response.ok:
                    response.failure(f"HTTP {response.status_code}")
                    return
                
                data = response.json()
                
                if data.get("productId") or data.get("id"):
                    response.success()
                else:
                    response.failure("Invalid product structure")
                    
            except Exception as e:
                response.failure(f"Error: {str(e)}")

    # ==================== REGISTRO (Baja Frecuencia) ====================
    
    @task(1)
    def register_user(self):
        """
        Registra un nuevo usuario.
        Peso: 1 - Solo se ejecuta una vez por sesión de usuario virtual.
        """
        if self.registered:
            return
        
        payload = {
            "firstName": f"LoadTest{self._random_string(4)}",
            "lastName": f"User{self._random_string(4)}",
            "email": self.email,
            "phone": f"555{random.randint(1000000, 9999999)}",
            "imageUrl": "https://via.placeholder.com/150",
            "credential": {
                "username": self.username,
                "password": self.password,
                "isEnabled": True,
                "isAccountNonExpired": True,
                "isAccountNonLocked": True,
                "isCredentialsNonExpired": True,
                "roleBasedAuthority": "ROLE_USER"
            }
        }
        
        endpoint = "/user-service/api/users"
        
        with self.client.post(
            endpoint,
            json=payload,
            name="POST /users [REGISTER]",
            catch_response=True,
            verify=False,
            timeout=15
        ) as response:
            try:
                if response.status_code not in [200, 201]:
                    error_msg = (
                        f"FALLO EN REGISTER_USER\n"
                        f"   Status: {response.status_code}\n"
                        f"   Body: {response.text[:500]}"
                    )
                    print(error_msg)
                    response.failure(f"HTTP {response.status_code}")
                    return
                
                data = response.json()
                user_id = data.get("userId") or data.get("id")
                
                if user_id:
                    self.user_id = user_id
                    self.registered = True
                    print(f"Usuario registrado: ID={user_id}, Username={self.username}")
                    response.success()
                else:
                    error_msg = f"No userId in response. Keys: {list(data.keys())}"
                    print(error_msg)
                    response.failure(error_msg)
                    
            except Exception as e:
                error_msg = f"Error registrando usuario: {type(e).__name__}: {str(e)}"
                print(error_msg)
                response.failure(error_msg)

    # ==================== CARRITO Y PEDIDO (Criticidad Alta) ====================
    
    @task(2)
    def create_cart(self):
        """
        Crea un carrito para el usuario.
        Peso: 2 - OBLIGATORIO antes de crear orden.
        """
        # Guardia: Solo crear carrito si usuario está registrado y no tiene carrito
        if not self.user_id or self.cart_created:
            return
        
        payload = {"userId": self.user_id}
        endpoint = "/order-service/api/carts"
        
        with self.client.post(
            endpoint,
            json=payload,
            name="POST /carts [CREATE]",
            catch_response=True,
            verify=False,
            timeout=10
        ) as response:
            try:
                if response.status_code not in [200, 201]:
                    error_msg = (
                        f"FALLO EN CREATE_CART\n"
                        f"   Status: {response.status_code}\n"
                        f"   Body: {response.text[:500]}"
                    )
                    print(error_msg)
                    response.failure(f"HTTP {response.status_code}")
                    return
                
                data = response.json()
                cart_id = data.get("cartId") or data.get("id")
                
                if cart_id:
                    self.cart_id = cart_id
                    self.cart_created = True
                    print(f"Carrito creado exitosamente: CartID={cart_id}, UserID={self.user_id}")
                    response.success()
                else:
                    error_msg = f"No cartId retornado. Response: {data}"
                    print(error_msg)
                    response.failure(error_msg)
                    
            except Exception as e:
                error_msg = f"Error creando carrito: {type(e).__name__}: {str(e)}"
                print(error_msg)
                response.failure(error_msg)
    
    @task(3)
    def place_order(self):
        """
        Realiza un pedido completo.
        Peso: 3 - **CRÍTICO** - Simula transacción de compra real.
        """
        if not self.user_id:
            print("No se puede crear orden: usuario no registrado")
            raise RescheduleTask()
        
        if not self.cart_id or not self.cart_created:
            print("No se puede crear orden: carrito no creado. Replanificando...")
            raise RescheduleTask()
        
        if not self.product_ids:
            print("No se puede crear orden: sin productos vistos")
            raise RescheduleTask()
        
        # Generar orden con datos realistas
        selected_product = random.choice(self.product_ids)
        order_fee = round(random.uniform(10.0, 500.0), 2)
        
        # PAYLOAD CORRECTO: Incluye userId dentro de cart
        payload = {
            "cart": {
                "cartId": self.cart_id,
                "userId": self.user_id
            },
            "orderDesc": f"Load Test Order - Product {selected_product}",
            "orderFee": order_fee
        }
        
        endpoint = "/order-service/api/orders"
        
        with self.client.post(
            endpoint,
            json=payload,
            name="POST /orders [PLACE ORDER]",
            catch_response=True,
            verify=False,
            timeout=15
        ) as response:
            try:
                if response.status_code not in [200, 201]:
                    error_msg = (
                        f"FALLO EN PLACE_ORDER\n"
                        f"   Status: {response.status_code}\n"
                        f"   Payload enviado: {payload}\n"
                        f"   Body respuesta: {response.text[:800]}"
                    )
                    print(error_msg)
                    response.failure(f"HTTP {response.status_code}")
                    return
                
                data = response.json()
                order_id = data.get("orderId") or data.get("id")
                
                if order_id:
                    print(f"Orden creada exitosamente: OrderID={order_id}, CartID={self.cart_id}, Fee=${order_fee}")
                    response.success()
                    
                    self.cart_id = None
                    self.cart_created = False
                else:
                    error_msg = f"No orderId en respuesta. Response: {data}"
                    print(error_msg)
                    response.failure(error_msg)
                    
            except Exception as e:
                error_msg = f"Error en place_order: {type(e).__name__}: {str(e)}"
                print(error_msg)
                response.failure(error_msg)


# ==================== EVENT HOOKS PARA MÉTRICAS ====================

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Evento al iniciar test de carga."""
    print("\n" + "="*70)
    print("INICIANDO PRUEBA DE CARGA E-COMMERCE")
    print("="*70)
    print(f"Target: {environment.host}")
    print("="*70 + "\n")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Evento al finalizar test de carga."""
    print("\n" + "="*70)
    print("PRUEBA DE CARGA FINALIZADA")
    print("="*70)
    
    stats = environment.stats
    total = stats.total.num_requests
    failed = stats.total.num_failures
    success_rate = ((total - failed) / total * 100) if total > 0 else 0
    
    print(f"Total de requests: {total}")
    print(f"Requests fallidas: {failed}")
    print(f"Tasa de éxito: {success_rate:.2f}%")
    print(f"Tiempo promedio de respuesta: {stats.total.avg_response_time:.2f} ms")
    print(f"RPS (Requests por segundo): {stats.total.total_rps:.2f}")
    
    # Análisis por endpoint
    print("\nMétricas por Endpoint:")
    for name, stat in stats.entries.items():
        if stat.num_requests > 0:
            endpoint_success = ((stat.num_requests - stat.num_failures) / stat.num_requests * 100)
            print(f"   {name}: {endpoint_success:.1f}% éxito ({stat.num_requests} reqs, {stat.avg_response_time:.0f}ms avg)")
    
    print("="*70 + "\n")


# ==================== CONFIGURACIÓN ADICIONAL ====================

class QuickTestUser(HttpUser):
    """
    Usuario rápido para smoke tests.
    
    Ejecución:
      locust -f tests/load-test/locustfile.py QuickTestUser \
             --host https://ecommerce.20.245.95.38.nip.io \
             --users 10 --spawn-rate 5 --run-time 1m
    """
    wait_time = between(0.5, 1.5)
    
    @task
    def quick_health_check(self):
        """Health check rápido de productos."""
        self.client.get(
            "/product-service/api/products",
            name="[SMOKE] GET /products",
            verify=False
        )

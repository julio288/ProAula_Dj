# Registro y gestión de usuarios

Aplicación web para registrar cuentas, iniciar sesión y administrar usuarios con Spring
Boot, Spring Security, Thymeleaf y MySQL.

## Tecnologías

- Java 25
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- Thymeleaf
- MySQL

## Requisitos

- JDK 25
- MySQL en ejecución
- Maven Wrapper incluido (`mvnw` / `mvnw.cmd`)

## Material complementario

- Guía de OAuth 2.0 y OpenID Connect: [Guia_Taller_OAuth2_OIDC_Spring_Security.pdf](docs/Guia_Taller_OAuth2_OIDC_Spring_Security.pdf)
- Guía del taller: [Guia_Taller_JWT_SpringSecurity_MySQL.pdf](docs/Guia_Taller_JWT_SpringSecurity_MySQL.pdf)
- Respuestas de análisis y checklist de evidencias OIDC: [entrega-oauth2-oidc.md](docs/entrega-oauth2-oidc.md)

## Preparar la base de datos

Crea la base de datos de desarrollo:

```sql
CREATE DATABASE universidad CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

La configuración local usa `localhost:3306/universidad`, el usuario `root` y contraseña
vacía, como en una instalación local típica de XAMPP. Si tus datos son distintos, define
`DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en el archivo `.env` local o en el entorno. La API
JWT requiere además `JWT_SECRET`, una clave aleatoria de al menos 32 bytes. En PowerShell
puedes generarla con:

```powershell
$bytes = [byte[]]::new(32)
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[BitConverter]::ToString($bytes).Replace('-', '')
$rng.Dispose()
```

Guarda el resultado como `JWT_SECRET` en `.env`. La aplicación no inicia si falta esta
clave o tiene menos de 32 bytes. El archivo `.env` está excluido de Git;
no guardes ni compartas credenciales en archivos versionados. Puedes usar `.env.example`
como plantilla.

Hibernate actualiza el esquema de `users` al iniciar (`spring.jpa.hibernate.ddl-auto=update`).
La aplicación no utiliza tablas de migración.

## Ejecutar en desarrollo

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En macOS o Linux:

```bash
./mvnw spring-boot:run
```

Abre <http://localhost:8080>.

## Portal de transporte de carga

La aplicación ofrece dos espacios con permisos separados, con una interfaz adaptable, jerarquía visual de alto contraste y animaciones específicas por pantalla. Los efectos de movimiento se reducen cuando el sistema operativo tiene activada la preferencia `prefers-reduced-motion`:

- **Cliente (`ROLE_USER`)**: consulta rutas, horarios, tarifas de referencia en COP y avisos; crea solicitudes de carga y consulta únicamente su propio historial y estado.
- **Administrador (`ROLE_ADMIN`)**: administra cuentas, publica/pausa rutas, actualiza horarios y tarifas, publica avisos y mueve las solicitudes por los estados recibida, cotizada, en tránsito, entregada o cancelada.

Al iniciar con el perfil `default`, `dev` o `prod` (excepto `test`), se crean datos ficticios colombianos de rutas y avisos si esas tablas están vacías, y solicitudes de demostración si aún no existen solicitudes. La generación es idempotente y no reemplaza registros existentes. Las tarifas y el seguimiento son de muestra; no representan una cotización ni una operación logística real.

La tarifa estimada se calcula con el peso por el valor por kilogramo de la ruta, respetando la tarifa mínima. Las solicitudes nuevas comienzan como **Recibida**; los estados solo avanzan por el flujo operativo permitido. Las tablas `freight_lanes`, `transport_notices` y `freight_requests` se crean/actualizan al iniciar mediante Hibernate (`ddl-auto=update`).

## API REST con JWT

La API JWT es adicional al login web con sesión: el formulario y las vistas siguen
funcionando igual. La API usa usuarios persistidos en MySQL y el correo electrónico como
`username`, de acuerdo con el modelo existente. Las contraseñas se protegen con BCrypt.

| Método | Ruta | Acceso | Resultado |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Público | Registra al usuario y responde `201`; un correo duplicado responde `409` |
| `POST` | `/api/auth/token` | Público | Valida credenciales y entrega un JWT Bearer HS256 con duración de 30 minutos |
| `GET` | `/api/secured` | Bearer JWT | Devuelve el nombre autenticado; sin token o con uno inválido responde `401` |

Ejemplo de registro o autenticación:

```json
{
  "username": "persona@ejemplo.test",
  "password": "ClaveSegura123!"
}
```

Envía el token recibido en `Authorization: Bearer <token>` al endpoint protegido. La API
es stateless y solo desactiva CSRF para `/api/**`; la protección CSRF del login web con
sesión se mantiene.

Puedes probar el flujo completo desde [requests.http](requests.http).

## Cuentas de demostración

La aplicación crea automáticamente cuentas de demostración cuando la base de datos está vacía en el perfil por defecto o `dev`:

- Usuario: `demo.usuario@universidad.test` / `Demo123!`
- Administrador: `demo.admin@universidad.test` / `Admin123!`

Puedes personalizar estas credenciales con `APP_DEMO_USER_PASSWORD` y `APP_DEMO_ADMIN_PASSWORD` si lo necesitas. Son solo para desarrollo y demostración: no las uses en un despliegue público. El archivo [`.env.example`](.env.example) recoge las variables de entorno principales del proyecto.

## Autenticación y autorización

Estos conceptos representan controles distintos en la aplicación:

- **Autenticación:** Spring Security valida el correo y la contraseña en `/login`. Si las
  credenciales son correctas, inicia la sesión y redirige al panel `/dashboard`.
- **Autorización:** las rutas `/admin/**` y `/users/**` requieren el rol `ADMIN`. Una cuenta
  autenticada como `USER` recibe HTTP 403 y ve la página explicativa de acceso restringido.
- El resto de las rutas protegidas requiere que la persona haya iniciado sesión.
- Las contraseñas se almacenan usando BCrypt. El cierre de sesión invalida la sesión y
  elimina la cookie `JSESSIONID`.

El registro normal asigna el rol `USER`. Para permitir registros con rol `ADMIN`, configura
`APP_ADMIN_REGISTRATION_CODE` con un código privado y compártelo solo con quien deba usarlo.
Si no está configurado, el registro de administradores queda deshabilitado.

El acceso con Google también abre el panel correspondiente. La primera autenticación con un
correo verificado crea automáticamente una cuenta de cliente; los siguientes accesos
reutilizan esa cuenta y sus solicitudes. Si el correo ya pertenece a una cuenta local, se
vincula a esa cuenta sin cambiar su contraseña ni su rol.

## Perfiles

El perfil predeterminado está pensado para desarrollo: desactiva la caché de Thymeleaf y
activa registros SQL útiles para depuración. Para iniciar con el perfil `prod`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

En macOS o Linux:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Antes de desplegar, configura credenciales MySQL seguras mediante `DB_URL`, `DB_USERNAME` y
`DB_PASSWORD`. No uses valores locales de XAMPP en producción.

En el perfil `prod`, el sembrador crea 20 usuarios aleatorios cuando `users` está vacía.
También puede crear las cuentas de demostración `prueba.usuario@universidad.test` y
`prueba.admin@universidad.test` si defines `APP_SEED_TEST_PASSWORD`. La contraseña debe
tener entre 8 y 72 caracteres; se almacena con BCrypt y no se imprime en los registros.
No habilites estas cuentas de prueba ni el código de registro de administrador en un
despliegue público.

## Organización del código

El código fuente está bajo `src/main/java/com/taller/registro_login`, organizado por
responsabilidad:

| Paquete | Responsabilidad |
| --- | --- |
| `appcontroller` | Rutas web y selección de vistas |
| `config` | Configuración de seguridad |
| `dto` | Formularios y validación de entrada |
| `model` | Entidades JPA |
| `repository` | Acceso a datos |
| `security` | Identidad y carga de usuarios para Spring Security |
| `service` | Reglas de negocio y sembrado de datos de demostración |

Las vistas Thymeleaf están en `src/main/resources/templates` y los recursos web en
`src/main/resources/static`. Los nombres de carpetas y archivos estáticos usan minúsculas
para que las rutas funcionen también en sistemas sensibles a mayúsculas, como Linux.

## Pruebas

Las pruebas de integración usan una base MySQL separada. Créala antes de ejecutar la suite:

```sql
CREATE DATABASE universidad_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Por defecto, las pruebas se conectan a `localhost:3306/universidad_test` como `root` sin
contraseña. Puedes sustituir la configuración con `TEST_DB_URL`, `DB_USERNAME` y
`DB_PASSWORD`. Ejecuta la suite en Windows:

```powershell
.\mvnw.cmd test
```

En macOS o Linux:

```bash
./mvnw test
```

## Inicio de sesión con Google (OAuth 2.0 y OpenID Connect)

El inicio de sesión OIDC se agrega al formulario local sin reemplazarlo. Cuando el perfil
`google` está activo, la portada y `/login` muestran el acceso con Google; después del
callback, Spring Security exige un correo verificado, crea o reutiliza la cuenta interna y
redirige al cliente a `/dashboard` o al administrador a `/admin`. Así Google y el formulario
local usan los mismos paneles y funcionalidades. La vista `/perfil` permanece disponible
para consultar las claims OIDC; el formulario POST de `/logout` cierra la sesión y vuelve a
la portada.

### Registrar el cliente OAuth en Google

1. Entra a [Google Auth Platform](https://console.cloud.google.com/auth) con tu propia
   cuenta y crea o selecciona un proyecto.
2. Completa Branding y Audience. Si la aplicación está en modo de pruebas, agrega tu
   cuenta como usuario de prueba.
3. Crea un cliente OAuth de tipo **Web application**.
4. Agrega esta URI de redirección autorizada exactamente:
   `http://localhost:8080/login/oauth2/code/google`
5. Copia el Client ID y Client Secret. Son credenciales privadas de tu proyecto de Google;
   esta aplicación no las genera ni las almacena en el repositorio.

### Configurar y ejecutar

1. Copia `.env.example` a `.env` en la carpeta `registro-login` si aún no tienes ese
   archivo. Completa `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` con los valores que Google
   te entregó. Mantén `.env` local: está excluido de Git. No pegues secretos en el código,
   capturas, commits o mensajes.
2. Conserva también las variables locales que ya requiere esta aplicación, en particular
   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`. El login OIDC no reemplaza la
   base de datos ni la clave JWT requeridas por el resto del proyecto.
3. Desde `registro-login`, inicia el perfil de Google en PowerShell:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=google"
   ```

4. Abre <http://localhost:8080>, selecciona **Continuar con Google** y autoriza una cuenta
   habilitada en la audiencia de prueba. También puedes iniciar desde
   <http://localhost:8080/login>.

Sin el perfil `google`, la autenticación local actual sigue funcionando y no se muestra
un enlace que no pueda usarse. El cliente OIDC se configura solo desde variables de
entorno/`.env`; no hay credenciales de Google en los archivos versionados. En producción,
registra únicamente una URI HTTPS que pertenezca a tu despliegue y usa un gestor de
secretos del entorno.

### Alcance del seguimiento

El panel permite consultar rutas, tarifas de referencia, crear solicitudes y ver sus
estados de demostración. Este proyecto todavía no integra mapas, GPS ni ubicación en tiempo
real; los datos de seguimiento son ficticios y no deben interpretarse como la localización
real de un paquete.

### Verificación de la entrega

La suite automatizada cubre la protección y redirección OIDC, la creación/reutilización de
la cuenta y el acceso del cliente Google al panel y a sus solicitudes, además del cierre de
sesión y la compatibilidad con el perfil local. Para completar la evidencia
que depende de Google, ejecuta además los seis pasos P01–P06 y guarda capturas sin
credenciales, tokens ni cookies en `evidencias/oauth2/`. Para P04 usa una cuenta de prueba
y entrega la captura solo por un canal privado. Las respuestas a las preguntas de análisis
están en [`docs/entrega-oauth2-oidc.md`](docs/entrega-oauth2-oidc.md).

## Monitoreo de red en Windows Server

El panel administrativo `/admin/monitoring` ejecuta sondeos TCP periódicos sobre los
destinos configurados, muestra disponibilidad y latencia observadas, registra cambios
de estado y consulta Windows Event Log cuando se ejecuta en Windows. Los eventos
recientes se conservan en memoria (hasta 500 entradas) y se vuelven a recopilar tras un
reinicio. El acceso al panel continúa protegido por `ROLE_ADMIN`.

Configura `MONITORING_TARGETS` en el entorno del servidor, sin modificar el código:

```text
MONITORING_TARGETS=Web|<host autorizado>|<puerto>|TCP|Servicio web;Base de datos|<host autorizado>|<puerto>|TCP|Servicio de datos
```

Cada destino debe tener nombre único, host, puerto, protocolo TCP y descripción. No
incluyas los signos `< >`; reemplázalos por los valores reales definidos por redes. No
se aceptan destinos desde formularios web, protocolos distintos de TCP ni direcciones
de ejemplo. Deja la variable vacía hasta tener la lista autorizada. El sondeo confirma
que se puede abrir una conexión TCP al puerto; no sustituye una prueba funcional del
protocolo o de la aplicación.

`MONITORING_POLL_INTERVAL` establece el intervalo (10 segundos a 1 hora; predeterminado
30 segundos) y `MONITORING_CONNECT_TIMEOUT` el tiempo máximo de conexión (hasta 10
segundos; predeterminado 2 segundos). En Windows,
`MONITORING_WINDOWS_EVENT_LOG_ENABLED` activa la lectura de canales con `wevtutil`;
por defecto se consultan `System`, `Application` y `Security`.
`MONITORING_WINDOWS_EVENT_LOG_CHANNELS` permite definir otros canales separados por
coma. Para leer canales protegidos como `Security`, asigna a la cuenta del servicio
solo los permisos de lectura necesarios para los canales configurados; habilita las
directivas de auditoría pertinentes si necesitas eventos de seguridad de red. No
ejecutes la aplicación como administrador para evitar configurar permisos.

La clasificación inicial identifica indisponibilidad de destinos y eventos de Windows
con IDs asociados a fallos de inicio de sesión, cambios de cuentas, bloqueos de red y
fallos de servicios/aplicaciones. No bloquea tráfico ni modifica el firewall. El estado
de acceso a los canales se muestra en el panel para que los errores de permisos o de
configuración no se confundan con ausencia de eventos.

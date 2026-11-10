# Entrega: OAuth 2.0 y OpenID Connect

## Respuestas de análisis

1. **¿Cuál es la diferencia entre autenticación y autorización?**  
   La autenticación verifica quién es la persona que inició sesión. La autorización decide
   qué recursos puede usar esa persona una vez identificada.

2. **¿Por qué OAuth 2.0 no es por sí mismo un protocolo de autenticación?**  
   OAuth 2.0 delega autorización para acceder a recursos; no define por sí solo un formato
   estándar para afirmar la identidad del usuario.

3. **¿Qué añade OpenID Connect al flujo OAuth 2.0?**  
   OIDC agrega una capa de identidad interoperable, incluido el ID Token con claims
   autenticados y endpoints estándar de descubrimiento.

4. **¿Qué función cumple el ID Token?**  
   Comunica a la aplicación la identidad autenticada y sus claims. Su claim `sub` identifica
   de forma estable al usuario dentro del emisor; no es una contraseña ni un token para
   autorizar cualquier API.

5. **¿Qué sucede cuando alguien solicita `/perfil` sin sesión?**  
   Spring Security lo considera un recurso protegido y redirige a `/login`. Desde esa página
   se puede iniciar el flujo de Google si está activa la configuración OIDC.

6. **¿Por qué no se debe publicar el Client Secret?**  
   Es una credencial confidencial del cliente OAuth. Si se filtra, otra persona podría
   suplantar la aplicación en el flujo OAuth; debe guardarse fuera del repositorio y
   rotarse si se expone.

7. **¿Qué diferencia hay entre cerrar sesión en la aplicación y en Google?**  
   El logout de esta aplicación invalida la sesión local y su cookie. No cierra la sesión
   general del navegador en Google, por lo que Google puede autenticar de nuevo sin pedir
   la contraseña.

## Evidencias manuales P01–P06

La autenticación real con Google necesita un cliente OAuth propio y una cuenta permitida
como usuario de prueba. No se incluyen credenciales ni capturas simuladas. Después de
configurar Google y ejecutar el perfil `google`, guardar las capturas siguientes en
`evidencias/oauth2/`:

| ID | Acción | Resultado esperado | Archivo sugerido |
| --- | --- | --- | --- |
| P01 | Abrir `/` | Portada pública con la opción de Google | `P01-inicio.png` |
| P02 | Abrir `/perfil` sin sesión | Acceso protegido; redirección a `/login` | `P02-perfil-protegido.png` |
| P03 | Iniciar sesión con Google | Retorno autenticado a `/perfil` | `P03-login-google.png` |
| P04 | Revisar el perfil | Nombre, correo y `sub` visibles | `P04-perfil-oidc.png` |
| P05 | Seleccionar **Cerrar sesión** | La aplicación vuelve a `/` | `P05-logout.png` |
| P06 | Abrir `/perfil` después del logout | Se requiere autenticación otra vez | `P06-perfil-tras-logout.png` |

Antes de guardar o compartir evidencia, verifica que no muestre Client Secret, códigos de
autorización, tokens o cookies. P04 muestra deliberadamente identidad; usa una cuenta de
prueba dedicada y comparte esa captura solo con quien evalúe la entrega.

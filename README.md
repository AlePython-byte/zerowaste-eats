# ZeroWaste Eats

Aplicación Android para rescatar comida en Pasto. Los negocios (panaderías, restaurantes, fruterías) publican al final del día lo que no vendieron, con descuento. Los compradores lo encuentran cerca, lo reservan y lo recogen en el local con un código corto. Así se aprovechan alimentos en buen estado y cada persona ve cuánto ha ahorrado y cuánta comida ha rescatado.

La app tiene dos espacios:

- **Comprador:** ofertas cercanas en lista y mapa, búsqueda y filtros, favoritos, reserva con código de recogida (ZW-XXXX), pedidos y estadísticas de impacto.
- **Negocio:** panel con métricas, publicación de ofertas con foto y sugerencias con IA (título, descripción y precio), reservas recibidas y canje del código del cliente.

Funciona en **modo real** (cuentas y datos en el servidor) y en **modo demostración** (todo en el teléfono, sin internet).

## Capturas

<!-- Reemplaza cada marcador por la imagen correspondiente, guardada en docs/capturas/ -->

| Comprador | | |
|---|---|---|
| **[CAPTURA: Inicio con ofertas cercanas]** `docs/capturas/inicio.png` | **[CAPTURA: Mapa de Explorar]** `docs/capturas/mapa.png` | **[CAPTURA: Detalle con selector de cantidad]** `docs/capturas/detalle.png` |
| **[CAPTURA: Hoja «¡Reservado!» con el código]** `docs/capturas/reservado.png` | **[CAPTURA: Pedidos activos]** `docs/capturas/pedidos.png` | **[CAPTURA: Perfil con impacto]** `docs/capturas/perfil.png` |

| Negocio | | |
|---|---|---|
| **[CAPTURA: Panel con métricas]** `docs/capturas/panel.png` | **[CAPTURA: Editor de oferta con sugerencia de IA]** `docs/capturas/editor.png` | **[CAPTURA: Canje del código de recogida]** `docs/capturas/canje.png` |

## Stack

| Parte | Tecnología |
|---|---|
| App | Kotlin, Jetpack Compose (Material 3), Navigation Compose, ViewModel + StateFlow |
| Red | Retrofit, OkHttp, kotlinx.serialization |
| Imágenes y mapa | Coil, Google Maps Compose, servicios de ubicación de Google Play |
| Servidor | NestJS sobre Node.js 24, TypeScript |
| Base de datos | PostgreSQL en Supabase, con Prisma 7 (migraciones versionadas) |
| Cuentas y fotos | Supabase Auth y Supabase Storage |
| Sugerencias de ofertas | API de Anthropic (sin clave, funcionan en modo simulado) |
| Despliegue | Render (servicio web, plan gratuito) |

Estructura del repositorio:

```
android/   App Android (Gradle)
server/    API REST (NestJS + Prisma)
docs/      Licencias y SQL de Supabase Storage
render.yaml  Despliegue del servidor en Render
```

## Cómo correrlo en local

### Requisitos

- Android Studio (o el SDK de Android) y un teléfono con Android 8.0 o superior, o un emulador.
- Node.js 24 y npm.
- Un proyecto de Supabase (base de datos, autenticación y almacenamiento).
- Una clave de Google Maps para Android.

### Servidor

```bash
cd server
npm install
cp .env.example .env          # y completa los valores (ver «Variables de entorno»)
npx prisma migrate deploy     # crea las tablas en la base de datos
npm run start:dev             # http://localhost:3000/api/v1
```

Comprueba que responde en `http://localhost:3000/api/v1/salud` y, con la base de datos, en `http://localhost:3000/api/v1/salud/base-datos`.

Pruebas y análisis estático:

```bash
npm test
npm run lint
```

### App Android

1. Copia `android/local.properties.example` como `android/local.properties` y completa los valores. Ese archivo no se sube al repositorio.
2. Abre la carpeta `android/` en Android Studio, o compila desde la terminal:

```bash
cd android
gradlew.bat assembleDebug testDebugUnitTest      # Windows
./gradlew assembleDebug testDebugUnitTest        # macOS / Linux
```

3. Para usar el servidor local:
   - **Emulador:** el valor por defecto `http://10.0.2.2:3000/api/v1/` ya apunta al computador.
   - **Teléfono por USB:** ejecuta `adb reverse tcp:3000 tcp:3000` y usa `API_BASE_URL_DEBUG=http://localhost:3000/api/v1/`.

Sin endpoints configurados la app arranca igual, solo con datos de demostración.

## Cómo desplegar

### Servidor en Render

El archivo `render.yaml` describe el servicio: raíz en `server/`, construcción con `npm ci --include=dev && npm run build`, arranque con `npx prisma migrate deploy && npm run start:prod` (las migraciones pendientes se aplican antes de arrancar) y chequeo de salud en `/api/v1/salud`. No contiene secretos: Render los pide al crear el servicio.

1. En Render: **New → Blueprint**, conecta el repositorio y elige la rama que quieres desplegar.
2. Completa las variables que Render pide (ver la tabla siguiente) y crea el servicio.
3. Cuando termine el despliegue, abre `https://<tu-servicio>.onrender.com/api/v1/salud`.

La base de datos debe usar el **pooler de Supabase en modo sesión** (puerto 5432): Render solo sale por IPv4 y la conexión directa de Supabase es IPv6.

### App de release (APK de demostración)

1. En `android/local.properties`, define `API_BASE_URL_RELEASE=https://<tu-servicio>.onrender.com/api/v1/` (con `/` al final).
2. Opcional: define el almacén de claves (`RELEASE_STORE_FILE` y compañía). Sin él, el APK se firma con la clave de depuración. Sirve para una demostración, no para publicar en una tienda.
3. Compila:

```bash
cd android
gradlew.bat assembleRelease
```

4. El APK queda en `android/app/build/outputs/apk/release/app-release.apk`. Instálalo con `adb install -r app-release.apk`, o cópialo al teléfono y ábrelo (Android pedirá permitir apps de origen desconocido).

El build de release va minificado con R8. Las reglas de `android/app/src/main/keepRules/network.keep` conservan lo que Retrofit y kotlinx.serialization usan por reflexión. Solo admite HTTPS.

## Variables de entorno

Solo se listan los nombres. Los valores nunca van al repositorio.

**Servidor** (`server/.env` en local, panel de Render en producción):

| Variable | Obligatoria | Para qué sirve |
|---|---|---|
| `NODE_ENV` | sí en producción | `development`, `test` o `production` |
| `PORT` | no | Puerto de escucha (Render lo pone solo) |
| `DATABASE_URL` | sí | Conexión a PostgreSQL de Supabase (pooler en modo sesión) |
| `SUPABASE_URL` | sí | URL del proyecto de Supabase |
| `SUPABASE_PUBLISHABLE_KEY` | sí | Clave publicable de Supabase |
| `ANTHROPIC_API_KEY` | no | Sugerencias de ofertas con IA; sin ella, modo simulado |
| `AI_MODEL` | no | Modelo usado para las sugerencias |
| `AI_DAILY_LIMIT` | no | Sugerencias por negocio y día |
| `CORS_ORIGINS` | no | Orígenes web permitidos (la app no lo necesita) |
| `RATE_LIMIT_PER_MINUTE` | no | Peticiones por cliente y minuto |
| `OBSERVE_APP_KEY`, `OBSERVE_APP_SECRET` | no | Telemetría opcional (ambas o ninguna) |

**App** (`android/local.properties`):

| Variable | Para qué sirve |
|---|---|
| `MAPS_API_KEY` | Google Maps |
| `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` | Inicio de sesión y fotos |
| `API_BASE_URL_DEBUG` | Servidor para los builds de depuración |
| `API_BASE_URL_RELEASE` | Servidor desplegado para los builds de release |
| `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Firma de release (opcional) |

## Cuentas y datos de demostración

**Modo demostración** (todo en el teléfono, sin servidor ni internet): en la pantalla de ingreso activa **«Usar datos de demostración»**. Mientras está activo, una insignia «Modo demo» se ve arriba en cada pantalla.

| Rol | Correo | Contraseña |
|---|---|---|
| Comprador | `comprador@demo.co` | `Demo1234` |
| Negocio | `negocio@demo.co` | `Demo1234` |

El comprador demo trae pedidos de ejemplo. El negocio demo trae ofertas y reservas con códigos de prueba. Lo que se cambia se pierde al cerrar la app.

**Modo real:** crea las cuentas desde la propia app («Crear cuenta», eligiendo Comprador o Negocio). Para la entrega, prepara de antemano una cuenta de cada rol y al menos una oferta publicada con unidades y horario de recogida para el día de la demostración.

## Plan B si el servidor está dormido

En el plan gratuito, Render duerme el servicio tras unos 15 minutos sin uso, y despertarlo tarda cerca de un minuto.

1. **Antes de presentar:** abre en el navegador `https://<tu-servicio>.onrender.com/api/v1/salud` y espera la respuesta `{"estado":"ok",…}`. Así el servidor ya está despierto.
2. **Si igual tarda:** la app avisa «Despertando el servidor… puede tardar hasta un minuto» y espera hasta 60 s en la primera llamada. Al abrirse, la app ya despierta el servidor mientras se escribe el correo y la contraseña.
3. **Si el servidor no responde:** cierra sesión, activa **«Usar datos de demostración»** en la pantalla de ingreso y sigue con las cuentas demo. El recorrido es el mismo.
4. **Si la base de datos no responde** (`/api/v1/salud/base-datos` da error): en el plan gratuito, Supabase pausa los proyectos tras una semana sin actividad. Reactívalo desde su panel, o usa el modo demostración.

## Guion de demostración (5 minutos)

Prepara dos teléfonos (o uno y cierra sesión entre roles): uno con la cuenta de **negocio** y otro con la de **comprador**. Despierta el servidor antes de empezar.

| Tiempo | Quién | Qué mostrar |
|---|---|---|
| 0:00–0:30 | — | El problema: comida en buen estado que se bota al cierre. La idea: venderla con descuento y recogerla en el local. |
| 0:30–1:30 | Negocio | **Panel** con métricas. **Publicar oferta**: foto, categoría, precios. Tocar **Sugerir con IA**: propone título, categoría, descripción y precio con descuento. Guardar. |
| 1:30–2:30 | Comprador | **Inicio**: aparece la oferta nueva cerca. **Explorar** en mapa. Abrir el detalle, guardar como favorito, elegir **2 unidades** y **Reservar**. Mostrar el código grande en la hoja «¡Reservado!». |
| 2:30–3:00 | Comprador | **Pedidos → Activos**: la reserva con su código. **Perfil**: estadísticas actuales. |
| 3:00–4:00 | Negocio | **Reservas → Pendientes**: aparece el cliente. Escribir un código equivocado («quedan 4 intentos») y luego el correcto: check de confirmación. El **Panel** se actualiza. |
| 4:00–4:30 | Comprador | **Pedidos → Historial**: «Recogido». **Perfil**: un pedido completado más, kilos rescatados (aprox.) y ahorro. |
| 4:30–5:00 | — | Cierre: modo oscuro e idioma inglés en Perfil, y la insignia de modo demostración como respaldo sin internet. |

## Créditos

Tipografías: [Bricolage Grotesque](https://github.com/ateliertriay/bricolage) y [DM Sans](https://github.com/googlefonts/dm-fonts), bajo la SIL Open Font License 1.1 (textos en `docs/licenses/`).

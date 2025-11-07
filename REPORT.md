# Lab 4 — WebSockets ELIZA — Project Report

## Description of Changes

He terminado la práctica implementando y verificando el **chat WebSocket** con ELIZA.

**Qué he cambiado:**
- **Test `onChat` (habilitado)**: he completado el test para que valide el flujo de conversación y no falle por asincronía.
  - Uso `CountDownLatch(4)` y capturo un *snapshot* del tamaño de la lista (`val size = list.size`) para evitar condiciones de carrera.
  - Compruebo un **rango estable** con `assertTrue(size in 4..5)` en vez de un número exacto, porque el quinto mensaje (`"---"`) puede llegar justo antes o justo después del `await()`.
  - Verifico un mensaje **determinista**: `assertEquals("---", list[2])` (el tercer mensaje enviado por el servidor en `@OnOpen`).
- **Cliente `ComplexClient`**: ahora responde **solo** cuando corresponde y cierra la sesión de forma limpia.
  - En el **primer** `"---"` envía: `I feel happy today.`
  - En el **segundo** `"---"` envía: `bye` para provocar `NORMAL_CLOSURE`.
  - Con esto evitamos que el test se quede colgado esperando el `latch`.

**Qué funciona ahora:**
- Al abrir la conexión el servidor envía saludo, pregunta y un marcador `"---"`.
- Tras enviar un mensaje, ELIZA contesta con estilo DOCTOR y vuelve a enviar `"---"`.
- El test `onChat` pasa de forma **no flaky** y la sesión se cierra correctamente con `bye`.

---

## Technical Decisions (explicado sencillo)

- **Aserciones resilientes**: en WebSockets los mensajes llegan de forma asíncrona. Por eso no comparo “tamaño exacto” con `assertEquals`, sino un **intervalo** (`4..5`). Así el test no falla por milisegundos de diferencia.
- **Snapshot del tamaño**: hago `val size = list.size` para congelar el valor y que no cambie entre líneas si entra otro mensaje en segundo plano.
- **Cierre ordenado**: mando `bye` *solo* en el **segundo** `"---"`. Si lo mandas antes, el servidor cierra y el `latch` no llega a 0 → el test se queda colgado.
- **Sincronización de envíos en el servidor**: se usa `sendTextSafe(...)` (bloque sincronizado) para evitar `IllegalStateException` si el endpoint está “ocupado” mientras se intenta enviar otro mensaje.

---

## Learning Outcomes

- **Ciclo de vida WebSocket**: conectar → recibir mensajes de bienvenida → chatear → cerrar con código normal.
- **Testing de código asíncrono**: por qué usar **rangos/tiempos** y *latches* en vez de esperar un número exacto de eventos.
- **Buenas prácticas de cierre**: terminar la conversación con `bye` para `NORMAL_CLOSURE`, evitando sesiones zombi y errores raros.
- **Disciplina de formato**: ejecutar `./gradlew ktlintFormat` antes del build para no romper por re-formateo automático.

---

## How to run & verify

```bash
# 1) Formatear (recomendado)
./gradlew ktlintFormat

# 2) Ejecutar tests
./gradlew test

# 3) (Opcional) Probar manualmente
./gradlew bootRun
# WebSocket: ws://localhost:8080/eliza
# Conecta con Postman o wscat y verás:
# - "The doctor is in."
# - "What's on your mind?"
# - "---"
# Escribe algo (p. ej. "I feel happy today.") y recibirás respuesta estilo DOCTOR + "---"
```

---

## AI Disclosure

### AI Tools Used
- ChatGPT (GPT‑5 Thinking)

### AI‑Assisted Work
- Ayuda para razonar el flujo del test asíncrono (uso de `CountDownLatch`, verificación por rango y cierre con `bye`).
- Ayuda en la redacción de este **REPORT.md**.

**Estimación**: ~30% asistido / 70% trabajo propio (implementación, ejecución de pruebas, verificación manual y ajuste fino).

### Original Work
- Implementación de `ComplexClient` (primer `"---"` → mensaje; segundo `"---"` → `bye`).
- Diseño de aserciones del test `onChat` para que no sean frágiles.
- Verificación manual del endpoint y limpieza con Ktlint.

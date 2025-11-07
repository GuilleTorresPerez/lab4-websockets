@file:Suppress("NoWildcardImports")

package websockets

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.websocket.ClientEndpoint
import jakarta.websocket.ContainerProvider
import jakarta.websocket.OnMessage
import jakarta.websocket.Session
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.util.concurrent.CountDownLatch

private val logger = KotlinLogging.logger {}

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ElizaServerTest {
    @LocalServerPort
    private var port: Int = 0

    @Disabled
    @Test
    fun onOpen() {
        logger.info { "This is the test worker" }
        val latch = CountDownLatch(3)
        val list = mutableListOf<String>()

        val client = SimpleClient(list, latch)
        client.connect("ws://localhost:$port/eliza")
        latch.await()
        assertEquals(3, list.size)
        assertEquals("The doctor is in.", list[0])
    }

    // @Disabled // Remove this line when you implement onChat
    @Test
    fun onChat() {
        logger.info { "Test thread" }
        val latch = CountDownLatch(4)
        val list = mutableListOf<String>()

        val client = ComplexClient(list, latch)
        client.connect("ws://localhost:$port/eliza")
        latch.await()

        val size = list.size
        // 1. EXPLAIN WHY size = list.size IS NECESSARY
        //    hacemos captura inmutable del tamaño. La lista se rellena en callbacks
        //    asíncronos y puede crecer entre lecturas, capturarlo evita condiciones de carrera
        //    en las aserciones.

        // 2. REPLACE BY assertXXX expression that checks an interval; assertEquals must not be used;
        //    El servidor puede haber enviado ya 4 o 5 mensajes cuando despertemos del await,
        //    según timings. Verificamos un rango estable:
        org.junit.jupiter.api.Assertions.assertTrue(
            size in 4..5,
            "Expected 4..5 messages due to async timing, but was $size",
        )

        // 3. EXPLAIN WHY assertEquals CANNOT BE USED AND WHY WE SHOULD CHECK THE INTERVAL
        //    assertEquals fallaría de forma intermitente porque el número exacto de mensajes
        //    recibidos al despertar del latch no es determinista: el 5º ('---') puede entrar
        //    antes o después. Por eso comprobamos un intervalo robusto en lugar de igualdad exacta.

        // 4. COMPLETE assertEquals(XXX, list[XXX])
        //    Los 3 primeros mensajes son deterministas en orden; validamos uno concreto:
        assertEquals("---", list[2])

        logger.info { "Client size: (${list.size})" }
    }
}

@ClientEndpoint
class SimpleClient(
    private val list: MutableList<String>,
    private val latch: CountDownLatch,
) {
    @OnMessage
    fun onMessage(message: String) {
        logger.info { "Client received: $message" }
        list.add(message)
        latch.countDown()
    }
}

@ClientEndpoint
class ComplexClient(
    private val list: MutableList<String>,
    private val latch: CountDownLatch,
) {
    private var askedOnce = false

    @OnMessage
    @Suppress("UNUSED_PARAMETER")
    fun onMessage(
        message: String,
        session: Session,
    ) {
        list.add(message)
        logger.info { "Client received (${list.size}): $message" }
        latch.countDown()

        if (message == "---") {
            if (!askedOnce) {
                session.basicRemote.sendText("I feel happy today.") // 1er prompt al server
                askedOnce = true
            } else {
                session.basicRemote.sendText("bye") // 2º '---' -> cerrar
            }
        }
    }
}

fun Any.connect(uri: String) {
    ContainerProvider.getWebSocketContainer().connectToServer(this, URI(uri))
}

package me.jorgecastillo.hiroaki.internal

import me.jorgecastillo.hiroaki.dispatcher.DispatcherRetainer
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import java.net.InetAddress

/**
 * Base class to provide the mock server before and after the test execution. Intentionally avoided
 * using a rule for readability (not requiring tests to access server through the rule like
 * rule.server.enqueue() but directly server.enqueue()).
 */
open class MockServerSuite @JvmOverloads constructor(
    val inetAddress: InetAddress = InetAddress.getByName("localhost"),
    val port: Int = 0
) {
    lateinit var server: MockWebServer

    @Before
    open fun setup() {
        server = MockWebServer()
        DispatcherRetainer.registerRetainer()
        DispatcherRetainer.resetDispatchers()
        server.start(inetAddress, port)
    }

    @After
    open fun tearDown() {
        server.shutdown()
        DispatcherRetainer.resetDispatchers()
        server.setDispatcher(DispatcherRetainer.queueDispatcher)
    }
}

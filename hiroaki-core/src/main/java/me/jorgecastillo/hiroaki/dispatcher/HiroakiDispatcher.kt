package me.jorgecastillo.hiroaki.dispatcher

import me.jorgecastillo.hiroaki.Either
import me.jorgecastillo.hiroaki.left
import me.jorgecastillo.hiroaki.right
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matcher

/**
 * This is the MockWebServer dispatcher auto-attached to the server at start. It's required to mock
 * responses that are dependant on the request, instead of plain enqueuing (which just adds them to
 * a sequential queue).
 *
 * Thanks to this Dispatcher, we can program different answers depending on which endpoint is
 * queried and regardless of the execution order. That's very handy for Android end to end UI tests
 * where you don't care when are requests made but just that they return your mocks when they are
 * done.
 *
 * It's open so end users can extend it to create their own dispatchers if they need to.
 */
open class HiroakiDispatcher : Dispatcher() {

    private val mockRequests: MutableList<Pair<Matcher<RecordedRequest>,
            Either<MockResponse, (recordedRequest: RecordedRequest) -> MockResponse>>> =
            mutableListOf()
    val dispatchedRequests: MutableList<RecordedRequest> = mutableListOf()

    fun addMockRequest(
        matcher: Matcher<RecordedRequest>,
        mockResponse: MockResponse
    ) {
        mockRequests.add(Pair(matcher, mockResponse.left()))
    }

    fun addDispatchableBlock(
        matcher: Matcher<RecordedRequest>,
        dispatchableBlock: (recordedRequest: RecordedRequest) -> MockResponse
    ) {
        mockRequests.add(Pair(matcher, dispatchableBlock.right()))
    }

    /**
     * Resets both collections to their initial state. Called after any test.
     */
    fun reset() {
        mockRequests.clear()
        dispatchedRequests.clear()
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
        dispatchedRequests.add(request)
        val mockRequest = mockRequests.find { (matcher, _) -> matcher.matches(request) }
        return if (mockRequest != null) {
            mockRequests.remove(mockRequest)
            mockRequest.second.fold({ it }, { it(request) })
        } else {
            notMockedResponse()
        }
    }

    private fun notMockedResponse(): MockResponse {
        val mockResponse = MockResponse().setResponseCode(500)
        mockResponse.setBody("{ \"error\" : \"not mocked response\" }")
        return mockResponse
    }
}

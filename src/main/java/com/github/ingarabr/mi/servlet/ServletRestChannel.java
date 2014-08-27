package com.github.ingarabr.mi.servlet;

import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

class ServletRestChannel extends HttpChannel {

    private final RestRequest restRequest;
    private final HttpServletResponse resp;
    private final CountDownLatch latch;

    IOException sendFailure;

    ServletRestChannel(RestRequest restRequest, HttpServletResponse resp) {
        super(restRequest);
        this.restRequest = restRequest;
        this.resp = resp;
        this.latch = new CountDownLatch(1);
    }

    public void await() throws InterruptedException {
        latch.await();
    }

    @Override
    public void sendResponse(RestResponse response) {
        resp.setContentType(response.contentType());
        resp.addHeader("Access-Control-Allow-Origin", "*");
        if (response.status() != null) {
            resp.setStatus(response.status().getStatus());
        } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        if (restRequest.method() == RestRequest.Method.OPTIONS) {
            // also add more access control parameters
            resp.addHeader("Access-Control-Max-Age", "1728000");
            resp.addHeader("Access-Control-Allow-Methods", "OPTIONS, HEAD, GET, POST, PUT, DELETE");
            resp.addHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Content-Length");
        }
        String opaque = restRequest.header("X-Opaque-Id");
        if (opaque != null) {
            resp.addHeader("X-Opaque-Id", opaque);
        }
        Map<String, List<String>> customHeaders = response.getHeaders();
        if (customHeaders != null) {
            for (Map.Entry<String, List<String>> headerEntry : customHeaders.entrySet()) {
                for (String headerValue : headerEntry.getValue()) {
                    resp.addHeader(headerEntry.getKey(), headerValue);
                }
            }
        }
        try {
            int contentLength = response.content().length();
            resp.setContentLength(contentLength);
            ServletOutputStream out = resp.getOutputStream();
            response.content().writeTo(out);
            // TODO: close in a finally?
            out.close();
        } catch (IOException e) {
            sendFailure = e;
        } finally {
            latch.countDown();
        }
    }

    protected HttpServletResponse getServletResponse() {
        return resp;
    }

    protected void errorOccured(IOException e) {
        sendFailure = e;
    }

    protected void finish() {
        latch.countDown();
    }

}

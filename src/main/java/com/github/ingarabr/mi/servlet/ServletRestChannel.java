package com.github.ingarabr.mi.servlet;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;

class ServletRestChannel implements RestChannel {

    private final RestRequest restRequest;
    final HttpServletResponse resp;

    final CountDownLatch latch;

    IOException sendFailure;

    ServletRestChannel(RestRequest restRequest, HttpServletResponse resp) {
        this.restRequest = restRequest;
        this.resp = resp;
        this.latch = new CountDownLatch(1);
    }

    public void sendResponse(RestResponse response) {
        HttpServletResponse resp = getServletResponse();
        resp.setStatus(response.status().getStatus());
        resp.setContentType(response.contentType());
        String opaque = restRequest.header("X-Opaque-Id");
        if (opaque != null) {
            resp.addHeader("X-Opaque-Id", opaque);
        }
        try {
            int contentLength = response.contentLength();
            if (response.prefixContent() != null) {
                contentLength += response.prefixContentLength();
            }
            if (response.suffixContent() != null) {
                contentLength += response.suffixContentLength();
            }

            resp.setContentLength(contentLength);

            ServletOutputStream out = resp.getOutputStream();
            if (response.prefixContent() != null) {
                out.write(response.prefixContent(), 0, response.prefixContentLength());
            }
            out.write(response.content(), 0, response.contentLength());
            if (response.suffixContent() != null) {
                out.write(response.suffixContent(), 0, response.suffixContentLength());
            }
            out.close();
        } catch (IOException e) {
            errorOccured(e);
        } finally {
            finish();
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

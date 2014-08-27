package com.github.ingarabr.mi.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestController;

public class ElasticSearchHttpServlet extends HttpServlet {

    private RestController restController;

    public ElasticSearchHttpServlet(Node node) {
        this.restController = ((InternalNode) node).injector().getInstance(RestController.class);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletRestRequest request = new ServletRestRequest(req);
        ServletRestChannel channel = new ServletRestChannel(request, resp);
        try {
            restController.dispatchRequest(request, channel);
            channel.await();
        } catch (Exception e) {
            throw new IOException("failed to dispatch request", e);
        }
        if (channel.sendFailure != null) {
            throw channel.sendFailure;
        }
    }

}

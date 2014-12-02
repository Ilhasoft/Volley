package br.com.ilhasoft.volley;

import com.android.volley.*;
import com.android.volley.error.VolleyError;

import org.apache.http.HttpStatus;

import java.util.Map;

/**
 * Created by dev on 31/07/2014.
 */
public class RedirectErrorListener implements Response.ErrorListener {

    Response.ErrorListener nextListener;
    com.android.volley.Request request;
    RequestQueue requestQueue;

    @Override
    public void onErrorResponse(VolleyError error) {
        NetworkResponse networkResponse = error.networkResponse;
        int statusCode = networkResponse.statusCode;
        Map<String, String> responseHeaders = networkResponse.headers;
        if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
            String newUrl = responseHeaders.get("Location");
            request.setRedirectUrl(newUrl);
            requestQueue.add(request);
            return;
        }
        nextListener.onErrorResponse(error);
    }
}

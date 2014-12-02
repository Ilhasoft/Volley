package br.com.ilhasoft.volley;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by dev on 23/07/2014.
 */
public class RequestBuilder {

    String url;
    int httpMethod;
    String contentType;
    RequestQueue requestQueue;
    Response.ErrorListener errorListener;
    Response.Listener<String> responseListener;
    Request.Priority priority;
    RetryPolicy retryPolicy;

    int retryTimeout = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS;
    int retryMaxRetries = DefaultRetryPolicy.DEFAULT_MAX_RETRIES;
    float retryBackoffMult = DefaultRetryPolicy.DEFAULT_BACKOFF_MULT;

    HashMap<String, String> headers = new HashMap<String, String>();
    private String body;
    private boolean shouldCache;
    private Integer fakeCacheOverride = null;

    public RequestBuilder(RequestQueue requestQueue, String url) {
        errorListener = new LogOnErrorListener(url);
//        responseListener = new LogOnSuccessListener<String>();
        this.requestQueue = requestQueue;
        this.url = url;
        useGet();
        setNormalPriority();
        shouldCache();
    }

    public RequestBuilder putHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public RequestBuilder setBody(String body) {
        this.body = body;
        return this;
    }

    /**
     * Set request body and change content type to json
     *
     * @param json will replace body
     */
    public RequestBuilder setJsonAsBody(String json) {
        setBody(json);
        setContentTypeJson();
        return this;
    }

    public RequestBuilder setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public RequestBuilder setContentTypeJson() {
        return setContentType("application/json");
    }

    public RequestBuilder setErrorListener(Response.ErrorListener errorListener) {
        this.errorListener = errorListener;
        return this;
    }

    public RequestBuilder setResponseListener(Response.Listener<String> responseListener) {
        this.responseListener = responseListener;
        return this;
    }

    public RequestBuilder setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    public RequestBuilder setRetryPolicy(int initialTimeoutMs, int maxNumRetries, float backoffMultiplier) {
        this.retryPolicy = new DefaultRetryPolicy(initialTimeoutMs, maxNumRetries, backoffMultiplier);
        return this;
    }

    public com.android.volley.Request<String> addToRequestQueue() {
        Request request = buildRequest();
        return requestQueue.add(request);
    }


    public String addToRequestQueueAndWaitResponseAlternative() {
        return ResponseWaiter.buildRequestAndWaitResponse(this);
    }

    public String addToRequestQueueAndWaitResponse() throws ExecutionException, InterruptedException, TimeoutException {
        com.android.volley.Request<String> request = addToRequestQueue();
        String requestFromCache = getRequestFromCache(request.getCacheKey());
        if (requestFromCache != null) {
            VolleyLog.d("Sync load from cache.");
            return requestFromCache;
        }
        RequestFuture<String> requestFuture = RequestFuture.newFuture();
        requestFuture.setRequest(request);
        return requestFuture.get(retryTimeout, TimeUnit.MILLISECONDS);
    }

    private Request buildRequest() {
//        VolleyLog.d(String.format("%s - addToRequestQueue \"%s\"", "RequestBuilder", url));
        Request request = new Request(httpMethod, url, responseListener, errorListener);
        request.setPriority(priority);
        if (fakeCacheOverride != null)
            request.setFakeCacheOverride(fakeCacheOverride);
        request.headers = headers;
        if (contentType != null)
            request.setConntentType(contentType);
        request.setBody(body);
        if ((retryPolicy == null))//&& ((retryTimeout ) || (retryMaxRetries) || (retryBackoffMult))
            request.setRetryPolicy(new DefaultRetryPolicy(retryTimeout, retryMaxRetries, retryBackoffMult));
        else
            request.setRetryPolicy(retryPolicy);

        request.setShouldCache(shouldCache);
        try {
            String tag = toMd5(url, body, null);
            request.cachekey = tag;
            callListenerIfExistOnCache(tag);
        } catch (NoSuchAlgorithmException e) {
        }
        VolleyLog.d("RequestBuilt %s %s", url, body);
//        VolleyLog.d("Url \"%s\"\nCacheKey \"%s\"", url,request.getCacheKey());
        return request;
    }

    private void callListenerIfExistOnCache(String tag) {
        try {
            String parsed = getRequestFromCache(tag);
            if (parsed == null) return;
            responseListener.onResponse(parsed);
        } catch (Exception e) {
        }
    }

    private String getRequestFromCache(String cacheKey) {
        Cache.Entry entry = requestQueue.getCache().get(cacheKey);
        if (entry == null || responseListener == null)
            return null;
//            VolleyLog.d("CacheEntry found");
        String parsed;
        try {
            parsed = new String(entry.data, HttpHeaderParser.parseCharset(entry.responseHeaders));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(entry.data);
        }
        return parsed;
    }

    public RequestBuilder setTimeout(int retryTimeoutMilliseconds) {
        this.retryTimeout = retryTimeoutMilliseconds;
        return this;
    }

    public RequestBuilder setMaxRetries(int retryMaxRetries) {
        this.retryMaxRetries = retryMaxRetries;
        return this;
    }

    // sets for http methods
    public RequestBuilder useGet() {
        httpMethod = Request.Method.GET;
        return this;
    }

    public RequestBuilder usePost() {
        httpMethod = Request.Method.POST;
        return this;
    }

    public RequestBuilder usePut() {
        httpMethod = Request.Method.PUT;
        return this;
    }

    public RequestBuilder useDelete() {
        httpMethod = Request.Method.DELETE;
        return this;
    }

    public RequestBuilder useHead() {
        httpMethod = Request.Method.HEAD;
        return this;
    }

    public RequestBuilder useOptions() {
        httpMethod = Request.Method.OPTIONS;
        return this;
    }

    public RequestBuilder useTrace() {
        httpMethod = Request.Method.TRACE;
        return this;
    }

    public RequestBuilder usePatch() {
        httpMethod = Request.Method.PATCH;
        return this;
    }

    // set for Priority
    public RequestBuilder setLowPriority() {
        priority = Request.Priority.LOW;
        return this;
    }

    public RequestBuilder setNormalPriority() {
        priority = Request.Priority.NORMAL;
        return this;
    }

    public RequestBuilder setHighPriority() {
        priority = Request.Priority.HIGH;
        return this;
    }

    public RequestBuilder setImmediatePriority() {
        priority = Request.Priority.IMMEDIATE;
        return this;
    }

    // set for cache
    public RequestBuilder shouldCache() {
        shouldCache = true;
        return this;
    }

    public RequestBuilder shouldNotCache() {
        shouldCache = false;
        return this;
    }

    public RequestBuilder fakeCacheOverride(int fakeCacheOverride) {
        this.fakeCacheOverride = fakeCacheOverride;
        return this;
    }

    private static String toMd5(String url, String body, Map<String, String> headers) throws NoSuchAlgorithmException {
        StringBuilder builder = new StringBuilder();
        builder.append(url);
        if (body != null) {
            builder.append(body.replaceAll("\\s+", ""));
        }
        if (headers != null)
            for (Map.Entry entry : headers.entrySet())
                builder.append(entry.getKey()).append(entry.getValue());
        String toParse = builder.toString();
//        VolleyLog.d("toMd5 \"%s\"", toParse);
        return toMd5(toParse);
    }

    private static String toMd5(String s) throws NoSuchAlgorithmException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(s.getBytes(), 0, s.length());
        return new BigInteger(1, m.digest()).toString(16);
    }

    private static String toMd5(Map<String, String> map) throws NoSuchAlgorithmException {
        return toMd5(mapToStr(map));
    }

    private static String mapToStr(Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry entry : map.entrySet())
            builder.append(entry.getKey()).append(entry.getValue());
        return builder.toString();
    }
}

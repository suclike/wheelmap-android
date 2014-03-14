package org.wheelmap.android.net.request;

/**
 * Created by SMF on 10/03/14.
 */
public class PhotosRequestBuilder extends BasePhotosRequestBuilder{

    // TEST: wheelmap.org/api/nodes/927092067/photos?api_key=jWeAsb34CJq4yVAryjtc

    private static final String RESOURCE = "nodes";
    private static final String PHOTOS = "photos";
    private String id = "927092067";


    public PhotosRequestBuilder(final String server, final String apiKey,
            final AcceptType acceptType) {
        super(server, apiKey, acceptType);
    }

    @Override
    protected String resourcePath() {
        return String.format("%s/%s/%s", RESOURCE, id, PHOTOS);
    }

    @Override
    public int getRequestType() {
        return RequestBuilder.REQUEST_GET;
    }
}
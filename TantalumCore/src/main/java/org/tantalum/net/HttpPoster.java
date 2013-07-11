/*
 Copyright (c) 2013 Nokia Corporation. All rights reserved.
 Nokia and Nokia Connecting People are registered trademarks of Nokia Corporation.
 Oracle and Java are trademarks or registered trademarks of Oracle and/or its
 affiliates. Other product and company names mentioned herein may be trademarks
 or trade names of their respective owners.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package org.tantalum.net;

import org.tantalum.Task;

/**
 * HTTP POST a message to a given URL
 *
 * @author phou
 */
public class HttpPoster extends HttpGetter {

    private String url;
    private volatile boolean isChainInputPostData = false;

    /**
     * HTTP POST a message to a given URL
     *
     * Make sure you call setPostData(byte[]) to specify what you want to POST
     * or you will get an IllegalArgumentException
     *
     * If you wish to locally cache multiple responses from the server in which
     * the URL is the same but the POST data is different, you should append to
     * the url field additional line(s) of text. This will be used create a
     * unique String hash in your StaticWebCache. You can control which POST
     * responses are "unique" from the point of caching the most recent unique
     * response by appending all or just some of the POST data fields.
     *
     * @param priority
     * @param url 
     */
    public HttpPoster(final int priority, final String url) {
        super(priority, url);

        this.url = url;
    }

    /**
     * Create a Task.NORMAL_PRIORITY poster
     * 
     * @param url 
     */
    public HttpPoster(final String url) {
        this(Task.NORMAL_PRIORITY, url);
    }
    
    public HttpPoster() {
        super();
    }
    
    
    /**
     * Create an HTTP POST operation
     *
     * @param url
     * @param postData
     * @param priority
     */
    public HttpPoster(final int priority, final String url, final byte[] postData) {
        this(priority, url);

        setPostData(postData);
    }

    /**
     * Set the message to be HTTP POSTed to the server
     *
     * @param postData
     * @return
     */
    public final HttpPoster setPostData(final byte[] postData) {
        if (postData == null) {
            throw new NullPointerException(getClassName() + " was passed null message- meaningless POST or PUT operation");
        }
        this.postMessage = new byte[postData.length];
        System.arraycopy(postData, 0, this.postMessage, 0, postData.length);

        return this;
    }

    /**
     * The default when an HttpPoster is not the first Task in a chain is for
     * the previous Task output to be passed as the URL, not post data, to this
     * task.
     *
     * You can override this to have the previous Task in the chain set the post
     * data instead of the URL. The previous Task should then output a byte[]
     *
     * @param isPostData
     * @return
     */
    public HttpPoster setChainInputIsPostData(final boolean isPostData) {
        this.isChainInputPostData = isPostData;

        return this;
    }

    /**
     * Execute the POST operation
     * 
     * @param in
     * @return
     */
    public Object exec(final Object in) throws InterruptedException {
        if (isChainInputPostData) {
            setPostData((byte[]) in);

            return super.exec(url);
        } else {
            return super.exec(in);
        }
    }
}

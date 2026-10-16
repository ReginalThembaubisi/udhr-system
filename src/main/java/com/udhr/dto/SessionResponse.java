package com.udhr.dto;

/**
 * What every sign-in, refresh and password change hands back. The refresh
 * token is only ever sent to the browser as an HttpOnly cookie (see
 * AuthController), never in the JSON body where page scripts could read it.
 */
public interface SessionResponse {
    String getRefreshToken();
}

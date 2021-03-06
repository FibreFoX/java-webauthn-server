package com.yubico.webauthn.extension.appid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.net.InetAddresses;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.Value;

@Value
@JsonSerialize(using = AppId.JsonSerializer.class)
public class AppId {

    private final String id;

    @JsonCreator
    public AppId(String appId) throws InvalidAppIdException {
        checkIsValid(appId);
        this.id = appId;
    }

    /**
     * Throws {@link InvalidAppIdException} if the given App ID is found to be incompatible with the U2F specification or any major
     * U2F Client implementation.
     *
     * @param appId the App ID to be validated
     */
    private static void checkIsValid(String appId) throws InvalidAppIdException {
        if(!appId.contains(":")) {
            throw new InvalidAppIdException("App ID does not look like a valid facet or URL. Web facets must start with 'https://'.");
        }
        if(appId.startsWith("http:")) {
            throw new InvalidAppIdException("HTTP is not supported for App IDs (by Chrome). Use HTTPS instead.");
        }
        if(appId.startsWith("https://")) {
            URI url = checkValidUrl(appId);
            checkPathIsNotSlash(url);
            checkNotIpAddress(url);
        }
    }

    private static void checkPathIsNotSlash(URI url) throws InvalidAppIdException {
        if("/".equals(url.getPath())) {
            throw new InvalidAppIdException("The path of the URL set as App ID is '/'. This is probably not what you want -- remove the trailing slash of the App ID URL.");
        }
    }

    private static URI checkValidUrl(String appId) throws InvalidAppIdException {
        try {
            return new URI(appId);
        } catch (URISyntaxException e) {
            throw new InvalidAppIdException("App ID looks like a HTTPS URL, but has syntax errors.", e);
        }
    }

    private static void checkNotIpAddress(URI url) throws InvalidAppIdException {
        if (InetAddresses.isInetAddress(url.getAuthority()) || (url.getHost() != null && InetAddresses.isInetAddress(url.getHost()))) {
            throw new InvalidAppIdException("App ID must not be an IP-address, since it is not supported (by Chrome). Use a host name instead.");
        }
    }

    static class JsonSerializer extends com.fasterxml.jackson.databind.JsonSerializer<AppId> {
        @Override
        public void serialize(AppId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.getId());
        }
    }

}

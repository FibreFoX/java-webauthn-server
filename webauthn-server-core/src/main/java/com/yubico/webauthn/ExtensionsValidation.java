package com.yubico.webauthn;

import com.upokecenter.cbor.CBORObject;
import com.yubico.webauthn.data.AuthenticatorResponse;
import com.yubico.webauthn.data.ClientExtensionOutputs;
import com.yubico.webauthn.data.ExtensionInputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;


@UtilityClass
class ExtensionsValidation {

    static boolean validate(ExtensionInputs requested, PublicKeyCredential<? extends AuthenticatorResponse, ? extends ClientExtensionOutputs> response) {
        Set<String> requestedExtensionIds = requested.getExtensionIds();
        Set<String> clientExtensionIds = response.getClientExtensionResults().getExtensionIds();

        if (!requestedExtensionIds.containsAll(clientExtensionIds)) {
            throw new IllegalArgumentException(String.format(
                "Client extensions {%s} are not a subset of requested extensions {%s}.",
                String.join(", ", clientExtensionIds),
                String.join(", ", requestedExtensionIds)
            ));
        }

        Set<String> authenticatorExtensionIds = response.getResponse().getParsedAuthenticatorData().getExtensions()
            .map(extensions -> extensions.getKeys().stream()
                .map(CBORObject::AsString)
                .collect(Collectors.toSet())
            )
            .orElseGet(HashSet::new);

        if (!requestedExtensionIds.containsAll(authenticatorExtensionIds)) {
            throw new IllegalArgumentException(String.format(
                "Authenticator extensions {%s} are not a subset of requested extensions {%s}.",
                String.join(", ", authenticatorExtensionIds),
                String.join(", ", requestedExtensionIds)
            ));
        }

        return true;
    }

}

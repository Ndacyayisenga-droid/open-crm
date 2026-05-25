package com.openelements.crm.contact;

import com.openelements.spring.base.data.image.util.HeicSupportCheck;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thin CRM-side wrapper around the spring-services {@link HeicSupportCheck}
 * static helper. Probes once at startup whether the runtime can actually decode
 * HEIC (the {@code imageio-heif} reader plus the {@code libheif} native library)
 * and caches the result, so {@code ContactService} can short-circuit HEIC
 * uploads with HTTP 415 (the Spec 102 contract) without re-probing on every
 * request.
 */
@Component
public class CrmHeicSupportCheck {

    private static final Logger log = LoggerFactory.getLogger(CrmHeicSupportCheck.class);

    private volatile boolean heicAvailable = false;

    @PostConstruct
    void verify() {
        heicAvailable = HeicSupportCheck.verifyHeicSupport();
        if (!heicAvailable) {
            log.warn("HEIC support is not available in this deployment. HEIC uploads will be "
                + "rejected with 415 until libheif1 + libheif-plugin-libde265 are installed in "
                + "the runtime image.");
        }
    }

    /**
     * Returns whether the runtime can decode HEIC images. The value is probed
     * exactly once at startup. Callers must short-circuit HEIC uploads with 415
     * when this returns {@code false}.
     *
     * @return {@code true} if HEIC decoding is available
     */
    public boolean isHeicAvailable() {
        return heicAvailable;
    }
}

package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IItemTransferHelper;
import com.dwinovo.chiikawa.platform.services.IPlatformHelper;
import com.dwinovo.chiikawa.platform.services.IRegistryHelper;
import com.dwinovo.chiikawa.platform.services.IEntityHelper;
import com.dwinovo.chiikawa.platform.services.ICapabilityHelper;
import com.dwinovo.chiikawa.platform.services.IPlatformRegistryAccess;
import com.dwinovo.chiikawa.platform.services.INetworkHelper;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Services {
    private static final Logger LOG = LoggerFactory.getLogger("Chiikawa/Services");

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IItemTransferHelper ITEM_TRANSFER = load(IItemTransferHelper.class);
    public static final IRegistryHelper REGISTRY = load(IRegistryHelper.class);
    public static final IEntityHelper ENTITY = load(IEntityHelper.class);
    public static final ICapabilityHelper CAPABILITY = load(ICapabilityHelper.class);
    public static final IPlatformRegistryAccess PLATFORM_REGISTRY = load(IPlatformRegistryAccess.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);

    private static <T> T load(Class<T> clazz) {
        var loadedService = ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}

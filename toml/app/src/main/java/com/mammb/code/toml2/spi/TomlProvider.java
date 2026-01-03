package com.mammb.code.toml2.spi;

import com.mammb.code.toml2.api.TomlReader;
import java.io.InputStream;
import java.util.Iterator;
import java.util.ServiceLoader;

import static java.lang.System.Logger.Level.*;

public abstract class TomlProvider {

    private static final System.Logger log = System.getLogger(TomlProvider.class.getName());

    public static final String TOMLP_PROVIDER_FACTORY = "com.mammb.code.toml.provider";
    private static final String DEFAULT_PROVIDER = "com.mammb.code.toml.impl.JsonProviderImpl";

    protected TomlProvider() {
    }

    public static TomlProvider provider() {
        log.log(DEBUG, "Checking system property {0}", TOMLP_PROVIDER_FACTORY);
        final String factoryClassName = System.getProperty(TOMLP_PROVIDER_FACTORY);

        if (factoryClassName != null) {
            TomlProvider provider = newInstance(factoryClassName);
            log.log(DEBUG, "System property used; returning object [{0}]", provider.getClass().getName());
            return provider;
        }

        log.log(DEBUG, "Checking ServiceLoader");
        ServiceLoader<TomlProvider> loader = ServiceLoader.load(TomlProvider.class);
        Iterator<TomlProvider> it = loader.iterator();
        if (it.hasNext()) {
            TomlProvider provider = it.next();
            log.log(DEBUG, "ServiceLoader loading Facility used; returning object [{0}]",
                provider.getClass().getName());
            return provider;
        }

        // else no provider found
        log.log(DEBUG, "Trying to create the platform default provider");
        return newInstance(DEFAULT_PROVIDER);

    }

    private static TomlProvider newInstance(String className) {
        try {
            @SuppressWarnings({"unchecked"})
            Class<TomlProvider> clazz = (Class<TomlProvider>) Class.forName(className);
            return clazz.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Provider " + className + " not found", e);
        } catch (Exception e) {
            throw new RuntimeException("Provider " + className + " could not be instantiated: " + e, e);
        }
    }

    public abstract TomlReader createReader(InputStream in);

}

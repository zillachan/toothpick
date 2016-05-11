package toothpick;

import javax.inject.Provider;
import toothpick.registries.FactoryRegistryLocator;

/**
 * A non thread safe internal provider. It should never be exposed outside of ToothPick.
 *
 * @param <T> the class of the instances provided by this provider.
 */
public class InternalProviderImpl<T> {
  private volatile T instance;
  private Factory<T> factory;
  private Class<T> factoryClass;
  private volatile Provider<? extends T> providerInstance;
  private Factory<Provider<T>> providerFactory;
  private Class<Provider<T>> providerFactoryClass;

  public InternalProviderImpl(T instance) {
    if (instance == null) {
      throw new IllegalArgumentException("The instance can't be null.");
    }

    this.instance = instance;
  }

  public InternalProviderImpl(Provider<? extends T> providerInstance) {
    if (providerInstance == null) {
      throw new IllegalArgumentException("The provider can't be null.");
    }

    this.providerInstance = providerInstance;
  }

  public InternalProviderImpl(Factory<?> factory, boolean isProviderFactory) {
    if (factory == null) {
      throw new IllegalArgumentException("The factory can't be null.");
    }

    if (isProviderFactory) {
      this.providerFactory = (Factory<Provider<T>>) factory;
    } else {
      this.factory = (Factory<T>) factory;
    }
  }

  public InternalProviderImpl(Class<?> factoryKeyClass, boolean isProviderFactoryClass) {
    if (factoryKeyClass == null) {
      throw new IllegalArgumentException("The factory class can't be null.");
    }

    if (isProviderFactoryClass) {
      this.providerFactoryClass = (Class<Provider<T>>) factoryKeyClass;
    } else {
      this.factoryClass = (Class<T>) factoryKeyClass;
    }
  }

  //we lock on the unbound provider itself to prevent concurrent usage
  //of the unbound provider (
  public synchronized T get(Scope scope) {
    if (instance != null) {
      return instance;
    }

    if (providerInstance != null) {
      return providerInstance.get();
    }

    if (factoryClass != null && factory == null) {
      factory = FactoryRegistryLocator.getFactory(factoryClass);
      //gc
      factoryClass = null;
    }

    if (factory != null) {
      if (!factory.hasScopeAnnotation()) {
        return factory.createInstance(scope);
      }
      instance = factory.createInstance(scope);
      //gc
      factory = null;
      return instance;
    }

    if (providerFactoryClass != null && providerFactory == null) {
      providerFactory = FactoryRegistryLocator.getFactory(providerFactoryClass);
      //gc
      providerFactoryClass = null;
    }

    if (providerFactory != null) {
      if (providerFactory.hasScopeInstancesAnnotation()) {
        instance = providerFactory.createInstance(scope).get();
        //gc
        providerFactory = null;
        return instance;
      }
      if (providerFactory.hasScopeAnnotation()) {
        providerInstance = providerFactory.createInstance(scope);
        //gc
        providerFactory = null;
        return providerInstance.get();
      }

      return providerFactory.createInstance(scope).get();
    }

    throw new IllegalStateException("A provider can only be used with an instance, a provider, a factory or a provider factory. Should not happen.");
  }
}
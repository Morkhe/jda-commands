package com.github.kaktushose.jda.commands.reflect;

import com.github.kaktushose.jda.commands.annotations.Component;
import com.github.kaktushose.jda.commands.annotations.Inject;
import com.github.kaktushose.jda.commands.annotations.constraints.Constraint;
import com.github.kaktushose.jda.commands.dependency.DependencyInjector;
import com.github.kaktushose.jda.commands.dispatching.adapter.TypeAdapter;
import com.github.kaktushose.jda.commands.dispatching.adapter.TypeAdapterRegistry;
import com.github.kaktushose.jda.commands.dispatching.filter.Filter;
import com.github.kaktushose.jda.commands.dispatching.filter.FilterRegistry;
import com.github.kaktushose.jda.commands.dispatching.router.Router;
import com.github.kaktushose.jda.commands.dispatching.router.impl.CommandRouter;
import com.github.kaktushose.jda.commands.dispatching.sender.MessageSender;
import com.github.kaktushose.jda.commands.dispatching.sender.impl.DefaultMessageSender;
import com.github.kaktushose.jda.commands.dispatching.validation.Validator;
import com.github.kaktushose.jda.commands.dispatching.validation.ValidatorRegistry;
import com.github.kaktushose.jda.commands.embeds.error.DefaultErrorMessageFactory;
import com.github.kaktushose.jda.commands.embeds.error.ErrorMessageFactory;
import com.github.kaktushose.jda.commands.embeds.help.DefaultHelpMessageFactory;
import com.github.kaktushose.jda.commands.embeds.help.HelpMessageFactory;
import com.github.kaktushose.jda.commands.permissions.DefaultPermissionsProvider;
import com.github.kaktushose.jda.commands.permissions.PermissionsProvider;
import com.github.kaktushose.jda.commands.settings.DefaultSettingsProvider;
import com.github.kaktushose.jda.commands.settings.SettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Central registry for all custom user implementations. This class will look for custom implementations that
 * override the default implementation of this framework. Supports the following interfaces:
 * <ul>
 *     <li>{@link SettingsProvider}</li>
 *     <li>{@link PermissionsProvider}</li>
 *     <li>{@link HelpMessageFactory}</li>
 *     <li>{@link ErrorMessageFactory}</li>
 *     <li>{@link Router}</li>
 *     <li>{@link MessageSender}</li>
 *     <li>{@link TypeAdapter}</li>
 *     <li>{@link com.github.kaktushose.jda.commands.dispatching.filter.Filter Filter}</li>
 *     <li>{@link com.github.kaktushose.jda.commands.dispatching.validation.Validator Validator}</li>
 * </ul>
 *
 * @author Kaktushose
 * @version 2.2.0
 * @see Component
 * @since 2.0.0
 */
public class ImplementationRegistry {

    private static final Logger log = LoggerFactory.getLogger(ImplementationRegistry.class);
    private static Reflections reflections;
    private final DependencyInjector dependencyInjector;
    private final FilterRegistry filterRegistry;
    private final TypeAdapterRegistry typeAdapterRegistry;
    private final ValidatorRegistry validatorRegistry;
    private SettingsProvider settingsProvider;
    private PermissionsProvider permissionsProvider;
    private HelpMessageFactory helpMessageFactory;
    private ErrorMessageFactory errorMessageFactory;
    private Router router;
    private MessageSender messageSender;

    /**
     * Constructs a new ImplementationRegistry.
     *
     * @param dependencyInjector  the corresponding {@link DependencyInjector}
     * @param filterRegistry      the corresponding {@link FilterRegistry}
     * @param typeAdapterRegistry the corresponding {@link TypeAdapterRegistry}
     * @param validatorRegistry   the corresponding {@link ValidatorRegistry}
     */
    public ImplementationRegistry(DependencyInjector dependencyInjector,
                                  FilterRegistry filterRegistry,
                                  TypeAdapterRegistry typeAdapterRegistry,
                                  ValidatorRegistry validatorRegistry) {
        settingsProvider = new DefaultSettingsProvider();
        permissionsProvider = new DefaultPermissionsProvider();
        helpMessageFactory = new DefaultHelpMessageFactory();
        errorMessageFactory = new DefaultErrorMessageFactory();
        router = new CommandRouter();
        messageSender = new DefaultMessageSender();

        this.dependencyInjector = dependencyInjector;
        this.filterRegistry = filterRegistry;
        this.typeAdapterRegistry = typeAdapterRegistry;
        this.validatorRegistry = validatorRegistry;
    }

    /**
     * Scans the whole classpath for custom implementations.
     *
     * @param packages package(s) to exclusively scan
     * @param clazz    a class of the classpath to scan
     */
    public void index(@NotNull Class<?> clazz, @NotNull String... packages) {
        log.debug("Indexing custom implementations...");
        ConfigurationBuilder config = new ConfigurationBuilder()
                .setScanners(new SubTypesScanner())
                .setUrls(ClasspathHelper.forClass(clazz))
                .filterInputsBy(new FilterBuilder().includePackage(packages));
        reflections = new Reflections(config);

        findImplementation(SettingsProvider.class).ifPresent(this::setSettingsProvider);
        findImplementation(PermissionsProvider.class).ifPresent(this::setPermissionsProvider);
        findImplementation(HelpMessageFactory.class).ifPresent(this::setHelpMessageFactory);
        findImplementation(ErrorMessageFactory.class).ifPresent(this::setErrorMessageFactory);
        findImplementation(Router.class).ifPresent(this::setRouter);
        findImplementation(MessageSender.class).ifPresent(this::setMessageSender);

        findFilters().forEach(filterRegistry::register);
        findAdapters().forEach(typeAdapterRegistry::register);
        findValidators().forEach(validatorRegistry::register);
    }

    /**
     * Gets the {@link SettingsProvider}.
     *
     * @return the {@link SettingsProvider}
     */
    public SettingsProvider getSettingsProvider() {
        return settingsProvider;
    }

    /**
     * Sets the {@link SettingsProvider}.
     *
     * @param settingsProvider the new {@link SettingsProvider}
     */
    public void setSettingsProvider(SettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    /**
     * Gets the {@link PermissionsProvider}.
     *
     * @return the {@link PermissionsProvider}
     */
    public PermissionsProvider getPermissionsProvider() {
        return permissionsProvider;
    }

    /**
     * Sets the {@link PermissionsProvider}.
     *
     * @param permissionsProvider the new {@link PermissionsProvider}
     */
    public void setPermissionsProvider(PermissionsProvider permissionsProvider) {
        this.permissionsProvider = permissionsProvider;
    }

    /**
     * Gets the {@link HelpMessageFactory}.
     *
     * @return the {@link HelpMessageFactory}
     */
    public HelpMessageFactory getHelpMessageFactory() {
        return helpMessageFactory;
    }

    /**
     * Sets the {@link HelpMessageFactory}
     *
     * @param helpMessageFactory the new {@link HelpMessageFactory}
     */
    public void setHelpMessageFactory(HelpMessageFactory helpMessageFactory) {
        this.helpMessageFactory = helpMessageFactory;
    }

    /**
     * Gets the {@link ErrorMessageFactory}.
     *
     * @return the {@link ErrorMessageFactory}
     */
    public ErrorMessageFactory getErrorMessageFactory() {
        return errorMessageFactory;
    }

    /**
     * Sets the {@link ErrorMessageFactory}
     *
     * @param errorMessageFactory the new {@link ErrorMessageFactory}
     */
    public void setErrorMessageFactory(ErrorMessageFactory errorMessageFactory) {
        this.errorMessageFactory = errorMessageFactory;
    }

    /**
     * Gets the {@link Router}.
     *
     * @return the {@link Router}
     */
    public Router getRouter() {
        return router;
    }

    /**
     * Sets the {@link Router}.
     *
     * @param router the new {@link Router}
     */
    public void setRouter(Router router) {
        this.router = router;
    }

    /**
     * Gets the {@link MessageSender}.
     *
     * @return the {@link MessageSender}
     */
    public MessageSender getMessageSender() {
        return messageSender;
    }

    /**
     * Sets the {@link MessageSender}.
     *
     * @param sender the new {@link MessageSender}
     */
    public void setMessageSender(MessageSender sender) {
        this.messageSender = sender;
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> findImplementation(Class<T> type) {
        T instance = null;
        for (Class<?> clazz : reflections.getSubTypesOf(type)) {
            if (!clazz.isAnnotationPresent(Component.class)) {
                continue;
            }

            log.debug("Found {}", clazz.getName());
            try {
                instance = (T) clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("Unable to create an instance of the custom implementation!", e);
                continue;
            }

            List<Field> fields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }
                fields.add(field);
            }
            dependencyInjector.registerDependencies(instance, fields);
        }
        return Optional.ofNullable(instance);
    }

    private Map<Filter, FilterRegistry.FilterPosition> findFilters() {
        Map<Filter, FilterRegistry.FilterPosition> result = new HashMap<>();
        for (Class<? extends Filter> clazz : reflections.getSubTypesOf(Filter.class)) {
            if (!clazz.isAnnotationPresent(Component.class)) {
                continue;
            }

            log.debug("Found {}", clazz.getName());

            Filter instance;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("Unable to create an instance of the custom implementation!", e);
                continue;
            }

            FilterRegistry.FilterPosition position = clazz.getAnnotation(Component.class).position();
            if (position == FilterRegistry.FilterPosition.UNKNOWN) {
                log.error("Invalid filter position {}!", position);
                continue;
            }

            result.put(instance, position);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private Map<Class<?>, TypeAdapter<?>> findAdapters() {
        Map<Class<?>, TypeAdapter<?>> result = new HashMap<>();
        for (Class<? extends TypeAdapter> clazz : reflections.getSubTypesOf(TypeAdapter.class)) {
            if (!clazz.isAnnotationPresent(Component.class)) {
                continue;
            }

            log.debug("Found {}", clazz.getName());

            Class<?> generic;
            try {
                generic = Class.forName(
                        ((ParameterizedType) clazz.getGenericInterfaces()[0]).getActualTypeArguments()[0].getTypeName()
                );
            } catch (ClassNotFoundException e) {
                log.error("Unable to find class of type adapter!", e);
                continue;
            }

            TypeAdapter<?> instance;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("Unable to create an instance of the custom implementation!", e);
                continue;
            }

            result.put(generic, instance);

            List<Field> fields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }
                fields.add(field);
            }
            dependencyInjector.registerDependencies(instance, fields);
        }
        return result;
    }

    private Map<Class<? extends Annotation>, Validator> findValidators() {
        Map<Class<? extends Annotation>, Validator> result = new HashMap<>();
        for (Class<? extends Validator> clazz : reflections.getSubTypesOf(Validator.class)) {
            if (!clazz.isAnnotationPresent(Component.class)) {
                continue;
            }

            log.debug("Found {}", clazz.getName());

            Validator instance;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("Unable to create an instance of the custom implementation!", e);
                continue;
            }

            Class<? extends Annotation> annotation = clazz.getAnnotation(Component.class).annotation();
            if (Constraint.class.isAssignableFrom(annotation)) {
                log.error("Invalid annotation type {}!", Constraint.class);
                continue;
            }

            result.put(annotation, instance);
        }
        return result;
    }
}

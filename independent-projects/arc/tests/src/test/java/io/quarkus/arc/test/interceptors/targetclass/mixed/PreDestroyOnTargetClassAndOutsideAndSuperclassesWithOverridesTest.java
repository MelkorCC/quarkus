package io.quarkus.arc.test.interceptors.targetclass.mixed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

public class PreDestroyOnTargetClassAndOutsideAndSuperclassesWithOverridesTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        arc.instance(MyBean.class).destroy();
        assertEquals(List.of("MyInterceptorSuperclass", "MyInterceptor", "MyBean"), MyBean.invocations);
    }

    static class MyBeanSuperclass {
        @PreDestroy
        void preDestroy() {
            MyBean.invocations.add("this should not be called as the method is overridden in MyBean");
        }
    }

    @Singleton
    @MyInterceptorBinding
    static class MyBean extends MyBeanSuperclass {
        static final List<String> invocations = new ArrayList<>();

        @PreDestroy
        @Override
        void preDestroy() {
            invocations.add(MyBean.class.getSimpleName());
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface MyInterceptorBinding {
    }

    static class MyInterceptorSuperclass {
        @PreDestroy
        void superPreDestroy(InvocationContext ctx) throws Exception {
            MyBean.invocations.add(MyInterceptorSuperclass.class.getSimpleName());
            ctx.proceed();
        }
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    public static class MyInterceptor extends MyInterceptorSuperclass {
        @PreDestroy
        Object preDestroy(InvocationContext ctx) throws Exception {
            MyBean.invocations.add(MyInterceptor.class.getSimpleName());
            return ctx.proceed();
        }
    }
}

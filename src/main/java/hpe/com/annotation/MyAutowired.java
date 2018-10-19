package hpe.com.annotation;

import java.lang.annotation.*;

/*
作用域方法上
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    String value() default "";
}

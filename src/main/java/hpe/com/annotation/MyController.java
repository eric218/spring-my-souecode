package hpe.com.annotation;

import java.lang.annotation.*;

//作用域class
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {
    String value() default "";
}